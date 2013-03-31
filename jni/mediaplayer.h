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

#ifndef MEDIAPLAYER_H
#define MEDIAPLAYER_H

#include <gst/gst.h>
#include <jni.h>
#include <pthread.h>
#include <string>

using std::string;

class MediaPlayer {
 public:
  MediaPlayer(JavaVM* vm,
              JNIEnv* env,
              jobject object,
              jmethodID state_changed_callback,
              jmethodID fade_finished_callback,
              const char* url);

  void Release(JNIEnv* env);

  // Can be called from any thread.
  void Start();
  void Pause();
  void SetVolume(float volume);
  void FadeVolumeTo(float volume, int64_t duration_ms);

  // Must be kept in sync with MediaPlayer.java
  enum State {
    PREPARING = 0,
    PAUSED = 1,
    PLAYING = 2,
    COMPLETED = 3,
    ERROR = 4,
  };

 private:
  static const int kFadeVolumeIntervalMs;

  // Our thread's main function.
  static void* ThreadMainCallback(void* self);
  void ThreadMain();

  // Invoked on gstreamer threads.
  static void ErrorCallback(GstBus* bus, GstMessage* msg, void* self);
  static void StateChangedCallback(GstBus* bus, GstMessage* msg, void* self);
  void Error(GstMessage* msg);
  void StateChanged(GstMessage* msg);

  // Invoked on our thread.
  static int IdleStartCallback(void* self);
  static int IdlePauseCallback(void* self);
  static int IdleExitCallback(void* self);

  struct SetVolumeArgs {
    MediaPlayer* self;
    float volume;
    int64_t duration_ms;
  };
  static int IdleSetVolume(void* args);
  static int IdleFadeVolumeTo(void* args);

  static int FadeVolumeTimeout(void* self);

  // Can be invoked from any thread attached to the JVM.
  void SetState(State state, const char* message = NULL);

 private:
  string url_;
  pthread_t thread_;

  GMainContext* context_;
  GMainLoop* main_loop_;
  GstElement* pipeline_;

  JavaVM* vm_;
  JNIEnv* env_;
  jobject object_;
  jmethodID state_changed_callback_;
  jmethodID fade_finished_callback_;

  float current_volume_;
  float target_volume_;
  float fade_volume_step_;
  u_int32_t fade_volume_timeout_id_;
};

#endif // MEDIAPLAYER_H
