package org.clementine_player.clementine.providers;

import java.net.URI;
import java.net.URL;

import org.clementine_player.clementine.PB;

import android.content.Context;
import android.support.v4.content.Loader;

public abstract interface ProviderInterface {
  public abstract String provider_key();
  public abstract String[] uri_schemes();
  
  // The item to show in the top-level view.  The item's key and has_children
  // fields are ignored.
  public abstract PB.BrowserItem provider_item();
  
  // Load items to display in the music browser view.  If parent_key is null
  // then load the list of top-level items, otherwise load children of the item
  // with the given key.
  public abstract Loader<PB.BrowserItemList> LoadItems(
      Context context, String parent_key);
  
  // Load full metadata for the given ListItems.
  public abstract Loader<PB.SongList> LoadSongs(
      Context context, PB.BrowserItemList items);
  
  // Resolve a media URI to an actual URL.
  public abstract Loader<URL> ResolveURI(Context context, URI url);
}
