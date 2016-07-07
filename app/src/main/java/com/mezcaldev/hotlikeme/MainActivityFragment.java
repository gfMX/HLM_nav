package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "FacebookLogin";

    //Facebook
    LoginButton loginButton;
    CallbackManager callbackManager;
    static AccessToken accessToken;
    static AccessTokenTracker accessTokenTracker;
    static Profile profile;
    static ProfileTracker profileTracker;

    //UI Elements
    static String imageProfileFileName = "profile_im.jpg";
    static String pathProfileImage;
    private ProfilePictureView profilePic;
    private ImageView imageProfileHLM;
    private TextView fb_welcome_text;
    private TextView text_instruct;
    private Button btn_image;
    private Button btn_start;

    //Firebase
    static FirebaseUser user;
    static FirebaseAuth mAuth;
    static FirebaseAuth.AuthStateListener mAuthListener;
    static FirebaseDatabase database;
    static DatabaseReference fireRef;
    static FirebaseStorage storage;
    static StorageReference storageRef;

    //Other elements
    String strValue;
    ImageSaver imageSaver = new ImageSaver();
    File profileImageCheck;

    public MainActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

        //Path to save and load images locally:
        Context context = getContext();
        File pathP = (new ContextWrapper(context).getDir("imageDir", Context.MODE_PRIVATE));
        pathProfileImage = pathP.getAbsolutePath();
        //Log.i(TAG, "Path obtained: " + pathP.getAbsolutePath());

        //Initialize Firebase
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
        fireRef = database.getReference();
        mAuth = FirebaseAuth.getInstance();

        //Gaining Tokens in Background:
        getUserAccess userAccess = new getUserAccess();
        userAccess.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        callbackManager = CallbackManager.Factory.create();

        profileImageCheck = new File(pathProfileImage + "/" + imageProfileFileName);
        Log.i(TAG, "Profile check path: " + profileImageCheck.getAbsolutePath());

        //View references for UI elements
        fb_welcome_text = (TextView) view.findViewById(R.id.fb_textWelcome);
        profilePic = (ProfilePictureView) view.findViewById(R.id.fb_image);
        imageProfileHLM = (ImageView) view.findViewById(R.id.hlm_image);

        btn_image = (Button) view.findViewById(R.id.btn_choose_img);
        btn_start = (Button) view.findViewById(R.id.btn_start);
        text_instruct = (TextView) view.findViewById(R.id.text_instruct);

        btn_image.setTransformationMethod(null);
        btn_start.setTransformationMethod(null);

        loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "public_profile", "user_photos");
        loginButton.setFragment(this);

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                accessToken = loginResult.getAccessToken();
                // App code
                Log.d(TAG, "FB: onSuccess:" + loginResult);
                Toast.makeText(getActivity(),"Welcome!",Toast.LENGTH_SHORT).show();

                handleFacebookAccessToken(accessToken);
                updateUI(accessToken);
                if (!profileImageCheck.exists()) {
                    imageSaver.iCreateBitmap(profile,
                            imageProfileFileName,
                            getActivity().getApplicationContext());
                }
            }

            @Override
            public void onCancel() {
                // App code
                Log.d(TAG, "FB: onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d(TAG, "FB: Error", exception);
            }
        });

        //Button behavior
        btn_image.setOnClickListener(settingsButtons);
        btn_start.setOnClickListener(settingsButtons);

        //Image Profile Behavior
        imageProfileHLM.isClickable();
        imageProfileHLM.setOnClickListener(settingsButtons);

    }
    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
      public void onClick (View v){
          switch (v.getId()){
              case R.id.btn_choose_img:
                  Toast.makeText(getActivity(), "Please choose a few Images for Hot Like Me!",
                          Toast.LENGTH_LONG).show();

                  strValue = "Facebook";

                  Intent ib = new Intent(getActivity(), ImageBrowser.class);
                  ib.putExtra(Intent.EXTRA_TEXT, strValue);
                  startActivity(ib);
                  break;
              case R.id.btn_start:
                  Toast.makeText(getActivity(), "Almost there!",
                          Toast.LENGTH_LONG).show();
                  //startActivity(new Intent(getActivity(), HomeActivity.class));
                  break;
              case R.id.hlm_image:
                  Toast.makeText(getActivity(), "Bla, bla bla... Change your profile Pic",
                          Toast.LENGTH_LONG).show();
                  strValue = "Firebase";

                  Intent ic = new Intent(getActivity(), ImageBrowser.class);
                  ic.putExtra(Intent.EXTRA_TEXT, strValue);
                  startActivity(ic);
                  break;
          }
      }
    };

    private class getUserAccess extends AsyncTask <Void, Void, Void>{
        @Override
        protected void onPreExecute(){

        }
        @Override
        protected Void doInBackground(Void...params){
            //Facebook Access Token & Profile:
            accessTokenTracker = new AccessTokenTracker() {
                @Override
                protected void onCurrentAccessTokenChanged(
                        AccessToken oldAccessToken,
                        AccessToken currentAccessToken) {
                    // Set the access token using.
                    // currentAccessToken when it's loaded or set.
                    //If the User is logged in, display the options for the user.
                    updateUI(currentAccessToken);
                    if (currentAccessToken == null){
                        Toast.makeText(getActivity(),"See you soon!",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(),"Welcome back!",Toast.LENGTH_SHORT).show();
                    }
                }
            };
            profileTracker = new ProfileTracker() {
                @Override
                protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {

                }
            };

            // If the access token is available already assign it.
            accessToken = AccessToken.getCurrentAccessToken();
            accessTokenTracker.startTracking();

            profile = Profile.getCurrentProfile();
            profileTracker.startTracking();

            //Auth Listener
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    user = firebaseAuth.getCurrentUser();
                    if (user != null){
                        //User sign in
                        Log.d(TAG, "Firebase: Signed In: " + user.getUid());

                        //Stores references needed by the App on Firebase:
                        fireRef = database.getReference(user.getUid() + "/name");
                        fireRef.setValue(user.getDisplayName());
                        fireRef = database.getReference(user.getUid() + "/alias");
                        fireRef.setValue("Your display name in here");
                    } else {
                        // User signed out
                        Log.d(TAG, "Firebase: Signed Out: ");
                    }
                    //Update UI
                    updateUI(accessToken);
                }
            };

            return null;
        }
        @Override
        protected void onPostExecute(Void result){

        }
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // ...
                    }
                });
    }

    //UI Status Updater
    private void updateUI (AccessToken accessToken) {
        //Update UI Elements according to the Given Token
        if (accessToken != null){
            if (profile != null) {
                fb_welcome_text.setText("Welcome " + profile.getFirstName() + "!");
            } else {
                fb_welcome_text.setText("Welcome!");
            }
            if (profileImageCheck.exists()) {
                imageProfileHLM.setImageBitmap(imageSaver.iLoadImageFromStorage(getView(),pathProfileImage,imageProfileFileName));
                imageSaver.iUploadFBImageToFirebase(profileImageCheck.getAbsolutePath(), user);
            }
            imageProfileHLM.setVisibility(View.VISIBLE);
            text_instruct.setText("Please choose some images for Hot Like Me. After that, click on your image profile to change it for one of your choice.\n\nThank you!");
            profilePic.setProfileId(accessToken.getUserId());
            btn_image.setVisibility(View.VISIBLE);
            btn_start.setVisibility(View.VISIBLE);
        } else {
            profilePic.setProfileId(null);
            imageProfileHLM.setVisibility(View.VISIBLE);
            imageProfileHLM.setImageBitmap(null);
            //imageProfileHLM.setImageResource(R.drawable.no_user);
            fb_welcome_text.setText("Welcome to Hot Like Me \n Please Log In");
            text_instruct.setText("");
            btn_image.setVisibility(View.INVISIBLE);
            btn_start.setVisibility(View.GONE);
            if (profileImageCheck.exists()) {
                try {
                    profileImageCheck.delete();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        accessTokenTracker.startTracking();
        profileTracker.startTracking();
    }

    @Override
    public void onPause() {
        super.onPause();
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

}
