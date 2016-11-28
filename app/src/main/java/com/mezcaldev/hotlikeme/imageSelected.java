package com.mezcaldev.hotlikeme;


import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;

import java.io.File;

import static com.mezcaldev.hotlikeme.FireConnection.databaseGlobal;
import static com.mezcaldev.hotlikeme.FireConnection.user;

public class ImageSelected extends DialogFragment {
    static final String TAG = "Saving the Image: ";
    static String imageProfileFileName = LoginFragment.imageProfileFileName;
    static Uri uriImage;
    static String imageKey;

    File localStorage;
    //FirebaseUser firebaseUser = FireConnection.getInstance().getUser();

    Button btn_ok;
    Button btn_cancel;

    //Time to wait before Launching MainActivity
    int timerToGo = 2500;

    static ImageSelected newInstance(Uri uri, String key) {
        uriImage = uri;
        imageKey = key;
        return new ImageSelected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_selected, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fb_prof_image);

        getDialog().setTitle("HotLikeMe Profile Pic:");

        localStorage = new File(LoginFragment.pathProfileImage + "/" + imageProfileFileName);

        btn_ok = (Button) view.findViewById(R.id.btn_ok_image);
        btn_cancel = (Button) view.findViewById(R.id.btn_cancel_image);

        Glide
                .with(getActivity())
                .load(uriImage)
                .into(imageView);

        btn_ok.setOnClickListener(settingsButtons);
        btn_cancel.setOnClickListener(settingsButtons);

        return view;
    }

    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
        public void onClick (View v){
            switch (v.getId()){
                case R.id.btn_ok_image:
                    saveNewProfileImage(uriImage, imageKey);
                    Snackbar.make(v,
                            getResources().getString(R.string.text_profile_picture_selected),
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                    break;
                case  R.id.btn_cancel_image:
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.text_cancel_selection),
                            Toast.LENGTH_LONG).show();
                    getDialog().dismiss();
                    break;
            }
        }
    };
    private void saveNewProfileImage (final Uri uri, String key){
        if (uri != null) {

            DatabaseReference databaseReference = databaseGlobal.getReference()
                    .child("users")
                    .child(user.getUid())
                    .child("preferences");

            databaseReference.child("profile_pic_url").setValue(uri);
            databaseReference.child("profile_pic_storage").setValue(key);

            FirebaseUser userToUpdate = FirebaseAuth.getInstance().getCurrentUser();
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();

            if (userToUpdate != null) {
                userToUpdate.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User profile updated.");
                                }
                            }
                        });
            }


            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getDialog().dismiss();
                    startActivity(new Intent(getActivity(), HLMActivity.class));
                    getActivity().finish();
                }
            }, timerToGo);
        }
    }
    /*
    private void createBitmap (final Uri uri){
        if (uri != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL imgUrl = new URL(uri.toString());
                        //Log.d(TAG, "Image URL: " + imgUrl);
                        InputStream inputStream = (InputStream) imgUrl.getContent();
                        Bitmap pImage = BitmapFactory.decodeStream(inputStream);
                        if (pImage != null) {
                            ImageSaver saveImage = new ImageSaver();
                            saveImage.iSaveToInternalStorage(
                                    pImage,
                                    imageProfileFileName,
                                    getActivity().getApplicationContext()
                            );
                            saveImage.iUploadProfileImageToFirebase(
                                    localStorage.getAbsolutePath(),
                                    user
                            );
                        }
                        Log.v(TAG, "Everything Ok in here! We got the Image");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getDialog().dismiss();
                    startActivity(new Intent(getActivity(), HLMActivity.class));
                    getActivity().finish();
                }
            }, timerToGo);
        }
    }
    */
}
