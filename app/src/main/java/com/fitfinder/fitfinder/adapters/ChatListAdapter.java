package com.fitfinder.fitfinder.adapters;

/**
 * Created by Wayne on 4/23/2015.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fitfinder.fitfinder.R;
import com.fitfinder.fitfinder.application.FitFinderApplication;
import com.fitfinder.fitfinder.utils.Constants;
import com.fitfinder.fitfinder.utils.FbUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends ArrayAdapter<ParseObject> {

    private Context mContext;
    private ArrayList<ParseObject> mRelations;
    private DisplayImageOptions options;
    private ImageLoader imageLoader;

    public ChatListAdapter(Context context, List<ParseObject> objects) {
        super(context, R.layout.chat_item_adapter, objects);

        this.mContext = context;
        this.mRelations = (ArrayList<ParseObject>) objects;

        imageLoader = FitFinderApplication.getImageLoaderInstance();
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        ParseObject relation = null;
        try {
            relation = mRelations.get(position).fetchIfNeeded();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.chat_item_adapter, null);

            holder = new ViewHolder();
            holder.profImageField = (ImageView) convertView.findViewById(R.id.profImage);
            holder.nameField = (TextView) convertView.findViewById(R.id.nameField);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.imageProgressBar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ParseUser user = null;
        ParseUser currentUser = ParseUser.getCurrentUser();

        if (relation != null) {
            ParseUser user1 = (ParseUser) relation.get(Constants.USER1);
            String userId = user1.getObjectId();

            if (userId.equals(currentUser.getObjectId())) {
                user = (ParseUser) relation.get(Constants.USER2);
            }
            else {
                user = (ParseUser) relation.get(Constants.USER1);
            }
        }

        String name = "";
        ParseFile profImage = null;
        if (user != null) {
            name = (String) user.get(Constants.NAME);
            profImage = (ParseFile) user.get(Constants.PROFILE_IMAGE);
        }

        holder.nameField.setText(name);

        if (profImage != null) {
            Glide.with(holder.profImageField.getContext())
                    .load(profImage.getUrl())
                    .crossFade()
                    .fallback(R.drawable.ic_def_pic)
                    .error(R.drawable.ic_def_pic)
                    .into(holder.profImageField);
            /*//imageLoader.displayImage(profImage.getUrl(), holder.profImageField, options, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String s, View view) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingFailed(String s, View view, FailReason failReason) {}

                @Override
                public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                    holder.progressBar.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onLoadingCancelled(String s, View view) {}
            });*/
        }/*else {
            String fbId = FbUtils.getCurrentUserFbId();
            imageLoader.displayImage("https://graph.facebook.com/" + fbId + "/picture?type=large", holder.profImageField);
        }*/

        return convertView;
    }

    private static class ViewHolder {
        ImageView profImageField;
        TextView nameField;
        ProgressBar progressBar;
    }
}

