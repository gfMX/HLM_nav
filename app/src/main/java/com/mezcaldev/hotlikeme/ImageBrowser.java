package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowser extends AppCompatActivity {

    final static String TAG = "IB: ";

    FirebaseUser fireUser;
    FirebaseDatabase database;
    String browseImages;

    List<String> uploadUrls = new ArrayList<>();
    List<String> uploadTiny = new ArrayList<>();
    List<Integer> deleteList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fireUser = MainActivityFragment.user;
        database = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            browseImages = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            if (browseImages.equals("Firebase")){
                fab.setVisibility(View.GONE);
            } else {
                fab.setVisibility(View.VISIBLE);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        uploadUrls = ImageBrowserFragment.imUrlsSelected;
                        uploadTiny = ImageBrowserFragment.imThumbSelected;

                        String textImages;

                        if (uploadUrls.size() == 1) {
                            textImages = " Image ";
                        } else {
                            textImages = " Images ";
                        }

                        if (uploadUrls.size() > 0 && uploadTiny.size() > 0) {
                            //databaseReference.setValue(uploadUrls.size());

                            Snackbar.make(view,
                                    getResources().getString(R.string.text_uploading) + uploadUrls.size() + textImages,
                                    Snackbar.LENGTH_LONG)
                                    .setAction("Action", null)
                                    .show();
                            final DatabaseReference numberOfImages = database.getReference(fireUser.getUid());

                            numberOfImages.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    int imagesOnFirebase;
                                    try {
                                        imagesOnFirebase = Integer.valueOf(dataSnapshot.child("total_images").getValue().toString());
                                    } catch (NullPointerException e){
                                        imagesOnFirebase = 0;
                                        e.printStackTrace();
                                    }

                                    ImageSaver uploadImages = new ImageSaver();
                                    uploadImages.uploadToFirebase(
                                            uploadUrls,
                                            uploadTiny,
                                            fireUser,
                                            getApplicationContext(),
                                            uploadUrls.size(),
                                            imagesOnFirebase);
                                   /* ImageSaver uploadThumbs = new ImageSaver();
                                    uploadThumbs.iUploadThumbsToFirebase(
                                            uploadTiny,
                                            fireUser,
                                            uploadTiny.size(),
                                            pathThumbs,
                                            imagesOnFirebase);*/

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(getApplication(), MainActivity.class));
                                    //finish();
                                }
                            }, 1500);

                        } else {
                            Snackbar.make(view,
                                    getResources().getString(R.string.text_no_images_selected),
                                    Snackbar.LENGTH_LONG)
                                    .setAction("Action", null)
                                    .show();
                        }
                    }
                });
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_image_browser, menu);
        MenuItem trashCan = menu.findItem(R.id.action_delete_image);

        if (browseImages.equals("Firebase")){
            trashCan.setVisible(true);
        } else {
            trashCan.setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_delete_image){
            Log.i(TAG, "Delete");
            deleteList = ImageBrowserFragment.imIdsSelected;
            if (deleteList.size()>0){
                Integer numberOfImages = deleteList.size();
                String deleteText =
                        getResources().getString(R.string.text_deleting_selected_images_1) +
                        numberOfImages.toString() +
                        getResources().getString(R.string.text_deleting_selected_images_2);

                ImageSaver deleteImages = new ImageSaver();
                deleteImages.DeleteImagesOnFire(deleteList);

                Snackbar.make(getWindow().getDecorView(),
                        deleteText,
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            } else {
                Snackbar.make(getWindow().getDecorView(),
                        getResources().getString(R.string.text_delete_images_no_selected),
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
