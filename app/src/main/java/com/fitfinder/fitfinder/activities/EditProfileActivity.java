package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.Message;
import com.fitfinder.fitfinder.utils.PlaceAutocompleteAdapter;
import com.fitfinder.fitfinder.widgets.ToggleableRadioButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EditProfileActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener, ToggleableRadioButton.UnCheckListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
    private String mGenderPref;
    private String mLookingFor;
    private Boolean mSmokes;
    private Boolean mDrinks;
    private Double mLat;
    private Double mLng;
    private String mPlace;
    private ParseUser mCurrentUser;
    private String mLocation;
    private String currentToken;
    private Toolbar mToolbar;

    protected GoogleApiClient mGoogleApiClient;
    private PlaceAutocompleteAdapter mAdapter;

    @InjectView(R.id.locationField) AutoCompleteTextView locationField;

    @InjectView(R.id.genderGroup) RadioGroup genderPrefGroup;
    @InjectView(R.id.lookingForGroup) RadioGroup lookingForGroup;

    @InjectView(R.id.aboutMe) EditText aboutMeField;
    @InjectView(R.id.profileNameText) EditText usernameField;
    /*@InjectView(R.id.yesDrinkCheckBox) CheckBox yesDrink;
    @InjectView(R.id.noDrinkCheckBox) CheckBox noDrink;
    @InjectView(R.id.yesSmokeCheckBox) CheckBox yesSmoke;
    @InjectView(R.id.noSmokeCheckBox) CheckBox noSmoke;*/
    @InjectView(R.id.save_update_button) View saveUpdate;
    @InjectView(R.id.cancelButton) View cancelUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Construct a GoogleApiClient for the {@link Places#GEO_DATA_API} using AutoManage
        // functionality, which automatically sets up the API client to handle Activity lifecycle
        // events. If your activity does not extend FragmentActivity, make sure to call connect()
        // and disconnect() explicitly.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();
        //getSupportActionBar().setTitle(getString(R.string.action_bar_my_profile));
        setContentView(R.layout.activity_edit_profile);
        ButterKnife.inject(this);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.editProfile);

        mCurrentUser = ParseUser.getCurrentUser();
        currentToken = mCurrentUser.getSessionToken();

        saveUpdate.setOnClickListener(this);
        cancelUpdate.setOnClickListener(this);

        genderPrefGroup.setOnCheckedChangeListener(this);
        lookingForGroup.setOnCheckedChangeListener(this);

        /*yesSmoke.setOnCheckedChangeListener(this);
        noSmoke.setOnCheckedChangeListener(this);
        yesDrink.setOnCheckedChangeListener(this);
        noDrink.setOnCheckedChangeListener(this);*/
        mLocation = (String) mCurrentUser.get(Constants.LOCATION);
        mGenderPref = (String) mCurrentUser.get(Constants.GENDER_PREF);
        mLookingFor = (String) mCurrentUser.get(Constants.LOOKING_FOR);
        /*mSmokes = (Boolean) mCurrentUser.get(Constants.SMOKES);
        mDrinks = (Boolean) mCurrentUser.get(Constants.DRINKS);*/
        String aboutMeText = (String) mCurrentUser.get(Constants.ABOUT_ME);
        String usernameText = (String) mCurrentUser.get(Constants.NAME);

        locationField.setText(mLocation);
        aboutMeField.setText(aboutMeText);
        usernameField.setText(usernameText);

        //LocationAutocompleteUtil.setAutoCompleteAdapter(this, locationField);
        locationField.setOnItemClickListener(mAutocompleteClickListener);
        //locationField.setOnItemClickListener(this);
        //locationField.setListSelection(0);

        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, BOUNDS_GREATER_SYDNEY,
                null);
        locationField.setAdapter(mAdapter);

        switch (mGenderPref) {
            case Constants.MALE:
                genderPrefGroup.check(R.id.maleCheckBox);
                break;
            case Constants.FEMALE:
                genderPrefGroup.check(R.id.femaleCheckBox);
                break;
            case Constants.BOTH:
                genderPrefGroup.check(R.id.bothCheckBox);
                break;
        }


        switch (mLookingFor) {
            case Constants.TRAINER:
                lookingForGroup.check(R.id.trainerCheckBox);
                break;
            case Constants.OFFER:
                lookingForGroup.check(R.id.trainerOfferCheckBox);
                break;
            case Constants.PARTNER:
                lookingForGroup.check(R.id.partnerCheckBox);
                break;
        }


        /*setCheckedItems(mSmokes, yesSmoke, noSmoke);
        setCheckedItems(mDrinks, yesDrink, noDrink);*/
    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            mPlace = item.getDescription();
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);
            locationField.setText(mPlace);
            //locationField.setText(place.getAddress());

            Log.i("MyApp", "Autocomplete item selected: " + primaryText);


            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);

            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);


            /*Toast.makeText(getApplicationContext(), "Clicked: " + primaryText,
                    Toast.LENGTH_SHORT).show();*/
            Log.i("MyApp", "Called getPlaceById to get Place details for " + placeId);
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e("MyApp", "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);
            final LatLng placeLatLng = place.getLatLng();
            mLat = placeLatLng.latitude;
            mLng = placeLatLng.longitude;


            // Format details of the place for display and show it in a TextView.

            /*mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
                    place.getId(), place.getAddress(), place.getPhoneNumber(),
                    place.getWebsiteUri()));

            // Display the third party attributions if set.
            final CharSequence thirdPartyAttribution = places.getAttributions();
            if (thirdPartyAttribution == null) {
                mPlaceDetailsAttribution.setVisibility(View.GONE);
            } else {
                mPlaceDetailsAttribution.setVisibility(View.VISIBLE);
                mPlaceDetailsAttribution.setText(Html.fromHtml(thirdPartyAttribution.toString()));
            }*/

            Log.i("MyApp", "Place details received: " + place.getName());

            places.release();
        }
    };

    /*private static Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                              CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
        Log.e("MyApp", res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));
        return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));

    }*/

    /**
     * Called when the Activity could not connect to Google Play services and the auto manager
     * could resolve the error automatically.
     * In this case the API is not available and notify the user.
     *
     * @param connectionResult can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e("MyApp", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }


    /**
     * This method is called when the activity is created and sets the previously selected
     * values of the radiogroups based upon the users saved profile. This is only used for Yes/No
     * questions
     *
     * @param field This is the boolean value of the questions being checked(true=yes, false=no)
     * @param yes the "yes" checkbox
     * @param no the "no" checkbox
     */
    private void setCheckedItems(Boolean field, CheckBox yes, CheckBox no) {
        if(field != null) {
            if(field) {
                yes.setChecked(true);
            }
            else {
                no.setChecked(true);
            }
        }
    }

    /**
     * This method handles the check responses for the radio groups for setting preferences.
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.maleCheckBox:
                mGenderPref = Constants.MALE;
                break;
            case R.id.femaleCheckBox:
                mGenderPref = Constants.FEMALE;
                break;
            case R.id.bothCheckBox:
                mGenderPref = Constants.BOTH;
                break;

            case R.id.trainerCheckBox:
                mLookingFor = Constants.TRAINER;
                break;
            case R.id.trainerOfferCheckBox:
                mLookingFor = Constants.OFFER;
                break;
            case R.id.partnerCheckBox:
                mLookingFor = Constants.PARTNER;
                break;

            /*case R.id.yesDrinkCheckBox:
                mDrinks = true;
                break;
            case R.id.noDrinkCheckBox:
                mDrinks = false;
                break;*/
        }
    }

    /**
     * This method handles the check responses for the yes/no questions
     */
    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        /*if(v == yesSmoke) {
            if (isChecked) {
                if (noSmoke.isChecked()) {
                    noSmoke.setChecked(false);
                }
                mSmokes = true;
            }
            else {
                mSmokes = null;
            }
        }
        else if(v == noSmoke) {
            if(isChecked) {
                if (yesSmoke.isChecked()) {
                    yesSmoke.setChecked(false);
                }
                mSmokes = false;
            }
            else {
                mSmokes = null;
            }
        }

        if(v == yesDrink) {
            if (isChecked) {
                if (noDrink.isChecked()) {
                    noDrink.setChecked(false);
                }
                mDrinks = true;
            }
            else {
                mDrinks = null;
            }
        }
        else if(v == noDrink) {
            if(isChecked) {
                if (yesDrink.isChecked()) {
                    yesDrink.setChecked(false);
                }
                mDrinks = false;
            }
            else {
                mDrinks = null;
            }
        }*/
    }

    /*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPlace = (String) parent.getItemAtPosition(position);

        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(mPlace, 1);
            if(addresses.size() > 0) {
                mLat = addresses.get(0).getLatitude();
                mLng = addresses.get(0).getLongitude();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    /**
     * This method is called when the user decides to save his profile. It handles whether or not
     * to save a new value of remove the old value from all the yes or no fields
     *
     * @param field This is the boolean value of the fields being saved(true=yes, false=no)
     * @param fieldKey the Parse key of the field to save
     */
    private void saveYesNoFields(Boolean field, String fieldKey) {
        if(field != null) {
            mCurrentUser.put(fieldKey, field);
        }
        else {
            mCurrentUser.remove(fieldKey);
        }
    }

    @Override
     public void onUnchecked(View v) {
        /*if(v == yesDrink || v == noDrink) {
            mDrinks = null;
        }
        else if(v == yesSmoke || v == noSmoke) {
            mSmokes = null;
        }*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_save) {
            ParseUser user = ParseUser.getCurrentUser();
            currentToken = user.getSessionToken();
            ParseUser.becomeInBackground(currentToken, new LogInCallback() {
                public void done(ParseUser user, ParseException e) {
                    if (user != null) {
                        // The current user is now set to user.
                        updateProfile();
                    } else {
                        // The token could not be validated.
                        Message.message(getApplicationContext(), "Session token could not be validated");
                    }
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }


    /*
     * This method is called when the users clicks on the save button on the actionbar. It updates
     * the profile on the Parse backend.
     */
    private void updateProfile() {

        if (mLat == null && !mLocation.equals(locationField.getText().toString())) {
            Toast.makeText(EditProfileActivity.this,
                    getString(R.string.toast_valid_location), Toast.LENGTH_SHORT).show();
        }
        else {
            if (mPlace != null) {
                ParseGeoPoint geoPoint = new ParseGeoPoint(mLat, mLng);
                mCurrentUser.put(Constants.LOCATION, mPlace);
                mCurrentUser.put(Constants.GEOPOINT, geoPoint);
            }

            mCurrentUser.put(Constants.GENDER_PREF, mGenderPref);
            mCurrentUser.put(Constants.LOOKING_FOR, mLookingFor);
            mCurrentUser.put(Constants.NAME, usernameField.getText().toString());
            mCurrentUser.put(Constants.ABOUT_ME, aboutMeField.getText().toString());

            /*saveYesNoFields(mSmokes, Constants.SMOKES);
            saveYesNoFields(mDrinks, Constants.DRINKS);*/

            mCurrentUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(EditProfileActivity.this, getString(R.string.toast_profile_updated),
                                Toast.LENGTH_LONG).show();
                        try {
                            ParseUser.getCurrentUser().fetch();
                        } catch (ParseException e1) {
                            e1.printStackTrace();
                        }
                        Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in_quick, R.anim.slide_out_right);
                    } else {
                        Toast.makeText(EditProfileActivity.this, getString(R.string.toast_error_request),
                                Toast.LENGTH_LONG).show();
                        Log.d("MyApp", "error = " + e);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.save_update_button) {
            ParseUser user = ParseUser.getCurrentUser();
            currentToken = user.getSessionToken();
            ParseUser.becomeInBackground(currentToken, new LogInCallback() {
                public void done(ParseUser user, ParseException e) {
                    if (user != null) {
                        // The current user is now set to user.
                        updateProfile();
                    } else {
                        // The token could not be validated.
                        Message.message(getApplicationContext(), "Session token could not be validated");
                    }
                }
            });
        }
        if(v.getId() == R.id.cancelButton) {
            Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_quick, R.anim.slide_out_right);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}

