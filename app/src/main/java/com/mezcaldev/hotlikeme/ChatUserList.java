package com.mezcaldev.hotlikeme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.mezcaldev.hotlikeme.FireConnection.ONE_HOUR;
import static com.mezcaldev.hotlikeme.FireConnection.user;
import static com.mezcaldev.hotlikeme.FireConnection.weLike;

public class ChatUserList extends ListFragment {
    final static String TAG = "Chat: ";

    static ChatUserList newInstance() {
        ChatUserList newFragment = new ChatUserList();

        return newFragment;
    }

    //Notifications
    int maxTimeForNotifications = 48;       /* Time in Hours */
    int NOTIFICATION_ID = 71843;
    NotificationManager notificationManager;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageReference = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

    List<String> userName = new ArrayList<>();
    List<String> userKey = new ArrayList<>();
    List<String> userLastMessage = new ArrayList<>();
    List<String> userLastMessageId = new ArrayList<>();
    List<String> userTimeStamp = new ArrayList<>();
    List<String> userChatID = new ArrayList<>();
    List<Uri> userProfilePic = new ArrayList<>();

    Handler handler;
    Runnable runnable;
    int delayTime = 2500;
    Boolean undoFlag = false;

    RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    //Database References:
    DatabaseReference databaseMessageReference;
    DatabaseReference databaseReferenceCurrent;
    DatabaseReference databaseReferenceMyUsers;
    DatabaseReference databaseReferenceUsers;
    DatabaseReference databaseReferenceLastMessage;

    //Value Event Listeners:
    ValueEventListener valueEventListenerMyUsers;
    ValueEventListener valueEventListenerUsers;
    ValueEventListener valueEventListenerLastMessage;

    Bitmap icon;
    Paint paint = new Paint();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cleanVars();

        if (user!= null){
            getUsers();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_chat_user_list, container, false);

        return inflater.inflate(R.layout.activity_chat_user_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        databaseMessageReference = database.getReference();
        databaseReferenceCurrent = database.getReference().child("users").child(user.getUid()).child("like_user");

        notificationManager = (NotificationManager) getActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshUserList);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.user_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(false);

        // specify an adapter (see also next example)
        mAdapter = new ChatRecyclerAdapter(getContext(), userProfilePic, userName, userLastMessage, userTimeStamp, userChatID);
        mRecyclerView.setAdapter(mAdapter);

        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.i(TAG, "Refresh called!");

                        getUsers();
                    }
                }
        );


        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                //final int toPos = target.getAdapterPosition();
                // move item in `fromPos` to `toPos` in adapter.
                System.out.println("Delete here: " + fromPos);
                Toast.makeText(getContext(), "Disconnect from user?", Toast.LENGTH_LONG).show();
                return true;// true if moved, false otherwise
            }

            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // remove from adapter
                if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT){
                    final int position = viewHolder.getAdapterPosition();
                    System.out.println("Delete here: " + position);
                    System.out.println("User: " + userName.get(position) + " Key: " + userKey.get(position));
                    Snackbar snackbar = Snackbar.make(getActivity().getWindow().getDecorView(), "Disconnected from User", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    undoFlag =true;
                                }
                            });
                    snackbar.show();
                    handler = new Handler();
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!undoFlag) {
                                weLike = false;     //Could Cause conflict with User View if not set to False

                                Log.v(TAG, "Data to be removed: " + userKey.get(position) + userChatID.get(position)
                                        + userName.get(position) + userProfilePic.get(position) + userLastMessage.get(position)
                                        + userLastMessageId.get(position) + userTimeStamp.get(position) + " || Position: " + position);

                                try {
                                    databaseReferenceCurrent.child(userKey.get(position)).setValue(null);
                                    databaseMessageReference.child("users").child(user.getUid()).child("my_chats").child(userKey.get(position)).setValue(null);
                                    databaseMessageReference.child("chats_resume").child(userChatID.get(position)).setValue(null);
                                    databaseMessageReference.child("chats").child(userChatID.get(position)).setValue(null);
                                } catch (NullPointerException e){
                                    Log.e(TAG, "Missing values to delete!");
                                }

                                userKey.remove(position);
                                userChatID.remove(position);
                                userName.remove(position);
                                userProfilePic.remove(position);
                                userLastMessage.remove(position);
                                userLastMessageId.remove(position);
                                userTimeStamp.remove(position);

                                mAdapter.notifyItemRemoved(position);

                                Log.i(TAG, "User removed!");
                            } else {
                                Log.i(TAG, "User NOT removed!");
                                notifyDataChanged();
                            }
                            undoFlag = false;
                        }
                    };

                    handler.postDelayed(runnable, (delayTime));
                }
            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    icon = drawableToBitmap(ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_white_24dp));
                    if (dX > 0) {
                        paint.setColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        canvas.drawRect(background, paint);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width, (float) itemView.getTop() + width, (float) itemView.getLeft() + 2 * width, (float) itemView.getBottom() - width);
                        canvas.drawBitmap(icon, null, icon_dest, paint);
                    } else if (dX < 0) {
                        paint.setColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        canvas.drawRect(background, paint);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width, (float) itemView.getTop() + width, (float) itemView.getRight() - width, (float) itemView.getBottom() - width);
                        canvas.drawBitmap(icon, null, icon_dest, paint);
                    }
                    // Fade out the view as it is swiped out of the parent's bounds
                    final float alpha = 1 - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };

        ItemTouchHelper mIth = new ItemTouchHelper(simpleCallback);
        mIth.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();

        return true;
    }

    private void getUsers() {
        databaseReferenceMyUsers = database.getReference().child("users").child(user.getUid()).child("my_chats");
        valueEventListenerMyUsers = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                cleanVars();

                for (DataSnapshot data : dataSnapshot.getChildren()) {

                    userKey.add(data.getKey());
                    userChatID.add(data.getValue().toString());
                    userName.add("");
                    userProfilePic.add(null);
                    userLastMessage.add("");
                    userLastMessageId.add("");
                    userTimeStamp.add("");

                    Log.i(TAG, "Chat Key: " + data.getKey());
                    Log.i(TAG, "Chat Id: " + data.getValue());
                }
                getData(userKey.size());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReferenceMyUsers.addValueEventListener(valueEventListenerMyUsers);


    }
    private void getData(long size){
        Log.d(TAG, "Looking data for: " + userKey);
        //int size = userKey.size();
        databaseReferenceUsers = database.getReference().child("users");
        databaseReferenceLastMessage = database.getReference().child("chats_resume");
        for(int i = 0; i < size; i++){
            final int position = i;

            valueEventListenerUsers = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.i(TAG, "Alias found: " + dataSnapshot.getValue());
                    userName.set(position, dataSnapshot.getValue().toString());

                    //notifyDataChanged();
                    Log.d(TAG, "User Name: " + userName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            valueEventListenerLastMessage = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String lastMessageText;
                    String lastMessageId;
                    String lastDateFromMessage;
                    Long lastMessageTime = 0L;
                    if (dataSnapshot.child("text").getValue() != null){
                        lastMessageText = dataSnapshot.child("text").getValue().toString();
                    } else {
                        lastMessageText = "Sorry! The last Message wasn't found!";
                    }
                    if (dataSnapshot.child("userId").getValue() != null){
                        lastMessageId = dataSnapshot.child("userId").getValue().toString();
                    } else {
                        lastMessageId = "null";
                    }
                    if (dataSnapshot.child("timeStamp").getValue() != null){
                        lastDateFromMessage = dateFormatter(dataSnapshot.child("timeStamp").getValue().toString());
                        lastMessageTime = Long.valueOf(dataSnapshot.child("timeStamp").getValue().toString());
                    } else {
                        lastDateFromMessage = "Date Not Found!";
                    }
                    if (position < userLastMessage.size()) {
                        userLastMessage.set(position, lastMessageText);
                        userLastMessageId.set(position, lastMessageId);
                        userTimeStamp.set(position, lastDateFromMessage);


                        notifyDataChanged();

                        Long timeInHours = (Calendar.getInstance().getTimeInMillis() - lastMessageTime) / ONE_HOUR;
                        Log.i(TAG, "Time Since last Message: " + timeInHours + " hours.");

                        if (timeInHours < maxTimeForNotifications && !isInLayout() && !lastMessageId.equals(user.getUid())) {
                            String notificationText = userName.get(position) + ": " + userLastMessage.get(position);
                            sendNotification(getActivity(), notificationText);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            Log.d(TAG, "Looking data for: " + userKey.get(position));

            databaseReferenceUsers.child(userKey.get(position))
                    .child("preferences").child("alias")
                    .addValueEventListener(valueEventListenerUsers);

            storageReference.child(userKey.get(position))
                    .child("profile_pic")
                    .child("profile_im.jpg")
                    .getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Log.d(TAG, "Pic URL: " + uri);
                    userProfilePic.set(position, uri);

                    notifyDataChanged();
                    Log.d(TAG, "Pic URL: " + userProfilePic);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Log.d(TAG, "Pic Not Available");
                    userProfilePic.set(position, null);
                    notifyDataChanged();
                }
            });

            databaseReferenceLastMessage
                    .child(userChatID.get(position))
                    .addValueEventListener(valueEventListenerLastMessage);

        }
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);

            notificationManager.cancel(NOTIFICATION_ID);

            Log.i(TAG, "  Reloading Data Finished");
            Log.i(TAG, "¡-------------------------¡");
        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void sendNotification(Context context, String messageBody) {
        Intent intent = new Intent(context, HLMActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        android.support.v4.app.NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notifications_white_24dp)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }


    private String dateFormatter (String millis) {

        Long currentDateTime = Long.parseLong(millis);
        Date currentDate = new Date(currentDateTime);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm", Locale.US);
        System.out.println(sdf.format(currentDate));

        return sdf.format(currentDate);
    }

    private void notifyDataChanged(){
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "Adapter NULL");
        }
    }

    private void cleanVars(){
        userKey.clear();
        userName.clear();
        userChatID.clear();
        userProfilePic.clear();
        userLastMessage.clear();
        userTimeStamp.clear();
    }

    private void removeListeners(){

        try {
            databaseReferenceMyUsers.removeEventListener(valueEventListenerMyUsers);
            databaseReferenceUsers.removeEventListener(valueEventListenerUsers);
            databaseReferenceLastMessage.removeEventListener(valueEventListenerLastMessage);
            Log.i(TAG, "Listeners Successfully removed!");
        } catch(NullPointerException e){
            Log.e(TAG, "Failed to remove Listeners");
            //e.printStackTrace();
        }
        valueEventListenerMyUsers = null;
        valueEventListenerUsers = null;
        valueEventListenerLastMessage = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onStop(){
        super.onStop();
        removeListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        //getData(userKey.size());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeListeners();
        //handler.removeCallbacks(runnable);
        //handler.removeCallbacksAndMessages(null);
        //handler.postDelayed(runnable, delayTime);
    }
}
