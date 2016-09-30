package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static android.graphics.BitmapFactory.decodeByteArray;
import static android.graphics.PorterDuff.Mode.SRC_ATOP;
import static android.support.v4.content.ContextCompat.getColor;
import static android.view.Gravity.CENTER;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MASK;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.mezcaldev.hotlikeme.FireConnection.getInstance;
import static com.mezcaldev.hotlikeme.R.color.colorAccent;
import static com.mezcaldev.hotlikeme.R.id.fab_message;
import static com.mezcaldev.hotlikeme.R.id.imageView;
import static com.mezcaldev.hotlikeme.R.id.rating;
import static com.mezcaldev.hotlikeme.R.id.textView;
import static com.mezcaldev.hotlikeme.R.id.userDescription;
import static com.mezcaldev.hotlikeme.R.layout.hlm_screen_slide_page;
import static com.mezcaldev.hotlikeme.R.string.welcome_msg;
import static java.lang.Float.valueOf;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.random;
import static java.lang.System.out;
import static java.util.UUID.randomUUID;

public class HLMUsers extends ListFragment {

    String userKey = "nullKey";
    private static final String TAG = "Location";

    static HLMUsers newInstance(String key) {
        HLMUsers newFragment = new HLMUsers();

        Bundle args = new Bundle();
        args.putString("key", key);
        //args.putDouble("latitude", latitude);
        //args.putDouble("longitude", longitude);

        newFragment.setArguments(args);

        return newFragment;
    }

    TextView viewUserAlias;
    ImageView viewUserImage;
    RatingBar ratingBar;
    FloatingActionButton fabMessage;

    /* Position */
    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = ONE_SECOND * 60;
    private static final int MINUTES = ONE_MINUTE * 5;
    int maxUserDistance = 250;
    int delayTime = 500;

    /* Location with Google API */
    Location mCurrentLocation;
    Boolean mRequestingLocationUpdates;
    final int REQUEST_CHECK_SETTINGS = 2543;

    DisplayMetrics metrics = new DisplayMetrics();
    int displayHeight;
    int displayWidth;
    int tolerancePixels = 100; //Initial value is recalculate with display Height
    int screenUp;
    int screenDown;
    int screenPart;
    int screenParts = 13;
    float starsRating;
    float oldRating = 0;

    boolean flagOne = false;
    boolean flagTwo = false;
    boolean weLike = false;

    float distanceY = 0;
    float distanceX = 0;

    String uniqueChatID;

    TextView viewUserDescription;
    Toast toast1;
    Toast toast2;

    String gender;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor preferencesEditor;
    static List<String> users = new ArrayList<>();

    //Firebase Initialization
    FirebaseUser user = getInstance().getUser();
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
    DatabaseReference referenceLikeUser;
    DatabaseReference referenceUserRated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //userKey = getArguments() != null ? getArguments().getString("key") : "nullKey";
        //Double latitude = getArguments() != null ? getArguments().getDouble("latitude") : null;
        //Double longitude = getArguments() != null ? getArguments().getDouble("longitude") : null;
        //mCurrtenLocation = new Location("");
        //mCurrentLocation.setLatitude(latitude);
        //mCurrentLocation.setLongitude(longitude);

        //out.println("UserKey Received: " + userKey);

        user = getInstance().getUser();
        mCurrentLocation = HLMActivity.mCurrentLocation;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        gender = sharedPreferences.getString("looking_for", "both");
        maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "250"));
        mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);

        //if (user != null) {
            out.println("Actual user: " + user.getUid());
            getUriProfilePics(gender);
        //}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(hlm_screen_slide_page, container, false);

        return rootView;
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
                checkChat();
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("userChat", uniqueChatID);
                startActivity(intent);
            }
        });

        viewUserImage.setRotation(5 * ((float) random() * 2 - 1));

        ratingBar = (RatingBar) view.findViewById(rating);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(getColor(getContext(), colorAccent), SRC_ATOP);

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        displayHeight = metrics.heightPixels;
        displayWidth = metrics.widthPixels;
        tolerancePixels = metrics.heightPixels / 24; // Divided by 6 Gives one third of the Screen Height For tolerance.
        screenUp = displayHeight / 2 - tolerancePixels;
        screenDown = displayHeight / 2 + tolerancePixels;
        screenPart = displayHeight / screenParts;

        if (users.size() > 0){
            userKey = users.get(randomUser(users.size()));
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                showUser();

            }
        },delayTime);
    }

    private void getUserDetails(String userKey) {

        DatabaseReference databaseReference = database.getReference().child("users").child(userKey).child("preferences");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userData = dataSnapshot.getValue().toString();
                out.println("User data: " + userData);

                viewUserAlias.setText(dataSnapshot.child("alias").getValue().toString());
                viewUserDescription.setText(dataSnapshot.child("description").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        storageRef.child(userKey).child("/profile_pic/").child("profile_im.jpg").getBytes(MAX_VALUE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap image = decodeByteArray(bytes, 0, bytes.length);
                        viewUserImage.setImageBitmap(image);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                exception.printStackTrace();
            }
        });
    }

    private void userRating() {
        DatabaseReference databaseReference = database.getReference().child("users").child(userKey).child("user_rate");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.getKey().equals(user.getUid())) { //Shows only the rating given bye the actual User
                        starsRating = valueOf(data.getValue().toString());
                        oldRating = starsRating;
                        ratingBar.setRating(starsRating);
                        out.println("Long: " + starsRating);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void didWeLike() {
        final DatabaseReference databaseReferenceUserKey = database.getReference().child("users").child(userKey).child("like_user");
        DatabaseReference databaseReferenceCurrent = database.getReference().child("users").child(user.getUid()).child("like_user");

        databaseReferenceCurrent.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.getKey().equals(userKey)) {
                        //currentUserLike = true;
                        databaseReferenceUserKey.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    if (data.getKey().equals(user.getUid())) {
                                        //chatIcon.setVisible(true);
                                        fabMessage.setVisibility(VISIBLE);
                                        weLike = true;
                                        checkChat();
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
        });
    }

    private void checkChat() {

        final DatabaseReference databaseReferenceSetCurrentUserChat = database.getReference().child("users").child(user.getUid()).child("my_chats");
        final DatabaseReference databaseReferenceSetRemoteUserChat = database.getReference().child("users").child(userKey).child("my_chats");
        final DatabaseReference databaseReferenceChat = database.getReference().child("chats_resume");

        //Check if a chat exists already, if not a new Chat is assigned to the users.
        databaseReferenceSetCurrentUserChat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                out.println("Checking for chats");
                if (weLike && !dataSnapshot.hasChild(userKey)) {
                    uniqueChatID = "chat_" + randomUUID();

                    databaseReferenceSetCurrentUserChat.child(userKey).setValue(uniqueChatID);
                    databaseReferenceSetRemoteUserChat.child(user.getUid()).setValue(uniqueChatID);

                    databaseReferenceChat.child(uniqueChatID).child("text").setValue(getResources().getString(welcome_msg));
                    databaseReferenceChat.child(uniqueChatID).child("timeStamp").setValue(timeStamp());
                } else {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (data.getKey().equals(userKey)) {
                            uniqueChatID = data.getValue().toString();
                        }
                    }
                }
                out.println("Unique Chat ID: " + uniqueChatID);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getUriProfilePics (final String gender){

        final ValueEventListener valueEventListener0;

        final DatabaseReference databaseReference = database.getReference().child("groups").child(gender);
        final DatabaseReference databaseReferenceLocation = database.getReference().child("users");

        valueEventListener0 = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int numChildren = (int) dataSnapshot.getChildrenCount();
                System.out.println("Number of users: " + numChildren);

                for (DataSnapshot data: dataSnapshot.getChildren()){
                    final String dataKey = data.getKey();
                    databaseReferenceLocation.child(dataKey).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Location remoteUserLocation;

                                    //Check permission and proceed according to them
                                    if(!mRequestingLocationUpdates){
                                        System.out.println("All users are visible");
                                        users.add(dataKey);

                                        Snackbar.make(getActivity().getWindow().getDecorView(),
                                                getResources().getString(R.string.text_enable_gps_snack),
                                                Snackbar.LENGTH_LONG)
                                                .setAction("Enable GPS", new View.OnClickListener(){
                                                    @Override
                                                    public void onClick(View v) {
                                                        preferencesEditor = sharedPreferences.edit();
                                                        preferencesEditor.putBoolean("gps_enabled", true);
                                                        preferencesEditor.apply();
                                                        mRequestingLocationUpdates = true;

                                                        users.clear();

                                                        Handler handler = new Handler();
                                                        handler.postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                startActivity(new Intent(getActivity(), HLMActivity.class));
                                                            }
                                                        },500);

                                                    }
                                                }).show();

                                    } else if (mCurrentLocation != null) {
                                        Double userLongitude = (Double) dataSnapshot.child("location_last").child("loc_longitude")
                                                .getValue();
                                        Double userLatitude = (Double) dataSnapshot.child("location_last").child("loc_latitude")
                                                .getValue();
                                        //Request location of the Remote User
                                        if (userLongitude != null && userLatitude != null) {

                                            remoteUserLocation = new Location("");
                                            remoteUserLocation.setLongitude(userLongitude);
                                            remoteUserLocation.setLatitude(userLatitude);

                                            System.out.println("Remote User Location: " + remoteUserLocation);

                                            if (mCurrentLocation.distanceTo(remoteUserLocation) <= maxUserDistance
                                                    && !dataKey.equals(user.getUid())){

                                                System.out.println("User " + dataKey + " reachable!");
                                                users.add(dataKey);

                                            }
                                        }
                                    } else {
                                        System.out.println("Location Not Reachable! Please wait...");
                                        Toast.makeText(getApplicationContext(), "Please wait", Toast.LENGTH_SHORT).show();
                                    }
                                    showUser();
                                }
                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            }
                    );
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addListenerForSingleValueEvent(valueEventListener0);
    }

    private void showUser(){
        didWeLike();
        //Only if a Key si given proceed, else Show a blank (Default) page.
        if (!userKey.equals("nullKey")) {

            //Adds the users that Current User likes
            referenceLikeUser = database.getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("like_user")
                    .child(userKey);

            //Adds the rate of the Current User to the rating of the external user
            referenceUserRated = database.getReference()
                    .child("users")
                    .child(userKey)
                    .child("user_rate")
                    .child(user.getUid());

            getUserDetails(userKey);
            userRating();

            ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    starsRating = rating;
                    if (starsRating == 0) {
                        referenceUserRated.setValue(null);
                    } else {
                        referenceUserRated.setValue(starsRating);
                    }
                }
            });

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
                            distanceX = x;
                            distanceY = y;

                            break;
                        case ACTION_UP:
                            out.println("Up");
                            v.setTranslationY(0);
                            v.setTranslationX(0);

                            //Add users to the Like List
                            if (yRaw < screenUp) {
                                out.println("User ID: " + userKey);
                                referenceLikeUser.setValue(true);
                                makeText(getContext(), "Added!", LENGTH_SHORT).show();

                            }

                            //Remove users from the Like List
                            if (yRaw > screenDown) {
                                out.println("User ID: " + userKey);
                                referenceLikeUser.setValue(null);
                                makeText(getContext(), "Removed!", LENGTH_SHORT).show();
                                fabMessage.setVisibility(INVISIBLE);
                            }

                            referenceUserRated.setValue(starsRating);

                            resetFlags();
                            userKey = users.get(randomUser(users.size()));
                            showUser();

                            break;
                        case ACTION_POINTER_DOWN:
                            out.println("Pointer Down");

                            break;
                        case ACTION_POINTER_UP:
                            out.println("Pointer Up");

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
                                //System.out.println("None");
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
                                //Null zone doesn't add or change Rating
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
                                //System.out.println("None");
                            }
                            ratingBar.setRating(starsRating);
                            break;
                    }
                    return true;
                }
            });

        } else {
            Toast.makeText(getActivity(), "There's no one close to you right now", LENGTH_LONG).show();
        }
    }

    private int randomUser(int noMax){
        int nMin = 0;
        int nMax = noMax-1;
        Random random = new Random();

        return random.nextInt((nMax - nMin) + 1) + nMin;
    }

    private String timeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();

        return String.valueOf(calendar.getTimeInMillis());
    }

    private void resetFlags() {
        flagOne = false;
        flagTwo = false;

        weLike = false;
        fabMessage.setVisibility(INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewUserImage.setImageBitmap(null);
    }
}

