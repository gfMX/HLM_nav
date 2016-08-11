package com.mezcaldev.hotlikeme;

import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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


        return view;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstances){
        cleaningVars();

        imageAdapter = new ImageAdapter(getActivity(), imUrls, imIdsSelected);
        gridView = (GridView) view.findViewById(R.id.gridViewFace);
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

        getFbPhotos();
    }

    private void getFbPhotos(){
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

                imageAdapter.notifyDataSetChanged();

            } else {
                Log.w(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
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
        imageAdapter.clear();
    }

}
