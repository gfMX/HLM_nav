package com.mezcaldev.hotlikeme;

import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    ImageView dropZoneLeft;
    ImageView dropZoneRight;
    int totalPages = HLMSlidePagerActivity.users.size();
    int currentPage = HLMSlidePagerActivity.currentPage;
    int newPage;

    TextView viewUserDescription;
    Toast toastImage;
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

        dropZone1 = (ImageView) view.findViewById(R.id.dropZone1);
        dropZone2 = (ImageView) view.findViewById(R.id.dropZone2);
        dropZoneLeft = (ImageView) view.findViewById(R.id.dropZoneLeft);
        dropZoneRight = (ImageView) view.findViewById(R.id.dropZoneRight);


        DragEventListener dragEventListener = new DragEventListener();

        dropZone1.setOnDragListener(dragEventListener);
        dropZone2.setOnDragListener(dragEventListener);

        //view.findViewById(R.id.content).setOnDragListener(dragEventListener);

        viewUserImage.setRotation(5 * ((float) Math.random() * 2 - 1));
        viewUserImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        System.out.println("Down");
                        ClipData data = ClipData.newPlainText("userKey", userKey);
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                        v.startDrag(data, shadowBuilder, v, 0);
                        //v.setVisibility(View.INVISIBLE);

                        break;
                    case MotionEvent.ACTION_UP:
                        System.out.println("Up");

                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:

                        break;
                    case MotionEvent.ACTION_POINTER_UP:

                        break;
                    case MotionEvent.ACTION_MOVE:
                        System.out.println("Move");
                        break;
                }
                return true;
            }
        });

        getUserDetails(userKey);
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

    protected class DragEventListener implements View.OnDragListener {
        // This is the method that the system calls when it dispatches a drag event to the
        // listener.
        public boolean onDrag(View v, DragEvent event) {
            // Defines a variable to store the action type for the incoming event
            final int action = event.getAction();

            // Handles each of the expected events
            switch(action) {

                case DragEvent.ACTION_DRAG_STARTED:
                    Snackbar.make(v,
                            "Up: Like.\nDown:Dislike.",
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();

                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    if (v.getId() == R.id.dropZone1) {
                        toastImage = Toast.makeText(getContext(), "I like it!", Toast.LENGTH_SHORT);
                        toastImage.show();
                    }
                    if (v.getId() == R.id.dropZone2){
                        Toast toast = Toast.makeText(getContext(), "Nop!", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP,0,175);
                        toast.show();
                    }
                    // Invalidate the view to force a redraw in the new tint
                    v.invalidate();

                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:


                    // Ignore the event
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // Invalidate the view to force a redraw in the new tint
                    v.invalidate();

                    return true;

                case DragEvent.ACTION_DROP:
                    // Gets the item containing the dragged data
                    ClipData.Item item = event.getClipData().getItemAt(0);

                    // Invalidates the view to force a redraw
                    v.invalidate();

                    // Returns true. DragEvent.getResult() will return true.
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // Invalidates the view to force a redraw
                    v.invalidate();

                    // Does a getResult(), and displays what happened.
                    if (event.getResult()) {
                        System.out.println("Result: " + event.toString());
                        toastImage = Toast.makeText(getContext(), "If you say so...", Toast.LENGTH_SHORT);
                        toastImage.show();
                    } else {
                        toastImage = Toast.makeText(getContext(), "Think about it", Toast.LENGTH_SHORT);
                        toastImage.show();
                    }
                    // returns true; the value is ignored.
                    return true;

                // An unknown action type was received.
                default:
                    Log.e("DragDrop Example","Unknown action type received by OnDragListener.");
                    break;

            }

            return false;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        viewUserImage.setImageBitmap(null);
        viewUserImage.destroyDrawingCache();
    }
}

