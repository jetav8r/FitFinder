package com.fitfinder.fitfinder.activities;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.fitfinder.fitfinder.utils.PlaceAutocompleteAdapter;
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

import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.utils.Constants;

import java.io.IOException;
import java.sql.BatchUpdateException;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class OnBoardActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
    protected GoogleApiClient mGoogleApiClient;
    private PlaceAutocompleteAdapter mAdapter;

    private String mSexGender;
    private String mGenderPref;
    private String mLookingFor;
    private Double mLat;
    private Double mLng;
    private String mPlace;
    private Toolbar mToolbar;
    private String mName;

    @InjectView(R.id.locationField) AutoCompleteTextView locationField;
    @InjectView(R.id.profileNameText) EditText profileNameField;
    @InjectView(R.id.sex_genderGroup) RadioGroup mSexGenderGroup;
    @InjectView(R.id.genderGroup) RadioGroup mGenderGroup;
    @InjectView(R.id.lookingForGroup) RadioGroup mLookingForGroup;
    @InjectView(R.id.cancelButton) Button mCancelButton;
    //@InjectView(R.id.submit_button) Button mSetPrefButton;
    @InjectView(R.id.submit_button) View mSubmitButton;
    private String currentToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();
        setContentView(R.layout.activity_on_board);
        ButterKnife.inject(this);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.profileInfo);

        //AutoCompleteTextView locationField = (AutoCompleteTextView) findViewById(R.id.locationField);
        //placesField.setOnItemClickListener(this);
        //LocationAutocompleteUtil.setAutoCompleteAdapter(this, placesField);
        locationField.setOnItemClickListener(mAutocompleteClickListener);
        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, BOUNDS_GREATER_SYDNEY,
                null);
        locationField.setAdapter(mAdapter);

        mSexGenderGroup.setOnCheckedChangeListener(this);
        mGenderGroup.setOnCheckedChangeListener(this);
        mLookingForGroup.setOnCheckedChangeListener(this);
        mCancelButton.setOnClickListener(this);
        //mSetPrefButton.setOnClickListener(this);
        mSubmitButton.setOnClickListener(this);


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

            if (mSexGenderGroup.getCheckedRadioButtonId() != -1 && mGenderGroup.getCheckedRadioButtonId() != -1 &&
                    mLookingForGroup.getCheckedRadioButtonId() != -1) {
                //mSetPrefButton.setEnabled(true);
                mSubmitButton.setVisibility(View.VISIBLE);
            }
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

            /*// Format details of the place for display and show it in a TextView.
            mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
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


    /**
     * This method handles the check responses for the radio groups for setting preferences.
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sex_maleCheckBox:
                mSexGender = Constants.MALE;
                break;
            case R.id.sex_femaleCheckBox:
                mSexGender = Constants.FEMALE;
                break;

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
        }

        if (mSexGenderGroup.getCheckedRadioButtonId() != -1 && mGenderGroup.getCheckedRadioButtonId() != -1 &&
                mLookingForGroup.getCheckedRadioButtonId() != -1 && mLat != null) {
            //mSetPrefButton.setEnabled(true);
            mSubmitButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This method gets the location the user selects and extracts the coordinates from it
     */
    /*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPlace = (String) parent.getItemAtPosition(position);

        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocationName(mPlace, 1);
            if (addresses.size() > 0) {
                mLat = addresses.get(0).getLatitude();
                mLng = addresses.get(0).getLongitude();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mSexGenderGroup.getCheckedRadioButtonId() != -1 && mGenderGroup.getCheckedRadioButtonId() != -1 &&
                mLookingForGroup.getCheckedRadioButtonId() != -1) {
            //mSetPrefButton.setEnabled(true);
            mSubmitButton.setVisibility(View.VISIBLE);
        }
    }*/

    /**
     * This method  handles saving the new parse user when the user selects to finish onboarding
     */
    @Override
    public void onClick(View v) {
        //if (v == mSetPrefButton) {
        if (v == mSubmitButton) {
            //ParseUser.getCurrentUser().put(Constants.ALREADY_ONBOARD, true);
            //ParseUser.getCurrentUser().saveInBackground();
            mName =  profileNameField.getText().toString();
            ParseGeoPoint geoPoint = new ParseGeoPoint(mLat, mLng);

            ParseUser user = ParseUser.getCurrentUser();
            currentToken = user.getSessionToken();
            user.put(Constants.NAME,mName);
            user.put(Constants.ALREADY_ONBOARD, true);
            user.put(Constants.LOCATION, mPlace);
            user.put(Constants.GEOPOINT, geoPoint);
            user.put(Constants.GENDER, mSexGender);
            user.put(Constants.GENDER_PREF, mGenderPref);
            user.put(Constants.LOOKING_FOR, mLookingFor);
            user.put(Constants.ABOUT_ME, "");

            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(OnBoardActivity.this, getString(R.string.toast_account_created),
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(OnBoardActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
                    } else {
                        Toast.makeText(OnBoardActivity.this, getString(R.string.toast_error_request),
                                Toast.LENGTH_LONG).show();
                        Log.d("MyApp" , "Parse error = " + e);
                    }
                }
            });
        } else if (v == mCancelButton) {
            Intent intent = new Intent(OnBoardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_quick, R.anim.slide_out_right);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("MyApp", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }
}
