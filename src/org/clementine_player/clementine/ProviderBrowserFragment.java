package org.clementine_player.clementine;

import java.net.URI;
import java.net.URISyntaxException;

import org.clementine_player.clementine.playback.PlaybackService;
import org.clementine_player.clementine.playback.PlaybackService.PlaybackBinder;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ProviderBrowserFragment
    extends ListFragment
    implements LoaderCallbacks<PB.BrowserItemList> {
  private class ItemAdapter extends BaseAdapter {
    private PB.BrowserItemList items_;
    private LayoutInflater layout_inflater_;
    
    public ItemAdapter(Context context) {
      layout_inflater_ = (LayoutInflater)
           context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public int getCount() {
      if (items_ == null) {
        return 0;
      }
      return items_.getItemsCount();
    }

    @Override
    public Object getItem(int position) {
      return items_.getItems(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }
    
    @Override
    public View getView(int position, View convert_view, ViewGroup parent) {
      if (convert_view == null) {
        convert_view = layout_inflater_.inflate(R.layout.browseritem, null);
      }
      
      TextView text_view_1 = (TextView) convert_view.findViewById(R.id.text1);
      TextView text_view_2 = (TextView) convert_view.findViewById(R.id.text2);
      
      PB.BrowserItem item = items_.getItems(position);
      text_view_1.setText(item.getText1());
      
      if (item.hasText2() && !item.getText2().isEmpty()) {
        text_view_2.setVisibility(View.VISIBLE);
        text_view_2.setText(item.getText2());
      } else {
        text_view_2.setVisibility(View.GONE);
      }
      
      if (item.hasImageUrl() && !item.getImageUrl().isEmpty()) {
        Application.instance().image_loader().DisplayImage(
            item.getImageUrl(),
            (ImageView) convert_view.findViewById(R.id.image));
      }
      
      return convert_view;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    public void SetData(PB.BrowserItemList items) {
      items_ = items;
      notifyDataSetChanged();
    }
    
    public PB.BrowserItem GetListItem(int position) {
      return items_.getItems(position);
    }
  }

  private static final String TAG = "ProviderBrowserFragment";
  
  private ItemAdapter adapter_;
  private String provider_name_;
  private ProviderInterface provider_;
  private String parent_key_;

  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    
    adapter_ = new ItemAdapter(getActivity());
    setListAdapter(adapter_);
    
    provider_name_ = getArguments().getString("provider_name");
    provider_ = Application.instance().provider_manager().ProviderByType(
        provider_name_);
    parent_key_ = getArguments().getString("parent_key");
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onViewCreated(View view, Bundle saved_instance_state) {
    super.onViewCreated(view, saved_instance_state);
    setListShown(false);
  }
  
  @Override
  public Loader<PB.BrowserItemList> onCreateLoader(int id, Bundle args) {
    return provider_.LoadItems(getActivity(), parent_key_);
  }

  @Override
  public void onLoadFinished(
      Loader<PB.BrowserItemList> loader, PB.BrowserItemList items) {
    setListShown(true);
    adapter_.SetData(items);
  }

  @Override
  public void onLoaderReset(Loader<PB.BrowserItemList> loader) {
  }
  
  private ServiceConnection connection_;
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    final PB.BrowserItem item = adapter_.GetListItem(position);
    
    if (item.getHasChildren()) {
      Bundle args = new Bundle();
      args.putString("provider_name", provider_name_);
      args.putString("parent_key", item.getKey());
      
      ProviderBrowserFragment new_fragment = new ProviderBrowserFragment();
      new_fragment.setArguments(args);
      
      FragmentTransaction transaction =
          getActivity().getSupportFragmentManager().beginTransaction();
      transaction.replace(R.id.browser, new_fragment);
      transaction.addToBackStack(null);
      transaction.commit();
      
      return;
    }
    
    // TODO(dsansome): hack.
    connection_ = new ServiceConnection() {
      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.i("ServiceConnection", "Disconnected");
      }
      
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i("ServiceConnection", "Connected");
        
        PlaybackService playback_service =
            ((PlaybackBinder) service).GetService();
        try {
          playback_service.StartNewSong(new URI(item.getMediaUri()));
        } catch (URISyntaxException e) {
          Log.w(TAG, "Invalid URI: " + item.getMediaUri());
        }
      }
    };
    Intent intent = new Intent(getActivity(), PlaybackService.class);
    getActivity().bindService(intent, connection_, Context.BIND_AUTO_CREATE);
  }
}
