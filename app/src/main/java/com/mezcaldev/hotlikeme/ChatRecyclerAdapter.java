package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * User Adapter for Image Selection.
 * Created by Abraham on 27/06/16.
 */
public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.ViewHolder> {
    private final String TAG_i = "Image Resource: ";

    private List<Uri> userImage;
    private List<String> userName;
    private List<String> userMessage;
    private List<String> userTime;


    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView textName;
        TextView textMessage;
        TextView textTime;
        ImageView imageView;


        public ViewHolder(View view) {
            super(view);
            textName = (TextView) view.findViewById(R.id.user_alias);
            textMessage = (TextView) view.findViewById(R.id.user_last_message);
            textTime = (TextView) view.findViewById(R.id.user_time);
            imageView = (ImageView) view.findViewById(R.id.user_list_image);
        }
    }

    // Constructor
    public ChatRecyclerAdapter(Context context, List<Uri> urls, List<String> name, List<String> message, List<String> time) {

        this.mContext = context;
        this.userImage = urls;
        this.userName = name;
        this.userMessage = message;
        this.userTime = time;
    }

    @Override
    public ChatRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);


        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {


        holder.textName.setText(userName.get(position));
        holder.textMessage.setText(userMessage.get(position));
        holder.textTime.setText(userTime.get(position));
        holder.imageView.setBackgroundColor(Color.TRANSPARENT);

        Uri myUri = userImage.get(position);

        if (myUri != null){
            Glide
                    .with(mContext)
                    .load(myUri)
                    .centerCrop()
                    .into(holder.imageView);

        } else {
            holder.imageView.setImageResource(R.drawable.ic_person_gray);
        }

    }

    @Override
    public int getItemCount() {
        return userName.size();
    }


 }
