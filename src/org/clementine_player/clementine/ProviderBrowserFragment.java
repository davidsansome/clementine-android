package org.clementine_player.clementine;

import java.util.ArrayList;
import java.util.List;

import org.clementine_player.clementine.providers.ListItem;
import org.clementine_player.clementine.providers.ProviderInterface;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
        convert_view = layout_inflater_.inflate(
            android.R.layout.simple_list_item_1, null);
      }
      
      TextView text_view = (TextView)
          convert_view.findViewById(android.R.id.text1);
      text_view.setText(items_.get(position).text_);
      
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
  public Loader<List<ListItem>> onCreateLoader(int id, Bundle args) {
    return provider_.LoadData(getActivity(), parent_url_);
  }

  @Override
  public void onLoadFinished(
      Loader<List<ListItem>> loader, List<ListItem> items) {
    adapter_.SetData(items);
  }

  @Override
  public void onLoaderReset(Loader<List<ListItem>> arg0) {
  }
}
