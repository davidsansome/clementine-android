package org.clementine_player.clementine.playlist;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.playlist.PlaylistItem.Column;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class PlaylistContainer
    extends LinearLayout
    implements OnItemClickListener {
  private Column[] columns_;
  private PlaylistItem header_;
  private PlaylistAdapter adapter_;
  private ListView view_;
  
  public PlaylistContainer(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(LinearLayout.VERTICAL);
    
    int height_px =
        (int)(40 * getResources().getDisplayMetrics().density);
    
    columns_ = new PlaylistItem.Column[]{
      PlaylistItem.Column.TRACK_NUMBER,
      PlaylistItem.Column.ARTIST,
      PlaylistItem.Column.ALBUM,
      PlaylistItem.Column.TITLE,
      PlaylistItem.Column.URI,
    };
    
    header_ = new PlaylistItem(getContext(), height_px, columns_, null);
    adapter_ = new PlaylistAdapter(getContext(), height_px, columns_);
    view_ = new ListView(getContext());
    view_.setAdapter(adapter_);
    
    view_.setOnItemClickListener(this);
    
    ImageView separator = new ImageView(getContext());
    separator.setImageResource(android.R.drawable.divider_horizontal_dark);
    separator.setScaleType(ImageView.ScaleType.FIT_XY);
    separator.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, 1));
    
    addView(header_, new LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.0f));
    addView(separator);
    addView(view_, new LayoutParams(
        LayoutParams.MATCH_PARENT, 0, 1.0f));
  }

  public PlaylistAdapter adapter() {
    return adapter_;
  }

  @Override
  public void onItemClick(
      AdapterView<?> parent, View view, int position, long id) {
    PB.Song song = adapter_.GetSong(position);
    Application.instance().playlist_adder().PlayNow(song.getUri());
  }
}
