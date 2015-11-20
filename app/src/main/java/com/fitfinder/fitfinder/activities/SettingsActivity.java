package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fitfinder.fitfinder.LauncherActivity;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.fragments.PrefFragment;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.Message;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParsePush;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SettingsActivity extends BaseActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private Boolean mDiscoverable;
    private Boolean mGeneralNot;
    private Boolean mMessageNot;
    private Boolean mConnectionNot;
    private ParseUser mCurrentUser;
    private Toolbar mToolbar;

    @InjectView(R.id.discoveryCheckBox) CheckBox mDiscoveryCheckBox;
    /*@InjectView(R.id.privacyText) TextView mPrivacyButton;
    @InjectView(R.id.termText) TextView mTermsButton;*/
    @InjectView(R.id.changeFontText) TextView mFontButton;
    @InjectView(R.id.logoutText) TextView mLogoutButton;
    @InjectView(R.id.deleteAccountText) TextView mDeleteAccountButton;
    @InjectView(R.id.generalCheckBox) CheckBox mGeneralNotifications;
    @InjectView(R.id.messageCheckBox) CheckBox mMessageNotifications;
    @InjectView(R.id.connectionCheckBox) CheckBox mConnectionNotifications;
    @InjectView(R.id.radiusSearchText) TextView mSearchRadiusButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Message.message(this, "onCreate in Settings is called");
        setContentView(R.layout.activity_settings);
        ButterKnife.inject(this);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);

        mCurrentUser = ParseUser.getCurrentUser();

        mDiscoveryCheckBox.setOnCheckedChangeListener(this);
        mGeneralNotifications.setOnCheckedChangeListener(this);
        mMessageNotifications.setOnCheckedChangeListener(this);
        mConnectionNotifications.setOnCheckedChangeListener(this);

        mDiscoverable = (Boolean) mCurrentUser.get(Constants.DISCOVERABLE);
        mGeneralNot = (Boolean) mCurrentUser.get(Constants.GENERAL_NOTIFICATIONS);
        mMessageNot = (Boolean) mCurrentUser.get(Constants.MESSAGE_NOTIFICATIONS);
        mConnectionNot = (Boolean) mCurrentUser.get(Constants.CONNECTION_NOTIFICATIONS);

        setChecks(mDiscoverable, mDiscoveryCheckBox);
        setChecks(mGeneralNot, mGeneralNotifications);
        setChecks(mMessageNot, mMessageNotifications);
        setChecks(mConnectionNot, mConnectionNotifications);

        /*mPrivacyButton.setOnClickListener(this);
        mTermsButton.setOnClickListener(this);*/
        mSearchRadiusButton.setOnClickListener(this);
        mFontButton.setOnClickListener(this);
        mLogoutButton.setOnClickListener(this);
        mDeleteAccountButton.setOnClickListener(this);
    }

    private void setChecks(Boolean field, CheckBox checkBox) {
        if(field) {
            checkBox.setChecked(true);
        }
        else {
            checkBox.setChecked(false);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        if(v == mDiscoveryCheckBox) {
            mDiscoverable = isChecked;
            mCurrentUser.put(Constants.DISCOVERABLE, mDiscoverable);
            mCurrentUser.saveInBackground();
        }
        else if(v == mGeneralNotifications) {
            if(isChecked) {
                mGeneralNot = true;
                ParsePush.subscribeInBackground(Constants.GENERAL_PUSH);
            }
            else {
                mGeneralNot = false;
                ParsePush.unsubscribeInBackground(Constants.GENERAL_PUSH);
            }
            mCurrentUser.put(Constants.GENERAL_NOTIFICATIONS, mGeneralNot);
            mCurrentUser.saveInBackground();
        }
        else if(v == mMessageNotifications) {
            if(isChecked) {
                mMessageNot = true;
                ParsePush.subscribeInBackground(Constants.MESSAGE_PUSH);
            }
            else {
                mMessageNot = false;
                ParsePush.unsubscribeInBackground(Constants.MESSAGE_PUSH);
            }
            mCurrentUser.put(Constants.MESSAGE_NOTIFICATIONS, mMessageNot);
            mCurrentUser.saveInBackground();
        }
        else if(v == mConnectionNotifications) {
            if(isChecked) {
                mConnectionNot = true;
                ParsePush.subscribeInBackground(Constants.CONNECTION_PUSH);
            }
            else {
                mConnectionNot = false;
                ParsePush.unsubscribeInBackground(Constants.CONNECTION_PUSH);
            }
            mCurrentUser.put(Constants.CONNECTION_NOTIFICATIONS, mConnectionNot);
            mCurrentUser.saveInBackground();
        }
    }

    @Override
    public void onClick(View v) {
        /*if(v == mPrivacyButton) {
            Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show();
        }
        else if(v == mTermsButton) {
            Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show();
        }*/
        if(v == mDeleteAccountButton) {
            Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show();
        }
        else if (v == mSearchRadiusButton) {
            FragmentManager manager = getFragmentManager();
            //refFragment myPrefs = new PrefFragment();
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            //fragmentTransaction.setCustomAnimations(R.anim.frag_in, R.anim.frag_out, R.anim.frag_in, R.anim.frag_out)
            fragmentTransaction.replace(R.id.container, PrefFragment.newInstance())
                    //.addToBackStack("")
                    .commit();
        }
        else if(v == mLogoutButton) {
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.dialog_title_logout))
                    .content(getString(R.string.dialog_content_logout))
                    .positiveText(getString(R.string.dialog_positive_logout))
                    .negativeText(getString(R.string.dialog_negative))
                    .negativeColorRes(R.color.primary_text)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            //ParseFacebookUtils.getSession().closeAndClearTokenInformation();
                            ParseUser.logOut();

                            Intent intent = new Intent(SettingsActivity.this, LauncherActivity.class);
                            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
                        }
                    })
                    .show();
        } else if(v == mFontButton) {
            FragmentManager manager = getFragmentManager();
            //PrefFragment myPrefs = new PrefFragment();
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            //fragmentTransaction.setCustomAnimations(R.anim.frag_in, R.anim.frag_out, R.anim.frag_in, R.anim.frag_out)
            fragmentTransaction.replace(R.id.container, PrefFragment.newInstance())
                            //.addToBackStack("")
                    .commit();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        /*Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);*/
    }

    @Override
    protected void onStop(){
        super.onStop();
        //Message.message(this, "onStop in Settings is called");
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
    }
}

