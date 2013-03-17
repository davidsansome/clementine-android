package org.clementine_player.clementine;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {
  @Override
  public void onCreate(Bundle saved_instance_state) {
    super.onCreate(saved_instance_state);
    setContentView(R.layout.main);
    
    if (saved_instance_state == null) {
      TopLevelBrowserFragment fragment = new TopLevelBrowserFragment();
      
      FragmentTransaction transaction =
          getSupportFragmentManager().beginTransaction();
      transaction.add(R.id.browser, fragment);
      transaction.commit();
    }
  }
}
