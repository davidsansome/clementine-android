package org.clementine_player.clementine.providers;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.PB;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class CachingItemLoader
    extends AsyncTaskLoader<PB.BrowserItemList> {
  private String provider_key_;
  private String key_;
  
  public CachingItemLoader(Context context, String provider_key) {
    super(context);
    provider_key_ = provider_key;
    key_ = null;
  }
  
  public CachingItemLoader(Context context, String provider_key, String key) {
    super(context);
    provider_key_ = provider_key;
    key_ = key;
  }

  @Override
  protected void onStartLoading() {
    // Try to load the data from the cache.
    PB.BrowserItemList ret =
        Application.instance().browser_item_cache().Load(provider_key_, key_);
    if (ret != null) {
      deliverResult(ret);
      return;
    }
    
    // Otherwise get fresh data.
    forceLoad();
  }
  
  @Override
  protected PB.BrowserItemList onLoadInBackground() {
    PB.BrowserItemList ret = super.onLoadInBackground();
    Application.instance().browser_item_cache().Save(provider_key_, key_, ret);
    return ret;
  }
}
