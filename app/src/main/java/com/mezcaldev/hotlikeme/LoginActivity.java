package com.mezcaldev.hotlikeme;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    static FirebaseUser user = FireConnection.getInstance().getUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (checkUser()){
            setupActionBar();
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!checkUser()){
                System.out.println("Settings not reachable");
                return true;
            } else {
                System.out.println("Settings reachable");
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onBackPressed() {
        if (checkUser()) {
            //super.onBackPressed();
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
        } else {
            new AlertDialog.Builder(this)
                    //.setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Closing HotLikeMe")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Sure", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                finishAndRemoveTask();
                            } else {
                                ActivityCompat.finishAffinity(LoginActivity.this);
                            }
                        }

                    })
                    .setNegativeButton("Not yet", null)
                    .show();
        }
    }
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && checkUser()) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else if (actionBar != null && !checkUser()){
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home && checkUser()) {
            //NavUtils.navigateUpFromSameTask(this);
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
            return true;
        } else if (id == android.R.id.home && !checkUser()) {
            setupActionBar();
        }
        return true;
    }
    private Boolean checkUser(){
        user = FireConnection.getInstance().getUser();
        return user != null;
    }
}
