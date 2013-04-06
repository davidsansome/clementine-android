package org.clementine_player.clementine.playlist;

import org.clementine_player.clementine.PB;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlaylistItem extends LinearLayout {
  public enum Column {
    TRACK_NUMBER,
    ARTIST,
    ALBUM,
    TITLE,
    URI,
  }
  
  int height_px_;
  PB.Song song_;
  Column[] columns_;
  
  public PlaylistItem(
      Context context, int height_px, Column[] columns, PB.Song song) {
    super(context);
    setOrientation(HORIZONTAL);
    setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    
    height_px_ = height_px;
    song_ = song;
    columns_ = columns;
    Update();
  }
  
  @Override
  public LayoutParams generateDefaultLayoutParams() {
    return new LinearLayout.LayoutParams(
        0, height_px_, 1.0f);
  }
  
  public void SetSong(PB.Song song) {
    song_ = song;
    Update();
  }
  
  public void SetColumns(Column[] columns) {
    columns_ = columns;
    Update();
  }
  
  private void Update() {
    removeAllViews();
    
    for (Column column: columns_) {
      addView(ViewForColumn(column));
    }
  }
  
  private String TextForColumn(Column column) {
    switch (column) {
      case TRACK_NUMBER: return Integer.toString(song_.getTrack());
      case ARTIST:       return song_.getArtist();
      case ALBUM:        return song_.getAlbum();
      case TITLE:        return song_.getTitle();
      case URI:          return song_.getUri();
    }
    
    return "";
  }
  
  private String HeaderTextForColumn(Column column) {
    switch (column) {
      case TRACK_NUMBER: return "Track";
      case ARTIST:       return "Artist";
      case ALBUM:        return "Album";
      case TITLE:        return "Title";
      case URI:          return "URI";
    }
    
    return "";
  }
  
  private View ViewForColumn(Column column) {
    TextView ret = new TextView(getContext());
    ret.setSingleLine(true);
    ret.setTextSize(14.0f);
    ret.setGravity(Gravity.CENTER_VERTICAL);
    ret.setEllipsize(TextUtils.TruncateAt.END);
    
    if (song_ == null) {
      ret.setText(HeaderTextForColumn(column));
      ret.setTypeface(null, Typeface.BOLD);
      ret.setAllCaps(true);
      ret.setTextColor(Color.WHITE);
    } else {
      ret.setText(TextForColumn(column));
    }

    return ret;
  }
}
