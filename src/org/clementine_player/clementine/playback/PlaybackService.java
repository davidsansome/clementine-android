package org.clementine_player.clementine.playback;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.playback.Stream.Listener;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.app.Service;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;
import android.util.Log;

public class PlaybackService
    extends Service
    implements Stream.Listener,
               Visualizer.OnDataCaptureListener {
  public class PlaybackBinder extends Binder {
    public PlaybackService GetService() {
      return PlaybackService.this;
    }
  }
  
  public interface VisualizerListener {
    void UpdateFft(byte[] fft);
  }

  private static final String TAG = "PlaybackService";
  
  private List<Stream.Listener> stream_listeners_;
  private List<VisualizerListener> visualizer_listeners_;
  private Visualizer current_visualizer_;
  private Stream current_stream_;
  
  // TODO(dsansome): make these configurable.
  private long fade_duration_msec_ = 2000L;
  private int visualizer_capture_size_ = 128;
  private int visualizer_update_interval_mhz_ = 20 * 1000;  // 20 Hz
  
  @Override
  public void onCreate() {
    stream_listeners_ = new ArrayList<Stream.Listener>();
    visualizer_listeners_ = new ArrayList<VisualizerListener>();
  }
  
  public void Stop() {
    SwapStream(null);
    StreamStateChanged(Stream.State.COMPLETED);
  }

  public void PlayPause() {
    if (current_stream_ != null) {
      current_stream_.PlayPause();
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return new PlaybackBinder();
  }
  
  public void StartNewSong(URI uri) {
    // Check whether this URI scheme belongs to a provider.
    ProviderInterface provider =
        Application.instance().provider_manager().ProviderByURIScheme(uri);
    if (provider == null) {
      // This must be a http:// or file:// URI, in which case we can load it
      // directly.
      StartNewSong(uri.toString());
      return;
    }
    
    Loader<URL> loader = provider.ResolveURI(this, uri);
    loader.registerListener(0, new OnLoadCompleteListener<URL>() {
      @Override
      public void onLoadComplete(Loader<URL> loader, URL url) {
        if (url != null) {
          StartNewSong(url.toString());
        }
      }
    });
    loader.startLoading();
  }
  
  public void StartNewSong(String url) {
    SwapStream(new Stream(url));
    current_stream_.FadeIn(fade_duration_msec_);
    current_stream_.Play();
  }
  
  private void SwapStream(Stream new_stream) {
    // Stop the current stream.
    if (current_stream_ != null) {
      current_stream_.RemoveListener(this);
      current_stream_.FadeOutAndRelease(fade_duration_msec_);
    }
    
    // Remove the current visualizer.
    if (current_visualizer_ != null) {
      current_visualizer_.release();
      current_visualizer_ = null;
    }
    
    // Set the new stream.
    current_stream_ = new_stream;
    
    if (current_stream_ != null) {
      current_stream_.AddListener(this);
    }
  }
  
  public void AddStreamListener(Listener listener) {
    stream_listeners_.add(listener);
  }
  
  public void RemoveStreamListener(Listener listener) {
    stream_listeners_.remove(listener);
  }

  @Override
  public void StreamStateChanged(Stream.State state) {
    for (Stream.Listener listener : stream_listeners_) {
      listener.StreamStateChanged(state);
    }
    
    if (state == Stream.State.STARTED && current_visualizer_ == null &&
        !visualizer_listeners_.isEmpty()) {
      CreateVisualizer();
    }
  }
  
  private void CreateVisualizer() {
    current_visualizer_ = current_stream_.CreateVisualizer();
    current_visualizer_.setCaptureSize(visualizer_capture_size_);
    current_visualizer_.setDataCaptureListener(
        this, visualizer_update_interval_mhz_, false, true);
    current_visualizer_.setEnabled(true);
    Log.d(TAG, Visualizer.getCaptureSizeRange()[0] + ", " + Visualizer.getCaptureSizeRange()[1]);
    Log.d(TAG, "" + Visualizer.getMaxCaptureRate());
  }
  
  public void AddVisualizerListener(VisualizerListener listener) {
    visualizer_listeners_.add(listener);
    
    if (current_visualizer_ == null &&
        current_stream_ != null &&
        current_stream_.current_state() != Stream.State.PREPARING) {
      CreateVisualizer();
    }
  }
  
  public void RemoveVisualizerListener(VisualizerListener listener) {
    visualizer_listeners_.remove(listener);
    
    if (current_visualizer_ != null && visualizer_listeners_.isEmpty()) {
      current_visualizer_.release();
      current_visualizer_ = null;
    }
  }

  @Override
  public void onFftDataCapture(
      Visualizer visualizer, byte[] fft, int sampling_rate) {
    Log.d(TAG, "Got fft " + fft.length + ", " + sampling_rate);
    for (VisualizerListener listener : visualizer_listeners_) {
      listener.UpdateFft(fft);
    }
  }

  @Override
  public void onWaveFormDataCapture(
      Visualizer visualizer, byte[] waveform, int samplingRate) {
  }
}
