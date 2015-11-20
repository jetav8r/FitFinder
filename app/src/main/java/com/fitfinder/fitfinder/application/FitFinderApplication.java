package com.fitfinder.fitfinder.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.TypefaceUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Created by Wayne on 4/23/2015.
 */
public class FitFinderApplication extends Application {

    SharedPreferences sharedPreferences;
    @Override
    public void onCreate() {
        super.onCreate();
        /*sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String fontKey = this.getString(R.string.pref_font);
        String fontValue = sharedPreferences.getString(fontKey, "Roboto-Regular.ttf");
        //set font
            //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Roboto-Regular.ttf");
             // font from assets: "assets/fonts/Roboto-Regular.ttf
        String currentFont = "fonts/" + fontValue;
        Log.e("MyApp", "font = " + currentFont);
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", currentFont);
        switch (currentFont) {
            case "bodoni-mt-black.ttf":
                TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/bodoni-mt-black.ttf");
                break;
            case "mvboli.ttf":
                TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/mvboli.ttf");
                break;
            case "Norican-Regular.ttf":
                TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Norican-Regular.ttf");
                break;
            case "Roboto-Regular.ttf":
                TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Roboto-Regular.ttf");
                break;
            default:
                TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Roboto-Regular.ttf");
                break;

        }*/
        // Enable Local Datastore.
        //Parse.enableLocalDatastore(this);
        Parse.initialize(this, Constants.APPLICATION_ID, Constants.CLIENT_KEY);
        //ParseUser.enableRevocableSessionInBackground();
        ParseFacebookUtils.initialize(this);
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground(Constants.MESSAGE_PUSH, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.d("MyApp", "Subscribed to push channel successfully");
            }
        });

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

    }

    public static void updateParseInstallation() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();

        installation.put(Constants.USER_ID, ParseUser.getCurrentUser().getObjectId());
        installation.saveInBackground();
        ParseUser.enableRevocableSessionInBackground();
    }

    public static ImageLoader getImageLoaderInstance() {
        return ImageLoader.getInstance();
    }
}
