package org.clementine_player.clementine.providers.google_drive;

import java.io.IOException;

import org.clementine_player.clementine.R;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;

public class GoogleDriveAuthenticationActivity extends FragmentActivity {

  private static final String TAG = GoogleDriveAuthenticationActivity.class.getCanonicalName();
  private static final int ACCOUNT_PICKER_REQUEST_CODE = 42;
  private static final int AUTHORIZATION_REQUEST_CODE = 43;
  private static final String OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.readonly";

  private Button authenticateButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.googledrive);
    authenticateButton = (Button) findViewById(R.id.authenticate);
    authenticateButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        Intent intent =
            AccountPicker.newChooseAccountIntent(null, null, new String[] {"com.google"}, false,
                null, null, null, null);
        startActivityForResult(intent, ACCOUNT_PICKER_REQUEST_CODE);
      }
    });
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == ACCOUNT_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
      String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      Log.d(TAG, "Found account:" + accountName);
      final Context context = this;
      new AsyncTask<String, Void, String>() {
        @Override
        protected String doInBackground(String... params) {
          return getToken(params[0]);
        }
        
        @Override
        protected void onPostExecute(String result) {
          if (result == null) {
            Toast.makeText(context, "Failed to authenticate to Google Drive", Toast.LENGTH_SHORT).show();
          } else {
            Log.d(TAG, "Access token:" + result);
            SharedPreferences prefs = getSharedPreferences("googledrive", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("access_token", result);
            editor.commit();
          }
        }
      }.execute(accountName);
    } else if (requestCode == AUTHORIZATION_REQUEST_CODE && resultCode == RESULT_OK) {
      Log.d(TAG, data.getExtras().toString());
    }
  }

  private String getToken(String accountName) {
    try {
      return GoogleAuthUtil.getToken(this, accountName, OAUTH_SCOPE);
    } catch (IOException e) {
      Log.d(TAG, e.toString());
    } catch (UserRecoverableAuthException e) {
      startActivityForResult(e.getIntent(), AUTHORIZATION_REQUEST_CODE);
    } catch (GoogleAuthException e) {
      Log.d(TAG, e.toString());
    }
    return null;
  }

}
