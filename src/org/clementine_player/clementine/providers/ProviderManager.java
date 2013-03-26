package org.clementine_player.clementine.providers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.clementine_player.clementine.providers.di.DigitallyImportedProvider;
import org.clementine_player.clementine.providers.di.JazzRadioProvider;
import org.clementine_player.clementine.providers.di.RockRadioProvider;
import org.clementine_player.clementine.providers.di.SkyFmProvider;
import org.clementine_player.clementine.providers.mediastore.MediaStoreProvider;

public class ProviderManager {
  private List<ProviderInterface> providers_;
  private Map<String, ProviderInterface> providers_by_type_;
  private Map<String, ProviderInterface> providers_by_uri_scheme_;
  
  public ProviderManager() {
    providers_ = new ArrayList<ProviderInterface>();
    providers_by_type_ = new HashMap<String, ProviderInterface>();
    providers_by_uri_scheme_ = new HashMap<String, ProviderInterface>();
    
    AddProvider(new MediaStoreProvider());
    AddProvider(new DigitallyImportedProvider());
    AddProvider(new SkyFmProvider());
    AddProvider(new JazzRadioProvider());
    AddProvider(new RockRadioProvider());
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
}
