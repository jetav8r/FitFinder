package com.fitfinder.fitfinder.fragments;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.activities.MessagingActivity;
import com.fitfinder.fitfinder.activities.OnBoardActivity;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.FbUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 *
 */
public class FitFinderFragment extends Fragment {

    private ImageView mProfImageField;
    private ParseFile mProfImage;
    private String mName;
    private String mLocation;
    private String mAboutMe;
    private String mLookingFor;
    private String mUserId;
    //private Boolean mSmokes;
    //private Boolean mDrinks;
    private Boolean mPets;
    private String mAge;
    private TextView mNameField;
    private TextView mLocationField;
    private TextView mAboutMeTitle;
    private TextView mAboutMeField;
    private TextView mLookingForField;
    private TextView mSmokesField;
    private TextView mDrinksField;
    private ImageLoader loader;
    private View mChatButton;
    private ParseUser mUser;
    private ParseUser mCurrentUser;
    private List<String> mCurrentRelations;
    private Context context;
    //private ProgressBar mProgressBar;

    public FitFinderFragment() {} // Required empty public constructor


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fitfinder, container, false);



        Bundle bundle=getArguments();
        mUserId = bundle.getString("userId");

        mCurrentUser = ParseUser.getCurrentUser();
        context = getActivity();
        ButterKnife.inject(getActivity());
        loader = FitFinderApplication.getImageLoaderInstance();
        mChatButton = view.findViewById(R.id.chatButton);
        mProfImageField = (ImageView) view.findViewById(R.id.profImage);
        mNameField = (TextView) view.findViewById(R.id.nameField);
        mLocationField = (TextView) view.findViewById(R.id.locationField);
        mAboutMeTitle = (TextView) view.findViewById(R.id.aboutMeText);
        mAboutMeField = (TextView) view.findViewById(R.id.aboutMeField);
        mLookingForField = (TextView) view.findViewById(R.id.lookingForField);
        //mSmokesField = (TextView) view.findViewById(R.id.smokesField);
        //mDrinksField = (TextView) view.findViewById(R.id.drinksField);
        //mProgressBar = (ProgressBar) view.findViewById(R.id.imageProgressBar);
        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatRelationRequest(mUserId);
            }
        });

        return view;
    }

    //the following methods are the setters for the info for the fragment

    public void setProfImage(ParseFile profImage) {
        mProfImage = profImage;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public void setAboutMe(String aboutMe) {
        mAboutMe = aboutMe;
    }

    public void setLookingFor(String lookingFor) {
        mLookingFor = lookingFor;
    }

    /*public void setSmokes(Boolean smokes) {
        mSmokes = smokes;
    }

    public void setDrinks(Boolean drinks) {
        mDrinks = drinks;
    }*/

    public void setAge(String age) {
        mAge = age;
    }

    /*
     * This method is called once all the variables are reset and it then sets the field with
     * the new variable for the new FitFinder Card
     */
    public void setFields() {
        //mNameField.setText(mName + ", " + mAge);
        mNameField.setText(mName);
        mLocationField.setText(mLocation);

        mAboutMeTitle.setText(getActivity().getString(R.string.aboutMeText) + mName);
        mAboutMeField.setText(mAboutMe);

        /*mSmokesField.setText("");
        mDrinksField.setText("");*/
        mLookingForField.setText(mLookingFor);

        /*setYesNoFields(mSmokes, mSmokesField);
        setYesNoFields(mDrinks, mDrinksField);*/



        if(mProfImage != null) {
            Glide.with(getActivity())
                    .load(mProfImage.getUrl())
                    .crossFade()
                    .fallback(R.drawable.ic_def_pic)
                    .error(R.drawable.ic_def_pic)
                    .signature(new StringSignature(UUID.randomUUID().toString()))
                    .into(mProfImageField);
            /*ImageLoader loader = FitFinderApplication.getImageLoaderInstance();
            loader.displayImage(mProfImage.getUrl(), mProfImageField, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {
                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {
                }

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onLoadingCancelled(String s, View view) {
                }
            });*/
        } /*else {
            String fbId = FbUtils.getCurrentUserFbId();
            if(fbId != null) {
                Log.d("MyApp", "FB ID (Main Activity) = " + fbId);
                loader.displayImage("https://graph.facebook.com/" + fbId + "/picture?type=large", mProfImageField);
            }
        }*/
    }

    /*
     * This method is called once when a new card is being shown. It sets all the previous fields
     * to blank until the new fields are loaded
     */
    public void resetFields() {
        //mProgressBar.setVisibility(View.VISIBLE);

        mProfImageField.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_default_image));
        mNameField.setText("");
        mLocationField.setText("");
        mAboutMeTitle.setText("");
        mAboutMeField.setText("");
        mLookingForField.setText("");

        /*setYesNoFields(null, mSmokesField);
        setYesNoFields(null, mDrinksField);*/
    }

    private void setYesNoFields(Boolean field, TextView view) {
        if(field != null) {
            if(field) {
                view.setText(getString(R.string.yes));
            }
            else {
                view.setText(getString(R.string.no));
            }
        }
        else {
            view.setText("");
        }
    }

    private void chatRelationRequest(String mUserId) {
        //if (checkConnection()) return;
        //this first query will find the user object of the marker
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(Constants.OBJECT_ID, mUserId);
        Log.d("MyApp", "Recipient ID = " + mUserId);

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
                        mUser = parseUsers.get(0);


                        //name below just for info purposes... second query will now look for the
                        //currentUser's chats that already exist
                        String currentUserName = (String) mUser.get(Constants.NAME);
                        String name = (String) mUser.get(Constants.NAME);

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

                                chatAdditionRequest(mCurrentRelations, mUser);
                                chat(mCurrentUser, mUser);
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
    }
}
