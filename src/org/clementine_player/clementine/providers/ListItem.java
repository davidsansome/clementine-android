package org.clementine_player.clementine.providers;

public class ListItem {
  public String text_;
  public String url_;
  public boolean has_children_;
  
  public ListItem() {
  }
  
  public ListItem(String text, String url, boolean has_children) {
    text_ = text;
    url_ = url;
    has_children_ = has_children;
  }
}
