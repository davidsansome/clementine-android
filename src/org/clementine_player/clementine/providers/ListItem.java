package org.clementine_player.clementine.providers;

import java.net.URI;
import java.net.URL;

public class ListItem {
  // Lines of text to display in the list view. 
  public String text1_;
  public String text2_;
  
  // If this item is a song, the URI that will be used to resolve to the actual
  // audio content.
  public URI media_uri_;
  
  // The URL to an icon to display in the list view.
  public URL image_url_;
  
  public boolean has_children_;
  public String key_;
}
