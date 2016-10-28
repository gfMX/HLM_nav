package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;

import static android.graphics.PorterDuff.Mode.SRC_ATOP;
import static android.support.v4.content.ContextCompat.getColor;
import static android.view.Gravity.CENTER;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MASK;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.mezcaldev.hotlikeme.FireConnection.randomUserList;
import static com.mezcaldev.hotlikeme.FireConnection.user;
import static com.mezcaldev.hotlikeme.FireConnection.usersList;
import static com.mezcaldev.hotlikeme.FireConnection.weLike;
import static com.mezcaldev.hotlikeme.R.color.colorAccent;
import static com.mezcaldev.hotlikeme.R.id.fab_message;
import static com.mezcaldev.hotlikeme.R.id.imageView;
import static com.mezcaldev.hotlikeme.R.id.rating;
import static com.mezcaldev.hotlikeme.R.id.textView;
import static com.mezcaldev.hotlikeme.R.id.userDescription;
import static com.mezcaldev.hotlikeme.R.layout.hlm_screen_slide_page;
import static com.mezcaldev.hotlikeme.R.string.welcome_msg;
import static java.lang.Float.valueOf;
import static java.lang.Math.random;
import static java.lang.System.out;
import static java.util.UUID.randomUUID;

public class HLMUsers extends ListFragment {

    String userKey;
    String oldKey;
    private static final String TAG = "HLMUsers";

    static HLMUsers newInstance() {

        return new HLMUsers();
    }

    TextView viewUserAlias;
    ImageView viewUserImage;
    RatingBar ratingBar;
    FloatingActionButton fabMessage;

    //Bitmap image;
    Handler handlerBeforeNewUser = new Handler();
    Handler handlerWaitingUsers = new Handler();
    Runnable runnableWaitingUsers;
    Runnable runnableBeforeNewUser;

    int delayBeforeNewUser = 500;
    int delayTime = 2500;
    int nextUser = 0;

    DisplayMetrics metrics = new DisplayMetrics();
    int displayHeight;
    int displayWidth;
    int tolerancePixels = 100; //Initial value is recalculate with display Height
    int screenUp;
    int screenDown;
    int screenPart;
    int screenParts = 13;
    float starsRating = 0;
    float oldRating = 0;

    boolean flagOne = false;
    boolean flagTwo = false;

    String uniqueChatID;

    TextView viewUserDescription;
    Toast toast1;
    Toast toast2;

    SharedPreferences sharedPreferences;

    //Firebase Initialization:
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
    DatabaseReference referenceLikeUser;
    DatabaseReference referenceUserRated;

    /*************************
     * Database References:
     * **********************/
    //User Details
    DatabaseReference databaseReferenceUserDetails;
    //User Rating:
    DatabaseReference databaseReferenceRating;
    //Did we like
    DatabaseReference databaseReferenceUserKey;
    DatabaseReference databaseReferenceCurrent;
    //Chat
    DatabaseReference databaseReferenceSetCurrentUserChat;
    DatabaseReference databaseReferenceSetRemoteUserChat;
    DatabaseReference databaseReferenceChat;

    //Value Event Listeners:
    ValueEventListener valueEventListenerDidWeLike;
    ValueEventListener valueEventListenerGetUserDetails;
    ValueEventListener valueEventListenerCheckChat;
    ValueEventListener valueEventListenerUserRating;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        weLike = false;

        Log.i(TAG, "Number of Users from Singleton: " + usersList.size());
        if (usersList == null) {
            FireConnection.getInstance().getFirebaseUsers(sharedPreferences, HLMActivity.mCurrentLocation);
            Log.v(TAG, "Users from Singleton Empty");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //ViewGroup rootView = (ViewGroup) inflater.inflate(hlm_screen_slide_page, container, false);

        return inflater.inflate(hlm_screen_slide_page, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        viewUserAlias = (TextView) view.findViewById(textView);
        viewUserImage = (ImageView) view.findViewById(imageView);
        viewUserDescription = (TextView) view.findViewById(userDescription);

        fabMessage = (FloatingActionButton) view.findViewById(fab_message);
        fabMessage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("userChat", uniqueChatID);
                startActivity(intent);
            }
        });

        ratingBar = (RatingBar) view.findViewById(rating);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(getColor(getContext(), colorAccent), SRC_ATOP);

        //Calculations for the subdivisions and Rating based on screen Height
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        displayHeight = metrics.heightPixels;
        displayWidth = metrics.widthPixels;
        tolerancePixels = metrics.heightPixels / 24; // Divided by 6 Gives one third of the Screen Height For tolerance.
        screenUp = displayHeight / 2 - tolerancePixels;
        screenDown = displayHeight / 2 + tolerancePixels;
        screenPart = displayHeight / screenParts;

        //Only if a Key is given proceed, else Show a blank (Default) page.
        if (user != null && keyChecker()) {
            Log.i(TAG, "Getting user");
            userKey = genNoRepeatedKey(userKey);
            changeUserKey(userKey);
        } else {
            Log.i(TAG, "---> Waiting for users <---");
            waitForUsers();
        }

        viewUserImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                //int xRaw = (int) event.getRawX();
                int yRaw = (int) event.getRawY();

                switch (event.getAction() & ACTION_MASK) {
                    case ACTION_DOWN:
                        out.println("Down");
                        break;

                    case ACTION_UP:
                        out.println("Up");

                        v.setTranslationY(0);
                        v.setTranslationX(0);

                        //Add users to the Like List
                        if (userKey != null) {
                            //Set the New User Rating
                            referenceUserRated.setValue(starsRating);

                            if (yRaw < screenUp) {
                                out.println("User ID: " + userKey);
                                referenceLikeUser.setValue(true);
                                //makeText(getContext(), "Added!", LENGTH_SHORT).show();

                            }
                            //Remove users from the Like List
                            if (yRaw > screenDown) {
                                out.println("User ID: " + userKey);
                                referenceLikeUser.setValue(null);
                                //makeText(getContext(), "Removed!", LENGTH_SHORT).show();
                                fabMessage.setVisibility(INVISIBLE);
                            }
                            //Check if Users Like each other:
                            didWeLike(userKey);
                            removeReferences();

                            //Generate new Key and Load new User
                            if (usersList.size() > 1) {
                                runnableBeforeNewUser = new Runnable() {
                                    @Override
                                    public void run() {
                                        oldKey = userKey;
                                        userKey = genNoRepeatedKey(userKey);
                                        Log.v(TAG, "New randomKey: " + userKey + " oldKey: " + oldKey);
                                        changeUserKey(userKey);
                                    }
                                };
                                handlerBeforeNewUser.postDelayed(runnableBeforeNewUser, delayBeforeNewUser);
                            } else {
                                Toast.makeText(getContext(), "We're sorry, there are no More Users around, check in a few moments...", Toast.LENGTH_LONG).show();
                                Log.i(TAG, "No more users around!");
                            }
                        }

                        break;

                    case ACTION_MOVE:
                        v.setX((v.getX() - v.getWidth() / 2) + x);
                        v.setY((v.getY() - v.getHeight() / 2) + y);
                        if (yRaw < screenUp && !flagOne) {
                            out.println("Up");
                            toast1 = makeText(getContext(), "I'll like to get in touch!", LENGTH_SHORT);
                            toast1.setGravity(CENTER, 0, 400);
                            toast1.show();

                            resetFlags();
                            flagOne = true;
                        }
                        if (yRaw > screenDown && !flagTwo) {
                            out.println("Down");
                            toast2 = makeText(getContext(), "I don't wan't to get in touch.", LENGTH_SHORT);
                            toast2.setGravity(CENTER, 0, -350);
                            toast2.show();

                            resetFlags();
                            flagTwo = true;
                        }

                        if (yRaw < screenPart) {
                            //Upper limit of the screen
                            System.out.println("None");
                        } else if (yRaw < screenPart * 2) {
                            starsRating = 5;
                        } else if (yRaw < screenPart * 3) {
                            starsRating = 4;
                        } else if (yRaw < screenPart * 4) {
                            starsRating = 3;
                        } else if (yRaw < screenPart * 5) {
                            starsRating = 2;
                        } else if (yRaw < screenPart * 6) {
                            starsRating = 1;
                        } else if (yRaw < screenPart * 7) {

                            /*** (Center of Screen) Doesn't change Rating
                            The image Snaps to the Origin to let Users Know it is the center ***/

                            v.setTranslationX(0);
                            v.setTranslationY(0);
                            starsRating = oldRating;

                        } else if (yRaw < screenPart * 8) {
                            starsRating = 1;
                        } else if (yRaw < screenPart * 9) {
                            starsRating = 2;
                        } else if (yRaw < screenPart * 10) {
                            starsRating = 3;
                        } else if (yRaw < screenPart * 11) {
                            starsRating = 4;
                        } else if (yRaw < screenPart * 12) {
                            starsRating = 5;
                        } else if (yRaw < screenPart * 13) {
                            //Way too low of the screen
                            System.out.println("None");
                        }
                        //referenceUserRated.setValue(starsRating);
                        ratingBar.setRating(starsRating);
                        break;

                }
                return true;
            }
        });

    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_users, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch (item.getItemId()){
            case R.id.action_reloadUsers:
                if (usersList.size() > 0) {
                    userKey = genNoRepeatedKey(userKey);
                    changeUserKey(userKey);
                } else {
                    Toast.makeText(getContext(), "There are no users around", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getUserDetails(String key) {
        databaseReferenceUserDetails = database.getReference().child("users").child(key).child("preferences");

        valueEventListenerGetUserDetails = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userData = dataSnapshot.getValue().toString();
                Log.v(TAG, "User data: " + userData);

                viewUserAlias.setText(dataSnapshot.child("alias").getValue().toString());
                viewUserDescription.setText(dataSnapshot.child("description").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReferenceUserDetails.addListenerForSingleValueEvent(valueEventListenerGetUserDetails);

        storageRef.child(key).child("/profile_pic/").child("profile_im.jpg").getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide
                                .with(getContext())
                                .load(uri)
                                .fitCenter()
                                .into(viewUserImage);

                        viewUserImage.setRotation(4 * ((float) random() * 2 - 1));
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void userRating(final String key) {
        databaseReferenceRating = database.getReference().child("users").child(key).child("user_rate");

        valueEventListenerUserRating = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.getKey().equals(user.getUid())) { //Shows only the rating given bye the actual User
                        starsRating = valueOf(data.getValue().toString());
                        oldRating = starsRating;
                        ratingBar.setRating(starsRating);
                        out.println("Key: " + key + " Rating: " + starsRating);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReferenceRating.addListenerForSingleValueEvent(valueEventListenerUserRating);
    }

    private void didWeLike(final String key) {
        databaseReferenceUserKey = database.getReference().child("users").child(key).child("like_user");
        databaseReferenceCurrent = database.getReference().child("users").child(user.getUid()).child("like_user");

        valueEventListenerDidWeLike = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.getKey().equals(key)) {
                        databaseReferenceUserKey.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    if (data.getKey().equals(user.getUid())) {
                                        fabMessage.setVisibility(VISIBLE);
                                        weLike = true;
                                        checkChat(key);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReferenceCurrent.addListenerForSingleValueEvent(valueEventListenerDidWeLike);
    }

    private void checkChat(final String key) {

        databaseReferenceSetCurrentUserChat = database.getReference().child("users").child(user.getUid()).child("my_chats");
        databaseReferenceSetRemoteUserChat = database.getReference().child("users").child(key).child("my_chats");
        databaseReferenceChat = database.getReference().child("chats_resume");

        //Check if a chat exists already, if not a new Chat is assigned to the users.
        valueEventListenerCheckChat = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                out.println("Checking for chats");
                if (weLike && !dataSnapshot.hasChild(key)) {
                    uniqueChatID = "chat_" + randomUUID();

                    /************** Encrypt the Welcome Message for new conversations *****************/
                    String myKey = FireConnection.getInstance().genHashKey(uniqueChatID);
                    SecureMessage secureMessage = new SecureMessage(myKey);
                    String encryptedMessageToSend = secureMessage.EncryptToFinalTransferText(getResources().getString(welcome_msg));
                    /************** Encrypt the Welcome Message for new conversations *****************/

                    //try {
                        databaseReferenceSetCurrentUserChat.child(key).setValue(uniqueChatID);
                        databaseReferenceSetRemoteUserChat.child(user.getUid()).setValue(uniqueChatID);

                        databaseReferenceChat.child(uniqueChatID).child("text").setValue(encryptedMessageToSend);
                        databaseReferenceChat.child(uniqueChatID).child("timeStamp").setValue(timeStamp());
                    /*} catch (IllegalStateException ie){
                        Log.e(TAG, "Ups! Listeners detached earlier");
                    }*/
                } else {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (data.getKey().equals(key)) {
                            uniqueChatID = data.getValue().toString();
                        }
                    }
                }
                Log.i(TAG, "Unique Chat ID: " + uniqueChatID);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReferenceSetCurrentUserChat.addListenerForSingleValueEvent(valueEventListenerCheckChat);
    }

    private void changeUserKey(String newKey){
        if (user != null) {
            weLike = false;
            starsRating = 0;

            viewUserImage.setImageBitmap(null);
            viewUserImage.setImageResource(R.drawable.ic_person_gray);

            ratingBar.setRating(starsRating);
            fabMessage.setVisibility(INVISIBLE);

            //First Remove old Listeners:
            removeReferences();

            //Adds the users that Current User likes
            referenceLikeUser = database.getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("like_user")
                    .child(newKey);

            //Adds the rate of the Current User to the rating of the external user
            referenceUserRated = database.getReference()
                    .child("users")
                    .child(newKey)
                    .child("user_rate")
                    .child(user.getUid());

            getUserDetails(newKey);
            userRating(newKey);
            didWeLike(newKey);
        }
    }

    private void waitForUsers(){
        Log.e(TAG, "Waiting users");
        runnableWaitingUsers = new Runnable() {
            @Override
            public void run() {
                //FireConnection.getInstance().getFirebaseUsers(sharedPreferences, HLMActivity.mCurrentLocation);
                if (user != null && keyChecker()) {
                    userKey = genNoRepeatedKey(userKey);
                    changeUserKey(userKey);
                } else {
                    Log.e(TAG, "There are no users around");
                    FireConnection.getInstance().getFirebaseUsers(sharedPreferences, HLMActivity.mCurrentLocation);
                    //FireConnection.getInstance().genUserRandomCollection();
                    waitForUsers();
                }
            }
        };

        handlerWaitingUsers.postDelayed(runnableWaitingUsers, delayTime);
    }

    private void removeReferences (){
        try {
            //Avoid LOS for too many references:
            databaseReferenceUserDetails.removeEventListener(valueEventListenerGetUserDetails);
            databaseReferenceRating.removeEventListener(valueEventListenerUserRating);
            databaseReferenceCurrent.removeEventListener(valueEventListenerDidWeLike);
            databaseReferenceSetCurrentUserChat.removeEventListener(valueEventListenerCheckChat);
            Log.i(TAG, "Listeners Removed!");
        } catch (NullPointerException e){
            Log.e(TAG, "  Failed to remove Listeners");
            Log.e(TAG, "!----------------------------!");
            //e.printStackTrace();
        }
        valueEventListenerGetUserDetails = null;
        valueEventListenerUserRating = null;
        valueEventListenerDidWeLike = null;
        valueEventListenerCheckChat = null;
    }

    private boolean keyChecker (){
        Log.i(TAG, "Users List size: " + usersList.size());
        return (usersList.size() > 0);
    }

    private String genNoRepeatedKey (String oldKey){

        String newKey = usersList.get(randomUserList.get(nextUser));
        if (nextUser == (randomUserList.size()-1)){
            nextUser = 0;
        } else {
            nextUser++;
        }

        Log.i(TAG, "New randomKey: " + userKey + " oldKey: " + oldKey + " UserList Size: " + usersList.size());

        return newKey;
    }

    private String timeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();

        return String.valueOf(calendar.getTimeInMillis());
    }

    private void littleCleaning(){
        viewUserImage.setImageBitmap(null);
        viewUserImage.destroyDrawingCache();
        /*if (image != null) {
            image.recycle();
        } */
        try {
            handlerWaitingUsers.removeCallbacks(runnableWaitingUsers);
            handlerBeforeNewUser.removeCallbacks(runnableBeforeNewUser);

            handlerBeforeNewUser.removeCallbacksAndMessages(null);
            handlerWaitingUsers.removeCallbacksAndMessages(null);
            Log.i(TAG, "Callbacks Removed!");
        } catch (NullPointerException e){
            Log.e(TAG, "Failed to remove Callbacks");
            //e.printStackTrace();
        }

        removeReferences();
    }
    private void resetFlags() {
        flagOne = false;
        flagTwo = false;
    }

    @Override
    public void onStart(){
        super.onStart();
    }
    @Override
    public void onStop(){
        super.onStop();
    }
    @Override
    public void onResume (){
        super.onResume();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        littleCleaning();
        ratingBar.setRating(0);
    }
}
