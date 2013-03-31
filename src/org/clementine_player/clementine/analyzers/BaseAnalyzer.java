package org.clementine_player.clementine.analyzers;

import org.clementine_player.clementine.playback.PlaybackService;
import org.clementine_player.gstmediaplayer.MediaPlayer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public abstract class BaseAnalyzer
    implements SurfaceHolder.Callback,
               MediaPlayer.AnalyzerListener {
  private static final String TAG = "BaseAnalyzer";
  
  private Thread thread_;
  private boolean active_;
  private boolean stop_;
  
  private Context context_;
  private SurfaceHolder holder_;
  
  private Object data_mutex_;
  private float[] data_;
  private boolean data_updated_;
  
  private int current_width_;
  private int current_height_;
  
  public BaseAnalyzer(Context context, SurfaceHolder holder) {
    active_ = false;
    stop_ = false;
    thread_ = new Thread(new Worker());
    
    context_ = context;
    holder_ = holder;
    holder_.addCallback(this);
    
    data_mutex_ = new Object();
    
    // If the surface is already created, start right away.
    if (holder_.getSurface() != null) {
      surfaceCreated(holder);
    }
    
    thread_.start();
  }
  
  class Worker implements Runnable {
    @Override
    public void run() {
      while (true) {
        // Wait until both data and surface are available.
        synchronized (data_mutex_) {
          while (data_ == null || !data_updated_ || !active_) {
            try {
              data_mutex_.wait();
            } catch (InterruptedException e) {
              break;
            }
          }
        }
        
        // Stop the thread if we're shutting down.
        if (stop_) {
          return;
        }
        
        // Draw on the canvas.
        synchronized (data_mutex_) {
          if (data_ != null && data_updated_) {
            Canvas canvas = holder_.lockCanvas();
            if (canvas != null) {
              Update(data_, canvas);
              data_updated_ = false;
              holder_.unlockCanvasAndPost(canvas);
            }
          }
        }
      }
    }
  }
  
  @Override
  public void surfaceChanged(
      SurfaceHolder holder, int format, int width, int height) {
    if (current_height_ != height || current_width_ != width) {
      current_height_ = height;
      current_width_ = width;
      
      SizeChanged(width, height);
      
      synchronized (data_mutex_) {
        if (data_ != null) {
          Canvas canvas = holder_.lockCanvas();
          if (canvas != null) {
            Update(data_, canvas);
            holder_.unlockCanvasAndPost(canvas);
          }
        }
      }
    }
  }
  
  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    // Start the thread updating the surface.
    active_ = true;
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
  public void UpdateFft(float[] data) {
    synchronized (data_mutex_) {
      data_ = data;
      data_updated_ = true;
      data_mutex_.notify();
    }
  }
  
  protected abstract void SizeChanged(int width, int height);
  protected abstract void Update(float[] fft, Canvas canvas);
  
  protected int update_interval_msec() {
    return 1000 / PlaybackService.kVisualizerUpdateIntervalHz;
  }
  
  protected float screen_density() {
    WindowManager window_manager =
        (WindowManager) context_.getSystemService(Context.WINDOW_SERVICE);
    DisplayMetrics metrics = new DisplayMetrics();
    window_manager.getDefaultDisplay().getMetrics(metrics);
    
    Log.i(TAG, "Screen density is " + metrics.density);
    
    return metrics.density;
  }
}
