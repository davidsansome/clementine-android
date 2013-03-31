package org.clementine_player.clementine.playback;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.providers.ProviderInterface;
import org.clementine_player.gstmediaplayer.MediaPlayer;

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
    implements MediaPlayer.StateListener,
               Visualizer.OnDataCaptureListener {
  public class PlaybackBinder extends Binder {
    public PlaybackService GetService() {
      return PlaybackService.this;
    }
  }
  
  public interface VisualizerListener {
    void UpdateFft(int[] amplitudes);
  }

  private static final String TAG = "PlaybackService";
  
  private List<MediaPlayer.StateListener> stream_listeners_;
  private List<VisualizerListener> visualizer_listeners_;
  private Visualizer current_visualizer_;
  private Stream current_stream_;
  
  // TODO(dsansome): make these configurable.
  public static final long kFadeDurationMsec = 2000L;
  public static final int kVisualizerCaptureSize = 256;
  public static final int kVisualizerUpdateIntervalHz = 20;  // 20
  
  @Override
  public void onCreate() {
    stream_listeners_ = new ArrayList<MediaPlayer.StateListener>();
    visualizer_listeners_ = new ArrayList<VisualizerListener>();
  }
  
  public void Stop() {
    SwapStream(null);
    StreamStateChanged(MediaPlayer.State.COMPLETED, null);
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
    current_stream_.FadeIn(kFadeDurationMsec);
    current_stream_.Play();
  }
  
  private void SwapStream(Stream new_stream) {
    // Stop the current stream.
    if (current_stream_ != null) {
      current_stream_.RemoveListener(this);
      current_stream_.FadeOutAndRelease(kFadeDurationMsec);
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
  
  public void AddStreamListener(MediaPlayer.StateListener listener) {
    stream_listeners_.add(listener);
  }
  
  public void RemoveStreamListener(MediaPlayer.StateListener listener) {
    stream_listeners_.remove(listener);
  }

  @Override
  public void StreamStateChanged(MediaPlayer.State state, String message) {
    for (MediaPlayer.StateListener listener : stream_listeners_) {
      listener.StreamStateChanged(state, message);
    }
    
    if (state == MediaPlayer.State.PLAYING && current_visualizer_ == null &&
        !visualizer_listeners_.isEmpty()) {
      CreateVisualizer();
    }
  }
  
  private void CreateVisualizer() {
    current_visualizer_ = current_stream_.CreateVisualizer();
    if (current_visualizer_ == null) {
      return;
    }
    
    current_visualizer_.setCaptureSize(kVisualizerCaptureSize);
    current_visualizer_.setDataCaptureListener(
        this, kVisualizerUpdateIntervalHz * 1000, false, true);
    current_visualizer_.setEnabled(true);
  }
  
  public void AddVisualizerListener(VisualizerListener listener) {
    visualizer_listeners_.add(listener);
    
    if (current_visualizer_ == null &&
        current_stream_ != null &&
        current_stream_.current_state() != MediaPlayer.State.PREPARING) {
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
    // The first two bytes in the array are the real components of the first
    // and last frequency buckets (which we discard), followed by real,
    // imaginary pairs for the rest of the frequency buckets.
    if (fft.length <= 2) {
      return;
    }
    
    int[] amplitudes = new int[fft.length / 2 - 1];
    for (int i=0 ; i<amplitudes.length ; ++i) {
      int fft_index = 2 + i * 2;
      byte real = fft[fft_index];
      byte imaginary = fft[fft_index + 1];
      
      amplitudes[i] =
          (int) Math.sqrt(Math.pow(real, 2.0) + Math.pow(imaginary, 2.0));
    }
    
    for (VisualizerListener listener : visualizer_listeners_) {
      listener.UpdateFft(amplitudes);
    }
  }

  @Override
  public void onWaveFormDataCapture(
      Visualizer visualizer, byte[] waveform, int samplingRate) {
  }
}
