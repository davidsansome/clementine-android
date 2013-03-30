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

#include "mediaplayer.h"
#include "logging.h"

MediaPlayer::MediaPlayer(JavaVM* vm,
                         JNIEnv* env,
                         jobject object,
                         jmethodID state_changed_callback,
                         const char* url)
    : url_(url),
      context_(NULL),
      main_loop_(NULL),
      pipeline_(NULL),
      vm_(vm),
      env_(NULL),
      object_(env->NewGlobalRef(object)),
      state_changed_callback_(state_changed_callback) {
  SetState(PREPARING);

  // Create a context now so it's accessible as soon as the ctor returns.
  context_ = g_main_context_new();

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
}

void* MediaPlayer::ThreadMainCallback(void* self) {
  reinterpret_cast<MediaPlayer*>(self)->ThreadMain();
  return NULL;
}

void MediaPlayer::ErrorCallback(GstBus* bus, GstMessage* msg, void* self) {
  reinterpret_cast<MediaPlayer*>(self)->Error(msg);
}

void MediaPlayer::StateChangedCallback(GstBus* bus, GstMessage* msg, void* self) {
  reinterpret_cast<MediaPlayer*>(self)->StateChanged(msg);
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

  // Set the media URI.
  g_object_set(pipeline_, "uri", url_.c_str(), NULL);

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

void MediaPlayer::Error(GstMessage* msg) {
  GError* err = NULL;
  gchar* debug_info = NULL;
  gst_message_parse_error(msg, &err, &debug_info);

  LOG(ERROR, "%s: %s", GST_OBJECT_NAME (msg->src), err->message);

  g_clear_error(&err);
  g_free(debug_info);
  gst_element_set_state(pipeline_, GST_STATE_NULL);
}

void MediaPlayer::StateChanged(GstMessage* msg) {
  GstState old_state;
  GstState new_state;
  GstState pending_state;
  gst_message_parse_state_changed(msg, &old_state, &new_state, &pending_state);

  // Only pay attention to messages coming from the pipeline, not its children.
  if (GST_MESSAGE_SRC(msg) == GST_OBJECT(pipeline_)) {
    State my_state = PREPARING;
    switch (new_state) {
      case GST_STATE_VOID_PENDING: my_state = PREPARING; break;
      case GST_STATE_NULL:         my_state = COMPLETED; break;
      case GST_STATE_READY:
      case GST_STATE_PAUSED:       my_state = PAUSED; break;
      case GST_STATE_PLAYING:      my_state = PLAYING; break;
    }

    SetState(my_state);
  }
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
}
