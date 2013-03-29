package org.clementine_player.clementine;

import org.clementine_player.clementine.playlist_parsers.ParserManager;
import org.clementine_player.clementine.providers.ProviderManager;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.fedorvlasov.lazylist.ImageLoader;

public class Application extends android.app.Application {
  private static final String USER_AGENT = "Clementine %s (Android; %s)";

  private static Application sInstance;
  private Context context_;
  private BrowserItemCache browser_item_cache_;
  private ProviderManager provider_manager_;
  private ParserManager playlist_parser_manager_;
  private ImageLoader image_loader_;
  
  public void onCreate() {
    super.onCreate();
    sInstance = this;
    context_ = getApplicationContext();
    
    browser_item_cache_ = new BrowserItemCache(context_); 
    provider_manager_ = new ProviderManager();
    playlist_parser_manager_ = new ParserManager();
    image_loader_ = new ImageLoader(context_);
  }
  
  public static Application instance() {
    return sInstance;
  }
  
  public String version_name() {
    try {
      return context_.getPackageManager().getPackageInfo(
          context_.getPackageName(), 0).versionName;
    } catch (NameNotFoundException e) {
      return "Unknown";
    }
  }
  
  public String user_agent() {
    return String.format(USER_AGENT, version_name(), Build.PRODUCT);
  }
  
  public BrowserItemCache browser_item_cache() {
    return browser_item_cache_;
  }
  
  public ProviderManager provider_manager() {
    return provider_manager_;
  }
  
  public ImageLoader image_loader() {
    return image_loader_;
  }
  
  public ParserManager playlist_parser_manager() {
    return playlist_parser_manager_;
  }
}
