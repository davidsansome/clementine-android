package org.clementine_player.clementine.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.clementine_player.clementine.Application;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class URLFetcher {
  private URLConnection connection_;
  private String charset_;
  
  private static final String TAG = "URLFetcher";

  public URLFetcher(URL url) throws ProviderException {
    try {
      connection_ = url.openConnection();
    } catch (IOException e) {
      throw new ProviderException("Unable to open URL " + url, e);
    }
    
    connection_.setRequestProperty(
        "User-Agent", Application.instance().user_agent());
    
    Log.i(TAG, "Fetching URL " + url);
  }
  
  public URLConnection connection() {
    return connection_;
  }
  
  public InputStream GetStream() throws ProviderException {
    HttpURLConnection http_connection = (HttpURLConnection) connection_;
    
    try {
      if (http_connection.getResponseCode() != 200) {
        throw new ProviderException(
            "Unexpected HTTP response code " +
             http_connection.getResponseCode() +
             " while fetching URL " +
             connection_.getURL());
      }
      
      String content_type = connection_.getHeaderField("Content-Type");
      for (String param : content_type.replace(" ", "").split(";")) {
        if (param.startsWith("charset=")) {
          charset_ = param.split("=", 2)[1];
          break;
        }
      }
    
      return connection_.getInputStream();
    } catch (IOException e) {
      throw new ProviderException(
          "Failed to get input stream for URL " + connection_.getURL(), e);
    } 
  }
  
  public String GetData() throws ProviderException {
    InputStream stream = GetStream();
      
    try {
      char[] buffer = new char[4096];
      StringBuilder out = new StringBuilder();
    
      Reader reader;
      if (charset_ != null) {
        reader = new InputStreamReader(stream, charset_);
      } else {
        reader = new InputStreamReader(stream);
      }
      
      try {
        for (;;) {
          int rsz = reader.read(buffer, 0, buffer.length);
          if (rsz < 0)
            break;
          out.append(buffer, 0, rsz);
        }
      } finally {
        reader.close();
      }
      return out.toString();
    } catch (IOException e) {
      throw new ProviderException(
          "Failed to read data from URL " + connection_.getURL(), e);
    }
  }
  
  public JSONObject GetJSONObject() throws ProviderException {
    JSONTokener tokener = new JSONTokener(GetData());
    try {
      return (JSONObject) tokener.nextValue();
    } catch (JSONException e) {
      throw new ProviderException("Invalid JSON in response", e);
    }
  }
}
