package com.mezcaldev.hotlikeme;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
    Thread fireUris;
    FirebaseUser firebaseUser;
    FirebaseDatabase database;
    FirebaseStorage storage;
    StorageReference storageRef;
    DatabaseReference fireRef;
    FirebaseAuth mAuth;
    String firebaseThumbStorage;
    String firebaseImageStorage;
    List<String> imageKeyList = new ArrayList<>();
    List<String> thumbKeyList = new ArrayList<>();
    static List<String> deleteListImages = new ArrayList<>();
    static List<String> deleteListThumbs = new ArrayList<>();
    static List<String> keyOfImage = new ArrayList<>();
    static List<String> keyOfThumb = new ArrayList<>();

    //Internal parameters
    private GridView gridView;
    static List<String> imUrls = new ArrayList<>();
    static List<String> imImages = new ArrayList<>();
    static List<String> imIds = new ArrayList<>();
    static List<Integer> imIdsSelected = new ArrayList<>();     //Actual Position
    static List<String> imUrlsSelected = new ArrayList<>();     //URL Image full resolution
    static List<String> imThumbSelected = new ArrayList<>();    //URL Image Thumbnail

    //List<String> refImages = new ArrayList<>();
    //List<String> refThumbs = new ArrayList<>();

    getFbPhotos fbPhotos = new getFbPhotos();
    getFirePhotos firePhotos = new getFirePhotos();

    Boolean breakFlag = false;

    MenuItem item;
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

        item = (MenuItem) view.findViewById(R.id.action_delete_image);

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

        cleaningVars();

        if(browseImages.equals("Facebook")) {
            Log.i(TAG, "Section for Facebook Image Browser");
            //getFbPhotos fbPhotos = new getFbPhotos();
            fbPhotos.execute();
        } else if (browseImages.equals("Firebase")){
            Log.i(TAG, "Section for Firebase Browser");
            //final getFirePhotos firePhotos = new getFirePhotos();
            firePhotos.execute();
        }
    }

    //--------------------------------------------------------------------------------------------
    //Functions to browse and upload Facebook Photos
    //--------------------------------------------------------------------------------------------
    private class getFbPhotos extends AsyncTask <Void, Void, Void>{
        @Override
        protected void onPreExecute(){
            cleaningVars();
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
                }

                final ImageAdapter imageAdapter = new ImageAdapter(getActivity(), imUrls, imIdsSelected);
                gridView = (GridView) getActivity().findViewById(R.id.gridView);
                gridView.setAdapter(imageAdapter);
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        if (!imIdsSelected.contains(position)) {
                            imIdsSelected.add(position);
                            imUrlsSelected.add(imImages.get(position));
                            imThumbSelected.add(imUrls.get(position));
                        } else {
                            imUrlsSelected.remove(imIdsSelected.indexOf(position));
                            imThumbSelected.remove(imIdsSelected.indexOf(position));
                            imIdsSelected.remove(imIdsSelected.indexOf(position));
                        }
                        imageAdapter.notifyDataSetChanged();
                    }
                });


            } else {
                Log.w(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //--------------------------------------------------------------------------------------------
    //Functions to Browse and select Firebase Photos
    //--------------------------------------------------------------------------------------------
    private class getFirePhotos extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute (){
            cleaningVars();
        }
        @Override
        protected Void doInBackground(Void... params) {

            String userId = firebaseUser.getUid();
            final DatabaseReference dbTotalImagesRef =
                    database.getReference().child("users").child(userId).child("/total_images");

            database.getReference().child("users").child(userId).addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            int nElements = (int) dataSnapshot.child("images").getChildrenCount();

                            Log.i(TAG, "Total Images: " + nElements);
                            dbTotalImagesRef.setValue(nElements);
                            imagesFromFire(dataSnapshot);
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
    private void imagesFromFire (final DataSnapshot dataSnapshot){
        DataSnapshot snapImages = dataSnapshot.child("images");
        for (DataSnapshot data: snapImages.getChildren()) {
            imageKeyList.add(data.getKey());
        }
        DataSnapshot snapThumbs = dataSnapshot.child("thumbs");
        for (DataSnapshot data: snapThumbs.getChildren()) {
            thumbKeyList.add(data.getKey());
        }
        uriFromFirebase(0, dataSnapshot, imageKeyList, thumbKeyList);
    }
    private void uriFromFirebase(final int iteration, final DataSnapshot dataSnapshot, final List<String> imageList, final List<String> thumbList){

        final int size = imageList.size();
        fireUris = new Thread(new Runnable() {
            @Override
            public void run() {

                String imageKey = imageList.get(iteration);
                String thumbKey = thumbList.get(iteration);

                System.out.println("Key: " + imageKey);
                firebaseThumbStorage = dataSnapshot.child("thumbs").child(thumbKey).getValue().toString();
                firebaseImageStorage = dataSnapshot.child("images").child(imageKey).getValue().toString();
                System.out.println("Reference to an Image: " + storageRef.child(firebaseThumbStorage));

                    storageRef.child(firebaseThumbStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            imUrls.add(uri.toString());
                            //photoSelectionFire();

                            //Get the Full Images URLs from Firebase to show it on Image Browser:
                            storageRef.child(firebaseImageStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    imImages.add(uri.toString());
                                    photoSelectionFire();
                                    if (iteration < (size -1)){
                                        uriFromFirebase((iteration + 1),dataSnapshot,imageList, thumbList);
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Log.w(TAG, "Something went wrong getting the Full Image.");
                                    exception.printStackTrace();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.w(TAG, "Something went wrong getting the Thumbnail.");
                            exception.printStackTrace();
                        }
                    });
            }
        });
        fireUris.start();
    }
    public void photoSelectionFire (){
        try {
            if (!breakFlag) {
                final ImageAdapter imageAdapter = new ImageAdapter(getActivity(), imUrls, imIdsSelected);
                gridView = (GridView) getActivity().findViewById(R.id.gridView);
                gridView.setAdapter(imageAdapter);

                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        Uri imageUri = Uri.parse(imImages.get(position));
                        showSelectedImage(imageUri);
                    }
                });

                gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        //System.out.println("Key List1: " + imageKeyList);
                        //System.out.println("Key List2: " + thumbKeyList);

                        if (!deleteListImages.contains(imageKeyList.get(position)) && !imIdsSelected.contains(position)) {
                            deleteListImages.add(imageKeyList.get(position));
                            deleteListThumbs.add(thumbKeyList.get(position));
                            imIdsSelected.add(position);
                        } else {
                            deleteListImages.remove(imIdsSelected.indexOf(position));
                            deleteListThumbs.remove(imIdsSelected.indexOf(position));
                            imIdsSelected.remove(imIdsSelected.indexOf(position));
                        }

                        getValuesOfKeys(deleteListImages, deleteListThumbs);
                        imageAdapter.notifyDataSetChanged();

                        return true;
                    }
                });
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            breakFlag = true;
            firePhotos.cancel(true);
        }
    }
    private  void  getValuesOfKeys (final List <String> listImages, final List<String> listThumbs){
        if (listImages.size() > 0 && listThumbs.size() > 0){
            System.out.println("Images Keys: " + listImages);
            System.out.println("Thumbs Keys: " + listThumbs);

            keyOfImage.clear();
            keyOfThumb.clear();

            //Method for getting the values of the Images to delete:
            DatabaseReference databaseReferenceImages = database.getReference()
                    .child("users")
                    .child(firebaseUser.getUid())
                    .child("images");
            databaseReferenceImages.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (int a = 0; a < listImages.size(); a++){
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            if (listImages.get(a).equals(data.getKey())) {
                                System.out.println("Image found!: " + data.getValue());
                                if (!keyOfImage.contains(data.getValue().toString())) {
                                    keyOfImage.add(data.getValue().toString());
                                }
                            }

                        }
                    }

                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //Method for getting the values of the Thumbs to delete:
            DatabaseReference databaseReferenceThumbs = database.getReference()
                    .child("users")
                    .child(firebaseUser.getUid())
                    .child("thumbs");
            databaseReferenceThumbs.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (int a = 0; a < listThumbs.size(); a++){
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            if (listThumbs.get(a).equals(data.getKey())) {
                                System.out.println("Thumb found!: " + data.getValue());
                                if (!keyOfThumb.contains(data.getValue().toString())) {
                                    keyOfThumb.add(data.getValue().toString());
                                }
                            }

                        }
                    }

                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    private void showSelectedImage(Uri urImage){
        DialogFragment newFragment = imageSelected.newInstance(urImage);
        newFragment.show(getActivity().getFragmentManager(), "Image");
    }

    //General Functions:
    private void cleaningVars () {
        //Cleaning Arrays before proceed
        imUrls.clear();
        imImages.clear();
        imIds.clear();
        imIdsSelected.clear();
        imUrlsSelected.clear();
        imThumbSelected.clear();
        deleteListImages.clear();
        deleteListThumbs.clear();
        keyOfImage.clear();
        keyOfThumb.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fireUris.isAlive()){
            fireUris.interrupt();
        }
        cleaningVars();
    }

}
