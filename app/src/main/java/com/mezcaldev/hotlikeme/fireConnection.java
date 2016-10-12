package com.mezcaldev.hotlikeme;

import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Abraham on 17/08/16.
 * Manage user credentials on Firebase
 */
public class FireConnection {
    String TAG = "Singleton";
    static FirebaseUser user;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    SharedPreferences sharedPreferences;
    FirebaseDatabase database;
    //Location mCurrentLocation;
    static Boolean weLike = false;


    //private Boolean flagOneTime = false;
    static List<String> usersList = new ArrayList<>();

    static final int ONE_SECOND = 1000;
    static final int ONE_MINUTE = ONE_SECOND * 60;
    static final int ONE_HOUR = ONE_MINUTE * 60;

    private static FireConnection ourInstance = new FireConnection();

    public static FireConnection getInstance() {
        return ourInstance;
    }

    private FireConnection() {
        user = getUser();
    }

    public FirebaseUser getUser(){
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "User credentials granted: " + user.getUid());
                    /*if (!flagOneTime) {
                        getFirebaseUsers(null, null);
                        flagOneTime = true;
                    } */
                } else {
                    // User is signed out
                    Log.d(TAG, "User not logged.");
                }
                // ...

            }
        };

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
      return user;
    }

    public void getFirebaseUsers(SharedPreferences sharedPreferences, final Location mCurrentLocation){
        if (user !=null){
            usersList.clear();

            database = FirebaseDatabase.getInstance();

            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Log.i(TAG, "Preferences: " + sharedPreferences);

            String gender = sharedPreferences.getString("looking_for", "both");
            final Integer maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "250"));
            final Boolean mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);

            final DatabaseReference databaseReferenceUriProfile = database.getReference().child("groups").child(gender);
            final DatabaseReference databaseReferenceLocation = database.getReference().child("users");

            databaseReferenceUriProfile.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int numChildren = (int) dataSnapshot.getChildrenCount();
                    Log.i(TAG, "Number of users: " + numChildren);

                    for (DataSnapshot data: dataSnapshot.getChildren()){
                        final String dataKey = data.getKey();
                        databaseReferenceLocation.child(dataKey).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Location remoteUserLocation;

                                        //Check permission and proceed according to them
                                        if(!mRequestingLocationUpdates || mCurrentLocation == null){
                                            Log.i(TAG, "All users are visible");
                                            usersList.add(dataKey);

                                       } else {
                                            Double userLongitude = (Double) dataSnapshot.child("location_last").child("loc_longitude")
                                                    .getValue();
                                            Double userLatitude = (Double) dataSnapshot.child("location_last").child("loc_latitude")
                                                    .getValue();
                                            //Request location of the Remote User
                                            if (userLongitude != null && userLatitude != null) {

                                                remoteUserLocation = new Location("");
                                                remoteUserLocation.setLongitude(userLongitude);
                                                remoteUserLocation.setLatitude(userLatitude);

                                               Log.i(TAG, "Remote User Location: " + remoteUserLocation);

                                                if (mCurrentLocation.distanceTo(remoteUserLocation) <= maxUserDistance
                                                        && !dataKey.equals(user.getUid())){

                                                    System.out.println("User " + dataKey + " reachable!");
                                                    usersList.add(dataKey);

                                                }
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                }
                        );
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Log.i(TAG, "There's no User Logged");
        }
    }

}
