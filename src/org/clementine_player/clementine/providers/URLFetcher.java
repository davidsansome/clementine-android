package org.clementine_player.clementine.providers;

import java.io.IOException;
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

  public URLFetcher(URL url) throws ProviderException {
    try {
      connection_ = url.openConnection();
    } catch (IOException e) {
      throw new ProviderException("Unable to open URL " + url, e);
    }
    
    connection_.setRequestProperty(
        "User-Agent", Application.instance().user_agent());
  }
  
  public URLConnection connection() {
    return connection_;
  }
  
  public String GetData() throws ProviderException {
    try {
      HttpURLConnection http_connection = (HttpURLConnection) connection_;
      
      if (http_connection.getResponseCode() != 200) {
        throw new ProviderException(
            "Unexpected HTTP response code " +
             http_connection.getResponseCode() +
             " while fetching URL " +
             connection_.getURL());
      }
      
      String content_type = connection_.getHeaderField("Content-Type");
      String charset = null;
      for (String param : content_type.replace(" ", "").split(";")) {
        if (param.startsWith("charset=")) {
          charset = param.split("=", 2)[1];
          break;
        }
      }
      
      char[] buffer = new char[4096];
      StringBuilder out = new StringBuilder();
    
      Reader reader;
      if (charset != null) {
        reader = new InputStreamReader(connection_.getInputStream(), charset);
      } else {
        reader = new InputStreamReader(connection_.getInputStream());
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
      Log.i("URLFetcher", "Data: " + out.toString());
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
