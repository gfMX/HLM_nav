package com.mezcaldev.hotlikeme;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowserFragment extends Fragment {

    //Facebook parameters
    private static final String TAG = "Image Browser: ";
    private AccessToken accessToken;
    String fieldsParams = "picture,images";
    String limitParams = "120";

    //Firebase//Initialize Firebase
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    FirebaseStorage storage;
    StorageReference storageRef;
    DatabaseReference fireRef;
    FirebaseAuth mAuth;
    String firebaseImageStorage1;
    String firebaseImageStorage2;

    //Internal parameters
    private GridView gridView;
    static List<String> imUrls = new ArrayList<>();
    static List<String> imImages = new ArrayList<>();
    static List<String> imIds = new ArrayList<>();
    static List<Integer> imIdsSelected = new ArrayList<>();     //Actual Position
    static List<String> imUrlsSelected = new ArrayList<>();     //URL Image full resolution
    static List<String> imThumbSelected = new ArrayList<>();    //URL Image Thumbnail

    getFbPhotos fbPhotos = new getFbPhotos();
    getFirePhotos firePhotos = new getFirePhotos();

    String browseImages;

    public ImageBrowserFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = MainActivityFragment.user;
        fireRef = database.getReference();

        accessToken = AccessToken.getCurrentAccessToken();
        Log.i(TAG, "AccessToken"+ accessToken.toString());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_browser, container, false);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            browseImages = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "Browsing: " + browseImages);
        } else {
            Log.i(TAG, "No Extras");
        }

        return view;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstances){
        Log.i(TAG, "Browse Images Current Value: " + browseImages);

        //Cleaning Arrays before proceed
        imUrls.clear();
        imImages.clear();
        imIds.clear();
        imIdsSelected.clear();
        imUrlsSelected.clear();
        imThumbSelected.clear();

        if(browseImages.equals("Facebook")) {
            Log.i(TAG, "Section for Facebook Image Browser");
            //getFbPhotos fbPhotos = new getFbPhotos();
            fbPhotos.execute();
        } else {
            Log.i(TAG, "Section for Firebase Browser");
            //final getFirePhotos firePhotos = new getFirePhotos();
            firePhotos.execute();
        }
    }

    //Functions to browse and upload Facebook Photos
    private class getFbPhotos extends AsyncTask <Void, Void, Void>{
        @Override
        protected void onPreExecute(){

        }
        @Override
        protected Void doInBackground(Void... params) {
            GraphRequest request = GraphRequest.newGraphPathRequest(
                    accessToken,
                    "/me/photos",
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            // Application code
                            JSONObject photoOb = response.getJSONObject();
                            photoSelectionFace(photoOb);
                        }
                    }
            );
            Bundle parameters = new Bundle();
            parameters.putString("fields", fieldsParams);
            parameters.putString("limit", limitParams);
            request.setParameters(parameters);
            request.executeAsync();

            return null;
        }
        @Override
        protected void onPostExecute(Void result){

        }
    }
    public void photoSelectionFace (JSONObject photoObject){
        try {
            if (photoObject != null) {
                JSONArray jsonArray1 = photoObject.getJSONArray("data");
                Log.i(TAG, "Data length: " + photoObject.getJSONArray("data").length());

                for (int i = 0; i < jsonArray1.length(); i++) {
                    JSONObject object1 = jsonArray1.getJSONObject(i);
                    JSONObject object2 = object1.getJSONArray("images").getJSONObject(0);

                    String sObject1 = object1.get("picture").toString();
                    String sObject3 = object2.get("source").toString();
                    String sObject2 = object1.get("id").toString();

                    imUrls.add(sObject1);
                    imImages.add(sObject3);
                    imIds.add(sObject2);

                    //Log.i(TAG, "Object: " + sObject1 + " Id: " + sObject2);
                    //Log.i(TAG, "URL Image: " + sObject3);
                    //Log.i(TAG, "New elements: " + imUrls.get(i));
                }

                gridView = (GridView) getActivity().findViewById(R.id.gridView);
                gridView.setAdapter(new ImageAdapter(getActivity(), imUrls));
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                        int itemPosition;

                        if (!imIdsSelected.contains(position)) {
                            imIdsSelected.add(position);

                            imUrlsSelected.add(imImages.get(position));
                            v.setBackgroundColor(Color.GRAY);
                            imThumbSelected.add(imUrls.get(position));

                            itemPosition = imIdsSelected.indexOf(position);
                            Log.i(TAG, "Index: " + itemPosition + " URL: "
                                    + imUrlsSelected.get(itemPosition)
                                    + " Thumb: " + imThumbSelected.get(itemPosition));
                        } else {
                            itemPosition = imIdsSelected.indexOf(position);

                            Log.i(TAG, "Index: " + itemPosition + " URL: "
                                    + imUrlsSelected.get(itemPosition)
                                    + " Thumb: " + imThumbSelected.get(itemPosition));

                            imUrlsSelected.remove(itemPosition);
                            imThumbSelected.remove(itemPosition);
                            imIdsSelected.remove(itemPosition);

                            v.setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                });

            } else {
                Log.w(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //Functions to Browse and select Firebase Photos
    private class getFirePhotos extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute (){

        }
        @Override
        protected Void doInBackground(Void... params) {

            String userId = firebaseUser.getUid();
            final DatabaseReference dbTotalImagesRef = database.getReference(userId + "/total_images");

            database.getReference(userId).addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            int nElements = (int) dataSnapshot.child("images").getChildrenCount();

                            Log.i(TAG, "Total Images: " + nElements);
                            dbTotalImagesRef.setValue(nElements);
                            uriFromFirebase((nElements - 1), dataSnapshot);
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "Cancelled: ",databaseError.toException());
                        }

                    }
            );

            return null;
        }
        @Override
        protected void onPostExecute(Void result){

        }
    }
    private void uriFromFirebase(int uriLength, final DataSnapshot dataSnapshot){
        final int i = uriLength;

        firebaseImageStorage1 = dataSnapshot.child("thumbs").child(String.valueOf(i)).getValue().toString();
        firebaseImageStorage2 = dataSnapshot.child("images").child(String.valueOf(i)).getValue().toString();

        //Get the thumbnails URLs from Firebase to show it on Image Browser:
        storageRef.child(firebaseImageStorage1).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.i(TAG, "Uri thumbnail " + i + ": " + uri);
                imUrls.add(uri.toString());

                //Get the Full Res Images URL from Firebase to show it on click and set it as Profile Pic
                storageRef.child(firebaseImageStorage2).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.i(TAG, "Uri Image: " + i + ": " + uri);
                        imImages.add(uri.toString());
                        photoSelectionFire();
                        if (i > 0){
                            uriFromFirebase((i-1), dataSnapshot);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Log.w(TAG, "Something went wrong getting the Full Image.");
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.w(TAG, "Something went wrong getting the Thumbnail.");
            }
        });
    }
    public void photoSelectionFire (){
        try {
            gridView = (GridView) getActivity().findViewById(R.id.gridView);
            gridView.setAdapter(new ImageAdapter(getActivity(), imUrls));
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    Uri imageUri = Uri.parse(imImages.get(position));
                    showSelectedImage(imageUri);
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
            firePhotos.cancel(true);
        }
    }
    private void showSelectedImage(Uri urImage){
        DialogFragment newFragment = imageSelected.newInstance(urImage);
        newFragment.show(getActivity().getFragmentManager(), "Image");
    }
}
