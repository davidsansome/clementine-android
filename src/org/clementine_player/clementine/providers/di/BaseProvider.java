package org.clementine_player.clementine.providers.di;

import java.util.List;

import org.clementine_player.clementine.providers.ListItem;
import org.clementine_player.clementine.providers.ProviderException;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.content.Context;
import android.os.OperationCanceledException;
import android.support.v4.content.AsyncTaskLoader;

public class BaseProvider extends ProviderInterface {
  private String name_;
  private Client api_client_;
  
  protected BaseProvider(String name, String service_name) {
    name_ = name;
    api_client_ = new Client(service_name);
  }

  @Override
  public String name() {
    return name_;
  }

  @Override
  public AsyncTaskLoader<List<ListItem>> LoadData(
      Context context, String parent_url) {
    return new AsyncTaskLoader<List<ListItem>>(context) {
      @Override
      protected void onStartLoading() {
        forceLoad();
      }
      
      @Override
      public List<ListItem> loadInBackground() 
          throws android.os.OperationCanceledException{
        try {
          return api_client_.GetChannelList();
        } catch (ProviderException e) {
          throw new OperationCanceledException(e.getMessage());
        }
      }
    };
  }
}
