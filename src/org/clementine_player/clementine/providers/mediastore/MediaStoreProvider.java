package org.clementine_player.clementine.providers.mediastore;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.PB.BrowserItem.Builder;
import org.clementine_player.clementine.R;
import org.clementine_player.clementine.providers.NonCachingItemLoader;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.Loader;

public class MediaStoreProvider implements ProviderInterface {
  @Override
  public PB.BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("Library");
    ret.getImageBuilder().setResource(R.drawable.library);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }

  @Override
  public String provider_key() {
    return "library";
  }

  @Override
  public String[] uri_schemes() {
    return new String[0];
  }

  @Override
  public Loader<PB.BrowserItemList> LoadItems(
      final Context context, final String parent_key) {
    return new NonCachingItemLoader(context) {
      @Override
      public PB.BrowserItemList loadInBackground() {
        if (parent_key == null) {
          return LoadArtists(context);
        } else {
          return LoadSongs(context, parent_key);
        }
      }
    };
  }
  
  private PB.BrowserItemList LoadArtists(Context context) {
    Cursor cursor = context.getContentResolver().query(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        new String[] {
            MediaStore.Audio.AlbumColumns.ARTIST,
            MediaStore.Audio.AlbumColumns.ALBUM,
            MediaStore.Audio.AlbumColumns.ALBUM_ART,
            MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
            MediaStore.Audio.AlbumColumns.ALBUM_KEY,
        },
        null,
        null,
        MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
    );
    
    PB.BrowserItemList.Builder ret = PB.BrowserItemList.newBuilder();
    
    while (cursor.moveToNext()) {
      Builder builder = ret.addItemsBuilder();
      builder.setText1(cursor.getString(0));
      builder.setText2(cursor.getString(1));
      builder.setKey(cursor.getString(4));
      builder.setHasChildren(true);
    }
    
    return ret.build();
  }
  
  private PB.BrowserItemList LoadSongs(Context context, String key) {
    Cursor cursor = context.getContentResolver().query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        new String[] {
            MediaStore.MediaColumns.TITLE,
            MediaStore.Audio.AlbumColumns.ALBUM,
            MediaStore.MediaColumns.DATA,
            MediaStore.Audio.ArtistColumns.ARTIST,
        },
        MediaStore.Audio.AlbumColumns.ALBUM_KEY + " = ?",
        new String[]{key},
        MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    );
    
    PB.BrowserItemList.Builder ret = PB.BrowserItemList.newBuilder();
    
    while (cursor.moveToNext()) {
      Builder builder = ret.addItemsBuilder();
      builder.setText1(cursor.getString(0));
      builder.setText2(cursor.getString(1));
      builder.setMediaUri(
          Uri.fromFile(new File(cursor.getString(2))).toString());
      PB.Song.Builder metadata = builder.getMetadataBuilder();
      metadata.setArtist(cursor.getString(3));
    }
    
    return ret.build();
  }

  @Override
  public Loader<PB.SongList> LoadSongs(
      Context context, final PB.BrowserItemList items) {
    return new Loader<PB.SongList>(context) {
      @Override
      protected void onStartLoading() {
        PB.SongList.Builder ret = PB.SongList.newBuilder();
        for (PB.BrowserItem item: items.getItemsList()) {
          PB.Song.Builder song = ret.addSongsBuilder();
          song.setArtist(item.getMetadata().getArtist());
          song.setTitle(item.getText1());
          song.setAlbum(item.getText2());
          song.setUri(item.getMediaUri());
        }
        deliverResult(ret.build());
      }
    };
  }

  @Override
  public Loader<URL> ResolveURI(Context context, URI url) {
    return null;
  }

}
