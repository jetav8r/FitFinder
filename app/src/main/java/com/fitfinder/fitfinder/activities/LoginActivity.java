package com.fitfinder.fitfinder.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.IntentUtils;
import com.fitfinder.fitfinder.utils.Message;
import com.fitfinder.fitfinder.utils.MessageService;
import com.parse.GetCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
/**
 * Created by Wayne on 4/23/2015.
 */
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private Button loginButton;
    private Button emailLoginButton;
    private Button emailSignUpButton;

    Profile mFbProfile;
    ParseUser parseUser;
    String name = null;
    String email = null;


    public static final List<String> permissions = new ArrayList<String>() {{
        add("public_profile");
        add("email");
    }};

    //private Intent serviceIntent;
    //todo: we do not post anything to facebook message
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        TextView titleText = (TextView) findViewById(R.id.appTitleTextView);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Norican-Regular.ttf");
        titleText.setTypeface(font);
        titleText.setShadowLayer(10, 0, 0, Color.BLACK);

        mFbProfile = Profile.getCurrentProfile();

        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(this);
        emailLoginButton = (Button) findViewById(R.id.emailLoginButton);
        emailLoginButton.setOnClickListener(this);
        emailSignUpButton = (Button) findViewById(R.id.emailSignUpButton);
        emailSignUpButton.setOnClickListener(this);

        //serviceIntent = new Intent(getApplicationContext(), MessageService.class);


    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        if(id == loginButton.getId()){
            onLoginButtonClicked();
        }

        if(id == emailLoginButton.getId()) {
            onEmailLoginButtonClicked();
        }

        if(id == emailSignUpButton.getId()) {
            onEmailSignUpButtonClicked();
        }


        TextView titleText = (TextView) findViewById(R.id.appTitleTextView);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Norican-Regular.ttf");
        titleText.setTypeface(font);
        titleText.setShadowLayer(10, 0, 0, Color.BLACK);
    }

    private void onLoginButtonClicked() {

        //List<String> permissions = new ArrayList<>();
        //permissions.add("public_profile");


            //ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, permissions, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                AccessToken.getCurrentAccessToken();
                if (user == null) {
                    Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");

                    //finish();
                } else if (user.isNew()) {
                    Log.d("MyApp", "User signed up and logged in through Facebook!");

                    getUserDetailsFromFB();
                    String userName = user.getString("username");
                    String objectId = user.getObjectId();
                    user.put(Constants.ALREADY_ONBOARD, false);
                    user.saveInBackground();
                    Boolean onBoarded = user.getBoolean(Constants.ALREADY_ONBOARD);
                    Log.d("MyApp", "onBoarded = " + onBoarded);
                    IntentUtils.onBoardIntent(LoginActivity.this);
                } else {
                    Log.d("MyApp", "User logged in through Facebook!");
                    //user.put(Constants.ALREADY_ONBOARD, false);
                    if (IntentUtils.checkIfAlreadyOnBoarded()) {
                        IntentUtils.mainIntent(LoginActivity.this);
                    } else {
                        IntentUtils.onBoardIntent(LoginActivity.this);
                    }
                    //IntentUtils.onBoardIntent(LoginActivity.this);
                }
            }
        });
    }
    private void getUserDetailsFromFB() {


        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
            /* handle the result */
                        try {
                            email = response.getJSONObject().getString("email");
                            //mEmailID.setText(email);
                            name = response.getJSONObject().getString("name");
                            //mUsername.setText(name);


                            saveNewUser();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    private void saveNewUser() {
        parseUser = ParseUser.getCurrentUser();
        parseUser.setUsername(name);
        parseUser.setEmail(email);

        //We can't use the below code until the profile image is set...maybe in MainActivity

        //        Saving profile photo as a ParseFile
        //ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //Bitmap bitmap = ((BitmapDrawable) mProfileImage.getDrawable()).getBitmap();
        //bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        //byte[] data = stream.toByteArray();
        //String thumbName = parseUser.getUsername().replaceAll("\\s+", "");
        //final ParseFile parseFile = new ParseFile(thumbName + "_thumb.jpg", data);


        //parseFile.saveInBackground(new SaveCallback() {
            //@Override
            //public void done(ParseException e) {
                //parseUser.put("profileThumb", parseFile);

            parseUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Toast.makeText(LoginActivity.this, "New user:" + name + " Signed up", Toast.LENGTH_SHORT).show();
                }
            });
        //}
    //});
    }

    private void getUserDetailsFromParse() {
        parseUser = ParseUser.getCurrentUser();


//Fetch profile photo
        try {
            ParseFile parseFile = parseUser.getParseFile("profileThumb");
            byte[] data = parseFile.getData();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            //We can try to possibly use this to set the profile image in the Main Activity
            //mProfileImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We can possibly use this data to populate the OnBoard Activity or EditProfile
        //mEmailID.setText(parseUser.getEmail());
        //mUsername.setText(parseUser.getUsername());


        Toast.makeText(LoginActivity.this, "Welcome back " + parseUser.getUsername(), Toast.LENGTH_SHORT).show();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }


    private void finishActivity() {
        // Start an intent for the dispatch activity
        /*Intent intent = new Intent(LoginActivity.this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
    }

    private void onEmailLoginButtonClicked() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            Log.d("MyApp", "User logged in with email!");
            if (IntentUtils.checkIfAlreadyOnBoarded()) {
                IntentUtils.mainIntent(LoginActivity.this);
            } else {
                IntentUtils.onBoardIntent(LoginActivity.this);
            }
        } else {
            Intent intent = new Intent(LoginActivity.this, EmailLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }
    }
    private void onEmailSignUpButtonClicked() {
        Intent intent = new Intent(LoginActivity.this, AddUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }
}

