package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * User Adapter for Chat User List.
 * Created by Abraham on 15/09/16.
 */
public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.MyViewHolder> {

    int MAX_LENGTH_SHOW = 40;

    private String uid;
    private List<Uri> userImage;
    private List<String> userName;
    private List<String> userMessage;
    private List<String> userTime;
    private List<String> userChatID;
    private List<String> userLastMessageId;
    private List<Boolean> userMessageRead;


    private Context mContext;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView textName;
        TextView textMessage;
        TextView textTime;
        ImageView imageView;

        public MyViewHolder(View view) {
            super(view);

            textName = (TextView) view.findViewById(R.id.user_alias);
            textMessage = (TextView) view.findViewById(R.id.user_last_message);
            textTime = (TextView) view.findViewById(R.id.user_time);
            imageView = (ImageView) view.findViewById(R.id.user_list_image);
        }
    }

    // Constructor
    public ChatRecyclerAdapter(Context context, String uid, List<Uri> urls, List<String> name, List<String> message, List<String> time, List<String> chatID, List<String> userLastMessageId, List<Boolean> userMessageRead) {

        this.mContext = context;
        this.uid = uid;
        this.userImage = urls;
        this.userName = name;
        this.userMessage = message;
        this.userTime = time;
        this.userChatID = chatID;
        this.userLastMessageId =userLastMessageId;
        this.userMessageRead = userMessageRead;
    }

    @Override
    public ChatRecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);

        //MyViewHolder viewHolder = new MyViewHolder(view);
        return new MyViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        holder.textName.setText(userName.get(position));
        holder.textMessage.setText(StringUtils.abbreviate(userMessage.get(position), MAX_LENGTH_SHOW));
        holder.textTime.setText(userTime.get(position));
        holder.imageView.setBackgroundColor(Color.TRANSPARENT);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uniqueChatID = userChatID.get(holder.getAdapterPosition());
                String userNameChat = userName.get(holder.getAdapterPosition());

                Intent intent = new Intent(view.getContext(), ChatActivity.class);
                intent.putExtra("userChat", uniqueChatID);
                intent.putExtra("userName", userNameChat);
                view.getContext().startActivity(intent);
            }
        });

        if (!userMessageRead.get(position) && !userLastMessageId.get(position).equals(uid)){
            holder.textName.setTypeface(null, Typeface.BOLD);
            holder.textMessage.setTypeface(null, Typeface.BOLD);
        } else {
            holder.textName.setTypeface(null, Typeface.NORMAL);
            holder.textMessage.setTypeface(null, Typeface.NORMAL);
        }

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
