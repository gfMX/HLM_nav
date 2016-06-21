package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "FacebookLogin";

    //Facebook
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private AccessToken accessToken;
    private AccessTokenTracker accessTokenTracker;
    private Profile profile;
    private ProfileTracker profileTracker;
    private String profilePicUrl;

    //UI Elements
    private int flag1 = 0;
    private String imageProfileFileName;
    private String pathProfileImage;
    private Bitmap pImage;
    private Bitmap pImageShow;
    private ProfilePictureView profilePic;
    private ImageView imageProfileHLM;
    private TextView fb_welcome_text;
    private TextView text_instruct;
    private Button btn_image;
    private Button btn_settings;
    private Button btn_start;

    //Firebase
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase database;
    private DatabaseReference fireRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private UploadTask uploadTask;

    //Other elements
    private ContextWrapper contextWrapper;
    private File profileImageCheck;

    public MainActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

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

        //Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://hot-like-me.appspot.com");

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
                    fireRef = database.getReference(user.getUid() + "/photo");
                    fireRef.setValue(user.getPhotoUrl());
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

        imageProfileFileName = "profile_im.jpg";
        pathProfileImage = "/data/data/com.mezcaldev.hotlikeme/app_imageDir/";
        profileImageCheck = new File(pathProfileImage + imageProfileFileName);

        //View references for UI elements
        fb_welcome_text = (TextView) view.findViewById(R.id.fb_textWelcome);
        profilePic = (ProfilePictureView) view.findViewById(R.id.fb_image);
        imageProfileHLM = (ImageView) view.findViewById(R.id.hlm_image);

        btn_image = (Button) view.findViewById(R.id.btn_choose_img);
        btn_start = (Button) view.findViewById(R.id.btn_start);
        btn_settings = (Button) view.findViewById(R.id.btn_settings);
        text_instruct = (TextView) view.findViewById(R.id.text_instruct);

        btn_image.setTransformationMethod(null);
        btn_start.setTransformationMethod(null);
        btn_settings.setTransformationMethod(null);

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
                if (profileImageCheck.exists() == false) {
                    createBitmap(profile, accessToken);
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
        btn_settings.setOnClickListener(settingsButtons);
        btn_start.setOnClickListener(settingsButtons);

    }
    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
      public void onClick (View v){
          switch (v.getId()){
              case R.id.btn_choose_img:
                  Toast.makeText(getActivity(), "Almost there!", Toast.LENGTH_LONG).show();
                  //startActivity(new Intent(getActivity(), HomeActivity.class));
                  break;
              case  R.id.btn_settings:
                  startActivity(new Intent(getActivity(), SettingsActivity.class));
                  break;
              case R.id.btn_start:
                  //startActivity(new Intent(getActivity(), HomeActivity.class));
                  break;
          }
      }
    };
    
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


    // Sign out
    public void signOut(){
        mAuth.signOut();
        LoginManager.getInstance().logOut();

        //updateUI(null);
    }
    //UI Status Updater
    private void updateUI (AccessToken accessToken) {
        //Update UI Elements according to the Given Token
        if (accessToken != null){
            if (profile != null) {
                fb_welcome_text.setText("Welcome " + profile.getFirstName() + "!");
                /*if (flag1 == 0) {
                    createBitmap(profile, accessToken);
                    flag1 ++;
                }*/
            } else {
                fb_welcome_text.setText("Welcome!");
            }
            loadImageFromStorage(getView(), pathProfileImage, imageProfileFileName);
            imageProfileHLM.setVisibility(View.VISIBLE);
            text_instruct.setText("Please choose your Hot Like Me image. This image will be used " +
                    "as a display image for the App, and will be the Image which other users will see. " +
                    "By default HLM take your FB profile Picture.");
            profilePic.setProfileId(accessToken.getUserId());
            btn_image.setVisibility(View.VISIBLE);
            btn_settings.setVisibility(View.GONE);
            btn_start.setVisibility(View.VISIBLE);
        } else {
            profilePic.setProfileId(null);
            imageProfileHLM.setVisibility(View.GONE);
            imageProfileHLM.setImageResource(R.drawable.no_user);
            fb_welcome_text.setText("Welcome to Hot Like Me \n Please Log In");
            text_instruct.setText("");
            btn_image.setVisibility(View.INVISIBLE);
            btn_settings.setVisibility(View.GONE);
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
    //Create Image as object
    private void createBitmap (final Profile user, final AccessToken accessToken){
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback(){
                    @Override
                    public void onCompleted(JSONObject us2, GraphResponse response){
                        if (user != null) {
                            Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            URL imgUrl = new URL("https://graph.facebook.com/"
                                                    + user.getId() + "/picture?type=large");
                                            Log.d(TAG, "Image URL: " + imgUrl);
                                            InputStream inputStream = (InputStream) imgUrl.getContent();
                                            pImage = BitmapFactory.decodeStream(inputStream);
                                            if (pImage != null) {
                                                saveToInternalStorage(pImage, imageProfileFileName);
                                            }
                                            Log.v(TAG, "Everything Ok in here! We got the Image");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            thread.start();

                        }
                    }
        });
        request.executeAsync();
    }

    //Save Image
    private String saveToInternalStorage(Bitmap bitmapImage, String imageName){
        File directory = new ContextWrapper(getActivity().getApplicationContext()).getDir("imageDir",
                Context.MODE_PRIVATE);
        File imPath=new File(directory,imageName);
        FileOutputStream fOut;

        try {
            fOut = new FileOutputStream(imPath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG,"Image found at: " + directory.getAbsolutePath());
        return directory.getAbsolutePath();
    }
    //Load Image
    private void loadImageFromStorage(View view, String path, String imageName) {
        try {
            File f=new File(path, imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
            ImageView img = (ImageView) view.findViewById(R.id.hlm_image);
            img.setImageBitmap(bitmap);
            uploadFBImageToFirebase(path + imageName);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    //Upload Image to Firebase
    private void uploadFBImageToFirebase(String path){
        Uri file = Uri.fromFile(new File(path));
        StorageReference upImageRef = storageRef.child(user.getUid() + "/images/" + file.getLastPathSegment());
        uploadTask = upImageRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.v(TAG,"Image uploaded to Firebase");
                Log.v(TAG,"URL: " + downloadUrl);
            }
        });
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
