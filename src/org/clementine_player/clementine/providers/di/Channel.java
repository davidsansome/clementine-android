package org.clementine_player.clementine.providers.di;

import java.net.MalformedURLException;
import java.net.URL;

import org.clementine_player.clementine.providers.ListItem;
import org.json.JSONException;
import org.json.JSONObject;

public class Channel extends ListItem {
  public URL art_url_;
  public String director_;
  public String description_;
  public String name_;
  public String key_;
  
  public Channel(String service_name, JSONObject object) throws JSONException {
    try {
      art_url_ = new URL(object.getString("asset_url"));
    } catch (MalformedURLException e) {
      // Ignore this
    }
    
    description_ = object.optString("description");
    director_ = object.optString("channel_director");
    key_ = object.getString("key");
    name_ = object.getString("name");
    
    text_ = name_;
    url_ = "di://" + service_name + "/" + key_;
    has_children_ = false;
  }
}
