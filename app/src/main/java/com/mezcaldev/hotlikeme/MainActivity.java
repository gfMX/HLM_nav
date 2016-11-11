package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import static com.mezcaldev.hotlikeme.FireConnection.ONE_SECOND;
import static com.mezcaldev.hotlikeme.FireConnection.databaseGlobal;
import static com.mezcaldev.hotlikeme.FireConnection.user;
import static com.mezcaldev.hotlikeme.FireConnection.usersList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Start";
    Intent intent;
    Handler handler;
    int delayTime = ONE_SECOND * 2;
    int HLM_PAGES = 3;
    Snackbar snackNetworkRequired;
    Context mContext;
    FireConnection fireConnection;
    SharedPreferences sharedPreferences;

    //FaceBook
    AccessToken accessToken;

    public MainActivity (){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        if (databaseGlobal == null) {
            databaseGlobal = FirebaseDatabase.getInstance();
            databaseGlobal.setPersistenceEnabled(true);
            Log.i(TAG, "----> FireBase Persistence Enabled <----");
        } else {
            Log.e(TAG, "----> FireBase Persistence Not Enabled <----");
        }

        mContext = getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        FacebookSdk.sdkInitialize(this.getApplicationContext());

        fireConnection = FireConnection.getInstance();

        checkAccess();
        getFbToken();

        Log.i(TAG, "Data: " + user + " Users: " + usersList);
    }

    private void checkAccess (){
        snackNetworkRequired = Snackbar.make(this.findViewById(R.id.MainHLMCoordinator),
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
                    FireConnection.getInstance().getFirebaseUsers(sharedPreferences, getLocation());

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

    private void getFbToken() {
        //Facebook Access Token & Profile:
        accessToken = AccessToken.getCurrentAccessToken();
        Log.i(TAG, "Current Access Token from FaceBook: " + accessToken);

        if (accessToken == null) {
            FirebaseAuth.getInstance().signOut();
            Log.e(TAG, "Login out from FireBase, missing Token from FaceBook");

        } else {
            Log.i(TAG, "Valid Token: " + accessToken);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public Location getLocation() {
        Location location;
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
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


}
