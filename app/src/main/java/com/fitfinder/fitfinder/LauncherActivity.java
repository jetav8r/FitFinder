package com.fitfinder.fitfinder;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.fitfinder.fitfinder.activities.LoginActivity;
import com.fitfinder.fitfinder.activities.OnBoardActivity;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.utils.IntentUtils;
import com.fitfinder.fitfinder.utils.MessageService;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        TextView titleText = (TextView) findViewById(R.id.appTitleTextView);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Norican-Regular.ttf");
        titleText.setTypeface(font);
        titleText.setShadowLayer(10, 0, 0, Color.BLACK);
        //code to get SHA signature and place in Log
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.fitfinder.fitfinder",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        }

        /*ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();*/

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ParseUser.getCurrentUser() != null) {
                    if (ParseUser.getCurrentUser().isAuthenticated()) {
                        if (IntentUtils.checkIfAlreadyOnBoarded()) {
                            IntentUtils.mainIntent(LauncherActivity.this);
                        } else {
                            IntentUtils.onBoardIntent(LauncherActivity.this);
                        }
                    }
                } else {
                    Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
            }
        }, 3400);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
