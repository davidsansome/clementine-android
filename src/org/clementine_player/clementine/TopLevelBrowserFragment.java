package org.clementine_player.clementine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.clementine_player.clementine.providers.ProviderInterface;
import org.clementine_player.clementine.providers.ProviderManager;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class TopLevelBrowserFragment extends ListFragment {
  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    
    ProviderManager provider_manager =
        Application.instance().provider_manager();
    
    List<HashMap<String, String>> records =
        new ArrayList<HashMap<String, String>>();
    for (ProviderInterface provider : provider_manager.providers()) {
      HashMap<String, String> map = new HashMap<String, String>();
      map.put("text", provider.name());
      map.put("provider_name", provider.getClass().getName());
      records.add(map);
    }
    
    SimpleAdapter adapter = new SimpleAdapter(
        getActivity(),
        records,
        android.R.layout.simple_list_item_1,
        new String[]{"text"},
        new int[]{android.R.id.text1});
    setListAdapter(adapter);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    @SuppressWarnings("unchecked")
    HashMap<String, String> map =
        (HashMap<String, String>) getListAdapter().getItem(position);
    
    Bundle args = new Bundle();
    args.putString("provider_name", map.get("provider_name"));
    args.putString("parent_url", null);
    
    ProviderBrowserFragment new_fragment = new ProviderBrowserFragment();
    new_fragment.setArguments(args);
    
    FragmentTransaction transaction =
        getActivity().getSupportFragmentManager().beginTransaction();
    transaction.replace(R.id.browser, new_fragment);
    transaction.addToBackStack(null);
    transaction.commit();
  }
}
