package org.clementine_player.clementine.providers;

import java.util.List;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class CachingItemLoader
    extends AsyncTaskLoader<List<ListItem>> {
  private List<ListItem> cached_data_;
  
  public CachingItemLoader(Context context) {
    super(context);
  }

  @Override
  protected void onStartLoading() {
    if (cached_data_ != null) {
      deliverResult(cached_data_);
      return;
    }
    forceLoad();
  }
  
  @Override
  public void deliverResult(List<ListItem> data) {
    cached_data_ = data;
    super.deliverResult(data);
  }
}
