package com.fitfinder.fitfinder.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.fitfinder.fitfinder.R;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Wayne on 9/29/2015.
 */
public class FbUtils {

    public static String getCurrentUserFbId() {

        String fbId = null;
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser.has("profile")) {
            JSONObject userProfile = currentUser.getJSONObject("profile");
            try {
                /*Bundle parametersPicture = new Bundle();
                parametersPicture.putString("fields", "picture.width(450).height(450)");*/
                if (userProfile.has("facebookId")) {
                    fbId = userProfile.getString("facebookId");
                    /*profilePictureView.setPresetSize(ProfilePictureView.LARGE);
                    profilePictureView.setProfileId(userProfile.getString("facebookId"));*/

                    //pictureURL = "http://graph.facebook.com/"+fbId+"/picture?type=large";
                    Log.d("Wayne", "FB ID = " + fbId);

                } else {
                    // Show the default, blank user profile picture
                    //userProfilePictureView.setProfileId(null);
                }


                /*if (userProfile.has("name")) {
                    userNameView.setText(userProfile.getString("name"));
                } else {
                    userNameView.setText("");
                }*/


            } catch (JSONException e) {
                Log.d("Wayne", "Error parsing saved user data.");
            }
        }

        return fbId;
    }



    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        ParseUser mCurrentUser = ParseUser.getCurrentUser();

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
            //bmImage.setImageBitmap(result);
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
