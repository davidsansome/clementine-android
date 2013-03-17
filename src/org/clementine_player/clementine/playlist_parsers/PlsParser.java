package org.clementine_player.clementine.playlist_parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.clementine_player.clementine.PB.Song;
import org.clementine_player.clementine.TimeConstants;

import android.util.SparseArray;

public class PlsParser extends BaseParser {
  private static final String TAG = "PlsParser";
  
  @Override
  public String name() {
    return "PLS";
  }
  
  @Override
  public String[] file_extensions() {
    return new String[]{"pls"};
  }
  
  @Override
  public String mime_type() {
    return null;
  }
  
  @Override
  public List<Song> Load(
      BufferedInputStream stream, String playlist_path, String directory) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    SparseArray<Song.Builder> songs = new SparseArray<Song.Builder>();
    
    Pattern n_re = Pattern.compile("\\d+$");
    
    while (true) {
      String line;
      try {
        line = reader.readLine();
      } catch (IOException e) {
        break;
      }
      if (line == null) {
        break;
      }
      
      line = line.trim();
      
      int equals = line.indexOf('=');
      if (equals == -1) {
        continue;
      }
      
      String key = line.substring(0, equals).toLowerCase(Locale.US);
      String value = line.substring(equals + 1, line.length());
      
      Matcher matcher = n_re.matcher(key);
      if (!matcher.find()) {
        continue;
      }
      int n = Integer.parseInt(matcher.group());
      
      Song.Builder builder = songs.get(n);
      if (builder == null) {
        builder = Song.newBuilder();
        songs.put(n, builder);
      }
      
      if (key.startsWith("file")) {
        LoadSong(value, directory, builder);
      } else if (key.startsWith("title")) {
        builder.setTitle(value);
      } else if (key.startsWith("length")) {
        long seconds = Long.parseLong(value);
        if (seconds > 0) {
          builder.setLengthNanosec(seconds * TimeConstants.NSEC_PER_SEC);
        }
      }
    }
    
    List<Song> ret = new ArrayList<Song>();
    for (int i=0 ; i<songs.size() ; ++i) {
      ret.add(songs.valueAt(i).build());
    }
    return ret;
  }
  
  @Override
  public boolean TryMagic(String data) {
    return data.toLowerCase(Locale.US).contains("[playlist]");
  }
}
