package com.fitfinder.fitfinder.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.fragments.FitFinderFragment;
import com.fitfinder.fitfinder.fragments.MarkerDialogFragment;
import com.fitfinder.fitfinder.utils.ConnectionDetector;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.Message;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SearchMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double currentLong;
    private double currentLat;

    private LatLng destPosition;
    protected double destLong;
    protected double destLat;

    private ParseUser mCurrentUser;
    private ParseUser mUser;
    private List<String> mIndices = new ArrayList<>();
    private List<ParseUser> currentUsers = new ArrayList<>();
    private Bitmap markerBitmap;
    private List<String> mCurrentRelations;
    //String currentActivity;
    private MarkerDialogFragment markerDialogFragment;
    static HashMap<String,String> extramarkerData = new HashMap<String,String>();
    //public static HashMap<String, ParseUser> chatRecipient =new HashMap<String,ParseUser>();

    @InjectView(R.id.chatFrag) CardView mCardView;
    private FitFinderFragment mFitFinderFragment;


    public static boolean profFragShowing;


    private Toolbar mToolbar;
    private String markerPic;
    private SharedPreferences sharedPreferences;
    private boolean fromMarker2;
    private ProgressDialog pd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_map);
        //currentActivity = "mainActivity";
        ButterKnife.inject(this);
        // Show the ProgressDialog on this thread
        //this.pd = ProgressDialog.show(this, "Working..", "Getting connections...", true, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        profFragShowing = false;
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.searchResults);

        Intent intent = getIntent();
        fromMarker2 = intent.getBooleanExtra("fromMarker2", false);

        if (checkConnection()) return;
    }

    public Integer getSearchRadius() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String distanceKey = "pref_searchRadius";
        String distanceValue = sharedPreferences.getString(distanceKey, "25");
        Integer distanceInt = new Integer(distanceValue);
        Log.d("MyApp", "search distance = " + distanceValue);
        return distanceInt;
    }

    private void runQuery() {
        Integer searchDistance = getSearchRadius();

        if (checkConnection()) return;

        mCurrentUser = ParseUser.getCurrentUser();
        ParseGeoPoint userLocation = (ParseGeoPoint) mCurrentUser.get(Constants.GEOPOINT);
        currentLat = userLocation.getLatitude();
        currentLong = userLocation.getLongitude();




        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereWithinMiles(Constants.GEOPOINT, userLocation, searchDistance);
        query.whereNotEqualTo(Constants.OBJECT_ID, mCurrentUser.getObjectId());
        query.whereNotEqualTo(Constants.DISCOVERABLE, false);

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

        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < parseUsers.size(); i++) {
                        int min = 2;
                        int max = 14;
                        Random r = new Random();
                        double plus = ((r.nextInt(max - min + 1) + min) * .01);
                        double minus = (((r.nextInt(max - min + 1) + min) * .01) * -1);

                        //Log.d("myapp" , "user's name = " + parseUsers.get(i).getUsername());
                        ParseGeoPoint userLocation = (ParseGeoPoint) parseUsers.get(i).get(Constants.GEOPOINT);

                        currentLat = userLocation.getLatitude() + plus;
                        currentLong = userLocation.getLongitude() + minus;
                        final LatLng thisUserLocation = new LatLng(currentLat, currentLong);
                        final String userId = parseUsers.get(i).getObjectId();
                        final String title = (String) parseUsers.get(i).get(Constants.NAME);
                        final ParseFile profPic = (ParseFile) parseUsers.get(i).get(Constants.PROFILE_IMAGE);




                        if (profPic != null) {
                            profPic.getDataInBackground(new GetDataCallback() {
                                @Override
                                public void done(byte[] data, ParseException e) {
                                    if (e == null) {
                                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        Bitmap resized = Bitmap.createScaledBitmap(bmp, 108, 108, true);
                                        Marker m = mMap.addMarker(new MarkerOptions()
                                                .position(thisUserLocation)
                                                .icon(BitmapDescriptorFactory.fromBitmap(resized))
                                                //.snippet(userId)
                                                .title(title)
                                                .flat(true));
                                        extramarkerData.put(m.getId(), userId);
                                        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                                            @Override
                                            public void onInfoWindowClick(Marker marker) {

                                                destPosition = marker.getPosition();
                                                destLat = destPosition.latitude;
                                                destLong = destPosition.longitude;
                                                String thisParseUserId = extramarkerData.get(marker.getId());

                                                markerDialogFragment = new MarkerDialogFragment();
                                                Bundle args = new Bundle();


                                                args.putString("userId", thisParseUserId);
                                                args.putString("id", marker.getId());
                                                //args.putString("userId", marker.getSnippet());
                                                args.putString("title", marker.getTitle());
                                                args.putDouble("latitude", destLat);
                                                args.putDouble("longitude", destLong);
                                                markerDialogFragment.setArguments(args);
                                                markerDialogFragment.show(getFragmentManager(), "");
                                            }
                                        });

                                    } else {
                                        Log.d("myapp", "problem downloading data");
                                    }
                                }
                            });


                            /*mMap.addMarker(new MarkerOptions()
                                    .position(thisUserLocation)
                                    .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                    .title(title));*/
                        } else {
                            /*userLocation = (ParseGeoPoint) parseUsers.get(i).get(Constants.GEOPOINT);
                            currentLat = userLocation.getLatitude() + plus;
                            currentLong = userLocation.getLongitude() + minus;
                            final LatLng thisUserLocation2 = new LatLng(currentLat, currentLong);
                            final String title2 = (String) parseUsers.get(i).get(Constants.NAME);*/

                            if (parseUsers.get(i).has("profile")) {
                                JSONObject userProfile = parseUsers.get(i).getJSONObject("profile");

                                try {
                                    if (userProfile.has("facebookId")) {
                                        String fbId = userProfile.getString("facebookId");
                                        //Log.d(Constants.TAG, "user with FB ID = " + title2);

                                        //need to run this code in the background and return the markerBitmap
                                /*new DownloadImageTask((ImageView) findViewById(R.id.hiddenImageView))
                                        .execute("https://graph.facebook.com/" + fbId + "/picture?type=large");*/
                                        new DownloadImageTask(thisUserLocation, title, userId)
                                                .execute("https://graph.facebook.com/" + fbId + "/picture?width=108&height=108");
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }


                            } else {
                                final Marker m = mMap.addMarker(new MarkerOptions()
                                        .position(thisUserLocation)
                                        //.snippet(userId)
                                        .title(title)
                                        .flat(true));
                                extramarkerData.put(m.getId(), userId);

                                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                                    @Override
                                    public void onInfoWindowClick(Marker marker) {

                                        destPosition = marker.getPosition();
                                        destLat = destPosition.latitude;
                                        destLong = destPosition.longitude;
                                        String thisParseUserId = extramarkerData.get(marker.getId());

                                        MarkerDialogFragment markerDialogFragment = new MarkerDialogFragment();
                                        Bundle args = new Bundle();
                                        args.putString("userId", thisParseUserId);
                                        args.putString("id", marker.getId());
                                        //args.putString("userId", marker.getSnippet());
                                        args.putString("title", marker.getTitle());
                                        args.putDouble("latitude", destLat);
                                        args.putDouble("longitude", destLong);
                                        markerDialogFragment.setArguments(args);
                                        markerDialogFragment.show(getFragmentManager(), "");
                                    }
                                });
                            }
                        }

                    }


                    Log.d("myapp", "number of returned users = " + parseUsers.size());
                    if (!parseUsers.isEmpty() && parseUsers != null) {
                    }
                }
            }
        });
    }

    private boolean checkConnection() {
        ConnectionDetector detector = new ConnectionDetector(this);
        if(!detector.isConnectingToInternet()) {
            //Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_LONG).show();
            Message.message(this, getString(R.string.no_connection));


            return true;
        }
        return false;
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
            Intent intent = new Intent(SearchMapActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        runQuery();

        // Add a marker in Brazil and move the camera
       /* LatLng jorge = new LatLng(-7.9914, -38.2983);
        mMap.addMarker(new MarkerOptions()
                .position(jorge)
                .title("Marker in Serra Talhada"));*/
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMyLocationEnabled(true);;
        //Test moving camera to user's current location/search location

        mCurrentUser = ParseUser.getCurrentUser();
       /* ParseGeoPoint userLocation = (ParseGeoPoint) mCurrentUser.get(Constants.GEOPOINT);
        currentLat = userLocation.getLatitude();
        currentLong = userLocation.getLongitude();*/
        LatLng searchLocation = new LatLng(currentLat,currentLong);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(searchLocation)
                .zoom(10)
                .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.pd != null) {
            this.pd.dismiss();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        String userId;
        ImageView bmImage;
        Bitmap markerBmp;
        LatLng thisUserLocation;
        String title;
        String snippet;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        public DownloadImageTask(LatLng thisUserLocation, String title, String userId) {
            this.thisUserLocation = thisUserLocation;
            this.title = title;
            this.userId = userId;
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

        @Override
        protected void onPostExecute(Bitmap result) {
            //bmImage.setImageBitmap(result);

            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(thisUserLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(result))
                    //.snippet(snippet)
                    .title(title)
                    .flat(true));
            extramarkerData.put(m.getId(), userId);

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {

                    destPosition = marker.getPosition();
                    destLat = destPosition.latitude;
                    destLong = destPosition.longitude;
                    String thisParseUserId = extramarkerData.get(marker.getId());



                    markerDialogFragment = new MarkerDialogFragment();
                    Bundle args = new Bundle();
                    args.putString("userId", thisParseUserId);
                    args.putString("id", marker.getId());
                    //args.putString("userId", marker.getSnippet());
                    args.putString("title", marker.getTitle());
                    args.putDouble("latitude", destLat);
                    args.putDouble("longitude", destLong);
                    markerDialogFragment.setArguments(args);
                    markerDialogFragment.show(getFragmentManager(), "");
                }
            });

            //Log.d(Constants.TAG, "result = " + result.toString());

        }
    }

    @Override
    public void onBackPressed() {

        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0 ){
            //super.onBackPressed();
            getFragmentManager().popBackStack();
        /*if (profFragShowing) {
            mFitFinderFragment = MarkerDialogFragment.mFitFinderFragment;
            Log.d("MyApp","fitfinder frag = " + mFitFinderFragment);
            getFragmentManager().beginTransaction()
                    .remove(mFitFinderFragment)
                    .commit();
            profFragShowing = false;
            super.onBackPressed();*/
        } else {
            if(fromMarker2) {
                fromMarker2 = false;
                Intent intent = new Intent(SearchMapActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.hold);
            } else {
                super.onBackPressed();
            }
        }
    }
}
