package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatUserList extends AppCompatActivity {
    final static String TAG = "Chat: ";

    FirebaseUser user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageReference = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

    //ListView listView;
    List<String> userName = new ArrayList<>();
    List<String> userKey = new ArrayList<>();
    List<String> userLastMessage = new ArrayList<>();
    List<String> userTimeStamp = new ArrayList<>();
    List<String> userChatID = new ArrayList<>();
    List<Uri> userProfilePic = new ArrayList<>();
    //ChatUserAdapter chatUserAdapter;

    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_user_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        cleanVars();
        user = FireConnection.getInstance().getUser();

        /*chatUserAdapter = new ChatUserAdapter(getApplicationContext(), userProfilePic, userName, userLastMessage, userTimeStamp);
        listView = (ListView) findViewById(R.id.user_list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String uniqueChatID = userChatID.get(position);
                String userNameChat = userName.get(position);

                Intent intent = new Intent(getApplicationContext(), ChatHLMActivity.class);
                intent.putExtra("userChat", uniqueChatID);
                intent.putExtra("userName", userNameChat);
                startActivity(intent);
            }
        });

        listView.setAdapter(chatUserAdapter);*/

        mRecyclerView = (RecyclerView) findViewById(R.id.user_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);

        // specify an adapter (see also next example)
        mAdapter = new ChatRecyclerAdapter(getApplicationContext(), userProfilePic, userName, userLastMessage, userTimeStamp);
        mRecyclerView.setAdapter(mAdapter);

        if (user!= null){
            getUsers();
        }
    }
    @Override
    public void onBackPressed() {
            //super.onBackPressed();
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
    }
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home ) {
            //NavUtils.navigateUpFromSameTask(this);
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
            return true;
        }
        return true;
    }

    private void getUsers() {
        DatabaseReference databaseReferenceMyUsers = database.getReference().child("users").child(user.getUid()).child("my_chats");
        databaseReferenceMyUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {

                    userKey.add(data.getKey());
                    userChatID.add(data.getValue().toString());
                    userName.add("");
                    userProfilePic.add(null);
                    userLastMessage.add("");
                    userTimeStamp.add("");

                    Log.i(TAG, "Chat Key: " + data.getKey());
                    Log.i(TAG, "Chat Id: " + data.getValue());
                }
                getData(userKey.size());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                }
            });


    }
    private void getData(long size){
        Log.d(TAG, "Looking data for: " + userKey);
        //int size = userKey.size();
        final DatabaseReference databaseReferenceUsers = database.getReference().child("users");
        final DatabaseReference databaseReferenceLastMessage = database.getReference().child("chats_resume");
        for(int i = 0; i < size; i++){
            final int position = i;
            Log.d(TAG, "Looking data for: " + userKey.get(position));
            databaseReferenceUsers.child(userKey.get(position))
                    .child("preferences").child("alias")
                    .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.i(TAG, "Alias found: " + dataSnapshot.getValue());
                    userName.set(position, dataSnapshot.getValue().toString());
                    mAdapter.notifyDataSetChanged();
                    Log.d(TAG, "User Name: " + userName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            storageReference.child(userKey.get(position))
                    .child("profile_pic")
                    .child("profile_im.jpg")
                    .getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Log.d(TAG, "Pic URL: " + uri);
                    userProfilePic.set(position, uri);
                    mAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Pic URL: " + userProfilePic);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Log.d(TAG, "Pic Not Available");
                    userProfilePic.set(position, null);
                    mAdapter.notifyDataSetChanged();
                }
            });
            databaseReferenceLastMessage
                    .child(userChatID.get(position))
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            userLastMessage.set(position, dataSnapshot.child("text").getValue().toString());
                            userTimeStamp.set(position, dateFormatter(dataSnapshot.child("timeStamp").getValue().toString()));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    }

    /*private void touched () {
        for (int i=0; i < listView.getChildCount(); i ++){
            listView.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    int xRaw = (int) event.getRawX();
                    int yRaw = (int) event.getRawY();

                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:

                            break;
                        case MotionEvent.ACTION_UP:
                            //v.setTranslationY(0);
                            v.setTranslationX(0);

                            break;
                        case MotionEvent.ACTION_MOVE:
                            v.setX((v.getX() - v.getWidth() / 2) + x);
                            //v.setY((v.getY() - v.getHeight() / 2) + y);

                            break;
                    }
                    return false;
                }
            });;
        }
    } */

    private String dateFormatter (String millis) {

        Long currentDateTime = Long.parseLong(millis);
        Date currentDate = new Date(currentDateTime);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm", Locale.US);
        System.out.println(sdf.format(currentDate));

        return sdf.format(currentDate);
    }

    private void cleanVars(){
        userKey.clear();
        userName.clear();
        userProfilePic.clear();
        userLastMessage.clear();
        userTimeStamp.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getData(userKey.size());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
