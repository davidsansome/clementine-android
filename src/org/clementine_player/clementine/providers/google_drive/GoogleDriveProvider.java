package org.clementine_player.clementine.providers.google_drive;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.clementine_player.clementine.Application;
import org.clementine_player.clementine.PB;
import org.clementine_player.clementine.Utils;
import org.clementine_player.clementine.PB.BrowserItem;
import org.clementine_player.clementine.PB.BrowserItemList;
import org.clementine_player.clementine.PB.Song;
import org.clementine_player.clementine.PB.SongList;
import org.clementine_player.clementine.R;
import org.clementine_player.clementine.providers.CachingItemLoader;
import org.clementine_player.clementine.providers.ProviderException;
import org.clementine_player.clementine.providers.ProviderInterface;
import org.clementine_player.clementine.providers.URLFetcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.OperationCanceledException;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDriveProvider implements ProviderInterface {
  private static final String TAG = GoogleDriveProvider.class.getCanonicalName();
  
  private CachingItemLoader itemLoader;
  private Drive driveService;

  @Override
  public String provider_key() {
    return "googledrive";
  }

  @Override
  public String[] uri_schemes() {
    return new String[] { "googledrive" };
  }

  @Override
  public BrowserItem provider_item() {
    PB.BrowserItem.Builder ret = PB.BrowserItem.newBuilder();
    ret.setText1("Google Drive");
    ret.getImageBuilder().setResource(R.drawable.googledrive);
    ret.setHasChildren(true);
    ret.setProviderClassName(getClass().getName());
    return ret.build();
  }

  @Override
  public Loader<BrowserItemList> LoadItems(Context context, String parent_key) {
    final Context c = context;
    if (parent_key == null) {
      if (CheckAuthentication(context)) {
        if (itemLoader == null) {
          itemLoader = new CachingItemLoader(context, provider_key()) {
            @Override
            public PB.BrowserItemList loadInBackground() 
                throws OperationCanceledException {
              return GetFiles(c);
            }
          };
        }
        return itemLoader;
      }
    }
    return null;
  }
  
  private BrowserItemList GetFiles(Context context) {
    PB.BrowserItemList.Builder builder = PB.BrowserItemList.newBuilder();
    try {
      Files.List request = GetDriveService(context).files().list();
      request.setQ("mimeType = 'audio/mpeg'");
      FileList files = request.execute();
      Log.d(TAG, files.getItems().get(0).getTitle());
      
      for (File file : files.getItems()) {
        final String uri = "googledrive:" + file.getId();
        builder.addItemsBuilder()
            .setText1(file.getTitle())
            .setMediaUri(uri);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return builder.build();
  }
  
  private boolean CheckAuthentication(Context context) {
    String accessToken = GetAccessToken(context);
    if (accessToken == null) {
      context.startActivity(new Intent(context, GoogleDriveAuthenticationActivity.class));
    }
    return accessToken != null;
  }
  
  private String GetAccessToken(Context context) {
    SharedPreferences prefs = context.getSharedPreferences("googledrive", Context.MODE_PRIVATE);
    return prefs.getString("access_token", null);
  }

  @Override
  public Loader<SongList> LoadSongs(Context context, final BrowserItemList items) {
    return new Loader<PB.SongList>(context) {
      @Override
      protected void onStartLoading() {
        PB.SongList.Builder ret = PB.SongList.newBuilder();
        
        for (PB.BrowserItem item : items.getItemsList()) {
          Song.Builder builder = ret.addSongsBuilder();
          
          builder.setTitle(item.getText1());
          builder.setUri(item.getMediaUri());
        }
        
        deliverResult(ret.build());
      }
    };
  }

  @Override
  public Loader<URL> ResolveURI(final Context context, final URI url) {
    Log.i(TAG, "Resolving " + url);
    
    return new AsyncTaskLoader<URL>(context) {
      @Override
      protected void onStartLoading() {
        forceLoad();
      }

      @Override
      public URL loadInBackground() {
        String id = url.toString().split(":")[1];
        Log.d(TAG, "Resolved to id: " + id);
        GoogleCredential credentials = new GoogleCredential();
        credentials.setAccessToken(GetAccessToken(context));
       
        try {
          Drive.Files.Get request = GetDriveService(context).files().get(id);
          File file = request.execute();
          return new URL(file.getDownloadUrl() + "&access_token=" + GetAccessToken(context));
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return null;
      }
    };
  }
  
  private Drive GetDriveService(Context context) {
    if (driveService == null) {
      GoogleCredential credentials = new GoogleCredential();
      credentials.setAccessToken(GetAccessToken(context));
      driveService = new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credentials).build(); 
    }
    return driveService;
  }
  
}
