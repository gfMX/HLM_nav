package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.mezcaldev.hotlikeme.FireConnection.accessToken;
import static com.mezcaldev.hotlikeme.FireConnection.databaseGlobal;
import static com.mezcaldev.hotlikeme.FireConnection.user;

public class LoginFragment extends Fragment {

    static LoginFragment newInstance() {
        //LoginFragment newFragment = new LoginFragment();
        return new LoginFragment(); //newFragment;
    }

    private static final String TAG = "Login Details";
    //Delay Time to load Profile Picture if exists.
    Handler handler = new Handler();
    Runnable runnable;
    Integer minDelayTime = 100;
    Integer delayTime = 250;

    //Facebook
    LoginButton loginButton;
    CallbackManager callbackManager;
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
    private EditText editTextDisplayName;
    private TextView textViewDisplayName;
    private Button btn_image;
    Switch switchVisible;
    Switch switchNearby;
    SharedPreferences sharedPreferences;

    //Firebase
    static FirebaseAuth mAuth;
    static FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference fireRefAlias;
    DatabaseReference databaseReference;
    static FirebaseStorage storage;
    static StorageReference storageRef;
    Boolean flagImagesOnFirebase = false;
    File profileImageCheck;


    public LoginFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        //Path to save and load images locally:
        Context context = getContext();
        File pathP = (new ContextWrapper(context).getDir("imageDir", Context.MODE_PRIVATE));
        pathProfileImage = pathP.getAbsolutePath();

        //Initialize Firebase
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

        if (databaseGlobal == null)  {
            databaseGlobal = FirebaseDatabase.getInstance();

        }

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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

        switchVisible = (Switch) view.findViewById(R.id.switch_visible);
        switchNearby = (Switch) view.findViewById(R.id.switch_nearby);

        btn_image = (Button) view.findViewById(R.id.btn_choose_img);
        text_instruct = (TextView) view.findViewById(R.id.text_instruct);
        textViewDisplayName = (TextView) view.findViewById(R.id.text_display_name_helper);
        editTextDisplayName = (EditText) view.findViewById(R.id.text_display_name);
        if (user != null) {
            editTextDisplayName.setText(sharedPreferences.getString("alias", user.getDisplayName()));
        } else{
            editTextDisplayName.setText(null);
        }

        welcomeText = getResources().getString(R.string.text_welcome);
        instructionText = getResources().getString(R.string.text_start_HLM);
        signInText = getResources().getString(R.string.text_sign_in);

        loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "public_profile", "user_photos");
        loginButton.setFragment(this);

        //Switches
        switchVisible.setChecked(sharedPreferences.getBoolean("visible_switch", true));
        switchVisible.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (user != null) {
                    fireRefAlias = databaseGlobal.getReference().child("users").child(user.getUid()).child("preferences");
                    databaseReference = databaseGlobal.getReference();
                    sharedPreferences.edit().putBoolean("visible_switch", b).apply();
                    fireRefAlias.child("visible").setValue(b);

                    checkIfUserIsVisible();

                    FireConnection.getInstance().getFirebaseUsers(
                            sharedPreferences,
                            HLMActivity.mCurrentLocation)
                    ;

                }

            }
        });
        switchNearby.setChecked(sharedPreferences.getBoolean("gps_enabled", false));
        switchNearby.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (user != null) {
                    fireRefAlias = databaseGlobal.getReference().child("users").child(user.getUid()).child("preferences");
                    sharedPreferences.edit().putBoolean("gps_enabled", b).apply();
                    fireRefAlias.child("gps_enabled").setValue(b);

                    FireConnection.getInstance().getFirebaseUsers(
                            sharedPreferences,
                            HLMActivity.mCurrentLocation
                    );
                    Log.v(TAG, "Location from HLM: " + HLMActivity.mCurrentLocation);
                }
            }
        });

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                accessToken = loginResult.getAccessToken();
                // App code
                Log.d(TAG, "FB: onSuccess:" + loginResult);
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

        editTextDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String textDisplayName = charSequence.toString();
                Log.i(TAG, "Display name: " + textDisplayName);
                if (user != null){
                    fireRefAlias = databaseGlobal.getReference().child("users").child(user.getUid()).child("preferences");
                    sharedPreferences.edit().putString("alias", textDisplayName).apply();

                    fireRefAlias.child("alias").setValue(textDisplayName, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null){
                                Log.v(TAG, "User Name Changed: " + databaseReference);
                            } else {
                                Log.e(TAG, "Name couldn't be changed!" + databaseError);
                            }
                        }
                    });

                    //Update Profile on FireBase: Display Name
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(textDisplayName)
                            .build();
                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "User profile updated.");
                                    }
                                }
                            });

                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        btn_image.setTransformationMethod(null);
        btn_image.setOnClickListener(settingsButtons);

        //Image Profile Behavior
        imageProfileHLM.setOnClickListener(settingsButtons);

        updateUI();
    }

    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
      public void onClick (View v){
          switch (v.getId()) {

              case R.id.btn_choose_img:
                  Toast.makeText(getActivity(),
                          getResources().getString(R.string.text_choose_images),
                          Toast.LENGTH_LONG)
                          .show();

                  if (accessToken != null && !accessToken.isExpired()) {
                      startActivity(new Intent(getActivity(), ImageBrowser.class));
                  } else {
                      Toast.makeText(getActivity(), getResources().getString(R.string.text_log_into_facebook_again), Toast.LENGTH_LONG).show();
                  }
                  break;

              case R.id.hlm_image:
                  if (flagImagesOnFirebase) {
                      Toast.makeText(getActivity(), getResources().getString(R.string.text_hlm_change_profile_pic),
                              Toast.LENGTH_LONG).show();
                      startActivity(new Intent(getActivity(), FireBrowserActivity.class));
                  } else {
                      Toast.makeText(getActivity(), getResources().getString(R.string.text_first_select_images),
                              Toast.LENGTH_LONG).show();
                      if (accessToken != null && !accessToken.isExpired()) {
                          startActivity(new Intent(getActivity(), ImageBrowser.class));
                      }
                  }
                  break;
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

                    accessToken = currentAccessToken;

                    if (currentAccessToken == null){
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.text_see_you_soon),
                                Toast.LENGTH_SHORT).show();

                        FirebaseAuth.getInstance().signOut();
                        if (sharedPreferences != null) {
                            sharedPreferences.edit().clear().apply();
                            Log.v(TAG, "Shared Preferences Cleared!");
                        }

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

                        if (accessToken != null) {
                            loadProfileDetails(delayTime);
                        }
                    } else {
                        // User signed out
                        Log.d(TAG, "Firebase: Signed Out");
                        System.out.println("Singleton user: " + FireConnection.getInstance().getUser());
                    }
                    FireConnection.getInstance().getUser();
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
            editTextDisplayName.setVisibility(View.VISIBLE);
            textViewDisplayName.setVisibility(View.VISIBLE);
            text_instruct.setText(instructionText);
            profilePic.setVisibility(View.VISIBLE);
            if (accessToken !=null) {
                profilePic.setProfileId(accessToken.getUserId());
            }
            imageProfileHLM.setClickable(true);
            if (user.getPhotoUrl() != null && getContext() != null) {
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
            switchVisible.setVisibility(View.VISIBLE);
            switchNearby.setVisibility(View.VISIBLE);

        } else {
            profilePic.setProfileId(null);
            profilePic.setVisibility(View.INVISIBLE);
            imageProfileHLM.setImageResource(R.drawable.ic_account_circle_gray_64dp);
            imageProfileHLM.setClickable(false);
            fb_welcome_text.setText(signInText);
            editTextDisplayName.setVisibility(View.INVISIBLE);
            textViewDisplayName.setVisibility(View.INVISIBLE);
            text_instruct.setText(null);
            btn_image.setVisibility(View.GONE);
            switchVisible.setVisibility(View.GONE);
            switchNearby.setVisibility(View.GONE);

            deleteLocalProfilePic();
        }
    }
    private void loadProfileDetails (Integer delayTime){
        runnable = new Runnable() {
            @Override
            public void run() {
                if (profile != null) {
                    fb_welcome_text.setText(welcomeText);
                }
            }
        };
        handler.postDelayed(runnable, delayTime);
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
                            DatabaseReference databaseReference = databaseGlobal.getReference().child("users").child(user.getUid());
                            sharedPreferences.edit()
                                    .putString("gender", gender)
                                    .putBoolean("visible_switch", sharedPreferences.getBoolean("visible_switch", true))
                                    .putString("looking_for", sharedPreferences.getString("looking_for", "both"))
                                    .putString("alias", sharedPreferences.getString("alias", user.getDisplayName()))
                                    .apply();

                            //checkIfUserIsVisible();

                            databaseReference.child("preferences").child("gender").setValue(gender);
                            databaseReference.child("preferences").child("gender").setValue(sharedPreferences.getString("alias", user.getDisplayName()));
                            databaseReference.child("preferences").child("gender").setValue(sharedPreferences.getBoolean("visible_switch", true));
                            databaseReference.child("preferences").child("alias").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot != null) {
                                        editTextDisplayName.setText(dataSnapshot.getValue().toString());
                                        sharedPreferences.edit().putString("alias", sharedPreferences.getString("alias", dataSnapshot.getValue().toString())).apply();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

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

    private void checkIfUserIsVisible (){
        boolean visible = sharedPreferences.getBoolean("visible_switch", true);
        String thisGender = sharedPreferences.getString("gender", null);
        if (user != null){
            if (visible && thisGender != null) {
                Log.v(TAG, "User Visible");
                databaseReference.child("groups").child(thisGender).child(user.getUid()).setValue(true);
                databaseReference.child("groups").child("both").child(user.getUid()).setValue(true);
            } else if (thisGender != null) {
                Log.v(TAG, "User Not Visible");
                databaseReference.child("groups").child(thisGender).child(user.getUid()).setValue(null);
                databaseReference.child("groups").child("both").child(user.getUid()).setValue(null);
            } else{
                Log.e(TAG, "Something went Wrong!");
            }
        }
    }

    private boolean deleteLocalProfilePic(){
        boolean isDeleted = !profileImageCheck.exists();
        if (!isDeleted) {
            try {
                isDeleted = profileImageCheck.delete();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.v(TAG, "Local Profile Image Deleted: " + isDeleted);
        return isDeleted;
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
        updateUI();
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
