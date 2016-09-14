package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.util.List;

/**
 * User Adapter for Image Selection.
 * Created by Abraham on 27/06/16.
 */
public class ChatUserAdapter extends ArrayAdapter {
    private final String TAG_i = "Image Resource: ";

    private List<Uri> userImage;
    private List<String> userName;
    private List<String> userMessage;
    private List<String> userTime;

    private Context mContext;
    private LayoutInflater inflater;

    // Constructor
    public ChatUserAdapter(Context context, List<Uri> urls, List<String> name, List<String> message, List<String> time) {
        super(context, R.layout.item_chat_user, urls);

        mContext = context;
        userImage = urls;
        userName = name;
        userMessage = message;
        userTime = time;


        //Log.i(TAG_i,"URLS: " + imageUrls);

        inflater = LayoutInflater.from(context);
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_chat_user, parent, false);
        }

        TextView textName = (TextView) convertView.findViewById(R.id.user_alias);
        TextView textMessage = (TextView) convertView.findViewById(R.id.user_last_message);
        TextView textTime = (TextView) convertView.findViewById(R.id.user_time);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.user_list_image);
        imageView.setBackgroundColor(Color.TRANSPARENT);

        textName.setText(userName.get(position));
        textMessage.setText(userMessage.get(position));
        textTime.setText(userTime.get(position));
        Uri myUri = userImage.get(position);

        if (myUri != null){
            Glide
                    .with(mContext)
                    .load(myUri)
                    .centerCrop()
                    .into(imageView);

        } else {
            imageView.setImageResource(R.drawable.ic_person_gray);
        }

        convertView.setId(position);

        return convertView;
    }

}
