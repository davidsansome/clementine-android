package org.clementine_player.clementine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class BrowserItemCache {
  private static final String TAG = "BrowserItemCache";
  
  private Context context_;
  
  public BrowserItemCache(Context context) {
    context_ = context;
  }
  
  private String SanitiseKeyComponent(String key) {
    return key.replaceAll("[^a-zA-Z0-9_.-]", "_");
  }
  
  private String GetFilename(String provider_key, String key) {
    if (key == null) {
      return SanitiseKeyComponent(provider_key);
    }
    
    return SanitiseKeyComponent(provider_key) + "!" +
           SanitiseKeyComponent(key);
  }
  
  private File GetFile(String provider_key, String key) {
    return new File(context_.getCacheDir(), GetFilename(provider_key, key));
  }
  
  public PB.BrowserItemList Load(String provider_key, String key) {
    File file = GetFile(provider_key, key);
    PB.BrowserItemList ret;
    
    Log.d(TAG, "Lookup: " + file.getPath());
    
    try {
      ret = PB.BrowserItemList.parseFrom(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
    
    Log.d(TAG, "Cache hit: " + file.getPath());
    return ret;
  }
  
  public void Save(String provider_key, String key, PB.BrowserItemList data) {
    File file = GetFile(provider_key, key);
    
    Log.d(TAG, "Write: " + file.getPath());
    
    try {
      data.writeTo(new FileOutputStream(file));
    } catch (Exception e) {
    }
  }
}
