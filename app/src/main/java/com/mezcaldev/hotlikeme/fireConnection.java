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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Abraham on 17/08/16.
 * Manage user credentials on Firebase
 */
public class FireConnection {
    String TAG = "FireConnection";

    //Firebase Settings
    static FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    //SharedPreferences sharedPreferences;
    FirebaseDatabase database;

    //Facebook Settings
    //static boolean fbTokenStatus;
    //private AccessToken accessToken;

    //Location mCurrentLocation;
    static Boolean weLike = false;

    private String gender;
    private Integer maxUserDistance;
    private Boolean mRequestingLocationUpdates;


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
                } else {
                    // User is signed out
                    Log.d(TAG, "User not logged.");
                }
                if (user != null && usersList == null) {
                    Log.e(TAG, "Getting Full List of Users <-- Not Implemented Just checking if its called.");
                }
                // Check if FaceBook Token is valid, if not Sign Out from FireBase
            }
        };

        //getFbToken();

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
      return user;
    }

    void getFirebaseUsers(SharedPreferences sharedPreferences, final Location mCurrentLocation){
        if (user !=null){
            usersList.clear();
            database = FirebaseDatabase.getInstance();

            gender = sharedPreferences.getString("looking_for", "both");
            maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "1000"));
            mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);
            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);


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

    String genHashKey(String myFutureKey){
        
        Log.i(TAG, "-------> KEY For Decryption: " + myFutureKey);
        myFutureKey = new StringBuilder(myFutureKey.replace("chat_", "")).reverse().toString();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(myFutureKey.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            Log.i(TAG, "-------> HASHKey: " + sb.toString());

            return sb.toString();

        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();

            return "null";
        }
    }

}
