package org.clementine_player.clementine.providers.di;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.clementine_player.clementine.providers.ListItem;
import org.clementine_player.clementine.providers.ProviderException;
import org.clementine_player.clementine.providers.URLFetcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

public class ApiClient {
  private String service_name_;
  
  public static final String CHANNEL_LIST_URL =
      "http://api.v2.audioaddict.com/v1/%s/mobile/batch_update?" +
      "asset_group_key=mobile_icons&stream_set_key=";
  public static final String API_USERNAME = "ephemeron";
  public static final String API_PASSWORD = "dayeiph0ne@pp";
  
  public ApiClient(String service_name) {
    service_name_ = service_name;
  }
  
  private void SetAuthenticationHeader(URLConnection connection) {
    String value = String.format("%s:%s", API_USERNAME, API_PASSWORD);
    try {
      value = Base64.encodeToString(value.getBytes("US-ASCII"), Base64.NO_WRAP);
    } catch (UnsupportedEncodingException e) {
      // Never going to happen.
    }
    
    connection.setRequestProperty("Authorization", "Basic " + value);
  }
  
  public List<ListItem> GetChannelList() throws ProviderException {
    URL url;
    try {
      url = new URL(String.format(CHANNEL_LIST_URL, service_name_));
    } catch (MalformedURLException e) {
      throw new ProviderException(e);
    }
    
    URLFetcher fetcher = new URLFetcher(url);
    SetAuthenticationHeader(fetcher.connection());
    
    List<ListItem> ret = new ArrayList<ListItem>();
    
    try {
      JSONObject data = fetcher.GetJSONObject();
      JSONArray filters = data.getJSONArray("channel_filters");
      for (int i=0 ; i<filters.length() ; ++i) {
        JSONObject filter = filters.getJSONObject(i);
        if (!filter.getString("name").equals("All")) {
          continue;
        }
        
        JSONArray channels = filter.getJSONArray("channels");
        for (int j=0 ; j<channels.length() ; ++j) {
          JSONObject channel = channels.getJSONObject(j);
          ret.add(new Channel(service_name_, channel));
        }
        
        break;
      }
    } catch (JSONException e) {
      throw new ProviderException("Failed to parse channel list JSON", e);
    }
    
    return ret;
  }

  public String service_name() {
    return service_name_;
  }
}
