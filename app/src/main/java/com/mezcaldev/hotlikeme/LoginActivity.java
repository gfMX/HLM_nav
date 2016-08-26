package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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

        checkUser();
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
        user = FireConnection.getInstance().getUser();

        if (checkUser()) {
            //super.onBackPressed();
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            } else {
                ActivityCompat.finishAffinity(this);
            }
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //NavUtils.navigateUpFromSameTask(this);
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
            return true;
        }
        return true;
    }
    private Boolean checkUser(){
        user = FireConnection.getInstance().getUser();
        return user != null;
    }
}
