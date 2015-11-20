package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.fragments.FitFinderFragment;
import com.fitfinder.fitfinder.utils.ConnectionDetector;
import com.fitfinder.fitfinder.utils.Constants;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SearchActivity extends BaseActivity implements View.OnClickListener {

    private ParseUser mCurrentUser;
    private ParseUser mUser;
    private List<String> mCurrentRelations;
    private List<String> mPotentialRelations;
    private List<String> mNegativeRelations;

    private Animation mSlideOutRight;
    private Animation mSlideOutLeft;
    private Animation mExpandIn;
    private List<String> mIndices = new ArrayList<>();
    private FitFinderFragment mFitFinderFragment;
    private Toolbar mToolbar;

    @InjectView(R.id.accept_button) View mAcceptButton;
    @InjectView(R.id.rejectButton) View mRejectButton;
    @InjectView(R.id.emptyView) TextView mEmptyView;
    @InjectView(R.id.undiscoverableView) TextView mUndiscoverable;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.fitFinderFrag) CardView mCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.inject(this);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.searchResults);

        if (checkConnection()) return;

        Boolean discoverable = (Boolean) ParseUser.getCurrentUser().get(Constants.DISCOVERABLE);
        if(!discoverable) {
            mUndiscoverable.setVisibility(View.VISIBLE);
            mCardView.setVisibility(View.GONE);
            return;
        }

        mEmptyView.setOnClickListener(this);

        mFitFinderFragment = new FitFinderFragment(); //initialize the fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.fitFinderFrag, mFitFinderFragment)
                .commit();

        mCurrentUser = ParseUser.getCurrentUser();

        setAnimations();
        //previousRelationQuery();
        potentialRelationQuery();

        mAcceptButton.setOnClickListener(this);
        mRejectButton.setOnClickListener(this);
    }

    /**
     * This method sets the animations and listeners for the card animations used in this activity
     */
    private void setAnimations() {
        mExpandIn = AnimationUtils.loadAnimation(this, R.anim.card_expand_in);

        mSlideOutRight = AnimationUtils.loadAnimation(this, R.anim.card_slide_out_right);
        mSlideOutRight.setFillAfter(true);

        mSlideOutLeft = AnimationUtils.loadAnimation(this, R.anim.card_slide_out_left);
        mSlideOutLeft.setFillAfter(true);
    }

    @Override
    public void onClick(View v) {
        if(v == mAcceptButton) {
            mCardView.startAnimation(mSlideOutLeft);
            fitFinderRequestQuery();

        }
        else if(v == mRejectButton){
            mCardView.startAnimation(mSlideOutRight);
            saveNegativeRelation(mCurrentUser,mUser);
            potentialRelationQuery();
        }
        else if(v == mEmptyView) {
            mProgressBar.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                    previousRelationQuery();
                }
            }, 1000);
        }
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
            Intent intent = new Intent(SearchActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * This method is called when the user accepts or rejects a FitFinder card. It creates a list
     * of users that the current user is already in a relation with and adds them to a list. It
     * uses that list to exclude those users from the query
     */
    private void previousRelationQuery() {
        if (checkConnection()) return;

        //I also need to check to see if a "one sided relation exists - where the user has sent a request, but
        //it has not been accepted/rejected on the other side, which would establish a relation.  If the relation
        //doesn't exist yet, the current user could be shown the same potential match again and again.  I need to
        //search the FitFinder Request object for all users (receiver) that have been sent requests from current
        // user (sender) and add to the exclusion list as well

        //potentialRelationQuery();

        mCurrentRelations = new ArrayList<>();
        ParseQuery<ParseObject> query1 = ParseQuery.getQuery(Constants.RELATION);
        query1.whereEqualTo(Constants.USER1, mCurrentUser);
        ParseQuery<ParseObject> query2 = ParseQuery.getQuery(Constants.RELATION);
        query2.whereEqualTo(Constants.USER2, mCurrentUser);

        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(query1);
        queries.add(query2);

        ParseQuery<ParseObject> relationQuery = ParseQuery.or(queries);
        relationQuery.include(Constants.USER1);
        relationQuery.include(Constants.USER2);
        relationQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if(e == null) {
                    mCurrentRelations.clear();
                    for (int i = 0; i < parseObjects.size(); i++) {
                        Log.d("MyApp","current relation objects = " + parseObjects.size());
                        ParseUser user1 = (ParseUser) parseObjects.get(i).get(Constants.USER1);
                        String userId = user1.getObjectId();
                        ParseUser user;
                        if (userId.equals(mCurrentUser.getObjectId())) {
                            user = (ParseUser) parseObjects.get(i).get(Constants.USER2);
                        } else {
                            user = (ParseUser) parseObjects.get(i).get(Constants.USER1);
                        }
                        mCurrentRelations.add(user.getObjectId());
                        Log.e("MyApp", "mCurrentRelations size = " + mCurrentRelations.size());
                    }

                    fitFinderQuery(mCurrentRelations,mPotentialRelations,mNegativeRelations);

                }
            }
        });
    }

    private void potentialRelationQuery() {
        mPotentialRelations = new ArrayList<>();
        //String currentUserId = mCurrentUser.getObjectId();
        ParseQuery<ParseObject> query3 = ParseQuery.getQuery(Constants.FITFINDER_REQUEST);
        query3.whereEqualTo(Constants.SENDER, mCurrentUser);
        query3.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (parseObjects.size() > 0) {
                        Log.e("MyApp", "Number of potential receiver objects returned = " + parseObjects.size());
                        for (int i = 0; i < parseObjects.size(); i++) {
                            ParseUser receiver = (ParseUser) parseObjects.get(i).get(Constants.RECEIVER);
                            String receiverId = receiver.getObjectId();

                            //ParseObject p = parseObjects.get(i);
                            //String receiver = p.getString(Constants.RECEIVER);
                            Log.e("MyApp", "Potential receiver is: " + receiverId);
                            mPotentialRelations.add(receiverId);
                        }
                        Log.e("MyApp", "mPotentialRelations size: " + mPotentialRelations.size());
                        //previousRelationQuery();
                        //negativeRelationQuery();
                    }
                } else {
                    Log.e("MyApp", "Error returned from potentialRelation request");
                    Log.e("MyApp", "error = " + e.getMessage());
                }
                negativeRelationQuery();
            }
        });
    }

    private void negativeRelationQuery() {
        mNegativeRelations = new ArrayList<>();
        //String currentUserId = mCurrentUser.getObjectId();
        ParseQuery<ParseObject> query4 = ParseQuery.getQuery("NegativeRelation");
        query4.whereEqualTo(Constants.SENDER, mCurrentUser);
        query4.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (parseObjects.size() > 0) {
                        Log.e("MyApp", "Number of negative receiver objects returned = " + parseObjects.size());
                        for (int i = 0; i < parseObjects.size(); i++) {
                            ParseUser receiver = (ParseUser) parseObjects.get(i).get(Constants.RECEIVER);
                            String receiverId = receiver.getObjectId();

                            //ParseObject p = parseObjects.get(i);
                            //String receiver = p.getString(Constants.RECEIVER);
                            Log.e("MyApp", "Negative receiver is: " + receiverId);
                            mNegativeRelations.add(receiverId);
                        }
                        Log.e("MyApp", "mNegativeRelations size: " + mNegativeRelations.size());

                    }
                } else {
                    Log.e("MyApp", "Error returned from NegativeRelation request");
                    Log.e("MyApp", "error = " + e.getMessage());
                    //previousRelationQuery();
                }
                previousRelationQuery();
            }
        });
    }

    /**
     * This method is called when the user accepts a Fitfinder card. It first checks if the other user
     * has already sent a FitfinderRequest via a parse query. If so, then a relation is established
     * between the two users. If not, then a FitfinderRequest is sent to the other user
     */
    private void fitFinderRequestQuery() {
        if (checkConnection()) return;

        ParseQuery<ParseObject> requestQuery = ParseQuery.getQuery(Constants.FITFINDER_REQUEST);
        requestQuery.whereEqualTo(Constants.SENDER, mUser);
        requestQuery.whereEqualTo(Constants.RECEIVER, mCurrentUser);
        requestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    previousRelationQuery();

                    if (parseObjects.isEmpty()) {
                        ParseObject request = new ParseObject(Constants.FITFINDER_REQUEST);
                        request.put(Constants.SENDER, mCurrentUser);
                        request.put(Constants.RECEIVER, mUser);
                        request.saveInBackground();
                        //savePotentialRelation(mCurrentUser,mUser);
                    } else {
                        for (int i = 0; i < parseObjects.size(); i++) {
                            parseObjects.get(i).deleteInBackground();
                        }

                        ParseObject relation = new ParseObject(Constants.RELATION);
                        relation.put(Constants.USER1, mCurrentUser);
                        relation.put(Constants.USER2, mUser);
                        relation.saveInBackground();

                        try {
                            sendPushNotification();
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }

                    //starting process again
                    potentialRelationQuery();

                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveNegativeRelation(ParseUser mCurrentUser, ParseUser mUser) {
        ParseObject negativeRelation = new ParseObject("NegativeRelation");
        negativeRelation.put(Constants.SENDER, mCurrentUser);
        negativeRelation.put(Constants.RECEIVER, mUser);
        negativeRelation.saveInBackground();
    }

    /**
     * This method handles the push notifications sent when a connection is made. Push1 goes to
     * the other user while Push2 goes to the current user. Both open the chat when clicked on.
     */
    private void sendPushNotification() throws JSONException {
        ParseQuery<ParseInstallation> query1 = ParseInstallation.getQuery();
        query1.whereEqualTo(Constants.USER_ID, mUser.getObjectId());
        query1.whereEqualTo(Constants.CHANNELS, Constants.CONNECTION_PUSH);

        JSONObject data1 = new JSONObject();
        data1.put(Constants.PUSH_ALERT, getString(R.string.message_new_connection));
        data1.put(Constants.PUSH_ID, mCurrentUser.getObjectId());
        data1.put(Constants.PUSH_NAME, mCurrentUser.get(Constants.NAME));

        ParsePush push1 = new ParsePush();
        push1.setQuery(query1);
        push1.setData(data1);
        push1.sendInBackground();

        Boolean sendToCurrentUser = (Boolean) mCurrentUser.get(Constants.CONNECTION_NOTIFICATIONS);
        if(sendToCurrentUser) {
            ParseQuery<ParseInstallation> query2 = ParseInstallation.getQuery();
            query2.whereEqualTo(Constants.USER_ID, mCurrentUser.getObjectId());

            JSONObject data2 = new JSONObject();
            data2.put(Constants.PUSH_ALERT, getString(R.string.message_new_connection));
            data2.put(Constants.PUSH_ID, mUser.getObjectId());
            data2.put(Constants.PUSH_NAME, mUser.get(Constants.NAME));

            ParsePush push2 = new ParsePush();
            push2.setQuery(query2);
            push2.setData(data2);
            push2.sendInBackground();
        }
    }

    /**
     * This method performs the ParseQuery and returns a new "FitFinder" user object each time the user
     * either accepts or rejects the previous "FitFinder" user object. It then displays the object in
     * the FitFinderFragment
     */
    private void fitFinderQuery(List<String> mCurrentRelations, List<String> mPotentialRelations, List<String> mNegativeRelations) {
        if (checkConnection()) return;

        //will join here
        if (mPotentialRelations!=null) {
            for(int index = 0; index < mPotentialRelations.size(); index++ ){
                mCurrentRelations.add(mPotentialRelations.get(index));
            }
        }
        if (mNegativeRelations!=null) {
            for(int index = 0; index < mNegativeRelations.size(); index++ ){
                mCurrentRelations.add(mNegativeRelations.get(index));
            }
        }

        ParseGeoPoint userLocation = (ParseGeoPoint) mCurrentUser.get(Constants.GEOPOINT);
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereWithinMiles(Constants.GEOPOINT, userLocation, 50);
        query.whereNotEqualTo(Constants.OBJECT_ID, mCurrentUser.getObjectId());
        query.whereNotEqualTo(Constants.DISCOVERABLE, false);
        //check to see if search returns are in current, potential, or negative relations
        query.whereNotContainedIn(Constants.OBJECT_ID, mCurrentRelations);

        if ((mCurrentUser.get(Constants.GENDER_PREF)).equals(Constants.MALE)) {
            query.whereEqualTo(Constants.GENDER, Constants.MALE);
        } else if ((mCurrentUser.get(Constants.GENDER_PREF)).equals(Constants.FEMALE)) {
            query.whereEqualTo(Constants.GENDER, Constants.FEMALE);
        }

        if ((mCurrentUser.get(Constants.LOOKING_FOR)).equals(Constants.TRAINER)) {
            query.whereEqualTo(Constants.LOOKING_FOR, Constants.OFFER);
        } else if ((mCurrentUser.get(Constants.LOOKING_FOR)).equals(Constants.OFFER)) {
            query.whereEqualTo(Constants.LOOKING_FOR, Constants.TRAINER);
        } else if ((mCurrentUser.get(Constants.LOOKING_FOR)).equals(Constants.PARTNER)) {
            query.whereEqualTo(Constants.LOOKING_FOR, Constants.PARTNER);
        }

        int count = 0;
        try {
            count = query.count();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (mIndices.size() == count) {
            mIndices.clear();
        }

        int random = 0;
        if (count != 0) {
            Boolean check = false;
            while (!check) {
                random = (int) Math.floor(Math.random() * count);
                if (!mIndices.contains(String.valueOf(random))) {
                    check = true;
                    mIndices.add(String.valueOf(random));
                }
            }
        }

        query.setSkip(random);
        query.setLimit(1);
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


                        //mAcceptButton.setEnabled(true);
                        mAcceptButton.setVisibility(View.VISIBLE);
                        //mRejectButton.setEnabled(true);
                        mRejectButton.setVisibility(View.VISIBLE);

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



                        mFitFinderFragment.resetFields();
                        mCardView.startAnimation(mExpandIn);

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
                        setEmptyView();
                    }
                }
            }
        });
    }



    /**
     * This method checks if the device is connected to the internet and sets the empty view if not
     */
    private boolean checkConnection() {
        ConnectionDetector detector = new ConnectionDetector(this);
        if(!detector.isConnectingToInternet()) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();

            setEmptyView();

            return true;
        }
        return false;
    }

    /**
     * This method sets the search view to empty if either there are no search results or if there
     * is no connection available
     */
    private void setEmptyView() {
        mEmptyView.setVisibility(View.VISIBLE);
        mCardView.setVisibility(View.GONE);

        //mAcceptButton.setEnabled(false);
        mAcceptButton.setVisibility(View.GONE);
        //mRejectButton.setEnabled(false);
        mRejectButton.setVisibility(View.GONE);
    }
}

