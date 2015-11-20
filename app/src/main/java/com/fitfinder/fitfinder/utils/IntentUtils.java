package com.fitfinder.fitfinder.utils;

/**
 * Created by Wayne on 4/23/2015.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.activities.ChatActivity;
import com.fitfinder.fitfinder.activities.MainActivity;
import com.fitfinder.fitfinder.activities.OnBoardActivity;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.parse.ParseUser;

/**
* This class holds the intents used for login and the launcher activity
*/
public class IntentUtils {

    /**
     * This method  checks if the user has already gone through the onboard process and either
     * sends them to onboarding if they have not or skips that step and brings them to the
     * Main Activity
     *
     * @return Boolean true or false depending on if the user has onboarded or not
     */
    public static Boolean checkIfAlreadyOnBoarded() {
        return (Boolean) ParseUser.getCurrentUser().get(Constants.ALREADY_ONBOARD);
    }

    /**
     * This method  sends the user to onboarding using an intent and also set the current facebook
     * user id in the shared preferences.
     */
    public static void onBoardIntent(Context context) {
        final Intent serviceIntent = new Intent(context.getApplicationContext(), MessageService.class);
        context.startService(serviceIntent);

        FitFinderApplication.updateParseInstallation();

        Intent intent = new Intent(context, OnBoardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    /**
     * This method  sends the user to the main activity using an intent
     */
    public static void mainIntent(Context context) {
        final Intent serviceIntent = new Intent(context.getApplicationContext(), MessageService.class);
        context.startService(serviceIntent);

        FitFinderApplication.updateParseInstallation();

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }
}