package com.mezcaldev.hotlikeme;


import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class imageSelected extends DialogFragment {
    static final String TAG = "Saving the Image: ";
    static String imageProfileFileName = MainActivityFragment.imageProfileFileName;
    static Uri uriImage;

    File localStorage;
    FirebaseUser firebaseUser;

    Button btn_ok;
    Button btn_cancel;

    static imageSelected newInstance(Uri uri) {
        uriImage = uri;
        return new imageSelected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_selected, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fb_prof_image);

        getDialog().setTitle("Selected Image");

        localStorage = new File(MainActivityFragment.pathProfileImage + "/" + imageProfileFileName);
        firebaseUser = MainActivityFragment.user;

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
                    createBitmap(uriImage);
                    Snackbar.make(v, "New Profile Picture selected!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                    break;
                case  R.id.btn_cancel_image:
                    Toast.makeText(getActivity(), "Bla, bla bla...",
                            Toast.LENGTH_LONG).show();
                    getDialog().dismiss();
                    break;
            }
        }
    };
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
                                    firebaseUser
                            );
                        }
                        Log.v(TAG, "Everything Ok in here! We got the Image");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            /*try {
                thread.join();
            } catch (InterruptedException e){
                e.printStackTrace();
            }*/
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getDialog().dismiss();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                }
            }, 2000);
        }
    }
}
