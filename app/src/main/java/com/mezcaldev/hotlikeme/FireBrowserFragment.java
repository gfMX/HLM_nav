package com.mezcaldev.hotlikeme;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class FireBrowserFragment extends Fragment {

    String pathImages = "/images/";
    String pathThumbs = "/thumbs/";

    //Facebook parameters
    private static final String TAG = "Image Browser: ";
    AccessToken accessToken;

    //Firebase//Initialize Firebase
    FirebaseUser firebaseUser = FireConnection.getInstance().getUser();
    FirebaseDatabase database;
    FirebaseStorage storage;
    StorageReference storageRef;
    DatabaseReference databaseReference;
    DatabaseReference databaseReferenceImages;
    DatabaseReference databaseReferenceThumbs;
    String firebaseThumbStorage;
    String firebaseImageStorage;
    List<String> imageKeyList = new ArrayList<>();
    static List<String> deleteListImages = new ArrayList<>();
    static List<String> deleteListThumbs = new ArrayList<>();
    static List<String> keyOfImage = new ArrayList<>();
    static List<String> keyOfThumb = new ArrayList<>();
    ValueEventListener valueEventListener;
    ValueEventListener valueEventListenerThumbs;
    ValueEventListener valueEventListenerImages;

    //Internal parameters
    GridView gridView;
    static List<String> imThumbs = new ArrayList<>();
    static List<String> imImages = new ArrayList<>();
    static List<Integer> imIdsSelected = new ArrayList<>();     //Actual Position

    ImageAdapter imageAdapter;
    MenuItem trashCan;

    public FireBrowserFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

        accessToken = AccessToken.getCurrentAccessToken();
        Log.i(TAG, "AccessToken"+ accessToken.toString());
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_fire_fragment, menu);
        trashCan = menu.findItem(R.id.action_delete_image);
        trashCan.setVisible(false);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_delete_image){
            Log.i(TAG, "Delete");
            System.out.println("Keys: " + deleteListImages);
            System.out.println("Images: " + keyOfImage);
            System.out.println("Thumbs: " + keyOfThumb);


            if (keyOfImage.size()>0){
                Integer numberOfImages = keyOfImage.size();
                String deleteText =
                        getResources().getString(R.string.text_deleting_selected_images_1) +
                                numberOfImages.toString() +
                                getResources().getString(R.string.text_deleting_selected_images_2);

                Snackbar.make(getActivity().getWindow().getDecorView(),
                        deleteText,
                        Snackbar.LENGTH_LONG)
                        .setAction("DELETE", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DeleteImagesOnFire(keyOfImage, keyOfThumb, deleteListImages, 0);
                                Snackbar.make(getActivity().getWindow().getDecorView(),
                                        getResources().getString(R.string.text_deleting_images),
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Action", null)
                                        .show();
                            }
                        })
                        .show();
            } else {
                Snackbar.make(getActivity().getWindow().getDecorView(),
                        getResources().getString(R.string.text_delete_images_no_selected),
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fire_browser, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstances){
        cleaningVars();

        imageAdapter = new ImageAdapter(getActivity(), imThumbs, imIdsSelected);
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
                    imIdsSelected.add(position);
                } else {
                    deleteListImages.remove(imIdsSelected.indexOf(position));
                    imIdsSelected.remove(imIdsSelected.indexOf(position));
                }
                if (imIdsSelected.size() > 0){
                    trashCan.setVisible(true);
                } else{
                    trashCan.setVisible(false);
                }

                getValuesOfKeys(deleteListImages);
                imageAdapter.notifyDataSetChanged();

                return true;
            }
        });

        getFirePhotos();
    }

    private void getFirePhotos() {
        String userId = firebaseUser.getUid();
        databaseReference = database.getReference().child("users").child(userId);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int nElements = (int) dataSnapshot.child("images").getChildrenCount();
                Log.i(TAG, "Total Images: " + nElements);
                databaseReference.child("/total_images").setValue(nElements);
                imagesFromFire(dataSnapshot);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Cancelled: ", databaseError.toException());
            }
        };

        databaseReference.addListenerForSingleValueEvent(valueEventListener);
    }

    private void imagesFromFire (DataSnapshot dataSnapshot){
        DataSnapshot snapImages = dataSnapshot.child("images");
        for (DataSnapshot data : snapImages.getChildren()) {
            imageKeyList.add(data.getKey());
            imImages.add("");
            imThumbs.add("");
        }

        uriFromFirebase(dataSnapshot, imageKeyList);
    }
    private void uriFromFirebase(final DataSnapshot dataSnapshot, final List<String> imageList){
        final int size = imageList.size();

        for (int i = 0; i < size; i++) {
            final int position = i;
            String imageKey = imageList.get(i);

            System.out.println("Key: " + imageKey);

            firebaseThumbStorage = dataSnapshot.child("thumbs").child(imageKey).getValue().toString();
            storageRef.child(firebaseThumbStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    if(imThumbs.size() == size) {
                        imThumbs.set(position, uri.toString());
                        imageAdapter.notifyDataSetChanged();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.w(TAG, "Something went wrong getting the Thumbnail.");
                    exception.printStackTrace();
                }
            });

            firebaseImageStorage = dataSnapshot.child("images").child(imageKey).getValue().toString();
            storageRef.child(firebaseImageStorage).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    if(imThumbs.size() == size) {
                        imImages.set(position, uri.toString());
                        imageAdapter.notifyDataSetChanged();
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
    }
    private  void  getValuesOfKeys (final List <String> listImages){
        if (listImages.size() > 0 ){
            System.out.println("Images Keys: " + listImages);

            keyOfImage.clear();
            keyOfThumb.clear();

            valueEventListenerImages = new ValueEventListener() {
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
            };
            valueEventListenerThumbs = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (int a = 0; a < listImages.size(); a++){
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            if (listImages.get(a).equals(data.getKey())) {
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
            };

            //Method for getting the values of the Images and Thumbs to delete:
            databaseReferenceImages = database.getReference()
                    .child("users")
                    .child(firebaseUser.getUid())
                    .child("images");
            databaseReferenceThumbs = database.getReference()
                    .child("users")
                    .child(firebaseUser.getUid())
                    .child("thumbs");

            databaseReferenceImages.addValueEventListener(valueEventListenerImages);
            databaseReferenceThumbs.addValueEventListener(valueEventListenerThumbs);

        } else {
            keyOfImage.clear();
            keyOfThumb.clear();
        }
    }

    public void DeleteImagesOnFire (final List <String> imagesToDelete, final  List<String> thumbsToDelete, final List<String> keys, final int imageNumber) {
        final DatabaseReference imageReference = database.getReference().child("users").child(firebaseUser.getUid()).child(pathImages).child(keys.get(imageNumber));
        final DatabaseReference thumbReference = database.getReference().child("users").child(firebaseUser.getUid()).child(pathThumbs).child(keys.get(imageNumber));

        System.out.println("Keys to delete: " + keys);
        System.out.println("Images to delete: " + imagesToDelete);
        System.out.println("Thumbs to delete: " + thumbsToDelete);


        StorageReference deletetRefImage = storageRef
                .child(imagesToDelete.get(imageNumber));
        final StorageReference deletetRefThumb = storageRef
                .child(thumbsToDelete.get(imageNumber));

        deletetRefImage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                deletetRefThumb.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        imageReference.setValue(null);
                        thumbReference.setValue(null);
                        if (imageKeyList.contains(keys.get(imageNumber))){
                            int currentIndex = imageKeyList.indexOf(keys.get(imageNumber));

                            System.out.println("Key to remove: " + keys.get(imageNumber));
                            System.out.println("Key removed:" + imageKeyList.get(currentIndex));
                            imageKeyList.remove(currentIndex);
                            imImages.remove(currentIndex);
                            imThumbs.remove(currentIndex);

                            imageAdapter.notifyDataSetChanged();
                        }
                        if (imageNumber < imagesToDelete.size()-1) {
                            DeleteImagesOnFire(imagesToDelete, thumbsToDelete, keys, (imageNumber + 1));
                        } else {
                            Toast.makeText(getContext(), "Images Successfully deleted!", Toast.LENGTH_SHORT).show();
                            trashCan.setVisible(false);
                            cleanDeleteVars();
                            imageAdapter.notifyDataSetChanged();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Uh-oh, an error occurred!
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });
    }

    private void showSelectedImage(Uri urImage){
        DialogFragment newFragment = imageSelected.newInstance(urImage);
        newFragment.show(getActivity().getFragmentManager(), "Image");
    }

    //General Functions:
    private void cleaningVars () {
        //Cleaning Arrays before proceed
        imThumbs.clear();
        imImages.clear();
        cleanDeleteVars();
    }
    private void cleanDeleteVars () {
        imIdsSelected.clear();
        deleteListImages.clear();
        deleteListThumbs.clear();
        keyOfImage.clear();
        keyOfThumb.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseReference.removeEventListener(valueEventListener);
        imageAdapter.clear();
    }

}
