package com.fitfinder.fitfinder.utils;

/**
 * Created by Wayne on 4/24/2015.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fitfinder.fitfinder.activities.MessagingActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomPushBroadcastReceiver extends BroadcastReceiver {

    public CustomPushBroadcastReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        if(data != null) {
            String jsonData = data.getString(Constants.PARSE_DATA);
            try {
                JSONObject jsonObject = new JSONObject(jsonData);
                String id = jsonObject.getString(Constants.PUSH_ID);
                String name = jsonObject.getString(Constants.PUSH_NAME);

                Intent myIntent = new Intent(context, MessagingActivity.class);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                myIntent.putExtra(Constants.RECIPIENT_ID, id);
                myIntent.putExtra(Constants.RECIPIENT_NAME, name);
                context.startActivity(myIntent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}