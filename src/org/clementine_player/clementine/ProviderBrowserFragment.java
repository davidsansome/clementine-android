package org.clementine_player.clementine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.clementine_player.clementine.playback.PlaybackService;
import org.clementine_player.clementine.providers.ListItem;
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
    implements LoaderCallbacks<List<ListItem>> {
  private class ItemAdapter extends BaseAdapter {
    private List<ListItem> items_;
    private LayoutInflater layout_inflater_;
    
    public ItemAdapter(Context context) {
      items_ = new ArrayList<ListItem>();
      
      layout_inflater_ = (LayoutInflater)
           context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public int getCount() {
      return items_.size();
    }

    @Override
    public Object getItem(int position) {
      return items_.get(position);
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
      
      ListItem item = items_.get(position);
      text_view_1.setText(item.text1_);
      
      if (item.text2_ != null) {
        text_view_2.setVisibility(View.VISIBLE);
        text_view_2.setText(item.text2_);
      } else {
        text_view_2.setVisibility(View.GONE);
      }
      
      if (item.image_url_ != null) {
        Application.instance().image_loader().DisplayImage(
            item.image_url_.toString(),
            (ImageView) convert_view.findViewById(R.id.image));
      }
      
      return convert_view;
    }

    @Override
    public boolean hasStableIds() {
      return true;
    }

    public void SetData(List<ListItem> items) {
      items_ = items;
      notifyDataSetChanged();
    }
    
    public ListItem GetListItem(int position) {
      return items_.get(position);
    }
  }
  
  private ItemAdapter adapter_;
  private ProviderInterface provider_;
  private String parent_url_;

  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    
    adapter_ = new ItemAdapter(getActivity());
    setListAdapter(adapter_);
    
    provider_ = Application.instance().provider_manager().ProviderByType(
        getArguments().getString("provider_name"));
    parent_url_ = getArguments().getString("parent_url");
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onViewCreated(View view, Bundle saved_instance_state) {
    super.onViewCreated(view, saved_instance_state);
    setListShown(false);
  }
  
  @Override
  public Loader<List<ListItem>> onCreateLoader(int id, Bundle args) {
    return provider_.LoadItems(getActivity(), parent_url_);
  }

  @Override
  public void onLoadFinished(
      Loader<List<ListItem>> loader, List<ListItem> items) {
    setListShown(true);
    adapter_.SetData(items);
  }

  @Override
  public void onLoaderReset(Loader<List<ListItem>> arg0) {
  }
  
  private ServiceConnection connection_;
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    final ListItem item = adapter_.GetListItem(position);
    
    if (item.has_children_) {
      // TODO(dsansome): not supported yet.
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
        
        PlaybackService.PlaybackBinder playback_service =
            (PlaybackService.PlaybackBinder) service;
        playback_service.StartNewSong(item.media_uri_);
      }
    };
    Intent intent = new Intent(getActivity(), PlaybackService.class);
    getActivity().bindService(intent, connection_, Context.BIND_AUTO_CREATE);
  }
}
