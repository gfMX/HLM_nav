package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    Intent intent;
    Handler handler;
    Integer delayTime = 00;
    Snackbar snackNetworkRequired;
    static FirebaseUser user;
    FireConnection fireConnection = FireConnection.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        snackNetworkRequired = Snackbar.make(this.getWindow().getDecorView(),
                getResources().getString(R.string.text_network_access_required),
                Snackbar.LENGTH_LONG);

        if (!isNetworkAvailable()) {
            snackNetworkRequired.show();
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, delayTime);
        } else {
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    user = fireConnection.getUser();

                    if (user != null){
                        System.out.println("User: " + user.getUid());
                        intent = new Intent(getApplicationContext(), HLMSlidePagerActivity.class);
                    } else {
                        intent = new Intent(getApplicationContext(), LoginActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
            }, delayTime);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
