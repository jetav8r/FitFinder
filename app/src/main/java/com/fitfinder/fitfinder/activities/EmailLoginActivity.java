package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 5/5/2015.
 */

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
import android.widget.TextView;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.IntentUtils;
import com.fitfinder.fitfinder.utils.Message;
import com.fitfinder.fitfinder.utils.MessageService;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

public class EmailLoginActivity extends BaseActivity implements View.OnClickListener, TextWatcher {

    private EditText mEmail;
    private EditText mPassword;
    private Button submit;
    private Button cancel;
    private Boolean onBoarded;
    private String email, password;
    private TextView passReset;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        mEmail = (EditText) findViewById(R.id.enter_login_email);
        mEmail.addTextChangedListener(this);
        mPassword = (EditText) findViewById(R.id.enter_login_password);
        mPassword.setTypeface(Typeface.DEFAULT);
        mPassword.setTransformationMethod(new PasswordTransformationMethod());
        mPassword.addTextChangedListener(this);
        passReset = (TextView) findViewById(R.id.passwordResettextView);
        passReset.setOnClickListener(this);

        submit = (Button) findViewById(R.id.emailLoginSubmit);
        cancel = (Button) findViewById(R.id.cancelButton);
        submit.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    private void currentEmailUser() {
        ParseUser.logInInBackground(email, password, new LogInCallback() {
            public void done(ParseUser user, ParseException e) {
                if (user != null) {
                    // Hooray! The user is logged in.
                    Log.d("MyApp", "Email login was successful!");
                    if (IntentUtils.checkIfAlreadyOnBoarded()) {
                        IntentUtils.mainIntent(EmailLoginActivity.this);
                    } else {
                        IntentUtils.onBoardIntent(EmailLoginActivity.this);
                    }
                } else {
                    // Login failed. Look at the ParseException to see what happened.
                    Message.message(getApplicationContext(), "Login failed, please try again");
                    Log.d("MyApp", "Email login was Unsuccessful!");
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
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
        if (view.getId() == R.id.emailLoginSubmit) {
            //Message.message(this, "Submit button pressed");
            email = mEmail.getText().toString().trim().toLowerCase();
            password = mPassword.getText().toString().trim();
            Log.d("MyApp", "mEmail and mPassword: " + mEmail.getText() + ", " + mPassword.getText());
            currentEmailUser();
        }
        if (view.getId() == R.id.cancelButton)  {
            Intent intent = new Intent(EmailLoginActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_quick, R.anim.slide_out_right);
            //finish();
        }
        if (view.getId() == R.id.passwordResettextView) {
            resetPassword();
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
        String email = this.mEmail.getText().toString();
        String password = this.mPassword.getText().toString();

        if(email.isEmpty() || !email.contains("@") || email.replace(" ", "").isEmpty()){
            return false;
        }

        if (password.isEmpty() || password.replace(" ", "").isEmpty() || password.replace(" ", "").length() < 4 ){
            return false;
        }
        return true;
    }

    public void resetPassword() {
        ParseUser.requestPasswordResetInBackground((mEmail.getText().toString()),
                new RequestPasswordResetCallback() {
                    public void done(ParseException e) {
                        if (e == null) {
                            Message.message(getApplicationContext(), "Password email sent");
                            Log.e("MyApp", "Password reset email sent");
                            // An email was successfully sent with reset instructions.
                        } else {
                            Message.message(getApplicationContext(), "There was an error resetting your password");
                            Message.message(getApplicationContext(), "Check your login email address again");
                            Log.e("MyApp", "Password email reset error = " + e.getMessage());
                            // Something went wrong. Look at the ParseException to see what's up.
                        }
                    }
                });
    }
}