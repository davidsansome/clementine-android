package org.clementine_player.clementine.playlist_parsers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.clementine_player.clementine.PB.Song;

import android.util.Log;

public abstract class BaseParser {
  private static final String TAG = "BaseParser";
  
  public abstract String name();
  public abstract String[] file_extensions();
  public abstract String mime_type();
  
  public abstract List<Song> Load(
      BufferedInputStream stream, String path, String directory);
  
  public abstract boolean TryMagic(String data);
  
  protected void LoadSong(
      String filename_or_uri, String directory, Song.Builder builder) {
    if (filename_or_uri == null || filename_or_uri.isEmpty()) {
      return;
    }
    
    Log.d(TAG, "Loading song from " + filename_or_uri);
    
    if (filename_or_uri.matches("^[a-z]{2,}:.*")) {
      try {
        URI uri = new URI(filename_or_uri);
        if (uri.getScheme() != "file") {
          builder.setUri(filename_or_uri);
          builder.setType(Song.Type.STREAM);
          builder.setValid(true);
          return;
        }
      } catch (URISyntaxException e) {
        // Not a valid URI.
      }
    }
    
    throw new RuntimeException("Local files are not supported yet");
  }
  
  protected Song.Builder LoadSong(String filename_or_uri, String directory) {
    Song.Builder builder = Song.newBuilder();
    LoadSong(filename_or_uri, directory, builder);
    return builder;
  }
}
