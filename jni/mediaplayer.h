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
#include <pthread.h>
#include <string>

using std::string;

class MediaPlayer {
 public:
  MediaPlayer(const char* url);
  ~MediaPlayer();

  void Start();
  void Pause();
  void SetVolume(float volume);

  enum State {
    PREPARING = 0,
    PAUSED = 1,
    PLAYING = 2,
    COMPLETED = 3,
    ERROR = 4,
  };

 private:
  // Callbacks.  These just invoke their non-static counterparts.
  static void* ThreadMainCallback(void* self);
  static void ErrorCallback(GstBus* bus, GstMessage* msg, void* self);
  static void StateChangedCallback(GstBus* bus, GstMessage* msg, void* self);

  void ThreadMain();
  void Error(GstBus* bus, GstMessage* msg);
  void StateChanged(GstBus* bus, GstMessage* msg);

  // Idle sources that are invoked on the thread.
  static int IdleStart(void* self);
  static int IdlePause(void* self);

  void SetState(State state);

 private:
  string url_;
  pthread_t thread_;

  GMainContext* context_;
  GMainLoop* main_loop_;
  GstElement* pipeline_;
};

#endif // MEDIAPLAYER_H
