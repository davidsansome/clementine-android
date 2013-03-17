package org.clementine_player.clementine.providers.di;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.clementine_player.clementine.providers.ListItem;
import org.json.JSONException;
import org.json.JSONObject;

public class Channel extends ListItem {
  public String director_;
  
  public Channel(String service_name, JSONObject object) throws JSONException {
    try {
      media_uri_ = new URI(
          service_name + "://" + object.getString("key"));
    } catch (URISyntaxException e) {
      // Ignore
    }
    
    try {
      image_url_ = new URL(object.getString("asset_url"));
    } catch (MalformedURLException e) {
      // Ignore
    }
    
    text1_ = object.getString("name");
    text2_ = object.optString("description");
    director_ = object.optString("channel_director");
  }
}
