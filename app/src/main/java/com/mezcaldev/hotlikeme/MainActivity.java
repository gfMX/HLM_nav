package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    Intent intent;
    Handler handler;
    int delayTime = 1000 * 2;
    int HLM_PAGES = 3;
    Snackbar snackNetworkRequired;
    static Context mContext;
    static FirebaseUser user;
    FireConnection fireConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //FacebookSdk.sdkInitialize(this.getApplicationContext());

        fireConnection = FireConnection.getInstance();
        checkAccess();
    }

    private void checkAccess (){
        snackNetworkRequired = Snackbar.make(this.getWindow().getDecorView(),
                getResources().getString(R.string.text_network_access_required),
                Snackbar.LENGTH_INDEFINITE);

        if (!isNetworkAvailable()) {
            snackNetworkRequired.setAction("TRY AGAIN", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkAccess();
                }
            });
            snackNetworkRequired.show();
        } else {
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    user = fireConnection.getUser();

                    Bundle bundle = new Bundle();

                    if (user != null){
                        System.out.println("User: " + user.getUid());
                        bundle.putInt("pages", HLM_PAGES);
                    } else {
                        bundle.putInt("pages", 1);
                    }
                    intent = new Intent(getApplicationContext(), HLMActivity.class);
                    intent.putExtras(bundle);
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

    public static Location getLocation() {
        Location location;
        LocationManager locationManager = (LocationManager) MainActivity.getContext().getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(MainActivity.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null) {
                Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocationGPS != null) {
                    location = lastKnownLocationGPS;
                } else {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                }
            } else {
                location = null;
            }
        } else {
            System.out.println("No location acquired!");
            location = null;
        }
        System.out.println("Location: " + location);
        return location;
    }

    private static Context getContext() {
        return mContext;
    }

}
