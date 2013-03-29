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

MediaPlayer::MediaPlayer(const char* url)
    : url_(url),
      context_(NULL),
      main_loop_(NULL),
      pipeline_(NULL) {
  SetState(PREPARING);

  // Create a context now so it's accessible as soon as the ctor returns.
  context_ = g_main_context_new();

  // Start a new thread for the gobject mainloop.
  pthread_create(&thread_, NULL, &MediaPlayer::ThreadMainCallback, this);
}

MediaPlayer::~MediaPlayer() {
  g_main_context_unref(context_);
}

void* MediaPlayer::ThreadMainCallback(void* self) {
  reinterpret_cast<MediaPlayer*>(self)->ThreadMain();
  return NULL;
}

void MediaPlayer::ErrorCallback(GstBus* bus, GstMessage* msg, void* self) {
  reinterpret_cast<MediaPlayer*>(self)->Error(bus, msg);
}

void MediaPlayer::StateChangedCallback(GstBus* bus, GstMessage* msg, void* self) {
  reinterpret_cast<MediaPlayer*>(self)->StateChanged(bus, msg);
}

void MediaPlayer::ThreadMain(void) {
  GError* error = NULL;

  // Set the main context as the default for this thread.
  g_main_context_push_thread_default(context_);

  // Build the pipeline.
  pipeline_ = gst_parse_launch("playbin2", &error);
  if (error) {
    gchar* message = g_strdup_printf(
        "Unable to build pipeline: %s", error->message);
    g_clear_error(&error);
    // TODO(dsansome): pass error message somewhere.
    LOG(ERROR, "%s", message);
    g_free(message);

    SetState(ERROR);
    return;
  }

  // Set the media URI.
  g_object_set(pipeline_, "uri", url_.c_str(), NULL);

  // Connect to interesting signals on the bus.
  GstBus* bus = gst_element_get_bus(pipeline_);
  g_signal_connect(
      G_OBJECT(bus),
      "message::error",
      reinterpret_cast<GCallback>(&MediaPlayer::ErrorCallback),
      this);
  g_signal_connect(
      G_OBJECT(bus),
      "message::state-changed",
      reinterpret_cast<GCallback>(StateChangedCallback),
      this);
  gst_object_unref(bus);

  // Create a mainloop and start running.
  main_loop_ = g_main_loop_new(context_, false);
  g_main_loop_run(main_loop_);
  g_main_loop_unref(main_loop_);
  main_loop_ = NULL;

  // Free resources.
  g_main_context_pop_thread_default(context_);
  gst_element_set_state(pipeline_, GST_STATE_NULL);
  gst_object_unref(pipeline_);
}

void MediaPlayer::Error(GstBus* bus, GstMessage* msg) {
}

void MediaPlayer::StateChanged(GstBus* bus, GstMessage* msg) {
}

void MediaPlayer::SetState(State state) {
}

void MediaPlayer::Start() {
  g_main_context_invoke(context_, &MediaPlayer::IdleStart, this);
}

int MediaPlayer::IdleStart(void* self_vp) {
  MediaPlayer* self = reinterpret_cast<MediaPlayer*>(self_vp);
  gst_element_set_state(self->pipeline_, GST_STATE_PLAYING);

  return 0;
}

void MediaPlayer::Pause() {
  g_main_context_invoke(context_, &MediaPlayer::IdlePause, this);
}

int MediaPlayer::IdlePause(void* self_vp) {
  MediaPlayer* self = reinterpret_cast<MediaPlayer*>(self_vp);
  gst_element_set_state(self->pipeline_, GST_STATE_PAUSED);

  return 0;
}

void MediaPlayer::SetVolume(float volume) {
}
