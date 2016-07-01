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

import java.util.ArrayList;
import java.util.List;

public class ImageBrowser extends AppCompatActivity {

    static String TAG_b = "Log: ";

    static FirebaseUser fireUser;

    static List<String> uploadUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fireUser = MainActivityFragment.user;
        Log.i(TAG_b, "User: " + fireUser);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadUrls = ImageBrowserFragment.imUrlsSelected;
                if (uploadUrls != null) {
                    Snackbar.make(view, "Uploading selected Images", Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                    ImageSaver uploadImages = new ImageSaver();
                    uploadImages.iUploadImagesToFirebase(uploadUrls, fireUser, getApplicationContext());
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(getApplication(), MainActivity.class));
                        }
                    }, 1000);

                } else{
                    Snackbar.make(view, "No images selected, please select at least one.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                }
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_image_browser, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
