package org.clementine_player.gstmediaplayer;

import org.clementine_player.clementine.Application;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import com.gstreamer.GStreamer;

public class MediaPlayer {
  // Must be kept in sync with mediaplayer.h
  public enum State {
    PREPARING,
    PAUSED,
    PLAYING,
    COMPLETED,
    ERROR,
  };
  private static final State[] kStateValues = State.values();
 
  public interface Listener {
    public void StreamStateChanged(State state, String message);
  }
  
  private native long CreateNativeInstance(String url);
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
  
  private static final String TAG = "MediaPlayer";
  
  // A pointer to the native instance.
  private long handle_ = 0;
  
  private Listener listener_;
  
  public MediaPlayer(String url, Listener listener) {
    listener_ = listener;
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
  
  // Called by the native instance.
  private void NativeStateChanged(final int state, final String message) {
    Application.instance().RunOnUiThread(new Runnable() {
      @Override
      public void run() {
        listener_.StreamStateChanged(kStateValues[state], message);
      }
    });
  }
}
