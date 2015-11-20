package com.fitfinder.fitfinder.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.IntentUtils;
import com.fitfinder.fitfinder.utils.Message;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
;

public class AddUserActivity extends BaseActivity implements View.OnClickListener, TextWatcher {

    private EditText mEmail;
    private EditText mPassword;
    private Button submit;
    private Button cancel;
    private Boolean onBoarded;
    private String email, password;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.addUser);

        mEmail = (EditText) findViewById(R.id.enter_email);
        mEmail.addTextChangedListener(this);
        mPassword = (EditText) findViewById(R.id.enter_password);
        mPassword.setTypeface(Typeface.DEFAULT);
        mPassword.setTransformationMethod(new PasswordTransformationMethod());
        mPassword.addTextChangedListener(this);

        submit = (Button) findViewById(R.id.submit_button2);
        cancel = (Button) findViewById(R.id.cancelButton);
        submit.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    private void newEmailUser() {
        ParseUser user = new ParseUser();
        user.setUsername(email);
        user.setPassword(password);
        //user.setEmail("etEmail@example.com");

// other fields can be set just like with ParseObject
        //user.put("phone", "650-253-0000");
        user.put(Constants.ALREADY_ONBOARD, false);


        user.signUpInBackground(new SignUpCallback() {

            public void done(ParseException e) {
                if (e == null) {
                    // Hooray! Let them use the app now.
                    exitSignUp();
                    //IntentUtils.onBoardIntent(AddUserActivity.this);
                } else {
                    // Sign up didn't succeed. Look at the ParseException
                    // to figure out what went wrong
                    Log.d("MyApp", "Email login unsuccessful");
                    finish();
                }
            }
        });

    }

    private void exitSignUp() {
        ParseUser.logInInBackground(email, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    // Hooray! The user is logged in.
                    Log.d("MyApp", "Email login was successful!");
                    IntentUtils.onBoardIntent(AddUserActivity.this);
                } else {
                    // Signup failed. Look at the ParseException to see what happened.
                    Log.e("MyApp", "Email login failed. Error = "+e.getMessage());
                }
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_add_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.submit_button2) {
            //Message.message(this, "Submit button pressed");
            email = mEmail.getText().toString().trim().toLowerCase();
            password = mPassword.getText().toString();
            Log.d("MyApp", "mEmail and mPassword: " + mEmail.getText() + ", " + mPassword.getText());
            newEmailUser();
        }
        if (view.getId() == R.id.cancelButton)  {
            Intent intent = new Intent(AddUserActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_quick, R.anim.slide_out_right);
            //finish();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(validateData()){
            submit.setEnabled(true);
        }else {
            submit.setEnabled(false);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private boolean validateData(){
        String email = this.mEmail.getText().toString().trim().toLowerCase();
        String password = this.mPassword.getText().toString();

        if(email.isEmpty() || !email.contains("@") || email.replace(" ", "").isEmpty()){
            return false;
        }

        if (password.isEmpty() || password.replace(" ", "").isEmpty() || password.replace(" ", "").length() < 4 ){
            return false;
        }
        return true;
    }
}
