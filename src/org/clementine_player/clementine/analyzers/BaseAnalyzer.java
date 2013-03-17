package org.clementine_player.clementine.analyzers;

import org.clementine_player.clementine.playback.PlaybackService;

import android.graphics.Canvas;
import android.media.audiofx.Visualizer;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

public abstract class BaseAnalyzer
    implements SurfaceHolder.Callback,
               PlaybackService.VisualizerListener {
  private static final String TAG = "BaseAnalyzer";
  
  private Thread thread_;
  private boolean active_;
  private boolean stop_;
  
  private SurfaceHolder holder_;
  private byte[] data_;
  
  public BaseAnalyzer(SurfaceHolder holder) {
    active_ = false;
    stop_ = false;
    thread_ = new Thread(new Worker());
    thread_.start();
    
    holder_ = holder;
    holder_.addCallback(this);
    
    data_ = new byte[0];
    
    // If the surface is already created, start right away.
    if (holder_.getSurface() != null) {
      surfaceCreated(holder);
    }
  }
  
  class Worker implements Runnable {
    @Override
    public void run() {
      while (true) {
        // Sleep while the analyzer is paused.
        while (!active_ && !stop_) {
          try {
            thread_.wait();
          } catch (InterruptedException e) {
          }
        }
        
        // Stop the thread if we're shutting down.
        if (stop_) {
          return;
        }
        
        long start_time = SystemClock.uptimeMillis();
        
        // Draw on the canvas.
        Canvas canvas = holder_.lockCanvas();
        if (canvas != null) {
          synchronized (data_) {
            Update(data_, canvas);
          }
          holder_.unlockCanvasAndPost(canvas);
        }
        
        // Wait until the next update.
        // TODO(dsansome): wait until data_ gets updated.
        long elapsed_time = SystemClock.uptimeMillis() - start_time;
        if (elapsed_time >= 0 && elapsed_time < 100) {
          try {
            Thread.sleep(100 - elapsed_time);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }
  
  @Override
  public void surfaceChanged(
      SurfaceHolder holder, int format, int width, int height) {
  }
  
  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    // Start the thread updating the surface.
    active_ = true;
    thread_.interrupt();
  }
  
  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    // Pause the thread.
    active_ = false;
  }
  
  public void Release() {
    stop_ = true;
    holder_.removeCallback(this);
    thread_.interrupt();
    try {
      thread_.join();
    } catch (InterruptedException e) {
    }
  }
  
  @Override
  public void UpdateFft(byte[] data) {
    synchronized (data_) {
      data_ = data;
    }
  }
  
  protected abstract void Update(byte[] fft, Canvas canvas);
}
