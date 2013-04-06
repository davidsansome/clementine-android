package org.clementine_player.clementine.playlist_parsers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.clementine_player.clementine.PB.Song;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ParserManager {
  private static final int MAGIC_SIZE = 512;
  private static final String TAG = "ParserManager";
  
  private List<BaseParser> parsers_;
  
  public ParserManager() {
    parsers_ = Lists.newArrayList();
    
    AddParser(new PlsParser());
  }

  private void AddParser(BaseParser parser) {
    parsers_.add(parser);
  }
  
  public BaseParser ParserForMagic(String data) {
    for (BaseParser parser : parsers_) {
      Log.d(TAG, "Trying parser " + parser.name() + " for magic");
      if (parser.TryMagic(data)) {
        Log.d(TAG, "Using parser " + parser.name() + " for magic");
        return parser;
      }
    }
    return null;
  }
  
  public List<Song> LoadFromStream(
      InputStream stream, String path_hint, String dir_hint)
          throws IOException {
    byte[] magic_bytes = new byte[MAGIC_SIZE];
    BufferedInputStream buffered_stream = new BufferedInputStream(stream);
    buffered_stream.mark(MAGIC_SIZE);
    buffered_stream.read(magic_bytes, 0, MAGIC_SIZE);
    buffered_stream.reset();
    
    BaseParser parser = ParserForMagic(new String(magic_bytes));
    if (parser == null) {
      return ImmutableList.of();
    }
    
    return parser.Load(buffered_stream, path_hint, dir_hint);
  }
}
