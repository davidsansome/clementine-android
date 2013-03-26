package org.clementine_player.clementine.providers;

import org.clementine_player.clementine.PB;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class NonCachingItemLoader
    extends AsyncTaskLoader<PB.BrowserItemList> {
  public NonCachingItemLoader(Context context) {
    super(context);
  }

  @Override
  protected void onStartLoading() {
    forceLoad();
  }
}
