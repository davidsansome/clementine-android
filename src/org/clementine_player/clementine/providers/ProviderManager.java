package org.clementine_player.clementine.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.clementine_player.clementine.providers.di.DigitallyImportedProvider;

public class ProviderManager {
  private List<ProviderInterface> providers_;
  private Map<String, ProviderInterface> providers_by_type_;
  
  public ProviderManager() {
    providers_ = new ArrayList<ProviderInterface>();
    providers_by_type_ = new HashMap<String, ProviderInterface>();
    AddProvider(new DigitallyImportedProvider());
  }
  
  private void AddProvider(ProviderInterface instance) {
    providers_.add(instance);
    providers_by_type_.put(instance.getClass().getName(), instance);
  }
  
  public List<ProviderInterface> providers() {
    return providers_;
  }
  
  public ProviderInterface ProviderByType(String class_name) {
    return providers_by_type_.get(class_name);
  }
}
