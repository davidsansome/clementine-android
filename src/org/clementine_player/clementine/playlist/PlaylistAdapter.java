package org.clementine_player.clementine.playlist;

import java.util.ArrayList;
import java.util.List;

import org.clementine_player.clementine.PB;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListAdapter;

public class PlaylistAdapter implements ListAdapter {
  private class Item {
    public long id_;
    public PB.Song song_;
  }
  
  private PlaylistItem.Column[] columns_;
  private int height_px_;
  private Context context_;
  
  private List<Item> items_;
  private List<DataSetObserver> observers_;
  private int next_id_;
  
  public PlaylistAdapter(
      Context context, int height_px, PlaylistItem.Column[] columns) {
    height_px_ = height_px;
    columns_ = columns;
    context_ = context;
    
    items_ = new ArrayList<Item>();
    observers_ = new ArrayList<DataSetObserver>();
    next_id_ = 0;
  }

  @Override
  public int getCount() {
    return items_.size();
  }

  @Override
  public Object getItem(int position) {
    return items_.get(position);
  }

  @Override
  public long getItemId(int position) {
    return items_.get(position).id_;
  }

  @Override
  public int getItemViewType(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convert_view, ViewGroup parent) {
    PB.Song song = GetSong(position);
    
    if (convert_view == null) {
      convert_view = new PlaylistItem(context_, height_px_, columns_, song);
    } else {
      ((PlaylistItem)convert_view).SetSong(song);
    }
    
    return convert_view;
  }

  @Override
  public int getViewTypeCount() {
    return 1;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public boolean isEmpty() {
    return items_.isEmpty();
  }

  @Override
  public void registerDataSetObserver(DataSetObserver observer) {
    observers_.add(observer);
  }

  @Override
  public void unregisterDataSetObserver(DataSetObserver observer) {
    observers_.remove(observer);
  }
  
  @Override
  public boolean areAllItemsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled(int position) {
    return true;
  }
  
  public PB.Song GetSong(int position) {
    return items_.get(position).song_;
  }
  
  private Item SongToItem(PB.Song song) {
    Item item = new Item();
    item.id_ = next_id_ ++;
    item.song_ = song;
    return item;
  }
  
  private void NotifyObservers() {
    for (DataSetObserver observer : observers_) {
      observer.onChanged();
    }
  }
  
  public void InsertSong(PB.Song song, int position) { 
    items_.add(position, SongToItem(song));
    NotifyObservers();
  }
  
  public void InsertSongs(PB.SongList songs, int position) {
    for (PB.Song song: songs.getSongsList()) {
      items_.add(position++, SongToItem(song));
    }
    NotifyObservers();
  }
  
  public void AppendSong(PB.Song song) {
    InsertSong(song, items_.size());
  }
  
  public void AppendSongs(PB.SongList songs) {
    InsertSongs(songs, items_.size());
  }
}
