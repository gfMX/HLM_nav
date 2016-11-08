package com.mezcaldev.hotlikeme;

import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Abraham on 17/08/16.
 * Manage user credentials on Firebase
 */
public class FireConnection {
    String TAG = "FireConnection";

    //Flags to Activities / Fragments on Front
    static boolean chatIsInFront = false;

    //Firebase Settings
    static FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    static FirebaseDatabase databaseGlobal;

    //Remote Config from Firebase
    private boolean flagRunOnce = true;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    static final int fireConfigDecIterationDefault = 2000;
    static final int fireConfigMessageOldDefault = 5;
    static final int fireConfigMessageLengthDefault = 160;
    static final int fireConfigMessageLimitDefault = 20;
    static final int  fireConfigMessagesMaxDefault = 40;

    static int fireConfigMessagesMax;
    static int fireConfigMessageLength;
    static int fireConfigMessageLimit;
    static int fireConfigMessageOld;
    static long friendly_msg_length;
    static int fireConfigDecIteration = fireConfigDecIterationDefault;

    //Location mCurrentLocation;
    static Boolean weLike = false;

    private String gender;
    private Integer maxUserDistance;
    private Boolean mRequestingLocationUpdates;

    private int numChildren;
    static List<String> usersList = new ArrayList<>();
    static List<Integer> randomUserList = new ArrayList<>();

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
                    if (databaseGlobal == null) {
                        databaseGlobal = FirebaseDatabase.getInstance();
                    }
                    // User is signed in
                    Log.d(TAG, "User credentials granted: " + user.getUid());

                    //Fetch Remote Config from FireBase
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            remoteConfigFromFire();
                        }
                    });
                    if (flagRunOnce) {
                        thread.start();
                        flagRunOnce = false;
                    }


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

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
      return user;
    }

    void getFirebaseUsers(SharedPreferences sharedPreferences, final Location mCurrentLocation){
        if (user !=null){
            usersList.clear();

            databaseGlobal = FirebaseDatabase.getInstance();

            gender = sharedPreferences.getString("looking_for", "both");
            maxUserDistance = Integer.valueOf(sharedPreferences.getString("sync_distance", "1000"));
            mRequestingLocationUpdates = sharedPreferences.getBoolean("gps_enabled", false);

            final DatabaseReference databaseReferenceUriProfile = databaseGlobal.getReference().child("groups").child(gender);
            final DatabaseReference databaseReferenceLocation = databaseGlobal.getReference().child("users");

            databaseReferenceUriProfile.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    numChildren = (int) dataSnapshot.getChildrenCount();
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
                                        genUserRandomCollection(usersList.size());
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

    //Initialize Remote Config

    private void remoteConfigFromFire(){
        //FireBase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", fireConfigMessageLengthDefault);
        defaultConfigMap.put("messages_limit", fireConfigMessageLimitDefault);
        defaultConfigMap.put("load_old_messages", fireConfigMessageOldDefault);
        defaultConfigMap.put("max_messages", fireConfigMessagesMaxDefault);
        defaultConfigMap.put("iteration_count", fireConfigDecIterationDefault);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        fetchConfig();
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

    private void genUserRandomCollection(int nUsers){
        randomUserList.clear();
        if (nUsers > 0){
            for (int i = 0; i < nUsers; i++) {
                randomUserList.add(i);
            }
            //Log.i(TAG, "Original User List: " + randomUserList);
            Collections.shuffle(randomUserList);
            Log.i(TAG, "Shuffled User List: " + randomUserList);
        } else{
            Log.e(TAG, "No users found to generate random list");
        }

    }

    String genHashKey(String myFutureKey){
        
        //Log.i(TAG, "-------> KEY For Decryption: " + myFutureKey);
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
            //Log.i(TAG, "-------> HASHKey: " + sb.toString());
            return sb.toString();

        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return "null";
        }
    }

}
