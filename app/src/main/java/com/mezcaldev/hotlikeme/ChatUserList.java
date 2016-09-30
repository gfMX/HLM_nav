package com.mezcaldev.hotlikeme;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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

import static com.facebook.FacebookSdk.getApplicationContext;

public class ChatUserList extends ListFragment {
    final static String TAG = "Chat: ";

    static ChatUserList newInstance() {
        ChatUserList newFragment = new ChatUserList();

        return newFragment;
    }

    FirebaseUser user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageReference = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

    List<String> userName = new ArrayList<>();
    List<String> userKey = new ArrayList<>();
    List<String> userLastMessage = new ArrayList<>();
    List<String> userTimeStamp = new ArrayList<>();
    List<String> userChatID = new ArrayList<>();
    List<Uri> userProfilePic = new ArrayList<>();

    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cleanVars();
        user = FireConnection.getInstance().getUser();

        if (user!= null){
            getUsers();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_chat_user_list, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        mRecyclerView = (RecyclerView) view.findViewById(R.id.user_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);

        // specify an adapter (see also next example)
        mAdapter = new ChatRecyclerAdapter(getApplicationContext(), userProfilePic, userName, userLastMessage, userTimeStamp, userChatID);
        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper mIth = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();
                        // move item in `fromPos` to `toPos` in adapter.
                        return true;// true if moved, false otherwise
                    }
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        // remove from adapter
                    }
                });
        mIth.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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
                            String lasteMessageText;
                            String lastDateFromMessage;
                            if (dataSnapshot.child("text").getValue() != null){
                                lasteMessageText = dataSnapshot.child("text").getValue().toString();
                            } else {
                                lasteMessageText = "Sorry! The last Message wasn't found!";
                            }
                            if (dataSnapshot.child("timeStamp").getValue() != null){
                                lastDateFromMessage = dateFormatter(dataSnapshot.child("timeStamp").getValue().toString());
                            } else {
                                lastDateFromMessage = "Date Not Found!";
                            }
                            userLastMessage.set(position, lasteMessageText);
                            userTimeStamp.set(position, lastDateFromMessage);
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }
    }

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
