package org.clementine_player.clementine;

import java.net.URI;

import org.clementine_player.clementine.playback.PlaybackService;
import org.clementine_player.clementine.playback.PlaybackService.PlaybackBinder;
import org.clementine_player.clementine.playlist.PlaylistAdapter;
import org.clementine_player.gstmediaplayer.MediaPlayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.Loader;

public class PlaylistAdder
    implements Loader.OnLoadCompleteListener<PB.SongList> {
  private Context context_;
  private PlaylistAdapter playlist_;
  
  public PlaylistAdder(Context context, PlaylistAdapter playlist) {
    context_ = context;
    playlist_ = playlist;
  }
  
  public void AddToPlaylist(Loader<PB.SongList> loader) {
    loader.registerListener(0, this);
    loader.startLoading();
  }

  @Override
  public void onLoadComplete(Loader<PB.SongList> loader, PB.SongList songs) {
    playlist_.AppendSongs(songs);
    
    if (songs.getSongsCount() > 0) {
      PlayIfQuiet(songs.getSongs(0).getUri());
    }
  }
  
  public void PlayNow(String uri) {
    Play(uri, false);
  }
  
  public void PlayIfQuiet(String uri) {
    Play(uri, true);
  }
  
  private void Play(final String uri, final boolean only_if_quiet) {
    ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder binder) {
        PlaybackService service = ((PlaybackBinder) binder).GetService();
        
        if (only_if_quiet) {
          MediaPlayer.State state = service.current_state();
          if (state != MediaPlayer.State.COMPLETED &&
              state != MediaPlayer.State.ERROR) {
            return;
          }
        }
        
        service.StartNewSongFromUri(uri);
        
        context_.unbindService(this);
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
      }
    };
    
    Intent intent = new Intent(context_, PlaybackService.class);
    context_.bindService(intent, connection, 0);
  }
}
