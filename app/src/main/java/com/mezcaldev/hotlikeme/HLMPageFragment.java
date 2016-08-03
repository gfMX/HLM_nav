package com.mezcaldev.hotlikeme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
    TextView viewUserDescription;
    String imageProfileTemp = "profile_pic_tmp.jpg";

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

        //viewUserAlias.setText(userKey);
        getUserDetails(userKey);
    }

    private void getUserDetails (String userKey){
        String userName;
        String userDescription;
        Bitmap userImage;

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
                        //ImageSaver saveBitmap = new ImageSaver();
                        //saveBitmap.iSaveToInternalStorage(image, imageProfileTemp, getContext());
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
}

