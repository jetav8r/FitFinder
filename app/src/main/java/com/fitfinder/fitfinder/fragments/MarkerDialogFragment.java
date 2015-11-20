package com.fitfinder.fitfinder.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.activities.ChatActivity;
import com.fitfinder.fitfinder.activities.MainActivity;
import com.fitfinder.fitfinder.activities.MessagingActivity;
import com.fitfinder.fitfinder.activities.SearchMapActivity;
import com.fitfinder.fitfinder.utils.ConnectionDetector;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.FbUtils;
import com.fitfinder.fitfinder.utils.Message;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SimpleTimeZone;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * Created by Wayne on 12/4/2014.
 */
public class MarkerDialogFragment extends DialogFragment implements View.OnClickListener {
    AlertDialog.Builder builder;
    public static FitFinderFragment mFitFinderFragment;
    private ParseUser mCurrentUser;
    private ParseUser markerUser;
    private ParseUser mProfileUser;
    private List<String> mCurrentRelations;
    //public static HashMap<String, ParseUser> chatRecipient;

    private String recipientId;
    private String mRecipientName;
    private String mRelationId;
    //@InjectView(R.id.chatFrag) CardView mCardView;
    private CardView mCardView;
    ParseFile profImage;
    private ImageLoader loader;
    Context context;


    public MarkerDialogFragment(){
    }



    @Override
    public void onClick(View view) {
        int id = view.getId();

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getActivity();
        mCurrentUser = ParseUser.getCurrentUser();
        //Bundle bundle = getArguments();
        //chatRecipient = SearchMapActivity.chatRecipient;
        //ButterKnife.inject(getActivity());
        mCardView = (CardView) getActivity().findViewById(R.id.chatFrag);


        builder = new AlertDialog.Builder(getActivity());
        final String markerId = getArguments().getString("id");
        final String userId = getArguments().getString("userId");
        final String title = getArguments().getString("title");
        final double destLat = getArguments().getDouble("latitude");
        final double destLong = getArguments().getDouble("longitude");
        Log.d("MyApp","MarkerDialogFragment userId = " + userId);
        builder.setTitle(title)
                .setItems(getActivity().getResources().getStringArray(R.array.marker_menu), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showProfile(userId);
                                //addToFavorite(id, title, destLat, destLong, vicinity);
                                //Message.message(getActivity(), "Test marker dialog");
                                break;
                            case 1:
                                chatRelationRequest(userId);
                                break;
                            case 2:
                                //share(title, destLat, destLong);
                                break;
                            /*case 3:
                                //((BlocSpotActivity) getActivity()).setGeofence(title, destLat, destLong);
                                break;*/
                            default:
                                break;
                        }
                    }
                });

        Dialog dialog = builder.create();

        return dialog;
    }

    private void chat(ParseUser sender, ParseUser markerUser) {
        //put Parse relation for chat... then open Chat Activity to show list of chats
        //TODO open messaging window directly instead of going to ChatActivity first
        //Here I will try to open the messaging window directly

        String markerUserId = markerUser.getObjectId();
        Intent intent = new Intent(context, MessagingActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("fromMarker", true);
        intent.putExtra(Constants.RECIPIENT_ID, markerUser.getObjectId());
        intent.putExtra(Constants.RECIPIENT_NAME, (String) markerUser.get(Constants.NAME));
        intent.putExtra(Constants.LOCATION, (String) markerUser.get(Constants.LOCATION));
        intent.putExtra(Constants.ABOUT_ME, (String) markerUser.get(Constants.ABOUT_ME));
        intent.putExtra(Constants.LOOKING_FOR, (String) markerUser.get(Constants.LOOKING_FOR));
        //intent.putExtra(Constants.PROFILE_IMAGE,  (String) user.get(Constants.PROFILE_IMAGE));
        Log.e("MyApp", "profImage = " + markerUser.get(Constants.PROFILE_IMAGE));
        context.startActivity(intent);


        /*Intent intent = new Intent(context, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);*/
    }

    /**
     * This method is called when the user wants to create a chat. It first checks if there is already
     * a relation via a parse query. If so, then no new relation is created
     * between the two users. If not, then a relation is created for chat purposes
     * TODO make notification when chat is sent/request is created
     */
    private void chatRelationRequest(String markerUserId) {
        if (checkConnection()) return;
        //this first query will find the user object of the marker
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(Constants.OBJECT_ID, markerUserId);
        Log.d("MyApp", "Recipient ID = " + markerUserId);

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
                    Log.d("myapp", "number of returned users = " + parseUsers.size());
                    if (!parseUsers.isEmpty() && parseUsers != null) {
                        /*for (int i = 0; i < parseUsers.size(); i++){
                            mUser = parseUsers.get(i);
                            Log.e("myapp", "user id="+mUser.getObjectId() );
                        }*/
                        markerUser = parseUsers.get(0);


                        //name below just for info purposes... second query will now look for the
                        //currentUser's chats that already exist
                        String currentUserName = (String) mCurrentUser.get(Constants.NAME);
                        String name = (String) markerUser.get(Constants.NAME);

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
                                if (e == null) {
                                    mCurrentRelations.clear();
                                    for (int i = 0; i < parseObjects.size(); i++) {
                                        Log.d("MyApp", "current relation objects = " + parseObjects.size());
                                        ParseUser user1 = (ParseUser) parseObjects.get(i).get(Constants.USER1);
                                        String userId = user1.getObjectId();
                                        ParseUser recipient;
                                        if (userId.equals(mCurrentUser.getObjectId())) {
                                            recipient = (ParseUser) parseObjects.get(i).get(Constants.USER2);
                                        } else {
                                            recipient = (ParseUser) parseObjects.get(i).get(Constants.USER1);
                                        }
                                        mCurrentRelations.add(recipient.getObjectId());
                                        Log.e("MyApp", "mCurrentRelations size = " + mCurrentRelations.size());
                                        //chatRecipient.put("recipient", markerUser);
                                    }
                                }
                                //Now we need to run our chat request query, but exclude those in mCurrentRelations

                                chatAdditionRequest(mCurrentRelations, markerUser);
                                chat(mCurrentUser, markerUser);
                            }
                        });
                    } else {
                        //setEmptyView();
                        Log.e("MyApp", "Query returned zero result");
                    }
                }
            }

        });


    }

    //will try to add a new Chat Relation with the user clicked on in the map, but we will exclude
    //those in the mCurrentRelations list to avoid duplicates

    private void chatAdditionRequest(List<String> currentRelations, ParseUser recipientFromMarker) {
        Boolean exists = false;

        for (int i =0; i < currentRelations.size(); i++)  {
            String user = currentRelations.get(i);
            if (user.equals(recipientFromMarker.getObjectId())) {
                exists = true;
                i = currentRelations.size();
                Log.d("MyApp", "Chat relation already exists, not creating again");
            }
        }
        if (!exists) {
            ParseObject relation = new ParseObject(Constants.RELATION);
            relation.put(Constants.USER1, mCurrentUser);
            relation.put(Constants.USER2, recipientFromMarker);
            relation.saveInBackground();
        }
    }


    /**
     * This method checks if the device is connected to the internet
     */
    private boolean checkConnection() {
        ConnectionDetector detector = new ConnectionDetector(getActivity());
        if(!detector.isConnectingToInternet()) {
            //Toast.makeText(getActivity(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
            Message.message(getActivity(), getString(R.string.no_connection));
            return true;
        }
        return false;
    }

    private List<String> previousRelationQuery() {
        if (checkConnection()) {
            Log.d("MyApp","No internet connection found");
        }

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

                    //fitFinderQuery(mCurrentRelations,mPotentialRelations,mNegativeRelations);
                    //Now we need to run our chat request query, but exclude those in mCurrentRelations


                }
            }
        });
        return mCurrentRelations;
    }

    private void showProfile (String userId){
        Bundle bundle = new Bundle();
        bundle.putString("userId", userId);
        //mSendMessage.setVisibility(View.GONE);
        mFitFinderFragment = new FitFinderFragment();
        mFitFinderFragment.setArguments(bundle);

         //initialize the fragment
        //ButterKnife.inject(getActivity());

        getFragmentManager().beginTransaction()
                //.replace(R.id.map, mFitFinderFragment)

                .add(R.id.map, mFitFinderFragment)
                .addToBackStack("profileFrag")
                .commit();
        SearchMapActivity.profFragShowing = true;
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(Constants.OBJECT_ID, userId);
        Log.e("MyApp", "Recipient object Id = " + userId);

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

                        mProfileUser = parseUsers.get(0);

                        String name = (String) mProfileUser.get(Constants.NAME);
                        String age = (String) mProfileUser.get(Constants.AGE);
                        String location = (String) mProfileUser.get(Constants.LOCATION);
                        String aboutMe = (String) mProfileUser.get(Constants.ABOUT_ME);
                        String lookingFor = (String) mProfileUser.get(Constants.LOOKING_FOR);
                        /*Boolean smokes = (Boolean) mUser.get(Constants.SMOKES);
                        Boolean drinks = (Boolean) mUser.get(Constants.DRINKS);*/

                        /*if(profImage != null) {
                            Glide.with(getActivity())
                                    .load(profImage.getUrl())
                                    .crossFade()
                                    .fallback(R.drawable.ic_def_pic)
                                    .error(R.drawable.ic_def_pic)
                                    .into(mProfPicField);
                            //loader.displayImage(profImage.getUrl(), mProfPicField);

                        } else {
                            String fbId = FbUtils.getCurrentUserFbId();
                            Log.d("MyApp", "FB ID (Main Activity) = " + fbId);
                            loader.displayImage("https://graph.facebook.com/" + fbId + "/picture?type=large", mProfPicField);
                        }*/

                        ParseFile profImage = (ParseFile) mProfileUser.get(Constants.PROFILE_IMAGE);

                        /*if (mCardView.getVisibility() == View.GONE) {
                            mCardView.setVisibility(View.VISIBLE);
                        }*/
                        //mCardView.setVisibility(View.VISIBLE);

                        mFitFinderFragment.resetFields();
                        //mCardView.startAnimation(mExpandIn);

                        mFitFinderFragment.setName(name);
                        mFitFinderFragment.setAge(age);
                        mFitFinderFragment.setLocation(location);
                        mFitFinderFragment.setAboutMe(aboutMe);
                        mFitFinderFragment.setLookingFor(lookingFor);
                        mFitFinderFragment.setProfImage(profImage);

                        //mFitFinderFragment.setSmokes(smokes);
                        //mFitFinderFragment.setDrinks(drinks);
                        mFitFinderFragment.setFields();
                    } else {
                        //setEmptyView();
                        Log.e("MyApp", "Query returned zero result");
                    }
                }
            }
        });
    }

    //put code here to start the chat process
    /*private void chatRequestQuery() {
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
        });*/


}
