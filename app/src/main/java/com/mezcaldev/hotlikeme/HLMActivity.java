package com.mezcaldev.hotlikeme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.mezcaldev.hotlikeme.FireConnection.ONE_HOUR;
import static com.mezcaldev.hotlikeme.FireConnection.ONE_MINUTE;
import static com.mezcaldev.hotlikeme.FireConnection.ONE_SECOND;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigDecIteration;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLength;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLengthDefault;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLimit;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLimitDefault;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageOld;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageOldDefault;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessagesMax;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessagesMaxDefault;
import static com.mezcaldev.hotlikeme.FireConnection.friendly_msg_length;

public class HLMActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HLM Main";
    //FirebaseUser HLM_CURRENT_USER;
    int HLM_PAGES;
    int HLM_CURRENT_PAGE;
    final int HLM_PAGES_MAX = 3;
    final static int PAGE_LOGIN = 0;
    final static int PAGE_HLM = 1;
    final static int PAGE_CHAT = 2;

    private ViewPager mPager;
    PagerAdapter mPagerAdapter;
    SharedPreferences sharedPreferences;

    // Notifications:
    boolean isInFront;
    int MAX_NOTIFICATION_LENGTH = 42;
    NotificationManager mNotificationManager;
    static android.support.v4.app.NotificationCompat.Builder mBuilder;


    // Location variables Initialization
    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /* Position */
    private static final int MINUTES = ONE_MINUTE * 5;
    int maxUserDistance = 250;
    int fastInterval = ONE_SECOND * 30;
    int minInterval = ONE_MINUTE;

    /* Location with Google API */
    static Location mCurrentLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Boolean mRequestingLocationUpdates;
    String mLastUpdateTime;
    String mLastUpdateDay;
    Location mOldLocation;
    String mOldTime;
    String mOldDay;
    final int REQUEST_CHECK_SETTINGS = 2543;

    //Saved State
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    protected final static String NUMBER_OF_PAGES = "visible-pages";
    protected final static String NUMBER_OF_CURRENT_PAGE = "last-viewed-page";

    // Others
    int x;
    int y;

    String gender;
    Snackbar snackNetworkRequired;

    //Firebase Initialization
    FirebaseUser user;
    FirebaseAuth mAuth;
    FirebaseRemoteConfig mFirebaseRemoteConfig;
    FirebaseAuth.AuthStateListener mAuthListener;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();

    //Drawer variables and Settings
    ImageView drawerUserImage;
    TextView drawerUserAlias;
    TextView drawerUserDescription;

    View headerView;
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.hlm_screen_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            headerView = navigationView.getHeaderView(0);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("pages")) {
                HLM_PAGES = bundle.getInt("pages");
            }
            if (bundle.containsKey("currentPage")){
                HLM_CURRENT_PAGE = bundle.getInt("currentPage");
            }
        }
        Log.i(TAG, "Bundle: " + bundle);

        //Local FIrebase Initialization.
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "User credentials granted: " + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "User not logged.");
                }
                // ...
                userConnected();
            }
        };

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);

        drawerUserImage = (ImageView) headerView.findViewById(R.id.drawer_image);
        drawerUserAlias = (TextView) headerView.findViewById(R.id.drawer_user);
        drawerUserDescription = (TextView) headerView.findViewById(R.id.drawer_description);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gender = sharedPreferences.getString("looking_for", "Not specified");
        maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "250"));
        minInterval = (Integer.valueOf(sharedPreferences.getString("sync_frequency","1")) * ONE_MINUTE);
        mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        //mPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mPager.setOnTouchListener(new View.OnTouchListener() {
                                       @Override
                                       public boolean onTouch(View view, MotionEvent event) {
                                           x = (int) event.getX();
                                           y = (int) event.getY();
                                           return false;
                                       }
                                   }
        );

        drawerUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(PAGE_LOGIN);
                if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        //FireBase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(false)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", fireConfigMessageLengthDefault);
        defaultConfigMap.put("messages_limit", fireConfigMessageLimitDefault);
        defaultConfigMap.put("load_old_messages", fireConfigMessageOldDefault);
        defaultConfigMap.put("max_messages", fireConfigMessagesMaxDefault);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        checkNetworkAccess();

        buildGoogleApiClient();
        updateValuesFromBundle(savedInstanceState);

        fetchConfig();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_hlm, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case R.id.action_profile_settings:
                mPager.setCurrentItem(PAGE_LOGIN);
                return true;

            case R.id.action_chat:
                mPager.setCurrentItem(PAGE_CHAT);
                return true;

            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mPager.getCurrentItem() == 0) {

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
                                ActivityCompat.finishAffinity(HLMActivity.this);
                            }
                        }

                    })
                    .setNegativeButton("Not yet", null)
                    .show();

        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_hlm) {
            mPager.setCurrentItem(PAGE_HLM);

        } else if (id == R.id.nav_chat) {
            mPager.setCurrentItem(PAGE_CHAT);

        } else if (id == R.id.nav_profile) {
            mPager.setCurrentItem(PAGE_LOGIN);

        } else if (id == R.id.nav_settings) {
            if (user != null) {
                startActivity(new Intent(this, HLMSettings.class));
            }

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        private ScreenSlidePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            //return fragments[position];
            switch (position) {
                case PAGE_LOGIN:
                    return LoginFragment.newInstance();
                case PAGE_HLM:
                    return HLMUsers.newInstance();
                case PAGE_CHAT:
                    return ChatUserList.newInstance();
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            return HLM_PAGES;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }
    }

    public void selectPage(int page) {
        if (page < HLM_PAGES) {
            mPager.setCurrentItem(page);
        }
    }

    public void sendNotification(String messageTitle, String messageBody, int nID) {

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Bundle bundle = new Bundle();
        bundle.putInt("pages", HLM_PAGES);
        bundle.putInt("currentPage", PAGE_CHAT);

        Intent resultIntent = new Intent(this, HLMActivity.class);
        resultIntent.putExtras(bundle);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_chat_white_24dp)
                        .setContentTitle(messageTitle)
                        .setContentText(StringUtils.abbreviate(messageBody, MAX_NOTIFICATION_LENGTH))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(resultPendingIntent);
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (!isInFront) {
            mNotificationManager.notify(nID, mBuilder.build());
        }
    }

    public void updateNotification (String messageTitle, String messageBody, int nId){
        if (mBuilder != null && mNotificationManager != null && !isInFront){
            mBuilder.setContentTitle(messageTitle);
            mBuilder.setContentText(messageBody);
            mNotificationManager.notify(nId, mBuilder.build());

            Log.i(TAG, "Notification Updated!");
        }
    }

    public void cancelNotifications (){
        if (mNotificationManager != null && mBuilder != null){
            mNotificationManager.cancelAll();
        }
    }

    // Fetch the config to determine the allowed length of messages.
    private void fetchConfig() {
        Log.i(TAG, "Getting remote Config");
        long cacheExpiration = ONE_HOUR * 12;
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    private void applyRetrievedLengthLimit() {
        friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");

        fireConfigMessageLength = (int) mFirebaseRemoteConfig.getLong("friendly_msg_length");
        fireConfigMessageLimit = (int) mFirebaseRemoteConfig.getLong("messages_limit");
        fireConfigMessageOld = (int) mFirebaseRemoteConfig.getLong("load_old_messages");
        fireConfigDecIteration = (int) mFirebaseRemoteConfig.getLong("iteration_count");
        fireConfigMessagesMax = (int) mFirebaseRemoteConfig.getLong("max_messages");
        Log.d(TAG, "HLM Message Length is: " + fireConfigMessageLength
                + "\nHLM Display Messages: " + fireConfigMessageLimit
                + "\nHLM Old Messages: " + fireConfigMessageOld
                + "\nHLM Max Messages: " + fireConfigMessagesMax
                + "\nHLM Iteration Count: " + fireConfigDecIteration
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkNetworkAccess (){
        snackNetworkRequired = Snackbar.make(this.getWindow().getDecorView(),
                getResources().getString(R.string.text_network_access_required),
                Snackbar.LENGTH_INDEFINITE);

        if (!isNetworkAvailable()) {
            snackNetworkRequired.show();
        } else if (snackNetworkRequired.isShown()) {
            snackNetworkRequired.dismiss();
        }
    }

    private void userConnected(){
        if (user != null) {
            //Glide.clear(drawerUserImage);  // <-- Last Addition
            Glide
                    .with(this.getApplicationContext())
                    .load(user.getPhotoUrl())
                    .centerCrop()
                    .into(drawerUserImage);
            drawerUserAlias.setText(user.getDisplayName());
        } else {
            drawerUserImage.setImageResource(R.drawable.ic_account_circle_24dp);
            drawerUserAlias.setText(R.string.app_name);
        }
        checkPager();
    }

    private void checkPager(){
        if (user != null){
            HLM_PAGES = HLM_PAGES_MAX;
            mPagerAdapter.notifyDataSetChanged();

        } else {
            HLM_PAGES = 1;
            mPagerAdapter.notifyDataSetChanged();
        }
        mPager.setOffscreenPageLimit(HLM_PAGES);
        Log.i(TAG, "Offscreen Limit: " + HLM_PAGES);
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

                createLocationRequest();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, mCurrentLocation)) {
            mOldLocation = mCurrentLocation;
            mOldTime = mLastUpdateTime;
            mOldDay = mLastUpdateDay;
            mCurrentLocation = location;
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            mLastUpdateDay = DateFormat.getDateInstance().format(new Date());
            if (user != null) {
                //Update Users List:
                FireConnection.getInstance().getFirebaseUsers(sharedPreferences, mCurrentLocation);
                //New Location:
                DatabaseReference databaseReferenceNewLocation = database.getReference()
                        .child("users")
                        .child(user.getUid())
                        .child("location_last");
                databaseReferenceNewLocation.child("loc_longitude").setValue(mCurrentLocation.getLongitude());
                databaseReferenceNewLocation.child("loc_latitude").setValue(mCurrentLocation.getLatitude());
                databaseReferenceNewLocation.child("loc_accuracy").setValue(mCurrentLocation.getAccuracy());
                databaseReferenceNewLocation.child("time").setValue(mLastUpdateTime);
                databaseReferenceNewLocation.child("day").setValue(mLastUpdateDay);
                Log.i(TAG, "Current BEST " + mCurrentLocation + " Time: " + mLastUpdateTime + " Day: " + mLastUpdateDay);
                //Old Location:
                if (mOldLocation != null) {
                    DatabaseReference databaseReferenceOldLocation = database.getReference()
                            .child("users")
                            .child(user.getUid())
                            .child("location_old");
                    databaseReferenceOldLocation.child("loc_longitude").setValue(mOldLocation.getLongitude());
                    databaseReferenceOldLocation.child("loc_latitude").setValue(mOldLocation.getLatitude());
                    databaseReferenceOldLocation.child("loc_accuracy").setValue(mOldLocation.getAccuracy());
                    databaseReferenceOldLocation.child("time").setValue(mOldTime);
                    databaseReferenceOldLocation.child("day").setValue(mOldDay);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission Granted!");
                    startLocationUpdates();
                } else {
                    System.out.println("Permission NOT Granted!");
                }
                break;
            }

        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(minInterval);
        mLocationRequest.setFastestInterval(fastInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates locationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "Access GRANTED by the User!");
                        startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(HLMActivity.this, REQUEST_CHECK_SETTINGS);
                            Log.w(TAG, "Access requested.");
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "Nothing to do.");
                        break;
                }
            }
        });
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.i(TAG, "Permission GRANTED! Start tracking Location!");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
            Log.e(TAG, "Access Location services NOT ALLOWED! Requesting permission.");
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MINUTES;
        boolean isSignificantlyOlder = timeDelta < -MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            Log.i(TAG, "Updating from Bundle." + savedInstanceState);
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }
            // Update the value of mCurrentLocation from the Bundle
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            if (savedInstanceState.keySet().contains(NUMBER_OF_PAGES)){
                HLM_PAGES = savedInstanceState.getInt(
                        NUMBER_OF_PAGES);
            }
            if (savedInstanceState.keySet().contains(NUMBER_OF_CURRENT_PAGE)){
                HLM_CURRENT_PAGE = savedInstanceState.getInt(
                        NUMBER_OF_CURRENT_PAGE);
            }
        } else if (HLM_PAGES > 1 && HLM_CURRENT_PAGE == PAGE_CHAT){
            HLM_CURRENT_PAGE = PAGE_CHAT;

        } else if (HLM_PAGES > 1){
            HLM_CURRENT_PAGE = PAGE_HLM;
        }
        Log.i(TAG, "Recovered State: " + savedInstanceState);
        mPager.setCurrentItem(HLM_CURRENT_PAGE);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.clear();

        HLM_CURRENT_PAGE = mPager.getCurrentItem();

        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putInt(NUMBER_OF_PAGES, HLM_PAGES);
        savedInstanceState.putInt(NUMBER_OF_CURRENT_PAGE, HLM_CURRENT_PAGE);
        Log.v(TAG, "-------------------------------");
        Log.v(TAG, "Data Saved: State Recovery");
        Log.v(TAG, "-------------------------------");
        Log.i(TAG,"State: " + savedInstanceState);


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mCurrentLocation == null &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
        if (mRequestingLocationUpdates) {
            Log.i(TAG, "Requesting Location");
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /*public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    } */

    @Override
    protected void onStart() {
        super.onStart();

        isInFront =true;

        mGoogleApiClient.connect();
        if (user ==null && mGoogleApiClient.isConnected() && mRequestingLocationUpdates){
            mGoogleApiClient.disconnect();
        }
        checkNetworkAccess();
    }
    @Override
    protected void onStop() {
        super.onStop();

        isInFront =false;
        mGoogleApiClient.disconnect();
    }
    @Override
    protected void onResume(){
        super.onResume();
        isInFront = true;
        mPagerAdapter.notifyDataSetChanged();

        if (user != null && mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        } else if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates){
            stopLocationUpdates();
        }
        checkNetworkAccess();

        // <-- Last Addition
        if (user != null && drawerUserImage != null) {
            Glide.clear(drawerUserImage);
            Glide
                    .with(this.getApplicationContext())
                    .load(user.getPhotoUrl())
                    .centerCrop()
                    .into(drawerUserImage);
            drawerUserAlias.setText(user.getDisplayName());
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        isInFront = false;

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
