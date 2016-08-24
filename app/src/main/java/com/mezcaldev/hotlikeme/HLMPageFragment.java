package com.mezcaldev.hotlikeme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
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

public class HLMPageFragment extends ListFragment {
    String userKey;

    static HLMPageFragment newInstance(String key) {
        HLMPageFragment newFragment = new HLMPageFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("key", key);
        newFragment.setArguments(args);

        return newFragment;
    }

    TextView viewUserAlias;
    ImageView viewUserImage;
    ImageView dropZone1;
    ImageView dropZone2;
    RatingBar ratingBar;
    MenuItem chatIcon;

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
    boolean flagWeLike = false;

    float distanceY = 0;
    float distanceX = 0;

    TextView viewUserDescription;
    Toast toast1;
    Toast toast2;

    //Firebase Initialization
    FirebaseUser firebaseUser = FireConnection.getInstance().getUser();
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
    DatabaseReference referenceLikeUser;
    DatabaseReference referenceUserRated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        userKey = getArguments() != null ? getArguments().getString("key"): "None key given";

        System.out.println("Actual user: " + firebaseUser.getUid());
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_hlm_page, menu);

        chatIcon = menu.findItem(R.id.action_chat);
        didWeLike();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_chat) {
            Snackbar.make(getActivity().getWindow().getDecorView(),"We're almost there", Snackbar.LENGTH_LONG).show();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.hlm_screen_slide_page, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){

        //Adds the users that Current User likes
        referenceLikeUser = database.getReference()
                .child("users")
                .child(firebaseUser.getUid())
                .child("like_user")
                .child(userKey);

        //Adds the rate of the Current User to the rating of the external user
        referenceUserRated = database.getReference()
                .child("users")
                .child(userKey)
                .child("user_rate")
                .child(firebaseUser.getUid());

        viewUserAlias = (TextView) view.findViewById(R.id.textView);
        viewUserImage = (ImageView) view.findViewById(R.id.imageView);
        viewUserDescription = (TextView) view.findViewById(R.id.userDescription);

        ratingBar = (RatingBar) view.findViewById(R.id.rating);
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);

        dropZone1 = (ImageView) view.findViewById(R.id.dropZone1);
        dropZone2 = (ImageView) view.findViewById(R.id.dropZone2);

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        displayHeight = metrics.heightPixels;
        displayWidth = metrics.widthPixels;
        tolerancePixels = metrics.heightPixels / 24; // Divided by 6 Gives one third of the Screen Height For tolerance.
        screenUp = displayHeight / 2 - tolerancePixels;
        screenDown = displayHeight / 2 + tolerancePixels;
        screenPart = displayHeight / screenParts;

        getUserDetails(userKey);
        userRating();

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                starsRating = rating;
                if (starsRating == 0){
                    referenceUserRated.setValue(null);
                } else {
                    referenceUserRated.setValue(starsRating);
                }
            }
        });

        viewUserImage.setRotation(5 * ((float) Math.random() * 2 - 1));
        viewUserImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                //int xRaw = (int) event.getRawX();
                int yRaw = (int) event.getRawY();

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("Down");
                        distanceX = x;
                        distanceY = y;

                        break;
                    case MotionEvent.ACTION_UP:
                        System.out.println("Up");
                        v.setTranslationY(0);
                        v.setTranslationX(0);

                        //Add users to the Like List
                        if (yRaw < screenUp) {
                            System.out.println("User ID: " + userKey);
                            referenceLikeUser.setValue(true);
                            Toast.makeText(getContext(), "Added!", Toast.LENGTH_SHORT).show();

                        }

                        //Remove users from the Like List
                        if (yRaw > screenDown) {
                            System.out.println("User ID: " + userKey);
                            referenceLikeUser.setValue(null);
                            Toast.makeText(getContext(), "Removed!", Toast.LENGTH_SHORT).show();
                            if (chatIcon != null) {
                                chatIcon.setVisible(false);
                            }
                        }

                        referenceUserRated.setValue(starsRating);

                        resetFlags();

                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        System.out.println("Pointer Down");

                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        System.out.println("Pointer Up");

                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.setX((v.getX() - v.getWidth() / 2) + x);
                        v.setY((v.getY() - v.getHeight() / 2) + y);
                        if (yRaw < screenUp && !flagOne) {
                            System.out.println("Up");
                            toast1 = Toast.makeText(getContext(), "I will like to get in touch!", Toast.LENGTH_SHORT);
                            toast1.setGravity(Gravity.CENTER,0,400);
                            toast1.show();

                            resetFlags();
                            flagOne = true;
                        }
                        if (yRaw > screenDown && !flagTwo) {
                            System.out.println("Down");
                            toast2 = Toast.makeText(getContext(), "I don't wan't to get in touch.", Toast.LENGTH_SHORT);
                            toast2.setGravity(Gravity.CENTER,0,-350);
                            toast2.show();

                            resetFlags();
                            flagTwo = true;
                        }

                        if (yRaw < screenPart){
                            //Upper limit of the screen
                            //System.out.println("None");
                        } else if (yRaw < screenPart * 2) {
                            starsRating = 1;
                        } else if (yRaw < screenPart * 3) {
                            starsRating = 2;
                        } else if (yRaw < screenPart * 4) {
                            starsRating = 3;
                        } else if (yRaw < screenPart * 5) {
                            starsRating = 4;
                        } else if (yRaw < screenPart * 6) {
                            starsRating = 5;
                        } else if (yRaw < screenPart * 7) {
                            //Null zone doesn't add or change Rating
                            starsRating = oldRating;
                        } else if (yRaw < screenPart * 8) {
                            starsRating = 5;
                        } else if (yRaw < screenPart * 9) {
                            starsRating = 4;
                        } else if (yRaw < screenPart * 10) {
                            starsRating = 3;
                        } else if (yRaw < screenPart * 11) {
                            starsRating = 2;
                        } else if (yRaw < screenPart * 12) {
                            starsRating = 1;
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
    }

    private void getUserDetails (String userKey){

        DatabaseReference databaseReference = database.getReference().child("users").child(userKey).child("preferences");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userData = dataSnapshot.getValue().toString();
                System.out.println("User data: " + userData);

                viewUserAlias.setText(dataSnapshot.child("alias").getValue().toString());
                viewUserDescription.setText(dataSnapshot.child("description").getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        storageRef.child(userKey).child("/profile_pic/").child("profile_im.jpg").getBytes(Long.MAX_VALUE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
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

    private void userRating (){
        DatabaseReference databaseReference = database.getReference().child("users").child(userKey).child("user_rate");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.getKey().equals(firebaseUser.getUid())) { //Shows only the rating given bye the actual User
                        starsRating = Float.valueOf(data.getValue().toString());
                        oldRating = starsRating;
                        ratingBar.setRating(starsRating);
                        System.out.println("Long: " + starsRating);
                    }
                }
             }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void didWeLike(){
        final DatabaseReference databaseReferenceUserKey = database.getReference().child("users").child(userKey).child("like_user");
        DatabaseReference databaseReferenceCurrent = database.getReference().child("users").child(firebaseUser.getUid()).child("like_user");

        databaseReferenceCurrent.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data: dataSnapshot.getChildren()){
                    if (data.getKey().equals(userKey)){
                        //currentUserLike = true;
                        databaseReferenceUserKey.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot data: dataSnapshot.getChildren()){
                                    if (data.getKey().equals(firebaseUser.getUid())){
                                        chatIcon.setVisible(true);
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


        //return  (currentUserLike && userKeyLike);
    }

    private void resetFlags (){
        flagOne = false;
        flagTwo = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewUserImage.setImageBitmap(null);
    }
}

