package com.fitfinder.fitfinder.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fitfinder.fitfinder.fragments.FitFinderFragment;
import com.fitfinder.fitfinder.fragments.MarkerDialogFragment;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.adapters.MessageAdapter;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.MessageService;
import com.parse.PushService;
import com.parse.SaveCallback;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;
import com.sinch.android.rtc.messaging.WritableMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MessagingActivity extends BaseActivity {

    //todo: exit out if other user deletes connection

    private ParseUser mUser;
    private String recipientId;
    private EditText messageBodyField;
    private String messageBody;
    private MessageService.MessageServiceInterface messageService;
    private MessageAdapter messageAdapter;
    private ListView messagesList;
    private String currentUserId;
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private MyMessageClientListener messageClientListener = new MyMessageClientListener();
    private String mRecipientName;
    private String mRelationId;
    private Toolbar mToolbar;
    private View mSendMessage;
    private HashMap chatRecipient;


    private FitFinderFragment mFitFinderFragment;
    @InjectView(R.id.chatFrag) CardView mCardView;


    private String mLocation;
    private String mAboutMe;
    private String mLookingFor;
    private String mProfilePic;
    public static Boolean chatWindowShowing;
    private boolean fromMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        chatWindowShowing = true;
        ButterKnife.inject(this);
        bindService(new Intent(this, MessageService.class), serviceConnection, BIND_AUTO_CREATE);

        // Need to unsubscribe from push notifications when this is open

        ParsePush.unsubscribeInBackground("message", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("MyApp", "successfully unsubscribed to the broadcast channel.");
                } else {
                    Log.e("MyApp", "failed to unsubscribe for push" + e);
                }
            }
        });

        //get recipientId from the intent
        Intent intent = getIntent();
        fromMarker = intent.getBooleanExtra("fromMarker", false);
        recipientId = intent.getStringExtra(Constants.RECIPIENT_ID);
        mRecipientName = intent.getStringExtra(Constants.RECIPIENT_NAME);
        mRelationId = intent.getStringExtra(Constants.OBJECT_ID);
        mLocation = intent.getStringExtra(Constants.LOCATION);
        mAboutMe = intent.getStringExtra(Constants.ABOUT_ME);
        mLookingFor = intent.getStringExtra(Constants.LOOKING_FOR);
        mProfilePic = intent.getStringExtra(Constants.PROFILE_IMAGE);
        currentUserId = ParseUser.getCurrentUser().getObjectId();

        mFitFinderFragment = new FitFinderFragment(); //initialize the fragment

        mToolbar = (Toolbar) findViewById(R.id.app_bar);


        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(mRecipientName);
        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //put code to show user profile again here
                //optionally could put a menu item in toolbar
            }
        });

        mSendMessage = findViewById(R.id.relSendMessage);
        messagesList = (ListView) findViewById(R.id.listMessages);
        messageAdapter = new MessageAdapter(this);
        messagesList.setAdapter(messageAdapter);
        populateMessageHistory();

        messageBodyField = (EditText) findViewById(R.id.messageBodyField);

        //listen for a click on the send button
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    /**
     * This method is called when the messaging activity is opened. It queries the parse backend
     * for all previous messages and displayes them in the adapter
     */
    private void populateMessageHistory() {
        String[] userIds = {currentUserId, recipientId};
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.PARSE_MESSAGE);
        query.whereContainedIn(Constants.SENDER_ID, Arrays.asList(userIds));
        query.whereContainedIn(Constants.ID_RECIPIENT, Arrays.asList(userIds));
        query.orderByAscending(Constants.CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < messageList.size(); i++) {
                        WritableMessage message =
                                new WritableMessage(messageList.get(i).get(Constants.ID_RECIPIENT).toString(),
                                        messageList.get(i).get(Constants.MESSAGE_TEXT).toString());
                        Format formatter = new SimpleDateFormat("MM/dd HH:mm");
                        message.addHeader(Constants.DATE, formatter.format(messageList.get(i).getCreatedAt()));
                        if (messageList.get(i).get(Constants.SENDER_ID).toString().equals(currentUserId)) {
                            messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING, mRecipientName);
                        } else {
                            messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING, mRecipientName);
                        }
                    }
                }
            }
        });
    }

    private void sendMessage() {
        messageBody = messageBodyField.getText().toString();
        if (messageBody.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_LONG).show();
            return;
        }

        messageService.sendMessage(recipientId, messageBody);
        Log.d("MyApp", "MessageServiceInterface object = " + messageService);
        messageBodyField.setText("");
    }

    //unbind the service when the activity is destroyed
    @Override
    public void onDestroy() {
        messageService.removeMessageClientListener(messageClientListener);
        unbindService(serviceConnection);
        //PushService.startServiceIfRequired(this);
        ParsePush.subscribeInBackground("message", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("MyApp", "successfully re-subscribed to the broadcast channel.");
                } else {
                    Log.e("MyApp", "failed to re-subscribe for push" + e);
                }
            }
        });
        super.onDestroy();
    }

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            messageService = (MessageService.MessageServiceInterface) iBinder;
            messageService.addMessageClientListener(messageClientListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messageService = null;
        }
    }

    private class MyMessageClientListener implements MessageClientListener {
        //Notify the user if their message failed to send
        @Override
        public void onMessageFailed(MessageClient client, Message message,
                                    MessageFailureInfo failureInfo) {
            com.fitfinder.fitfinder.utils.Message.message(MessagingActivity.this,
                    getString(R.string.toast_message_send_failed));
            /*Toast.makeText(MessagingActivity.this,
                    getString(R.string.toast_message_send_failed), Toast.LENGTH_LONG).show();*/
        }

        @Override
        public void onIncomingMessage(MessageClient client, Message message) {
            if (message.getSenderId().equals(recipientId)) {
                WritableMessage writableMessage =
                        new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
                Format formatter = new SimpleDateFormat("MM/dd HH:mm");
                writableMessage.addHeader(Constants.DATE, formatter.format(new Date()));
                messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_INCOMING, mRecipientName);
            }
        }

        @Override
        public void onMessageSent(MessageClient client, final Message message, String recipientId) {
            Log.d("MyApp", "onMessageSent called" + message);
            final WritableMessage writableMessage =
                    new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
            Format formatter = new SimpleDateFormat("MM/dd HH:mm");
            writableMessage.addHeader(Constants.DATE, formatter.format(new Date()));
            Log.d("MyApp", "Sinch message Id = " + message.getMessageId());
            //need to add messageAdapter code below if not using Parse Query for history
            //messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_OUTGOING,
                   // mRecipientName);
            //only add message to parse database if it doesn't already exist there
            ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.PARSE_MESSAGE);
            query.whereEqualTo(Constants.SINCH_ID, message.getMessageId());
            //I'll try to add some more code to see if I can avoid the duplication of messages
            //query.whereEqualTo("createdAt", message.getTimestamp());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> messageList, com.parse.ParseException e) {
                    if (e == null) {
                        if (messageList.size() == 0) {
                            ParseObject parseMessage = new ParseObject(Constants.PARSE_MESSAGE);
                            parseMessage.put(Constants.SENDER_ID, currentUserId);
                            parseMessage.put(Constants.ID_RECIPIENT, writableMessage.getRecipientIds().get(0));
                            parseMessage.put(Constants.MESSAGE_TEXT, writableMessage.getTextBody());
                            parseMessage.put(Constants.SINCH_ID, message.getMessageId());
                            parseMessage.saveInBackground();

                            messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_OUTGOING,
                                    mRecipientName);

                            try {
                                sendPushNotification();
                            } catch (JSONException error) {
                                error.printStackTrace();
                            }
                        }
                    }
                }
            });


            /*try {
                sendPushNotification();
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
        }

        //Do you want to notify your user when the message is delivered?
        @Override
        public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {
            /*try {
                sendPushNotification();
            } catch (JSONException e) {
                e.printStackTrace();
            }*/

            Log.d("MyApp", "The message with id "+deliveryInfo.getMessageId()
                    +" was delivered to the recipient with id"+ deliveryInfo.getRecipientId());
            Log.d("MyApp", "onMessageDelivered called for " + client);
        }

        @Override
        public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {
            Log.d("MyApp", "onShouldSendPushData called for message: " + message);
        }
    }

    /**
     * This method handles the push notifications sent when a message is sent. The chat is opened
     * when the notification is clicked on
     */
    private void sendPushNotification() throws JSONException {
        ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
        query.whereEqualTo(Constants.USER_ID, recipientId);
        query.whereEqualTo(Constants.CHANNELS, Constants.MESSAGE_PUSH);

        JSONObject data = new JSONObject();
        data.put(Constants.PUSH_ALERT, "You have a message from " +
                ParseUser.getCurrentUser().get(Constants.NAME) + "!");
        data.put(Constants.PUSH_ID, ParseUser.getCurrentUser().getObjectId());
        data.put(Constants.PUSH_NAME, ParseUser.getCurrentUser().get(Constants.NAME));


        ParsePush push = new ParsePush();
        push.setQuery(query);
        push.setData(data);
        push.sendInBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_messaging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_delete) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.dialog_title_delete_connection))
                    .content(getString(R.string.dialog_content_delete_connection))
                    .positiveText(getString(R.string.dialog_positive_delete_connection))
                    .negativeText(getString(R.string.dialog_negative))
                    .negativeColorRes(R.color.primary_text)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);

                            ParseObject.createWithoutData(Constants.RELATION, mRelationId).deleteEventually();

                            Intent intent = new Intent(MessagingActivity.this, ChatActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
                        }
                    })
                    .show();
        } else if (item.getItemId() == R.id.action_show_profile) {

            showProfile(recipientId);

            return true;
        }
        else if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProfile (String recipientId){

        mSendMessage.setVisibility(View.GONE);

        //setting the argument to pass the recipient ID to the fragment
        Bundle bundle = new Bundle();
        bundle.putString("userId", recipientId);
        mFitFinderFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                //.replace(R.id.chatFrag, mFitFinderFragment)
                .replace(R.id.chatFrag, mFitFinderFragment)
                .commit();


        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(Constants.OBJECT_ID, recipientId);
        Log.e ("MyApp" , "Recipient object Id = " + recipientId);

        int count = 0;
        try {
            count = query.count();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e == null) {
                    Log.e("myapp", "number of returned users = " + parseUsers.size());
                    if (!parseUsers.isEmpty() && parseUsers != null) {
                        /*for (int i = 0; i < parseUsers.size(); i++){
                            mUser = parseUsers.get(i);
                            Log.e("myapp", "user id="+mUser.getObjectId() );
                        }*/


                        /*//mAcceptButton.setEnabled(true);
                        mAcceptButton.setVisibility(View.VISIBLE);
                        //mRejectButton.setEnabled(true);
                        mRejectButton.setVisibility(View.VISIBLE);*/

                        mUser = parseUsers.get(0);

                        String name = (String) mUser.get(Constants.NAME);
                        String age = (String) mUser.get(Constants.AGE);
                        String location = (String) mUser.get(Constants.LOCATION);
                        String aboutMe = (String) mUser.get(Constants.ABOUT_ME);
                        String lookingFor = (String) mUser.get(Constants.LOOKING_FOR);
                        /*Boolean smokes = (Boolean) mUser.get(Constants.SMOKES);
                        Boolean drinks = (Boolean) mUser.get(Constants.DRINKS);*/
                        ParseFile profImage = (ParseFile) mUser.get(Constants.PROFILE_IMAGE);

                        if (mCardView.getVisibility() == View.GONE) {
                            mCardView.setVisibility(View.VISIBLE);
                        }
                        //mCardView.setVisibility(View.VISIBLE);

                        mFitFinderFragment.resetFields();
                        //mCardView.startAnimation(mExpandIn);

                        mFitFinderFragment.setName(name);
                        mFitFinderFragment.setAge(age);
                        mFitFinderFragment.setLocation(location);
                        mFitFinderFragment.setAboutMe(aboutMe);
                        mFitFinderFragment.setLookingFor(lookingFor);
                        mFitFinderFragment.setProfImage(profImage);
                        /*mFitFinderFragment.setSmokes(smokes);
                        mFitFinderFragment.setDrinks(drinks);*/
                        mFitFinderFragment.setFields();
                    }
                    else {
                        //setEmptyView();
                        Log.e("MyApp", "Query returned zero result");
                    }
                }
            }
        });
    }
    public void onBackPressed() {
        if (fromMarker == true) {
            fromMarker = false;
            Intent intent = new Intent(MessagingActivity.this, SearchMapActivity.class);
            intent.putExtra("fromMarker2", true);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
