package org.clementine_player.clementine.providers.di;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.PB.Song;
import org.clementine_player.clementine.Utils;
import org.clementine_player.clementine.providers.CachingItemLoader;
import org.clementine_player.clementine.providers.ProviderException;
import org.clementine_player.clementine.providers.ProviderInterface;
import org.clementine_player.clementine.providers.URLFetcher;

import android.content.Context;
import android.os.OperationCanceledException;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

public abstract class BaseProvider implements ProviderInterface {
  private String hostname_;
  private ApiClient api_client_;
  private CachingItemLoader item_loader_;
  
  private int basic_audio_type_;
  
  private String log_tag_;
  
  private static final String[] BASIC_PLAYLISTS = new String[]{
    "http://listen.%s/public3/%s.pls",
    "http://listen.%s/public1/%s.pls",
    "http://listen.%s/public5/%s.asx",
  };
  
  private static final String[] PREMIUM_PLAYLISTS = new String[]{
    "http://listen.%s/premium_high/%s.pls?hash=%s",
    "http://listen.%s/premium_medium/%s.pls?hash=%s",
    "http://listen.%s/premium/%s.pls?hash=%s",
    "http://listen.%s/premium_wma_low/%s.asx?hash=%s",
    "http://listen.%s/premium_wma/%s.asx?hash=%s",
  };
  
  protected BaseProvider(String service_name,
                         String hostname) {
    hostname_ = hostname;
    api_client_ = new ApiClient(service_name);
    
    basic_audio_type_ = 0;
    
    log_tag_ = "BaseProvider(" + service_name + ")";
  }

  @Override
  public String provider_key() {
    return "di/" + api_client_.service_name();
  }

  @Override
  public Loader<PB.BrowserItemList> LoadItems(
      Context context, String parent_url) {
    if (item_loader_ == null) {
      item_loader_ = new CachingItemLoader(context, provider_key()) {
        @Override
        public PB.BrowserItemList loadInBackground() 
            throws OperationCanceledException {
          try {
            return api_client_.GetChannelList();
          } catch (ProviderException e) {
            throw new OperationCanceledException(Utils.ThrowableToString(e));
          }
        }
      };
    }
    return item_loader_;
  }
  
  @Override
  public Loader<PB.SongList> LoadSongs(
      Context context, final PB.BrowserItemList items) {
    return new Loader<PB.SongList>(context) {
      @Override
      protected void onStartLoading() {
        PB.SongList.Builder ret = PB.SongList.newBuilder();
        
        for (PB.BrowserItem item : items.getItemsList()) {
          Song.Builder builder = ret.addSongsBuilder();
          
          builder.setTitle(item.getText1());
          builder.setArtist(item.getMetadata().getArtist());
          builder.setArt(item.getImage());
          builder.setUri(item.getMediaUri());
        }
        
        deliverResult(ret.build());
      }
    };
  }
  
  @Override
  public String[] uri_schemes() {
    return new String[]{api_client_.service_name()};
  }

  @Override
  public Loader<URL> ResolveURI(Context context, final URI uri) {
    // TODO(dsansome): premium radio.
    
    Log.i(log_tag_, "Resolving " + uri);
    
    return new AsyncTaskLoader<URL>(context) {
      @Override
      protected void onStartLoading() {
        forceLoad();
      }

      @Override
      public URL loadInBackground() {
        URL playlist_url;
        try {
          playlist_url = new URL(String.format(
              BASIC_PLAYLISTS[basic_audio_type_],
              hostname_,
              uri.getHost()));
        
          // Fetch the playlist.
          URLFetcher fetcher = new URLFetcher(playlist_url);
          List<Song> songs =
              Application.instance().playlist_parser_manager().LoadFromStream(
                  fetcher.GetStream(), null, null);
          
          // Take the URL of the first song.
          if (songs.isEmpty()) {
            Log.w(log_tag_, "Playlist contained 0 songs");
            return null;
          }
          Log.i(log_tag_, "Resolved " + uri + " to " + songs.get(0));
          return new URL(songs.get(0).getUri());
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
    };
  }
}
