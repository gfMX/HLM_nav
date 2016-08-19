package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    static FirebaseUser user = FireConnection.getInstance().getUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (LoginFragment.user == null){
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
        if (user != null) {
            //super.onBackPressed();
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
        }
    }

}
