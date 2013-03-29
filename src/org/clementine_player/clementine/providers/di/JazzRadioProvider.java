package org.clementine_player.clementine.providers.di;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.R;

public class JazzRadioProvider extends BaseProvider {
  public JazzRadioProvider() {
    super("jazzradio", "jazzradio.com");
  }
  
  @Override
  public PB.BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("JAZZRADIO.com");
    ret.getImageBuilder().setResource(R.drawable.jazzradio);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }
}
