package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by developer on 27/06/16.
 */
public class ImageAdapter extends ArrayAdapter {
    private final String TAG_i = "Image Resource: ";

    private List<String> imageUrls;

    private Context mContext;
    private LayoutInflater inflater;

    // Constructor
    public ImageAdapter(Context context, List<String> urls) {
        super(context, R.layout.grid_item_layout, urls);

        mContext = context;
        imageUrls = urls;

        //Log.i(TAG_i,"URLS: " + imageUrls);

        inflater = LayoutInflater.from(context);
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item_layout, parent, false);
        }

        Uri myUri = Uri.parse(imageUrls.get(position));

        Glide
                .with(mContext)
                .load(myUri)
                .centerCrop()
                .into((ImageView) convertView);

        return convertView;
    }

}
