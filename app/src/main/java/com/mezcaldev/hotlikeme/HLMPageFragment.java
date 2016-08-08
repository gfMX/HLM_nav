package com.mezcaldev.hotlikeme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Gravity;
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

public class HLMPageFragment extends Fragment {

    String userKey = HLMSlidePagerActivity.userKey;

    TextView viewUserAlias;
    ImageView viewUserImage;
    ImageView dropZone1;
    ImageView dropZone2;
    RatingBar ratingBar;
    DisplayMetrics metrics = new DisplayMetrics();
    int displayHeight;
    int displayWidth;
    int tolerancePixels = 100; //Initial value is recalculate with display Height
    int screenUp;
    int screenDown;
    int screenPart;
    int screenParts = 13;
    int starsRating;
    boolean flagOne = false;
    boolean flagTwo = false;
    boolean flagThree = false;
    boolean flagFour = false;

    float distanceY = 0;
    float distanceX = 0;

    TextView viewUserDescription;
    Toast toast1;
    Toast toast2;
    String DEBUG_TAG = "Debug: ";


    //Firebase Initialization
    final FirebaseUser firebaseUser = MainActivityFragment.user;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.hlm_screen_slide_page, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){

        viewUserAlias = (TextView) view.findViewById(R.id.textView);
        viewUserImage = (ImageView) view.findViewById(R.id.imageView);
        viewUserDescription = (TextView) view.findViewById(R.id.userDescription);
        ratingBar = (RatingBar) view.findViewById(R.id.rating);

        dropZone1 = (ImageView) view.findViewById(R.id.dropZone1);
        dropZone2 = (ImageView) view.findViewById(R.id.dropZone2);

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        displayHeight = metrics.heightPixels;
        displayWidth = metrics.widthPixels;
        tolerancePixels = metrics.heightPixels / 6; // Gives one third of the Screen Height For tolerance.
        screenUp = displayHeight / 2 - tolerancePixels;
        screenDown = displayHeight / 2 + tolerancePixels;
        screenPart = displayHeight / screenParts;

        getUserDetails(userKey);

        viewUserImage.setRotation(5 * ((float) Math.random() * 2 - 1));
        viewUserImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                int xRaw = (int) event.getRawX();
                int yRaw = (int) event.getRawY();

                //Adds the users that Current User likes
                DatabaseReference referenceLikeUser = database.getReference()
                        .child("users")
                        .child(firebaseUser.getUid())
                        .child("like_user")
                        .child(userKey);
                
                //Adds the rate of the Current User to the rating of the external user
                DatabaseReference referenceUserRated = database.getReference()
                        .child("users")
                        .child(userKey)
                        .child("user_rate")
                        .child(firebaseUser.getUid());


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

                        if (yRaw < screenUp) {
                            System.out.println("User ID: " + userKey);
                            referenceLikeUser.setValue(true);
                            Toast.makeText(getContext(), "Added!", Toast.LENGTH_SHORT).show();

                        }
                        if (yRaw > screenDown) {
                            System.out.println("User ID: " + userKey);
                            referenceLikeUser.setValue(null);
                            Toast.makeText(getContext(), "Removed!", Toast.LENGTH_SHORT).show();
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
                            toast1 = Toast.makeText(getContext(), "I Like it!", Toast.LENGTH_SHORT);
                            toast1.setGravity(Gravity.CENTER,0,400);
                            toast1.show();

                            resetFlags();
                            flagOne = true;
                        }
                        if (yRaw > screenDown && !flagTwo) {
                            System.out.println("Down");
                            toast2 = Toast.makeText(getContext(), "I don't Like it!", Toast.LENGTH_SHORT);
                            toast2.setGravity(Gravity.CENTER,0,-350);
                            toast2.show();

                            resetFlags();
                            flagTwo = true;
                        }

                        if (yRaw < screenPart){
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
                            //Null zone doesn't add or change Rating
                            System.out.println("None");
                            starsRating = 0;
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
                            System.out.println("None");
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

                //for (DataSnapshot data: dataSnapshot.getChildren()){}
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

    private void showStars(int numberOfStars){

    }

    private void resetFlags (){
        flagOne = false;
        flagTwo = false;
        flagThree = false;
        flagFour = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewUserImage.setImageBitmap(null);
    }
}

