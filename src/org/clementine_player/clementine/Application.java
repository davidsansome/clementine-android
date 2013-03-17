package org.clementine_player.clementine;

import org.clementine_player.clementine.providers.ProviderManager;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

public class Application extends android.app.Application {
  private static final String USER_AGENT = "Clementine %s (Android; %s)";

  private static Application sInstance;
  private Context context_;
  private ProviderManager provider_manager_;
  
  public void onCreate() {
    super.onCreate();
    sInstance = this;
    context_ = getApplicationContext();
    provider_manager_ = new ProviderManager();
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
  
  public ProviderManager provider_manager() {
    return provider_manager_;
  }
}
