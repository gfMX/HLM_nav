package com.mezcaldev.hotlikeme;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by Abraham on 17/08/16.
 * Manage user credentials on Firebase
 */
public class FireConnection {
    String TAG = "FireConnection Singleton";
    FirebaseUser user;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;


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
                // ...

            }
        };

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
      return this.user;
    }

}
