package org.clementine_player.clementine;

import org.clementine_player.clementine.providers.ProviderInterface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
        text_view_1.getLayoutParams().height = LayoutParams.MATCH_PARENT;
      }
      
      if (item.hasImage()) {
        PB.Image image = item.getImage();
        ImageView view = (ImageView) convert_view.findViewById(R.id.image);
        
        if (image.hasUrl()) {
          Application.instance().image_loader().DisplayImage(
              image.getUrl(), view);
        } else if (image.hasResource()) {
          view.setImageResource(image.getResource());
        } else if (image.hasData()) {
          byte[] bytes = image.getData().toByteArray();
          Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
          view.setImageBitmap(bitmap);
        }
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
    
    Bundle arguments = getArguments();
    
    if (arguments == null) {
      // This is the top level view.
      provider_ = Application.instance().provider_manager();
    } else {
      provider_name_ = arguments.getString("provider_name");
      provider_ = Application.instance().provider_manager().ProviderByType(
          provider_name_);
      parent_key_ = arguments.getString("parent_key");
    }
    
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
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    final PB.BrowserItem item = adapter_.GetListItem(position);
    
    if (item.getHasChildren()) {
      Bundle args = new Bundle();
      if (item.hasProviderClassName()) {
        args.putString("provider_name", item.getProviderClassName());
      } else {
        args.putString("provider_name", provider_name_);
      }
      
      if (item.hasKey()) {
        args.putString("parent_key", item.getKey());
      }
      
      ProviderBrowserFragment new_fragment = new ProviderBrowserFragment();
      new_fragment.setArguments(args);
      
      FragmentTransaction transaction =
          getActivity().getSupportFragmentManager().beginTransaction();
      transaction.replace(R.id.browser, new_fragment);
      transaction.addToBackStack(null);
      transaction.commit();
      
      return;
    }
    
    PB.BrowserItemList.Builder list_builder = PB.BrowserItemList.newBuilder();
    list_builder.addItems(item);
    PB.BrowserItemList items = list_builder.build();
    
    Application.instance().playlist_adder().AddToPlaylist(
        provider_.LoadSongs(getActivity(), items));
  }
}
