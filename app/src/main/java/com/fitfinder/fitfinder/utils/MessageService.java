package com.fitfinder.fitfinder.utils;

import android.app.Service;
//import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fitfinder.fitfinder.activities.ChatActivity;
import com.fitfinder.fitfinder.activities.MessagingActivity;
import com.parse.ParseUser;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.NotificationResult;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.WritableMessage;

public class MessageService extends Service implements SinchClientListener {

    private static final String APP_KEY = "b7a15363-bcd8-4493-9a47-6c8c03319fbb";
    private static final String APP_SECRET = "ocB3aEBISEC9N0GdtuKABQ==";
    private static final String ENVIRONMENT = "sandbox.sinch.com";
    private final MessageServiceInterface serviceInterface = new MessageServiceInterface();
    private SinchClient sinchClient = null;
    //private SinchClient mSinchClient;
    private MessageClient messageClient = null;
    private String currentUserId;
    private LocalBroadcastManager broadcaster;
    private Intent broadcastIntent = new Intent("com.fitfinder.fitfinder.activities.ChatActivity");
    //private Context context;
    private PersistedSettings mSettings;
    private StartFailedListener mListener;


    @Override
    public void onCreate() {
        super.onCreate();
        mSettings = new PersistedSettings(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        currentUserId = ParseUser.getCurrentUser().getObjectId();
        if (currentUserId != null && !isSinchClientStarted()) {
            startSinchClient(currentUserId);
            sinchClient.startListeningOnActiveConnection();
        }
        broadcaster = LocalBroadcastManager.getInstance(this);
        return super.onStartCommand(intent, flags, startId);
    }

    /*private void createClient(String username) {
        mSinchClient = Sinch.getSinchClientBuilder().context(getApplicationContext()).userId(username)
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT).build();

        //mSinchClient.setSupportCalling(true);
        mSinchClient.setSupportManagedPush(true);
        mSinchClient.setSupportMessaging(true);
        //mSinchClient.checkManifest();

        mSinchClient.addSinchClientListener(new MySinchClientListener());
        //mSinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
    }*/


    public void startSinchClient(String username) {
        sinchClient = Sinch.getSinchClientBuilder().context(this).userId(username).applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET).environmentHost(ENVIRONMENT).build();
        sinchClient.addSinchClientListener(this);
        sinchClient.setSupportMessaging(true);
        sinchClient.setSupportActiveConnectionInBackground(true);
        sinchClient.setSupportPushNotifications(true);
        //sinchClient.setSupportManagedPush(true);
        // checkManifest call is only used during development to see if permissions are correct
        //sinchClient.checkManifest();
        sinchClient.start();
        sinchClient.startListeningOnActiveConnection();
        //Log.d("MyApp", "Message client = " + sinchClient);
    }

    private boolean isSinchClientStarted() {
        return sinchClient != null && sinchClient.isStarted();
    }

    @Override
    public void onClientFailed(SinchClient client, SinchError error) {
        broadcastIntent.putExtra("success", false);
        broadcaster.sendBroadcast(broadcastIntent);

        sinchClient = null;
    }

    @Override
    public void onClientStarted(SinchClient client) {
        broadcastIntent.putExtra("success", true);
        broadcaster.sendBroadcast(broadcastIntent);


        messageClient = client.getMessageClient();
        Log.d("MyApp", "Message client in onClientStarted() = " + messageClient);
    }

    @Override
    public void onClientStopped(SinchClient client) {
        sinchClient = null;
    }



    @Override
    public IBinder onBind(Intent intent) {
        return serviceInterface;
    }

    @Override
    public void onLogMessage(int level, String area, String message) {
    }

    @Override
    public void onRegistrationCredentialsRequired(SinchClient client, ClientRegistration clientRegistration) {
    }

    public void sendMessage(String recipientUserId, String textBody) {
        Log.d("MyApp", "messageClient = " + messageClient);
        if (messageClient != null) {
            WritableMessage message = new WritableMessage(recipientUserId, textBody);
            messageClient.send(message);
        }
    }

    public void addMessageClientListener(MessageClientListener listener) {
        if (messageClient != null) {
            messageClient.addMessageClientListener(listener);
        }
    }

    public void removeMessageClientListener(MessageClientListener listener) {
        if (messageClient != null) {
            messageClient.removeMessageClientListener(listener);
        }
    }

    @Override
    public void onDestroy() {
        sinchClient.stopListeningOnActiveConnection();
        sinchClient.terminate();
    }

    public class MessageServiceInterface extends Binder {
        public void sendMessage(String recipientUserId, String textBody) {
            MessageService.this.sendMessage(recipientUserId, textBody);
        }

        public void addMessageClientListener(MessageClientListener listener) {
            MessageService.this.addMessageClientListener(listener);
        }

        public void removeMessageClientListener(MessageClientListener listener) {
            MessageService.this.removeMessageClientListener(listener);
        }

        public boolean isSinchClientStarted() {
            return MessageService.this.isSinchClientStarted();
        }

        public NotificationResult relayRemotePushNotificationPayload(Intent intent) {
            if (sinchClient == null && !mSettings.getUsername().isEmpty()) {
                startSinchClient(mSettings.getUsername());
            } else if (sinchClient == null && mSettings.getUsername().isEmpty()) {
                Log.e("MyApp", "Can't start a SinchClient as no username is available, unable to relay push.");
                return null;
            }
            return sinchClient.relayRemotePushNotificationPayload(intent);
        }
    }

    public interface StartFailedListener {

        void onStartFailed(SinchError error);

        void onStarted();
    }

   /* private class MySinchClientListener implements SinchClientListener {

        @Override
        public void onClientFailed(SinchClient client, SinchError error) {
            if (mListener != null) {
                mListener.onStartFailed(error);
            }
            mSinchClient.terminate();
            mSinchClient = null;
        }

        @Override
        public void onClientStarted(SinchClient client) {
            Log.d(TAG, "SinchClient started");
            if (mListener != null) {
                mListener.onStarted();
            }
        }

        @Override
        public void onClientStopped(SinchClient client) {
            Log.d(TAG, "SinchClient stopped");
        }

        @Override
        public void onLogMessage(int level, String area, String message) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(area, message);
                    break;
                case Log.ERROR:
                    Log.e(area, message);
                    break;
                case Log.INFO:
                    Log.i(area, message);
                    break;
                case Log.VERBOSE:
                    Log.v(area, message);
                    break;
                case Log.WARN:
                    Log.w(area, message);
                    break;
            }
        }

        @Override
        public void onRegistrationCredentialsRequired(SinchClient client,
                                                      ClientRegistration clientRegistration) {
        }
    }*/

    private class PersistedSettings {

        private SharedPreferences mStore;

        private static final String PREF_KEY = "Sinch";

        public PersistedSettings(Context context) {
            mStore = context.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        }

        public String getUsername() {
            return mStore.getString("Username", "");
        }

        public void setUsername(String username) {
            SharedPreferences.Editor editor = mStore.edit();
            editor.putString("Username", username);
            editor.commit();
        }
    }
}