package com.fitfinder.fitfinder.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Wayne on 9/27/2014.
 */
public class Message {
    public static void message(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
