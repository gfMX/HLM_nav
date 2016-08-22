package com.mezcaldev.hotlikeme;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HLMSlidePagerActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Location";

    public static String userKey;
    private ViewPager mPager;
    PagerAdapter mPagerAdapter;
    SharedPreferences sharedPreferences;

    //Location variables Initialization
    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /* Position */
    int maxUserDistance = 250;
    int fastInterval = 1000 * 30; // 30s
    int minInterval = 1000 * 60;
    private static final int TWO_MINUTES = 1000 * 60 * 5;

    /* Location with Google API */
    Location mCurrentLocation;
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

    // Others
    int x;
    int y;

    String gender;
    static List<String> users = new ArrayList<>();

    //Firebase Initialization
    FirebaseUser user = FireConnection.getInstance().getUser();
    final FirebaseDatabase database = FirebaseDatabase.getInstance();

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

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

        /*if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }*/

        updateValuesFromBundle(savedInstanceState);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gender = sharedPreferences.getString("looking_for", "Not specified");
        maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "250"));
        System.out.println("Max user distance allowed: " + maxUserDistance);
        System.out.println("Looking for: " + gender);
        mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOffscreenPageLimit(3);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageTransformer(true, new RotatePageTransformer());
        mPager.setOnTouchListener(new View.OnTouchListener() {
                                       @Override
                                       public boolean onTouch(View view, MotionEvent event) {
                                           x = (int) event.getX();
                                           y = (int) event.getY();

                                           switch (event.getAction() & MotionEvent.ACTION_MASK) {
                                               case MotionEvent.ACTION_DOWN:

                                                   break;
                                               case MotionEvent.ACTION_UP:
                                                   //mPager.endFakeDrag();
                                                   break;
                                               case MotionEvent.ACTION_MOVE:
                                                   //mPager.setY((mPager.getY() + mPager.getWidth()/2) + y);
                                               break;
                                           }

                                           return false;
                                       }
                                   }
        );

        System.out.println("Users: " + users);

        if (users.size() <= 0) {
            System.out.println("Getting Users");
            getUriProfilePics(gender);
        } else {
            System.out.println("User list OK");
        }

        createLocationRequest();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_hlm, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            clearInfo();
            finish();
            return true;
        }
        if (id == R.id.action_profile_settings) {
            startActivity(new Intent(this, LoginActivity.class));
            clearInfo();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }
    //A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in sequence.
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            userKey = users.get(position);
            return HLMPageFragment.newInstance(userKey);
        }

        @Override
        public int getCount() {
            return users.size();
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }
    }

    public void getUriProfilePics (String gender){
        DatabaseReference databaseReference = database.getReference().child("groups").child(gender);
        final DatabaseReference databaseReferenceLocation = database.getReference().child("users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int numChildren = (int) dataSnapshot.getChildrenCount();

                System.out.println("Number of users: " + numChildren);

                for (DataSnapshot data: dataSnapshot.getChildren()){
                    final String dataKey = data.getKey();
                    databaseReferenceLocation.child(dataKey).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Location remoteUserLocation;
                            if (dataSnapshot.child("location_last").child("loc_longitude").getValue() != null
                                    && dataSnapshot.child("location_last").child("loc_latitude").getValue() != null) {
                                double userLongitude = Double.parseDouble(dataSnapshot.child("location_last").child("loc_longitude").getValue().toString());
                                double userLatitude = Double.parseDouble(dataSnapshot.child("location_last").child("loc_latitude").getValue().toString());
                                System.out.println("Longitude: " + userLongitude);
                                System.out.println("Latitude: " + userLatitude);
                                remoteUserLocation = new Location("");
                                remoteUserLocation.setLongitude(userLongitude);
                                remoteUserLocation.setLatitude(userLatitude);
                                //if (mCurrentLocation.distanceTo(remoteUserLocation) < maxUserDistance && !dataKey.equals(user.getUid())){
                                if (mCurrentLocation.distanceTo(remoteUserLocation) < maxUserDistance){
                                    System.out.println("User " + dataKey + " reachable!");
                                    users.add(dataKey);
                                    mPagerAdapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    //System.out.println("User: " + dataKey);
                    //users.add(dataKey);
                    //mPagerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
            //New Location:
            DatabaseReference databaseReferenceNewLocation = database.getReference().child("users").child(user.getUid()).child("location_last");
            databaseReferenceNewLocation.child("loc_longitude").setValue(mCurrentLocation.getLongitude());
            databaseReferenceNewLocation.child("loc_latitude").setValue(mCurrentLocation.getLatitude());
            databaseReferenceNewLocation.child("loc_accuracy").setValue(mCurrentLocation.getAccuracy());
            databaseReferenceNewLocation.child("time").setValue(mLastUpdateTime);
            databaseReferenceNewLocation.child("day").setValue(mLastUpdateDay);
            Log.i(TAG, "Current BEST " + mCurrentLocation + " Time: " + mLastUpdateTime + " Day: " + mLastUpdateDay);
            //Old Location:
            if (mOldLocation != null) {
                DatabaseReference databaseReferenceOldLocation = database.getReference().child("users").child(user.getUid()).child("location_old");
                databaseReferenceOldLocation.child("loc_longitude").setValue(mOldLocation.getLongitude());
                databaseReferenceOldLocation.child("loc_latitude").setValue(mOldLocation.getLatitude());
                databaseReferenceOldLocation.child("loc_accuracy").setValue(mOldLocation.getAccuracy());
                databaseReferenceOldLocation.child("time").setValue(mOldTime);
                databaseReferenceOldLocation.child("day").setValue(mOldDay);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    System.out.println("Permission Granted!");
                } else {
                    // permission denied
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
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates locationSettingsStates = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        System.out.println("Access GRANTED by the User!");
                        //startLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(HLMSlidePagerActivity.this, REQUEST_CHECK_SETTINGS);
                            System.out.println("Access requested.");
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        System.out.println("Nothing to do.");
                        break;
                }
            }
        });
    }
    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            System.out.println("Permission GRANTED! Start tracking Location!");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
            System.out.println("Access Location services NOT ALLOWED! Requesting permission.");
        }
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
    private void clearInfo(){
        users.clear();
        mPagerAdapter.notifyDataSetChanged();
    }
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
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
            System.out.println("Updating from Bundle.");
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
        }
    }

    public class RotatePageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View page, float position) {
            page.setPivotX(page.getWidth() + x);
            page.setPivotY(page.getHeight()/2 + y);
            page.setRotation(position * +15f);

            page.setAlpha(1 - position);
            if (position < -1) { // [-Infinity,-1)
                page.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                page.setTranslationX(0);
                page.setTranslationY(0);
                page.setScaleX(1);
                page.setScaleY(1);

            } else if (position <= 1) { // (0,1]

                // Counteract the default slide transition
                page.setTranslationX(page.getWidth() * -position);
                page.setTranslationY(y/2 * position);
                //page.setTranslationY(page.getHeight()/2 * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                page.setAlpha(0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult()", Integer.toString(resultCode));
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode)
        {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                    {
                        // All required changes were successfully made
                        Toast.makeText(this, "Location enabled by user!", Toast.LENGTH_LONG).show();
                        startLocationUpdates();
                        break;
                    }
                    case Activity.RESULT_CANCELED:
                    {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    default:
                    {
                        break;
                    }
                }
                break;
        }
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO Auto-generated method stub
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        if (mRequestingLocationUpdates) {
            System.out.println("Requesting Location");
            startLocationUpdates();
        }
    }
    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }
}
