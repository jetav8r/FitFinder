package com.fitfinder.fitfinder.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.utils.ConnectionDetector;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.FbUtils;
import com.fitfinder.fitfinder.utils.Message;
import com.fitfinder.fitfinder.utils.MessageService;
import com.fitfinder.fitfinder.utils.TypefaceUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.hdodenhof.circleimageview.CircleImageView;


/**
 * Created by Wayne on 4/23/2015.
 */
public class MainActivity extends BaseActivity implements ImageLoadingListener {

    private SharedPreferences sharedPreferences;

    private ParseUser mCurrentUser;
    private ImageLoader loader;
    @InjectView(R.id.imageProgressBar)
    ProgressBar mImageProgressBar;
    @InjectView(R.id.nameProgressBar) ProgressBar mNameProgressBar;
    @InjectView(R.id.profImage)
    ImageView mProfPicField;
    @InjectView(R.id.nameField)
    TextView mUsernameField;
    private Toolbar mToolbar;

    private int PICK_IMAGE_REQUEST = 1;
    private Bitmap compressedBitmap;
    private Bitmap smallBitmap;

    String username;
    ParseFile profImage;
    String fbId;

    String currentActivity;
    private ProgressDialog pd;
    private Context context;

    //todo:add progress bar indicators for profile progress
    //todo: go here on General notification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        if (pd != null) {
            pd.dismiss();
        }
        setFont();
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);


        loader = FitFinderApplication.getImageLoaderInstance();
        currentActivity = "MainActivity";

        //todo: fix no connection bug
        ConnectionDetector detector = new ConnectionDetector(this);
        if (!detector.isConnectingToInternet()) {
            Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
        }
        else {
            mCurrentUser = ParseUser.getCurrentUser();
            if ((mCurrentUser != null) && mCurrentUser.isAuthenticated()) {
                makeMeRequest();
                setDefaultSettings();
                username = (String) mCurrentUser.get(Constants.NAME);
                profImage = mCurrentUser.getParseFile(Constants.PROFILE_IMAGE);
                fbId = (String) mCurrentUser.get("facebookId");
                if(username != null) {
                    mUsernameField.setText(username);
                }

            }



            /*if(username != null) {
                mUsernameField.setText(username);
            }*/




            /*//todo: take into account edge cases
            if(username == null && profImage == null) {
                Session session = Session.getActiveSession();
                if (session != null && session.isOpened()) {
                    facebookRequest();
                }
            }
            else {
                if(username != null) {
                    mUsernameField.setText(username);
                }
                if(profImage != null) {
                    loader.displayImage(profImage.getUrl(), mProfPicField);
                }
            }*/


            if(profImage != null) {
                Glide.with(this)
                        .load(profImage.getUrl())
                        .crossFade()
                        .fallback(R.drawable.ic_def_pic)
                        .error(R.drawable.ic_def_pic)
                        .signature(new StringSignature(UUID.randomUUID().toString()))
                        .into(mProfPicField);
                //loader.displayImage(profImage.getUrl(), mProfPicField);

            } else {
                if (fbId != null){
                    Log.d("MyApp", "FB ID (Main Activity) = " + fbId);
                    new DownloadImageTask((CircleImageView) findViewById(R.id.profImage))
                            .execute("https://graph.facebook.com/" + fbId + "/picture?type=large");
                    //loader.displayImage("https://graph.facebook.com/" + fbId + "/picture?type=large", mProfPicField);
                }
            }
            ImageView image = (ImageView) findViewById(R.id.profImage);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectImage();

                }
            });




            TextView profileButton = (TextView) findViewById(R.id.editProfile);
            profileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.hold);

                }
            });

            RelativeLayout searchButton = (RelativeLayout) findViewById(R.id.searchButton);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pd = ProgressDialog.show(context, "Working...", "Getting connections...", true, false);
                    Intent intent = new Intent(MainActivity.this, SearchMapActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.expand_in_search, R.anim.hold);
                    //pd.dismiss();
                }
            });

            RelativeLayout chatButton = (RelativeLayout) findViewById(R.id.chatButton);
            chatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.expand_in_chat, R.anim.hold);
                }
            });
        }
    }

    private void makeMeRequest() {

        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                        if (jsonObject != null) {
                            JSONObject userProfile = new JSONObject();
                            ParseUser currentUser = ParseUser.getCurrentUser();

                            try {
                                //userProfile.put("facebookId", jsonObject.getString("id"));
                                //userProfile.put("name", jsonObject.getString("name"));


                                // Save the user profile info in a user property

                                if (currentUser.getString(Constants.NAME).equals("") ) {
                                    currentUser.put(Constants.NAME, jsonObject.getString("name"));
                                }
                                currentUser.put("facebookId",jsonObject.getString("id"));
                                //currentUser.put("profile", userProfile);

                                currentUser.saveInBackground();



                            } catch (JSONException e) {
                                Log.d("Wayne", "Error parsing returned user data. " + e);
                            }
                        } else if (graphResponse.getError() != null) {
                            switch (graphResponse.getError().getCategory()) {
                                case LOGIN_RECOVERABLE:
                                    Log.d("Wayne", "Authentication error: " + graphResponse.getError());
                                    break;

                                case TRANSIENT:
                                    Log.d("Wayne", "Transient error. Try again. " + graphResponse.getError());
                                    break;

                                case OTHER:
                                    Log.d("Wayne", "Some other error: " + graphResponse.getError());
                                    break;
                            }
                        }
                    }
                });


        request.executeAsync();
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }


        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("MyApp", e.getMessage());
                e.printStackTrace();
            }
            return mIcon;
        }

        protected void onPostExecute(Bitmap result) {
            if (bmImage != null) {
                bmImage.setImageBitmap(result);
                //convert bitmap to byte array and upload to Parse
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                result.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                final ParseFile file = new ParseFile(Constants.PROFILE_IMAGE_FILE, byteArray);
                file.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            mCurrentUser.put(Constants.PROFILE_IMAGE, file);
                            mCurrentUser.saveInBackground();
                        }
                    }
                });
            }
        }
    }

    /*public static String getFbPicture() {
        String pictureURL = null;
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser.has("profile")) {

            JSONObject userProfile = currentUser.getJSONObject("profile");
            try {
                *//*Bundle parametersPicture = new Bundle();
                parametersPicture.putString("fields", "picture.width(450).height(450)");*//*


                if (userProfile.has("facebookId")) {
                    String fbId = userProfile.getString("facebookId");
                    *//*profilePictureView.setPresetSize(ProfilePictureView.LARGE);
                    profilePictureView.setProfileId(userProfile.getString("facebookId"));*//*

                    pictureURL = "http://graph.facebook.com/"+fbId+"/picture?type=large";
                    Log.d("Wayne","Picture URL = " + pictureURL);

                } else {
                    // Show the default, blank user profile picture
                    //userProfilePictureView.setProfileId(null);
                }


                *//*if (userProfile.has("name")) {
                    userNameView.setText(userProfile.getString("name"));
                } else {
                    userNameView.setText("");
                }*//*


            } catch (JSONException e) {
                Log.d("Wayne", "Error parsing saved user data.");
            }
        }

        return pictureURL;
    }*/

    private void setFont() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String fontKey = this.getString(R.string.pref_font);
        String fontValue = sharedPreferences.getString(fontKey, "Roboto-Regular.ttf");
        //set font
        //TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/Norican-Regular.ttf");
        // font from assets: "assets/fonts/Roboto-Regular.ttf
        String currentFont = "fonts/" + fontValue;
        Log.e("MyApp", "font = " + currentFont);
        TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", currentFont);

    }


    private void selectImage() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

        /*Intent intent = new Intent(EditProfileActivity.this, PhotoChooserActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.expand_in_chat, R.anim.hold);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Log.d("MyApp", String.valueOf(bitmap));

                smallBitmap = getResizedBitmap(bitmap);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                smallBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Log.d("MyApp", "smallBitmap compressed stream size = "+byteArray.length);

                ImageView imageView = (ImageView) findViewById(R.id.profImage);
                imageView.setImageBitmap(bitmap);

                //convert bitmap to byte array and upload to Parse
                /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap = bitmap.createScaledBitmap(bitmap, 480, 640, true);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Log.d("MyApp", "first compressed stream size = "+byteArray.length);*/
                if (byteArray.length > 10485759) {
                    Log.d("MyApp", "Picture is too large");
                    compressedBitmap = Bitmap.createScaledBitmap(smallBitmap, 240, 240, true);
                    stream = new ByteArrayOutputStream();
                    compressedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byteArray = stream.toByteArray();
                    Log.d("MyApp", "byteArray = " + byteArray.length);
                    saveImageToParse(byteArray);
                   /* final ParseFile file = new ParseFile(Constants.PROFILE_IMAGE_FILE, byteArray);
                    file.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                mCurrentUser.put(Constants.PROFILE_IMAGE, file);
                                mCurrentUser.saveInBackground();
                            }
                        }
                    });*/
                } else {
                    /*final ParseFile file = new ParseFile(Constants.PROFILE_IMAGE_FILE, byteArray);
                    file.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                mCurrentUser.put(Constants.PROFILE_IMAGE, file);
                                mCurrentUser.saveInBackground();
                            }
                        }
                    });*/
                    saveImageToParse(byteArray);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap image) {
        Bitmap originalImage = image;
        Bitmap background = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888);
        float originalWidth = originalImage.getWidth(), originalHeight = originalImage.getHeight();
        Canvas canvas = new Canvas(background);
        float scale = 240/originalWidth;
        float xTranslation = 0.0f, yTranslation = (240 - originalHeight * scale)/2.0f;
        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(originalImage, transformation, paint);
        return background;
    }

    private void saveImageToParse(byte[] byteArray) {
        final ParseFile file = new ParseFile(Constants.PROFILE_IMAGE_FILE, byteArray);
        file.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    mCurrentUser.put(Constants.PROFILE_IMAGE, file);
                    mCurrentUser.saveInBackground();
                }
            }
        });

    }

    /*private void personalize() {
        String username = (String) mCurrentUser.get(Constants.NAME);
        //ParseFile profImage = mCurrentUser.getParseFile(Constants.PROFILE_IMAGE);

        *//*if(profImage != null) {
            loader.displayImage(profImage.getUrl(), mProfPicField);
        }*//*
    }*/

    /**
     * This method sets the default settings for discoverable and notification settings the first
     * time the user logs in
     */
    private void setDefaultSettings() {
        Boolean discoverable = (Boolean) mCurrentUser.get(Constants.DISCOVERABLE);
        if(discoverable == null) {
            mCurrentUser.put(Constants.DISCOVERABLE, true);
            mCurrentUser.saveInBackground();
        }

        Boolean generalNot = (Boolean) mCurrentUser.get(Constants.GENERAL_NOTIFICATIONS);
        if(generalNot == null) {
            mCurrentUser.put(Constants.GENERAL_NOTIFICATIONS, true);
            mCurrentUser.saveInBackground();
            ParsePush.subscribeInBackground(Constants.GENERAL_PUSH);
        }

        Boolean messageNot = (Boolean) mCurrentUser.get(Constants.MESSAGE_NOTIFICATIONS);
        if(messageNot == null) {
            mCurrentUser.put(Constants.MESSAGE_NOTIFICATIONS, true);
            mCurrentUser.saveInBackground();
            ParsePush.subscribeInBackground(Constants.MESSAGE_PUSH);
        }

        Boolean connectionNot = (Boolean) mCurrentUser.get(Constants.CONNECTION_NOTIFICATIONS);
        if(connectionNot == null) {
            mCurrentUser.put(Constants.CONNECTION_NOTIFICATIONS, true);
            mCurrentUser.saveInBackground();
            ParsePush.subscribeInBackground(Constants.CONNECTION_PUSH);
        }
    }

    /**
     * This method contains the facebook request and also sets the users info to parse as well as
     * setting the ui elements
     */
    /*private void facebookRequest() {
        SimpleFacebook simpleFacebook = SimpleFacebook.getInstance(this);
        Profile.Properties properties = new Profile.Properties.Builder()
                .add(Profile.Properties.FIRST_NAME)
                .add(Profile.Properties.GENDER)
                .add(Profile.Properties.BIRTHDAY)
                .add(Profile.Properties.ID)
                .build();

        simpleFacebook.getProfile(properties, new OnProfileListener() {
            @Override
            public void onComplete(Profile response) {
                String id = response.getId();
                String name = response.getFirstName();
                String gender = response.getGender();
                String birthday = response.getBirthday();
                String age = getAge(birthday);

                mCurrentUser.put(Constants.NAME, name);
                mCurrentUser.put(Constants.AGE, age);
                mCurrentUser.put(Constants.GENDER, gender);
                mCurrentUser.saveInBackground();

                if(id != null) {
                    loader.displayImage("https://graph.facebook.com/" + id + "/picture?type=large",
                            mProfPicField, MainActivity.this);
                }

                if (name != null) {
                    mUsernameField.setText(name);
                    mNameProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }*/

    /**
     * This method takes a string birthday and calculates the age of the person from it
     *
     * @param birthday the birthdate in string form
     *//*
    private String getAge(String birthday) {
        Date yourDate;
        String ageString = null;
        try {
            SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy");
            yourDate = parser.parse(birthday);
            Calendar dob = Calendar.getInstance();
            dob.setTime(yourDate);

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            Integer ageInt = age;
            ageString = ageInt.toString();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        return ageString;
    }*/

    /**
     * These next 4 methods are from the ImageLoadingListener Interface
     */
    @Override
    public void onLoadingStarted(String s, View view) {
        mImageProgressBar.setVisibility(View.VISIBLE);
        mNameProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoadingFailed(String s, View view, FailReason failReason) {}

    @Override
    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
        mImageProgressBar.setVisibility(View.INVISIBLE);

        //convert bitmap to byte array and upload to Parse
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        final ParseFile file = new ParseFile(Constants.PROFILE_IMAGE_FILE, byteArray);
        file.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    mCurrentUser.put(Constants.PROFILE_IMAGE, file);
                    mCurrentUser.saveInBackground();
                }
            }
        });

    }

    @Override
    public void onLoadingCancelled(String s, View view) {}



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
     public void onPause(){
        super.onPause();
        //Message.message(this, "onPause in Main Activity was called");

    }

    @Override
    public void onResume() {
        super.onResume();
        //Message.message(this, "onResume was called");
        setFont();
        if (pd != null) {
            pd.dismiss();
        }
        if(username != null) {
            mUsernameField.setText(username);
        }
    }

    @Override
    public void onBackPressed() {
        if(!currentActivity.equals("MainActivity")) {
                super.onBackPressed();
            } else {
                finish();
            }
    }
}