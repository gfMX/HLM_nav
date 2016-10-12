package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LoginFragment extends Fragment {

    static LoginFragment newInstance() {
        LoginFragment newFragment = new LoginFragment();

        return newFragment;
    }

    private static final String TAG = "Login Details";
    //Delay Time to load Profile Picture if exists.
    Handler handler = new Handler();
    Runnable runnable;
    Integer minDelayTime = 100;
    Integer delayTime = 250;
    Boolean oneTime = false;

    //Facebook
    LoginButton loginButton;
    CallbackManager callbackManager;
    static AccessToken accessToken;
    static AccessTokenTracker accessTokenTracker;
    static Profile profile;
    static ProfileTracker profileTracker;
    String profileName;
    String welcomeText;
    String instructionText;
    String signInText;
    static String gender;

    //UI Elements
    static String imageProfileFileName = "profile_im.jpg";
    static String pathProfileImage;
    private ProfilePictureView profilePic;
    private ImageView imageProfileHLM;
    private TextView fb_welcome_text;
    private TextView text_instruct;
    private Button btn_image;
    private Button btn_start;
    private Button btn_settings;
    Snackbar snackNetworkRequired;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    //Firebase
    static FirebaseUser user;
    static FirebaseAuth mAuth;
    static FirebaseAuth.AuthStateListener mAuthListener;
    static FirebaseDatabase database;
    static DatabaseReference fireRef;
    static FirebaseStorage storage;
    static StorageReference storageRef;
    Boolean flagImagesOnFirebase = false;

    //Other elements
    //ImageSaver imageSaver = new ImageSaver();
    File profileImageCheck;


    public LoginFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        snackNetworkRequired = Snackbar.make(getActivity().getWindow().getDecorView(),
                getResources().getString(R.string.text_network_access_required),
                Snackbar.LENGTH_LONG);

        if (!isNetworkAvailable()) {
            snackNetworkRequired.show();
        }

        //Path to save and load images locally:
        Context context = getContext();
        File pathP = (new ContextWrapper(context).getDir("imageDir", Context.MODE_PRIVATE));
        pathProfileImage = pathP.getAbsolutePath();

        //Initialize Firebase
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
        fireRef = database.getReference();
        mAuth = FirebaseAuth.getInstance();


        user = mAuth.getCurrentUser();
        if ( user != null){
            oneTime = true;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //View view = inflater.inflate(R.layout.fragment_login, container, false);

        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        //Gaining Tokens in Background:
        getUserAccess();

        profileImageCheck = new File(pathProfileImage + "/" + imageProfileFileName);

        //View references for UI elements
        fb_welcome_text = (TextView) view.findViewById(R.id.fb_textWelcome);
        profilePic = (ProfilePictureView) view.findViewById(R.id.fb_image);
        imageProfileHLM = (ImageView) view.findViewById(R.id.hlm_image);
        //imageProfileHLM.setRotation(5 * ((float) Math.random() * 2 - 1));

        btn_image = (Button) view.findViewById(R.id.btn_choose_img);
        btn_start = (Button) view.findViewById(R.id.btn_start);
        btn_settings = (Button) view.findViewById(R.id.btn_settings);
        text_instruct = (TextView) view.findViewById(R.id.text_instruct);

        welcomeText = getResources().getString(R.string.text_welcome);
        instructionText = getResources().getString(R.string.text_start_HLM);
        signInText = getResources().getString(R.string.text_sign_in);

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
                /*Toast.makeText(getActivity(),
                        getResources().getString(R.string.text_welcome),
                        Toast.LENGTH_SHORT)
                        .show();*/
                handleFacebookAccessToken(accessToken);
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
        
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btn_image.setTransformationMethod(null);
        btn_start.setTransformationMethod(null);
        btn_settings.setTransformationMethod(null);
        //Button behavior
        btn_image.setOnClickListener(settingsButtons);
        btn_start.setOnClickListener(settingsButtons);
        btn_settings.setOnClickListener(settingsButtons);

        //Image Profile Behavior
        imageProfileHLM.setOnClickListener(settingsButtons);

        updateUI();
    }

    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
      public void onClick (View v){
          if (isNetworkAvailable()){
              switch (v.getId()) {
                  case R.id.btn_choose_img:
                      Toast.makeText(getActivity(),
                              getResources().getString(R.string.text_choose_images),
                              Toast.LENGTH_LONG)
                              .show();

                      startActivity(new Intent(getActivity(), ImageBrowser.class));
                      break;
                  case R.id.btn_start:
                      Toast.makeText(getActivity(), getResources().getString(R.string.text_hlm_start_button),
                              Toast.LENGTH_LONG).show();

                      ((HLMActivity) getActivity()).selectPage(HLMActivity.PAGE_HLM);

                      break;
                  case R.id.btn_settings:
                      Toast.makeText(getActivity(), getResources().getString(R.string.text_settings_activity),
                              Toast.LENGTH_LONG).show();
                      startActivity(new Intent(getActivity(), HLMSettings.class));
                      break;
                  case R.id.hlm_image:

                      if (flagImagesOnFirebase) {
                          Toast.makeText(getActivity(), getResources().getString(R.string.text_hlm_change_profile_pic),
                                  Toast.LENGTH_LONG).show();

                          startActivity(new Intent(getActivity(), FireBrowserActivity.class));
                      } else {
                          Toast.makeText(getActivity(), getResources().getString(R.string.text_first_select_images),
                                  Toast.LENGTH_LONG).show();
                      }
                      break;
              }
          } else {
              snackNetworkRequired.show();
          }
      }
    };

    private void getUserAccess() {
            //Facebook Access Token & Profile:
            accessTokenTracker = new AccessTokenTracker() {
                @Override
                protected void onCurrentAccessTokenChanged(
                        AccessToken oldAccessToken,
                        AccessToken currentAccessToken) {
                    // Set the access token using.
                    // currentAccessToken when it's loaded or set.
                    //If the User is logged in, display the options for the user.
                    if (currentAccessToken == null){
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.text_see_you_soon),
                                Toast.LENGTH_SHORT).show();

                        FirebaseAuth.getInstance().signOut();
                        Log.i(TAG, "Firebase: " + FirebaseAuth.getInstance().toString());

                    } else {
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.text_welcome_again),
                                Toast.LENGTH_SHORT).show();
                    }
                    updateUI();
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
            if (profile != null) {
                profileName = profile.getFirstName();
                welcomeText = getResources().getString(R.string.text_welcome) + profileName;
            } else {
                welcomeText = getResources().getString(R.string.text_welcome);
            }
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
                        fireRef = database.getReference().child("users").child(user.getUid()).child("preferences").child("name");
                        fireRef.setValue(user.getDisplayName());

                        if (accessToken!=null) {
                            loadProfileDetails(delayTime);
                            /* if (!oneTime) {
                                FireConnection.getInstance().getFirebaseUsers(getActivity().getApplicationContext(), HLMActivity.mCurrentLocation);
                            } */
                        }
                    } else {
                        // User signed out
                        //deleteLocalProfilePic();
                        Log.d(TAG, "Firebase: Signed Out");
                        System.out.println("Singleton user: " + FireConnection.getInstance().getUser());
                    }
                    FireConnection.getInstance().getUser();
                    //FireConnection.getInstance().getFirebaseUsers(getActivity().getApplicationContext(), HLMActivity.mCurrentLocation);
                    updateUI();
                }
            };
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        //Log.d(TAG, "handleFacebookAccessToken:" + token);

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
                        getFacebookDetails();
                    }
                });
    }

    //UI Status Updater
    private void updateUI () {
        //Update UI Elements according to the Given Token
        if (user != null){
            fb_welcome_text.setText(welcomeText);

            loadProfileDetails(minDelayTime);

            text_instruct.setText(instructionText);
            profilePic.setVisibility(View.VISIBLE);
            if (accessToken !=null) {
                profilePic.setProfileId(accessToken.getUserId());
            }
            imageProfileHLM.setClickable(true);
            if (user.getPhotoUrl() != null) {
                //Glide do better RAM Optimization:
                Glide
                        .with(getContext())
                        .load(user.getPhotoUrl())
                        .centerCrop()
                        .into(imageProfileHLM);
                flagImagesOnFirebase = true;
            } else {
                flagImagesOnFirebase = false;
            }
            btn_image.setVisibility(View.VISIBLE);
            btn_start.setVisibility(View.VISIBLE);
            btn_settings.setVisibility(View.VISIBLE);
        } else {
            profilePic.setProfileId(null);
            profilePic.setVisibility(View.INVISIBLE);
            imageProfileHLM.setImageResource(R.drawable.ic_account_circle_gray_64dp);
            imageProfileHLM.setClickable(false);
            fb_welcome_text.setText(signInText);
            text_instruct.setText(null);
            btn_image.setVisibility(View.GONE);
            btn_start.setVisibility(View.GONE);
            btn_settings.setVisibility(View.GONE);

            /*boolean isDeleted = profileImageCheck.exists();
            if (isDeleted) {
                try {
                    isDeleted = profileImageCheck.delete();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            Log.v(TAG, "File Deleted: " + isDeleted);*/
        }
    }
    private void loadProfileDetails (Integer delayTime){
        runnable = new Runnable() {
            @Override
            public void run() {
                if (profile != null) {
                    fb_welcome_text.setText(welcomeText);
                }
                if (profileImageCheck.exists()) {
                    flagImagesOnFirebase = true;
                    //imageProfileHLM.setImageBitmap(imageSaver.iLoadImageFromStorage(pathProfileImage,imageProfileFileName));
                } else {
                    Log.i(TAG, "No need to store Image Profile locally...");
                    //fireProfilePic();
                }
            }
        };
        handler.postDelayed(runnable, delayTime);
    }
    /*private void fireProfilePic (){
        if (user != null) {
            storageRef
                    .child(user.getUid())
                    .child("/profile_pic/" + imageProfileFileName)
                    .getBytes(Long.MAX_VALUE)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            // Use the bytes to display the image
                            ImageSaver saveBitmap = new ImageSaver();
                            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            //Bitmap image = saveBitmap.decodeSampledBitmap(bytes, reqWidth, reqHeight);
                            saveBitmap.iSaveToInternalStorage(image, imageProfileFileName, getContext());

                            flagImagesOnFirebase = true;
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    flagImagesOnFirebase = false;
                    exception.printStackTrace();
                }
            });
        }
    }*/
    private boolean deleteLocalProfilePic(){
        boolean isDeleted = !profileImageCheck.exists();
        if (!isDeleted) {
            try {
                isDeleted = profileImageCheck.delete();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.v(TAG, "Profile Image Deleted: " + isDeleted);
        return isDeleted;
    }

    private void getFacebookDetails (){

        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            gender = response.getJSONObject().get("gender").toString();
                            System.out.println("Gender: " + gender);
                            DatabaseReference databaseReference = database.getReference().child("users").child(user.getUid());
                            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            editor = sharedPreferences.edit();
                            editor.putString("gender", gender);
                            editor.putBoolean("visible_switch", sharedPreferences.getBoolean("visible_switch", true));
                            editor.putString("looking_for", sharedPreferences.getString("looking_for", "both"));
                            editor.putString("alias", sharedPreferences.getString("alias", user.getDisplayName()));
                            editor.apply();

                            databaseReference.child("preferences").child("gender").setValue(gender);
                            //databaseReference.child("my_chats").child("HotLikeMe").setValue("one");

                            Log.i(TAG, "We got the gender: " + gender);
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "gender");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
        try {
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        } catch (NullPointerException e){
            Log.e(TAG, "Failed to remove Callbacks");
            e.printStackTrace();
        }

        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();
    }

}
