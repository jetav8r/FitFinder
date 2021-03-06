package com.fitfinder.fitfinder.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.fitfinder.fitfinder.activities.ChatActivity;
import com.fitfinder.fitfinder.utils.MessageService;
import com.sinch.android.rtc.NotificationResult;
import com.sinch.android.rtc.SinchHelpers;


public class GcmIntentService extends IntentService implements ServiceConnection {

    private Intent mIntent;
    private NotificationManager mNotificationManager;
    // Sets an ID for the notification
    int mNotificationId = 001;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (SinchHelpers.isSinchPushIntent(intent)) {
            mIntent = intent;
            connectToService();
        } else {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void connectToService() {
        getApplicationContext().bindService(new Intent(this, MessageService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (mIntent == null) {
            return;
        }

        if (SinchHelpers.isSinchPushIntent(mIntent)) {
            MessageService.MessageServiceInterface messageService = (MessageService.MessageServiceInterface) iBinder;
            if (messageService != null) {
                NotificationResult result = messageService.relayRemotePushNotificationPayload(mIntent);
                // handle result, e.g. show a notification or similar
                mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, ChatActivity.class), 0);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setContentTitle("New Message!");
                mBuilder.setContentIntent(contentIntent);
                mNotificationManager.notify(mNotificationId, mBuilder.build());
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(mIntent);
        mIntent = null;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

}