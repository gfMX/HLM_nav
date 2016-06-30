package com.mezcaldev.hotlikeme;

import android.app.DialogFragment;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowserFragment extends Fragment {

    private static final String TAG = "FacebookLogin";
    private AccessToken accessToken;
    private String fieldsParams = "picture,images";
    private String limitParams = "120";

    private GridView gridView;
    static List<String> imUrls = new ArrayList<>();
    static List<String> imImages = new ArrayList<>();
    static List<String> imIds = new ArrayList<>();
    static List<Integer> imIdsSelected = new ArrayList<>();
    static List<String> imUrlsSelected = new ArrayList<>();

    public ImageBrowserFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());

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
        getFbPhotos fbPhotos = new getFbPhotos();
        fbPhotos.execute();
    }


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
                                //Log.i(TAG, "Results (GraphResponse): " + response.toString()); //Query Results

                                JSONObject photoOb = response.getJSONObject();
                                photoSelection(photoOb);
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
    public void photoSelection (JSONObject photoObject){
        try {
            if (photoObject != null) {
                //Cleaning Arrays before proceed
                imUrls.clear();
                imImages.clear();
                imIds.clear();
                imIdsSelected.clear();
                imUrlsSelected.clear();

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
                //Log.i(TAG, "Elements: " + imUrls);
                gridView = (GridView) getActivity().findViewById(R.id.gridView);
                gridView.setAdapter(new ImageAdapter(getActivity(), imUrls));
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id){

                        Uri imageUri = Uri.parse(imImages.get(position));
                        //Log.i(TAG, "Uri Obtained: " + imageUri.toString());
                        //showSelectedImage(imageUri);

                        if (!imIdsSelected.contains(position)) {
                            imIdsSelected.add(position);
                            imUrlsSelected.add(imImages.get(position));
                            v.setBackgroundColor(Color.GRAY);

                            Log.i(TAG,"Index: " + imIdsSelected.indexOf(position) + " URL: "
                                   + imUrlsSelected.get(imIdsSelected.indexOf(position)));
                        } else {
                            Log.i(TAG,"Index: " + imIdsSelected.indexOf(position) + " URL: "
                            + imUrlsSelected.get(imIdsSelected.indexOf(position)));

                            imUrlsSelected.remove(imIdsSelected.indexOf(position));
                            imIdsSelected.remove(imIdsSelected.indexOf(position));

                            v.setBackgroundColor(Color.TRANSPARENT);
                        }
                        //Log.i(TAG, "Images selected: " + imIdsSelected.toString());
                    }
                });
            } else {
                Log.i(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

    }
    private void showSelectedImage(Uri urImage){
        DialogFragment newFragment = imageSelected.newInstance(urImage);
        newFragment.show(getActivity().getFragmentManager(), "Image");
    }
}
