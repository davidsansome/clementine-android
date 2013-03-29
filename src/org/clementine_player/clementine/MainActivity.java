package org.clementine_player.clementine;

import org.clementine_player.clementine.analyzers.BaseAnalyzer;
import org.clementine_player.clementine.analyzers.BlockAnalyzer;
import org.clementine_player.clementine.playback.PlaybackService;
import org.clementine_player.clementine.playback.PlaybackService.PlaybackBinder;
import org.clementine_player.clementine.playback.Stream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class MainActivity 
    extends FragmentActivity
    implements Stream.Listener,
               PlaybackService.VisualizerListener {
  private static final String TAG = "MainActivity";
  
  private ImageButton play_button_;
  private ImageButton pause_button_;
  private ImageButton stop_button_;
  private ProgressBar buffering_bar_;
  private SurfaceView analyzer_view_;
  
  private BaseAnalyzer analyzer_;
  
  private Stream.State state_;
  private PlaybackService playback_service_;
  private ServiceConnection playback_connection_ = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      playback_service_ = ((PlaybackBinder) binder).GetService();
      playback_service_.AddStreamListener(MainActivity.this);
      playback_service_.AddVisualizerListener(MainActivity.this);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      playback_service_.RemoveStreamListener(MainActivity.this);
      playback_service_.RemoveVisualizerListener(MainActivity.this);
      playback_service_ = null;
    }
  };
  
  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    setContentView(R.layout.main);
    
    // Get UI elements.
    play_button_ = (ImageButton) findViewById(R.id.play);
    pause_button_ = (ImageButton) findViewById(R.id.pause);
    stop_button_ = (ImageButton) findViewById(R.id.stop);
    buffering_bar_ = (ProgressBar) findViewById(R.id.buffering_bar);
    analyzer_view_ = (SurfaceView) findViewById(R.id.analyzer);
    
    analyzer_ = new BlockAnalyzer(this, analyzer_view_.getHolder());
    
    // Create the browser fragment.
    if (saved_instance_state == null) {
      ProviderBrowserFragment fragment = new ProviderBrowserFragment();
      
      FragmentTransaction transaction =
          getSupportFragmentManager().beginTransaction();
      transaction.add(R.id.browser, fragment);
      transaction.commit();
    }
    
    state_ = Stream.State.COMPLETED;
    StreamStateChanged(state_);
  }
  
  @Override
  public void onStart() {
    super.onStart();
    
    // Bind to the playback service.
    Intent intent = new Intent(this, PlaybackService.class);
    bindService(intent, playback_connection_, Context.BIND_AUTO_CREATE);
  }

  @Override
  public void StreamStateChanged(Stream.State state) {
    switch (state) {
      case PREPARING:
      case PREPARED:
        play_button_.setVisibility(View.INVISIBLE);
        pause_button_.setVisibility(View.VISIBLE);
        buffering_bar_.setVisibility(View.VISIBLE);
        pause_button_.setEnabled(false);
        stop_button_.setEnabled(true);
        break;
        
      case STARTED:
        play_button_.setVisibility(View.INVISIBLE);
        pause_button_.setVisibility(View.VISIBLE);
        buffering_bar_.setVisibility(View.INVISIBLE);
        pause_button_.setEnabled(true);
        stop_button_.setEnabled(true);
        break;
        
      case PAUSED:
        play_button_.setVisibility(View.VISIBLE);
        pause_button_.setVisibility(View.INVISIBLE);
        buffering_bar_.setVisibility(View.INVISIBLE);
        stop_button_.setEnabled(true);
        break;
        
      case COMPLETED:
        play_button_.setVisibility(View.VISIBLE);
        pause_button_.setVisibility(View.INVISIBLE);
        buffering_bar_.setVisibility(View.INVISIBLE);
        stop_button_.setEnabled(false);
        break;
    }
  }
  
  public void PreviousClicked(View button) {
    // TODO
  }
  
  public void PlayClicked(View button) {
    if (playback_service_ != null) {
      playback_service_.PlayPause();
    }
  }
  
  public void PauseClicked(View button) {
    if (playback_service_ != null) {
      playback_service_.PlayPause();
    }
  }
  
  public void StopClicked(View button) {
    if (playback_service_ != null) {
      playback_service_.Stop();
    }
  }
  
  public void NextClicked(View button) {
    // TODO
  }

  @Override
  public void UpdateFft(int[] fft) {
    analyzer_.UpdateFft(fft);
  }
}
