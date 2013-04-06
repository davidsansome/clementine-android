package org.clementine_player.clementine.playback;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.providers.ProviderInterface;
import org.clementine_player.gstmediaplayer.MediaPlayer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;

import com.google.common.collect.Lists;

public class PlaybackService
    extends Service
    implements MediaPlayer.StateListener,
               MediaPlayer.AnalyzerListener {
  public class PlaybackBinder extends Binder {
    public PlaybackService GetService() {
      return PlaybackService.this;
    }
  }
  
  private static final String TAG = "PlaybackService";
  
  private List<MediaPlayer.StateListener> stream_listeners_;
  private List<MediaPlayer.AnalyzerListener> analyzer_listeners_;
  private Stream current_stream_;
  
  // TODO(dsansome): make these configurable.
  public static final long kFadeDurationMsec = 2000L;
  
  @Override
  public void onCreate() {
    stream_listeners_ = Lists.newArrayList();
    analyzer_listeners_ = Lists.newArrayList();
    
    startService(new Intent(this, getClass()));
  }
  
  @Override
  public void onDestroy() {
    Stop();
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
  
  public void StartNewSongFromUri(String uri_string) {
    URI uri;
    try {
      uri = new URI(uri_string);
    } catch (URISyntaxException e) {
      StreamStateChanged(MediaPlayer.State.ERROR, "Invalid URI: " + uri_string);
      return;
    }
    
    // Check whether this URI scheme belongs to a provider.
    ProviderInterface provider =
        Application.instance().provider_manager().ProviderByURIScheme(uri);
    if (provider == null) {
      // This must be a http:// or file:// URI, in which case we can load it
      // directly.
      StartNewSongRaw(uri_string);
      return;
    }
    
    Loader<URL> loader = provider.ResolveURI(this, uri);
    loader.registerListener(0, new OnLoadCompleteListener<URL>() {
      @Override
      public void onLoadComplete(Loader<URL> loader, URL url) {
        if (url != null) {
          StartNewSongRaw(url.toString());
        }
      }
    });
    loader.startLoading();
  }
  
  public void StartNewSongRaw(String url) {
    SwapStream(new Stream(url, this));
    current_stream_.FadeIn(kFadeDurationMsec);
    current_stream_.Play();
  }
  
  private void SwapStream(Stream new_stream) {
    // Stop the current stream.
    if (current_stream_ != null) {
      current_stream_.RemoveListener(this);
      current_stream_.FadeOutAndRelease(kFadeDurationMsec);
    }
    
    // Set the new stream.
    current_stream_ = new_stream;
    
    if (current_stream_ != null) {
      current_stream_.AddListener(this);
      if (!analyzer_listeners_.isEmpty()) {
        current_stream_.SetAnalyzerEnabled(true);
      }
    }
  }
  
  public void AddStreamListener(MediaPlayer.StateListener listener) {
    stream_listeners_.add(listener);
    if (current_stream_ != null) {
      listener.StreamStateChanged(current_stream_.current_state(), null);
    }
  }
  
  public void RemoveStreamListener(MediaPlayer.StateListener listener) {
    stream_listeners_.remove(listener);
  }

  @Override
  public void StreamStateChanged(MediaPlayer.State state, String message) {
    for (MediaPlayer.StateListener listener : stream_listeners_) {
      listener.StreamStateChanged(state, message);
    }
  }
  
  public void AddAnalyzerListener(MediaPlayer.AnalyzerListener listener) {
    analyzer_listeners_.add(listener);

    if (current_stream_ != null) {
      current_stream_.SetAnalyzerEnabled(true);
    }
  }
  
  public void RemoveAnalyzerListener(MediaPlayer.AnalyzerListener listener) {
    analyzer_listeners_.remove(listener);

    if (current_stream_ != null && analyzer_listeners_.isEmpty()) {
      current_stream_.SetAnalyzerEnabled(false);
    }
  }
  
  @Override
  public void UpdateFft(float[] data) {
    for (MediaPlayer.AnalyzerListener listener : analyzer_listeners_) {
      listener.UpdateFft(data);
    }
  }
  
  public MediaPlayer.State current_state() {
    if (current_stream_ == null) {
      return MediaPlayer.State.COMPLETED;
    }
    
    return current_stream_.current_state();
  }
}
