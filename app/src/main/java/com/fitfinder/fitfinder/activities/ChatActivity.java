package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.adapters.ChatListAdapter;
import com.fitfinder.fitfinder.utils.ConnectionDetector;
import com.fitfinder.fitfinder.utils.Constants;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private SharedPreferences sharedPreferences;
    private ParseUser mCurrentUser;
    private ChatListAdapter mAdapter;
    private List<ParseObject> mChats;
    private Toolbar mToolbar;


    @InjectView(R.id.chatList) ListView mListView;
    @InjectView(R.id.emptyView) TextView mEmptyView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.inject(this);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.chats);
        /*ConnectionDetector detector = new ConnectionDetector(this);
        if(!detector.isConnectingToInternet()) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }
        else {
            mProgressBar.setVisibility(View.VISIBLE);
            mCurrentUser = ParseUser.getCurrentUser();
            mListView.setOnItemClickListener(this);

            ParseQuery<ParseObject> query1 = ParseQuery.getQuery(Constants.RELATION);
            query1.whereEqualTo(Constants.USER1, mCurrentUser);

            ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Constants.RELATION);
            query2.whereEqualTo(Constants.USER2, mCurrentUser);

            List<ParseQuery<ParseObject>> queries = new ArrayList<>();
            queries.add(query1);
            queries.add(query2);

            ParseQuery<ParseObject> query = ParseQuery.or(queries);
            query.include(Constants.USER1);
            query.include(Constants.USER2);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    mProgressBar.setVisibility(View.GONE);
                    if (e == null) {
                        if (parseObjects.isEmpty()) {
                            mEmptyView.setVisibility(View.VISIBLE);
                        }
                        else {
                            mEmptyView.setVisibility(View.GONE);
                            mChats = parseObjects;
                            mAdapter = new ChatListAdapter(ChatActivity.this, mChats);
                            mListView.setAdapter(mAdapter);

                        }
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        }*/
    }
    @Override
    public void onResume() {
        super.onResume();
        ConnectionDetector detector = new ConnectionDetector(this);
        if(!detector.isConnectingToInternet()) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }
        else {
            mProgressBar.setVisibility(View.VISIBLE);
            mCurrentUser = ParseUser.getCurrentUser();
            mListView.setOnItemClickListener(this);

            ParseQuery<ParseObject> query1 = ParseQuery.getQuery(Constants.RELATION);
            query1.whereEqualTo(Constants.USER1, mCurrentUser);

            ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Constants.RELATION);
            query2.whereEqualTo(Constants.USER2, mCurrentUser);

            List<ParseQuery<ParseObject>> queries = new ArrayList<>();
            queries.add(query1);
            queries.add(query2);

            ParseQuery<ParseObject> query = ParseQuery.or(queries);
            query.include(Constants.USER1);
            query.include(Constants.USER2);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    mProgressBar.setVisibility(View.GONE);
                    if (e == null) {
                        if (parseObjects.isEmpty()) {
                            mEmptyView.setVisibility(View.VISIBLE);
                        }
                        else {
                            mEmptyView.setVisibility(View.GONE);
                            mChats = parseObjects;
                            mAdapter = new ChatListAdapter(ChatActivity.this, mChats);
                            mListView.setAdapter(mAdapter);

                        }
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ParseUser user1 = (ParseUser) mChats.get(position).get(Constants.USER1);
        String userId = user1.getObjectId();
        String relationId = mChats.get(position).getObjectId();

        ParseUser user;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (userId.equals(mCurrentUser.getObjectId())) {
            user = (ParseUser) mChats.get(position).get(Constants.USER2);
        }
        else {
            user = (ParseUser) mChats.get(position).get(Constants.USER1);
        }


        Intent intent = new Intent(this, MessagingActivity.class);
        intent.putExtra(Constants.RECIPIENT_ID, user.getObjectId());
        intent.putExtra(Constants.RECIPIENT_NAME, (String) user.get(Constants.NAME));
        intent.putExtra(Constants.LOCATION, (String) user.get(Constants.LOCATION));
        intent.putExtra(Constants.ABOUT_ME, (String) user.get(Constants.ABOUT_ME));
        intent.putExtra(Constants.LOOKING_FOR, (String) user.get(Constants.LOOKING_FOR));
        //intent.putExtra(Constants.PROFILE_IMAGE,  (String) user.get(Constants.PROFILE_IMAGE));
        Log.e("MyApp", "profImage = " + user.get(Constants.PROFILE_IMAGE));

        intent.putExtra(Constants.OBJECT_ID, relationId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {
            Intent intent = new Intent(ChatActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }

        return super.onOptionsItemSelected(item);
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            Intent intent = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }

        return super.onOptionsItemSelected(item);
    }*/
    @Override
    public void onBackPressed() {
        /*Intent intent = new Intent(ChatActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);*/
        super.onBackPressed();
    }

}

