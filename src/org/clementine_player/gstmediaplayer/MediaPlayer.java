package org.clementine_player.gstmediaplayer;

import org.clementine_player.clementine.Application;

import com.gstreamer.GStreamer;

public class MediaPlayer {
  private static native long CreateNativeInstance(String url);
  private native void DestroyNativeInstance();
  
  public native void Start();
  public native void Pause();
  public native void SetVolume(float volume);
  
  static {
    System.loadLibrary("gstreamer_android");
    System.loadLibrary("gstmediaplayer");
    
    try {
      GStreamer.init(Application.instance().getApplicationContext());
    } catch (Exception e) {
      // WTF java
      e.printStackTrace();
    }
  }
  
  // A pointer to the native instance.
  private long handle_ = 0;
  
  public MediaPlayer(String url) {
    handle_ = CreateNativeInstance(url);
  }
  
  public void Release() {
    if (handle_ != 0) {
      DestroyNativeInstance();
      handle_ = 0;
    }
  }
  
  @Override
  public void finalize() {
    Release();
  }
}
