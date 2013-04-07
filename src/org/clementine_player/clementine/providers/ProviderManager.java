package org.clementine_player.clementine.providers;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.PB.BrowserItem;
import org.clementine_player.clementine.PB.BrowserItemList;
import org.clementine_player.clementine.PB.SongList;
import org.clementine_player.clementine.providers.di.DigitallyImportedProvider;
import org.clementine_player.clementine.providers.di.JazzRadioProvider;
import org.clementine_player.clementine.providers.di.RockRadioProvider;
import org.clementine_player.clementine.providers.di.SkyFmProvider;
import org.clementine_player.clementine.providers.google_drive.GoogleDriveProvider;
import org.clementine_player.clementine.providers.mediastore.MediaStoreProvider;

import android.content.Context;
import android.support.v4.content.Loader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ProviderManager implements ProviderInterface {
  private List<ProviderInterface> providers_;
  private Map<String, ProviderInterface> providers_by_type_;
  private Map<String, ProviderInterface> providers_by_uri_scheme_;
  
  public ProviderManager() {
    providers_ = Lists.newArrayList();
    providers_by_type_ = Maps.newHashMap();
    providers_by_uri_scheme_ = Maps.newHashMap();
    
    AddProvider(new MediaStoreProvider());
    AddProvider(new DigitallyImportedProvider());
    AddProvider(new SkyFmProvider());
    AddProvider(new JazzRadioProvider());
    AddProvider(new RockRadioProvider());
    AddProvider(new GoogleDriveProvider());
  }
  
  private void AddProvider(ProviderInterface instance) {
    providers_.add(instance);
    providers_by_type_.put(instance.getClass().getName(), instance);
    
    for (String scheme : instance.uri_schemes()) {
      providers_by_uri_scheme_.put(scheme, instance);
    }
  }
  
  public List<ProviderInterface> providers() {
    return providers_;
  }
  
  public ProviderInterface ProviderByType(String class_name) {
    return providers_by_type_.get(class_name);
  }
  
  public ProviderInterface ProviderByURIScheme(String scheme) {
    return providers_by_uri_scheme_.get(scheme);
  }
  
  public ProviderInterface ProviderByURIScheme(URI uri) {
    return providers_by_uri_scheme_.get(uri.getScheme());
  }

  @Override
  public String provider_key() {
    return null;
  }

  @Override
  public String[] uri_schemes() {
    return new String[0];
  }

  @Override
  public Loader<PB.BrowserItemList> LoadItems(Context context, String parent_key) {
    return new Loader<PB.BrowserItemList>(context) {
      @Override
      protected void onStartLoading() {
        PB.BrowserItemList.Builder ret = PB.BrowserItemList.newBuilder();
        for (ProviderInterface provider : providers_) {
          ret.addItems(provider.provider_item());
        }
        deliverResult(ret.build());
      }
    };
  }

  @Override
  public Loader<SongList> LoadSongs(Context context, BrowserItemList items) {
    return null;
  }

  @Override
  public Loader<URL> ResolveURI(Context context, URI url) {
    return null;
  }

  @Override
  public BrowserItem provider_item() {
    return null;
  }
}
