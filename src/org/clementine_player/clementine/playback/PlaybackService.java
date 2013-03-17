package org.clementine_player.clementine.playback;

import java.net.URI;
import java.net.URL;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;

public class PlaybackService extends Service {
  public class PlaybackBinder extends Binder {
    public void StartNewSong(URI uri) {
      PlaybackService.this.StartNewSong(uri);
    }
  }
  
  private Stream current_stream_;
  private long fade_duration_msec_ = 2000L;  // TODO(dsansome): configurable.
  
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
    if (current_stream_ != null) {
      current_stream_.FadeOutAndRelease(fade_duration_msec_);
    }
    
    current_stream_ = new Stream(url);
    current_stream_.FadeIn(fade_duration_msec_);
    current_stream_.Play();
  }
}
