package org.clementine_player.clementine.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.clementine_player.gstmediaplayer.MediaPlayer;
import org.clementine_player.gstmediaplayer.MediaPlayer.Listener;
import org.clementine_player.gstmediaplayer.MediaPlayer.State;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.util.Log;

public class Stream implements Listener {
  private MediaPlayer player_;
  private State current_state_;
  private State desired_state_;
  private List<Listener> listeners_;
  
  private float current_volume_;
  private boolean fade_in_desired_;
  private long fade_duration_msec_;
  private ValueAnimator volume_fader_;
  
  private static int next_stream_id_ = 0;
  private int stream_id_;
  private String log_tag_;
  
  public Stream(String url) {
    listeners_ = new ArrayList<Listener>();
    current_state_ = State.PREPARING;
    desired_state_ = State.PAUSED;
    current_volume_ = 1.0f;
    
    stream_id_ = next_stream_id_ ++;
    log_tag_ = "Stream(" + stream_id_ + ")";
    
    Log.i(log_tag_, "New stream for " + url);
    
    player_ = new MediaPlayer(url, this);
  }
  
  public void AddListener(Listener listener) {
    listeners_.add(listener);
    listener.StreamStateChanged(current_state_, null);
  }
  
  public void RemoveListener(Listener listener) {
    listeners_.remove(listener);
  }
  
  private void UpdateAllListeners(String message) {
    for (Listener listener : listeners_) {
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
              FadeTo(1.0f, fade_duration_msec_);
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
          FadeTo(1.0f, fade_duration_msec_);
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
  
  private void FadeTo(final float target_volume, long duration_msec) {
    if (player_ == null) {
      return;
    }
    
    Log.d(log_tag_, "Fading volume from " + current_volume_ +
                    " to " + target_volume + " over " + duration_msec + "ms");
    
    if (volume_fader_ != null) {
      volume_fader_.cancel();
      volume_fader_ = null;
    }
    
    volume_fader_ = ValueAnimator.ofFloat(current_volume_, target_volume);
    volume_fader_.setDuration(duration_msec);
    volume_fader_.addListener(new AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {}
      @Override public void onAnimationRepeat(Animator animation) {}
      @Override public void onAnimationEnd(Animator animation) {Finished();}
      @Override public void onAnimationCancel(Animator animation) {Finished();}
      
      private void Finished() {
        volume_fader_ = null;
        
        Log.d(log_tag_, "Fading volume finished");
        
        if (target_volume == 0.0f) {
          Release();
        }
      }
    });
    volume_fader_.addUpdateListener(new AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        if (player_ != null) {
          current_volume_ = (Float) animator.getAnimatedValue();
          player_.SetVolume(current_volume_);
        }
      }
    });
    volume_fader_.start();
  }
  
  public void FadeIn(long duration_msec) {
    current_volume_ = 0.0f;
    switch (current_state_) {
      case PREPARING:
      case PAUSED:
        fade_in_desired_ = true;
        fade_duration_msec_ = duration_msec;
        break;
      case PLAYING:
        FadeTo(1.0f, duration_msec);
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
        FadeTo(0.0f, duration_msec);
        break;
    }
  }
  
  public Visualizer CreateVisualizer() {
    return null;
    // TODO(dsansome): implement session IDs.
    // return new Visualizer(player_.getAudioSessionId());
  }
  
  public State current_state() {
    return current_state_;
  }
}
