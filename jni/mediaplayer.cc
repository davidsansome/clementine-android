/* This file is part of Clementine.
   Copyright 2012, David Sansome <me@davidsansome.com>
   
   Clementine is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   Clementine is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with Clementine.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <limits>

#include "fht.h"
#include "mediaplayer.h"
#include "logging.h"
#include "scoped_ptr.h"

const int MediaPlayer::kFadeVolumeIntervalMs = 100;


MediaPlayer::MediaPlayer(JavaVM* vm,
                         JNIEnv* env,
                         jobject object,
                         jfieldID analyzer_buffer_field,
                         jmethodID state_changed_callback,
                         jmethodID fade_finished_callback,
                         jmethodID analyzer_callback,
                         const char* url)
    : url_(url),
      context_(NULL),
      main_loop_(NULL),
      pipeline_(NULL),
      vm_(vm),
      env_(NULL),
      object_(env->NewGlobalRef(object)),
      state_changed_callback_(state_changed_callback),
      fade_finished_callback_(fade_finished_callback),
      analyzer_callback_(analyzer_callback),
      current_volume_(0.0),
      target_volume_(0.0),
      fade_volume_step_(0.0),
      fade_volume_timeout_id_(0),
      fht_(8),
      analyzer_buffer_(new float[fht_.size()]) {
  // Create a native byte buffer for the analyzer data.
  env->SetObjectField(
      object,
      analyzer_buffer_field,
      env->NewDirectByteBuffer(analyzer_buffer_, fht_.size() * sizeof(float)));

  // Create a context now so it's accessible as soon as the ctor returns.
  context_ = g_main_context_new();

  SetState(PREPARING);

  // Start a new thread for the gobject mainloop.
  pthread_create(&thread_, NULL, &MediaPlayer::ThreadMainCallback, this);
}

void MediaPlayer::Release(JNIEnv* env) {
  // Exit the main loop and wait for the thread to exit.
  g_main_context_invoke(context_, &MediaPlayer::IdleExitCallback, this);
  pthread_join(thread_, NULL);

  // Unref the main context.
  g_main_context_unref(context_);

  // Release the reference to the java object.
  env->DeleteGlobalRef(object_);

  delete[] analyzer_buffer_;
}

void* MediaPlayer::ThreadMainCallback(void* self) {
  reinterpret_cast<MediaPlayer*>(self)->ThreadMain();
  return NULL;
}

void MediaPlayer::ErrorCallback(
    GstBus* bus, GstMessage* msg, MediaPlayer* self) {
  GError* err = NULL;
  gchar* debug_info = NULL;
  gst_message_parse_error(msg, &err, &debug_info);

  LOG(ERROR, "%s: %s", GST_OBJECT_NAME (msg->src), err->message);

  g_clear_error(&err);
  g_free(debug_info);
  gst_element_set_state(self->pipeline_, GST_STATE_NULL);
}

void MediaPlayer::StateChangedCallback(
    GstBus* bus, GstMessage* msg, MediaPlayer* self) {
  GstState old_state;
  GstState new_state;
  GstState pending_state;
  gst_message_parse_state_changed(msg, &old_state, &new_state, &pending_state);

  // Only pay attention to messages coming from the pipeline, not its children.
  if (GST_MESSAGE_SRC(msg) == GST_OBJECT(self->pipeline_)) {
    LOG(DEBUG, "Pipeline state %s -> %s",
        gst_element_state_get_name(old_state),
        gst_element_state_get_name(new_state));

    State my_state = PREPARING;
    switch (new_state) {
      case GST_STATE_READY:
      case GST_STATE_VOID_PENDING: my_state = PREPARING; break;
      case GST_STATE_NULL:         my_state = COMPLETED; break;
      case GST_STATE_PAUSED:       my_state = PAUSED; break;
      case GST_STATE_PLAYING:      my_state = PLAYING; break;
    }

    self->SetState(my_state);
  }
}

void MediaPlayer::SourceSetupCallback(
    GstPipeline* pipeline, GstElement* source, MediaPlayer* self) {
  GstElement* uridecodebin = reinterpret_cast<GstElement*>(
      pipeline->bin.children->data);

  g_signal_connect(
      G_OBJECT(uridecodebin),
      "pad-added",
      reinterpret_cast<GCallback>(&MediaPlayer::PadAddedCallback),
      self);
}

void MediaPlayer::PadAddedCallback(
    GstElement* decodebin, GstPad* pad, MediaPlayer* self) {
  if (gst_pad_get_direction(pad) == GST_PAD_SRC) {
    gst_pad_add_buffer_probe(
        pad,
        reinterpret_cast<GCallback>(&MediaPlayer::BufferCallback),
        self);
  }
}

namespace {

template <typename T>
void CopyBufferData(
    const GstBuffer* buffer, int samples, int channels, float* out) {
  T* in_p = reinterpret_cast<T*>(buffer->data);
  float* out_p = out;

  const float divisor = float(channels) * std::numeric_limits<T>::max();

  for (int i=0 ; i<samples ; ++i) {
    float value = 0;
    for (int j=0 ; j<channels ; ++j) {
      value += *(in_p++);
    }
    *(out_p++) = value / divisor;
  }
}

}

bool MediaPlayer::BufferCallback(
    GstPad* pad, GstBuffer* buffer, MediaPlayer* self) {
  if (!buffer || !buffer->caps || !buffer->caps->structs ||
      buffer->caps->structs->len < 1) {
    return false;
  }

  const int size_bytes = buffer->size;
  const GstStructure* structure = reinterpret_cast<GstStructure*>(
      buffer->caps->structs->pdata[0]);

  int width_bits = 0;
  int depth_bits = 0;
  int channels = 0;
  if (!gst_structure_get_int(structure, "width", &width_bits) ||
      !gst_structure_get_int(structure, "depth", &depth_bits) ||
      !gst_structure_get_int(structure, "channels", &channels)) {
    char* structure_string = gst_structure_to_string(structure);
    LOG(ERROR,
        "Failed to get width, depth or channel information from caps "
        "structure: %s",
        structure_string);
    free(structure_string);
    return false;
  }

  const int samples = size_bytes / (channels * (width_bits / 8));
  if (samples < self->fht_.size()) {
    LOG(ERROR, "Buffer (%d) too small for FHT (%d)", samples, self->fht_.size());
    return false;
  }

  float* data = self->analyzer_buffer_;
  const int data_size = self->fht_.size();

  switch (width_bits) {
    case 8:  CopyBufferData<u_int8_t>(buffer, data_size, channels, data); break;
    case 16: CopyBufferData<u_int16_t>(buffer, data_size, channels, data); break;
    case 32: CopyBufferData<u_int32_t>(buffer, data_size, channels, data); break;
    default:
      LOG(ERROR, "Buffer width %d not supported", width_bits);
      return false;
  }

  self->fht_.spectrum(data);
  self->fht_.scale(data, 0.05);

  // TODO(dsansome): put this in a thread local.
  JNIEnv* env = NULL;
  if (self->vm_->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    LOG(WARN, "MediaPlayer::BufferCallback - thread is not attached to the JVM");
    return false;
  }

  env->CallVoidMethod(self->object_, self->analyzer_callback_);

  return true;
}

void MediaPlayer::ThreadMain(void) {
  GError* error = NULL;

  // Attach this thread to the JVM.
  JavaVMAttachArgs args;
  args.version = JNI_VERSION_1_4;
  args.name = NULL;
  args.group = NULL;

  if (vm_->AttachCurrentThread(&env_, &args) < 0) {
    SetState(ERROR, "Failed to attach thread to JVM");
    return;
  }

  // Set the main context as the default for this thread.
  g_main_context_push_thread_default(context_);

  // Build the pipeline.
  pipeline_ = gst_parse_launch("playbin2", &error);
  if (error) {
    gchar* message = g_strdup_printf(
        "Unable to build pipeline: %s", error->message);
    g_clear_error(&error);
    SetState(ERROR, message);
    g_free(message);

    vm_->DetachCurrentThread();
    return;
  }

  // Set the pipeline properties.
  g_object_set(pipeline_,
               "uri", url_.c_str(),
               "volume", gdouble(0.0),
               NULL);

  // Connect to interesting signals on the bus.
  GstBus* bus = gst_element_get_bus(pipeline_);
  GSource* bus_source = gst_bus_create_watch(bus);
  g_source_set_callback(
      bus_source,
      reinterpret_cast<GSourceFunc>(gst_bus_async_signal_func),
      NULL,
      NULL);
  g_source_attach(bus_source, context_);
  g_source_unref(bus_source);
  g_signal_connect(
      G_OBJECT(bus),
      "message::error",
      reinterpret_cast<GCallback>(&MediaPlayer::ErrorCallback),
      this);
  g_signal_connect(
      G_OBJECT(bus),
      "message::state-changed",
      reinterpret_cast<GCallback>(&MediaPlayer::StateChangedCallback),
      this);
  gst_object_unref(bus);

  g_signal_connect(
      G_OBJECT(pipeline_),
      "source-setup",
      reinterpret_cast<GCallback>(&MediaPlayer::SourceSetupCallback),
      this);

  // Preroll and pause the pipeline.
  gst_element_set_state(pipeline_, GST_STATE_PAUSED);

  // Create a mainloop and start running.
  main_loop_ = g_main_loop_new(context_, false);
  g_main_loop_run(main_loop_);
  g_main_loop_unref(main_loop_);
  main_loop_ = NULL;

  // Free resources.
  g_main_context_pop_thread_default(context_);
  gst_element_set_state(pipeline_, GST_STATE_NULL);
  gst_object_unref(pipeline_);

  vm_->DetachCurrentThread();
}

void MediaPlayer::SetState(State state, const char* message) {
  JNIEnv* env = NULL;
  if (vm_->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    LOG(WARN, "MediaPlayer::SetState - thread is not attached to the JVM");
    return;
  }

  jint jstate = state;
  jstring jmessage = NULL;
  if (message) {
    jmessage = env->NewStringUTF(message);
  }

  env->CallVoidMethod(object_, state_changed_callback_, jstate, jmessage);

  if (jmessage) {
    env->DeleteLocalRef(jmessage);
  }
}

void MediaPlayer::Start() {
  g_main_context_invoke(context_, &MediaPlayer::IdleStartCallback, this);
}

int MediaPlayer::IdleStartCallback(void* self) {
  gst_element_set_state(reinterpret_cast<MediaPlayer*>(self)->pipeline_,
                        GST_STATE_PLAYING);
  return 0;
}

void MediaPlayer::Pause() {
  g_main_context_invoke(context_, &MediaPlayer::IdlePauseCallback, this);
}

int MediaPlayer::IdlePauseCallback(void* self) {
  gst_element_set_state(reinterpret_cast<MediaPlayer*>(self)->pipeline_,
                        GST_STATE_PAUSED);
  return 0;
}

int MediaPlayer::IdleExitCallback(void* self) {
  g_main_loop_quit(reinterpret_cast<MediaPlayer*>(self)->main_loop_);
  return 0;
}

void MediaPlayer::SetVolume(float volume) {
  SetVolumeArgs* args = new SetVolumeArgs;
  args->self = this;
  args->volume = volume;
  args->duration_ms = 0;

  LOG(INFO, "Setting volume to %f", volume);
  g_main_context_invoke(context_, &MediaPlayer::IdleSetVolume, args);
}

void MediaPlayer::FadeVolumeTo(float volume, int64_t duration_ms) {
  SetVolumeArgs* args = new SetVolumeArgs;
  args->self = this;
  args->volume = volume;
  args->duration_ms = duration_ms;

  g_main_context_invoke(context_, &MediaPlayer::IdleFadeVolumeTo, args);
}

int MediaPlayer::IdleSetVolume(void* args_vp) {
  scoped_ptr<SetVolumeArgs> args(reinterpret_cast<SetVolumeArgs*>(args_vp));

  // Cancel a fade if there's one ongoing.
  if (args->self->fade_volume_timeout_id_) {
    g_source_remove(args->self->fade_volume_timeout_id_);
    args->self->fade_volume_timeout_id_ = 0;
  }

  args->self->current_volume_ = args->volume;
  g_object_set(args->self->pipeline_,
               "volume", gdouble(args->self->current_volume_),
               NULL);

  return 0;
}

int MediaPlayer::IdleFadeVolumeTo(void* args_vp) {
  scoped_ptr<SetVolumeArgs> args(reinterpret_cast<SetVolumeArgs*>(args_vp));

  // Cancel a fade if there's one ongoing.
  if (args->self->fade_volume_timeout_id_) {
    g_source_remove(args->self->fade_volume_timeout_id_);
    args->self->fade_volume_timeout_id_ = 0;
    LOG(INFO, "Aborting existing fade");
  }

  // Don't start fading if we're already at the target volume.
  if (args->self->target_volume_ == args->volume) {
    return 0;
  }

  args->self->target_volume_ = args->volume;
  args->self->fade_volume_step_ =
      (args->volume - args->self->current_volume_) /
      (args->duration_ms / kFadeVolumeIntervalMs);

  LOG(INFO, "Fading volume to %f over %dms step %f",
      args->volume, args->duration_ms, args->self->fade_volume_step_);

  GSource* source = g_timeout_source_new(kFadeVolumeIntervalMs);
  g_source_set_callback(source, &MediaPlayer::FadeVolumeTimeout, args->self, NULL);
  args->self->fade_volume_timeout_id_ =
      g_source_attach(source, args->self->context_);

  return 0;
}

int MediaPlayer::FadeVolumeTimeout(void* self_vp) {
  MediaPlayer* self = reinterpret_cast<MediaPlayer*>(self_vp);

  int ret = 1;
  float volume = self->current_volume_ + self->fade_volume_step_;

  LOG(DEBUG, "Fade changing volume to %f", volume);

  // Have we finished fading?
  if ((self->fade_volume_step_ > 0 && volume >= self->target_volume_) ||
      (self->fade_volume_step_ < 0 && volume <= self->target_volume_)) {
    volume = self->target_volume_;
    ret = 0;
    self->fade_volume_timeout_id_ = 0;

    LOG(INFO, "Fade finished");
    self->env_->CallVoidMethod(self->object_, self->fade_finished_callback_);
  }

  self->current_volume_ = volume;
  g_object_set(self->pipeline_, "volume", gdouble(self->current_volume_), NULL);

  return ret;
}
