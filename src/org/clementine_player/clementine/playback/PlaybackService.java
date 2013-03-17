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
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;

public class PlaybackService
    extends Service
    implements Stream.Listener {
  public class PlaybackBinder extends Binder {
    public void StartNewSong(URI uri) {
      PlaybackService.this.StartNewSong(uri);
    }
    
    public void PlayPause() {
      PlaybackService.this.PlayPause();
    }
    
    public void Stop() {
      PlaybackService.this.Stop();
    }
    
    public void AddListener(Stream.Listener listener) {
      PlaybackService.this.AddListener(listener);
    }
    
    public void RemoveListener(Stream.Listener listener) {
      PlaybackService.this.RemoveListener(listener);
    }
  }
  
  private List<Stream.Listener> listeners_;
  private Stream current_stream_;
  private long fade_duration_msec_ = 2000L;  // TODO(dsansome): configurable.
  
  @Override
  public void onCreate() {
    listeners_ = new ArrayList<Stream.Listener>();
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
    if (current_stream_ != null) {
      current_stream_.RemoveListener(this);
      current_stream_.FadeOutAndRelease(fade_duration_msec_);
    }
    
    current_stream_ = new_stream;
    
    if (current_stream_ != null) {
      current_stream_.AddListener(this);
    }
  }
  
  public void AddListener(Listener listener) {
    listeners_.add(listener);
  }
  
  public void RemoveListener(Listener listener) {
    listeners_.remove(listener);
  }

  @Override
  public void StreamStateChanged(Stream.State state) {
    for (Stream.Listener listener : listeners_) {
      listener.StreamStateChanged(state);
    }
  }
}
