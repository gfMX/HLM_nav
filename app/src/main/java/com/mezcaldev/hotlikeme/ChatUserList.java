package com.mezcaldev.hotlikeme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseUser;

public class ChatUserList extends AppCompatActivity {
    final static String TAG = "Chat: ";

    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_user_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        user = FireConnection.getInstance().getUser();
    }
    @Override
    public void onBackPressed() {
            //super.onBackPressed();
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
    }
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home ) {
            //NavUtils.navigateUpFromSameTask(this);
            startActivity(new Intent(this, HLMSlidePagerActivity.class));
            finish();
            return true;
        }
        return true;
    }
}
