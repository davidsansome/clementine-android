package org.clementine_player.clementine.providers.di;

import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.R;

public class DigitallyImportedProvider extends BaseProvider {
  public DigitallyImportedProvider() {
    super("di", "di.fm");
  }

  @Override
  public PB.BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("Digitally Imported");
    ret.getImageBuilder().setResource(R.drawable.digitallyimported);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }
}
