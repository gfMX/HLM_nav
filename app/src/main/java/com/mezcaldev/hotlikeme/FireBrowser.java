package com.mezcaldev.hotlikeme;

import android.app.DialogFragment;
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

import java.util.ArrayList;
import java.util.List;

public class FireBrowser extends Fragment {

    //Facebook parameters
    private static final String TAG = "Image Browser: ";
    AccessToken accessToken;

    //Firebase//Initialize Firebase
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
    GridView gridView;
    static List<String> imUrls = new ArrayList<>();
    static List<String> imImages = new ArrayList<>();
    static List<String> imIds = new ArrayList<>();
    static List<Integer> imIdsSelected = new ArrayList<>();     //Actual Position
    static List<String> imUrlsSelected = new ArrayList<>();     //URL Image full resolution
    static List<String> imThumbSelected = new ArrayList<>();    //URL Image Thumbnail

    ImageAdapter imageAdapter;

    Boolean breakFlag = false;
    int finished = 0;
    MenuItem item;

    public FireBrowser() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        View view = inflater.inflate(R.layout.fragment_fire_browser, container, false);

        item = (MenuItem) view.findViewById(R.id.action_delete_image);

        return view;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstances){
        cleaningVars();

        imageAdapter = new ImageAdapter(getActivity(), imUrls, imIdsSelected);
        gridView = (GridView) view.findViewById(R.id.gridViewFire);
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

        getFirePhotos firePhotos = new getFirePhotos();
        firePhotos.execute();
    }

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
    private void imagesFromFire (DataSnapshot dataSnapshot){
        //Start changes
        DataSnapshot snapThumbs = dataSnapshot.child("thumbs");
        for (DataSnapshot data : snapThumbs.getChildren()) {
            thumbKeyList.add(data.getKey());
        }
        DataSnapshot snapImages = dataSnapshot.child("images");
        for (DataSnapshot data : snapImages.getChildren()) {
            imageKeyList.add(data.getKey());
        }

        uriFromFirebase(dataSnapshot, imageKeyList, thumbKeyList);
    }
    private void uriFromFirebase(final DataSnapshot dataSnapshot, final List<String> imageList, final List<String> thumbList){
        final int size = imageList.size();
        for (int i= 0; i < size; i++){
            imUrls.add("");
            imImages.add("");
        }

        for (int i = 0; i < size; i++) {
            finished = i;
            if (!breakFlag) {
                final int position = i;
                String imageKey = imageList.get(i);
                String thumbKey = thumbList.get(i);

                System.out.println("Key: " + imageKey);
                firebaseThumbStorage = dataSnapshot.child("thumbs").child(thumbKey).getValue().toString();
                firebaseImageStorage = dataSnapshot.child("images").child(imageKey).getValue().toString();
                System.out.println("Reference to an Image: " + storageRef.child(firebaseThumbStorage));
                storageRef.child(firebaseThumbStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imUrls.set(position, uri.toString());
                        imageAdapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.w(TAG, "Something went wrong getting the Thumbnail.");
                        exception.printStackTrace();
                    }
                });

                storageRef.child(firebaseImageStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imImages.set(position, uri.toString());
                        //photoSelectionFire(imUrls, imImages);
                        imageAdapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.w(TAG, "Something went wrong getting the Full Image.");
                        exception.printStackTrace();
                    }
                });
            }
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

        } else {
            keyOfImage.clear();
            keyOfThumb.clear();
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

    }

}
