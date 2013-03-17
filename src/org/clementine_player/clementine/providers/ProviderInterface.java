package org.clementine_player.clementine.providers;

import java.util.List;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class ProviderInterface {
  public abstract String name();
  public abstract AsyncTaskLoader<List<ListItem>> LoadData(
       Context context, String parent_url);
}
