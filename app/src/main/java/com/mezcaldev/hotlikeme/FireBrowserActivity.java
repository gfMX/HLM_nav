package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class FireBrowserActivity extends AppCompatActivity {

    final static String TAG = "IB: ";

    FirebaseUser fireUser;
    FirebaseDatabase database;

    List<String> deleteListImages = new ArrayList<>();
    List<String> deleteListThumbs = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_browser);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarFire);
        setSupportActionBar(toolbar);

        fireUser = MainActivityFragment.user;
        database = FirebaseDatabase.getInstance();


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_fire_browser, menu);
        MenuItem trashCan = menu.findItem(R.id.action_delete_image);

        trashCan.setVisible(true);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return true;
        }
        if (id == R.id.action_delete_image){
            Log.i(TAG, "Delete");
            deleteListImages = ImageBrowserFragment.keyOfImage;
            deleteListThumbs = ImageBrowserFragment.keyOfThumb;

            if (deleteListImages.size()>0){
                Integer numberOfImages = deleteListImages.size();
                String deleteText =
                        getResources().getString(R.string.text_deleting_selected_images_1) +
                                numberOfImages.toString() +
                                getResources().getString(R.string.text_deleting_selected_images_2);

                Snackbar.make(getWindow().getDecorView(),
                        deleteText,
                        Snackbar.LENGTH_LONG)
                        .setAction("DELETE", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ImageSaver deleteImages = new ImageSaver();
                                deleteImages.DeleteImagesOnFire(deleteListImages, deleteListThumbs);
                                Snackbar.make(getWindow().getDecorView(),
                                        getResources().getString(R.string.text_deleting_images),
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Action", null)
                                        .show();
                            }
                        })
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
