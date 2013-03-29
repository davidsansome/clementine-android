package org.clementine_player.clementine.providers.di;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.R;

public class RockRadioProvider extends BaseProvider {
  public RockRadioProvider() {
    super("rockradio", "rockradio.com");
  }
  
  @Override
  public PB.BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("ROCKRADIO.com");
    ret.getImageBuilder().setResource(R.drawable.rockradio);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }
}
