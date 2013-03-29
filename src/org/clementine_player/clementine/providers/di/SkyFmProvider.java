package org.clementine_player.clementine.providers.di;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.PB.BrowserItem;
import org.clementine_player.clementine.R;

public class SkyFmProvider extends BaseProvider {
  public SkyFmProvider() {
    super("sky", "sky.fm");
  }
  
  PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();

  @Override
  public BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("SKY.fm");
    ret.getImageBuilder().setResource(R.drawable.skyfm);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }
}
