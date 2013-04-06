package org.clementine_player.clementine.playback;

import java.util.ArrayList;
import java.util.List;

import org.clementine_player.gstmediaplayer.MediaPlayer;
import org.clementine_player.gstmediaplayer.MediaPlayer.AnalyzerListener;
import org.clementine_player.gstmediaplayer.MediaPlayer.FadeListener;
import org.clementine_player.gstmediaplayer.MediaPlayer.State;
import org.clementine_player.gstmediaplayer.MediaPlayer.StateListener;

import android.util.Log;

public class Stream implements StateListener, FadeListener {
  private MediaPlayer player_;
  private State current_state_;
  private State desired_state_;
  private List<StateListener> listeners_;
  
  private boolean fade_in_desired_;
  private long fade_duration_msec_;
  private boolean release_after_fade_;
  
  private static int next_stream_id_ = 0;
  private int stream_id_;
  private String log_tag_;
  
  public Stream(String url, AnalyzerListener analyzer_listener) {
    listeners_ = new ArrayList<StateListener>();
    current_state_ = State.PREPARING;
    desired_state_ = State.PAUSED;
    release_after_fade_ = false;
    
    stream_id_ = next_stream_id_ ++;
    log_tag_ = "Stream(" + stream_id_ + ")";
    
    Log.i(log_tag_, "New stream for " + url);
    
    player_ = new MediaPlayer(url, this, this, analyzer_listener);
  }
  
  public void AddListener(StateListener listener) {
    listeners_.add(listener);
    listener.StreamStateChanged(current_state_, null);
  }
  
  public void RemoveListener(StateListener listener) {
    listeners_.remove(listener);
  }
  
  private void UpdateAllListeners(String message) {
    for (StateListener listener : listeners_) {
      listener.StreamStateChanged(current_state_, message);
    }
  }
  
  private void SetCurrentState(State state, String message) {
    Log.d(log_tag_, "Current state " + current_state_.name() +
                    " -> " + state.name());
    current_state_ = state;
    UpdateAllListeners(message);
  }
  
  private void SetDesiredState(State state) {
    Log.d(log_tag_, "Desired state " + desired_state_.name() +
                    " -> " + state.name());
    desired_state_ = state;
  }
  
  @Override
  public void StreamStateChanged(State state, String message) {
    switch (state) {
      case PAUSED:
        switch (desired_state_) {
          case PLAYING:
            player_.Start();
            
            if (fade_in_desired_) {
              fade_in_desired_ = false;
              player_.FadeVolumeTo(1.0f, fade_duration_msec_);
            }
            break;
          default:
            SetCurrentState(State.PAUSED, message);
            break;
        }
        break;
      case PLAYING:
        SetCurrentState(State.PLAYING, message);
        break;
      case COMPLETED:
      case ERROR:
        SetCurrentState(state, message);
        Release();
        break;
      default:
        break;
    }
  }

  public void Play() {
    SetDesiredState(State.PLAYING);
    switch (current_state_) {
      case PREPARING:
      case PLAYING:
      case COMPLETED:
      case ERROR:
        break;
      case PAUSED:
        player_.Start();
        
        if (fade_in_desired_) {
          fade_in_desired_ = false;
          player_.FadeVolumeTo(1.0f, fade_duration_msec_);
        }
        break;
    }
  }
  
  public void Pause() {
    SetDesiredState(State.PAUSED);
    switch (current_state_) {
      case PREPARING:
      case PAUSED:
      case COMPLETED:
      default:
        break;
      case PLAYING:
        player_.Pause();
        break;
    }
  }
  
  public void PlayPause() {
    if (current_state_ == State.PLAYING) {
      Pause();
    } else {
      Play();
    }
  }
  
  public void Release() {
    if (player_ != null) {
      SetCurrentState(State.COMPLETED, null);
      player_.Release();
      player_ = null;
    }
  }
  
  public void FadeIn(long duration_msec) {
    switch (current_state_) {
      case PREPARING:
      case PAUSED:
        fade_in_desired_ = true;
        fade_duration_msec_ = duration_msec;
        break;
      case PLAYING:
        player_.FadeVolumeTo(1.0f, duration_msec);
        break;
      default:
        break;
    }
  }
  
  public void FadeOutAndRelease(long duration_msec) {
    switch (current_state_) {
      case PREPARING:
      case PAUSED:
        Release();
        break;
      case PLAYING:
        release_after_fade_ = true;
        player_.FadeVolumeTo(0.0f, duration_msec);
        break;
      default:
        break;
    }
  }
  
  public State current_state() {
    return current_state_;
  }

  @Override
  public void FadeFinished() {
    if (release_after_fade_) {
      Release();
    }
  }
  
  public void SetAnalyzerEnabled(boolean enabled) {
    player_.SetAnalyzerEnabled(enabled);
  }
}
