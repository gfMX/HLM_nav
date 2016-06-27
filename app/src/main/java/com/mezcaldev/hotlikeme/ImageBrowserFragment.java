package com.mezcaldev.hotlikeme;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * A placeholder fragment containing a simple view.
 */
public class ImageBrowserFragment extends Fragment {

    private static final String TAG = "FacebookLogin";
    private AccessToken accessToken;
    private String fieldsParams = "picture";

    private GridView gridView;
    static List<String> imUrls = new ArrayList<>();
    static String[] imIds;

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
                                Log.i(TAG, "Results (GraphResponse): " + response.toString()); //Query Results

                                JSONObject photoOb = response.getJSONObject();
                                photoSelection(photoOb);
                        }
                    }
            );
            Bundle parameters = new Bundle();
            parameters.putString("fields", fieldsParams);
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
                JSONArray jsonArray1 = photoObject.getJSONArray("data");
                Log.i(TAG, "Data length: " + photoObject.getJSONArray("data").length());

                for (int i = 0; i < jsonArray1.length(); i++) {
                    JSONObject object1 = jsonArray1.getJSONObject(i);
                    String object2 = object1.get("picture").toString();
                    String object3 = object1.get("id").toString();
                    imUrls.add(object2);
                    //imIds[i] = object3;

                    //Log.i(TAG,"Object: " + object2 + " Id: " + object3);
                    //Log.i(TAG, "New elements: " + imUrls.get(i));
                }
                //Log.i(TAG, "Elements: " + imUrls);
                gridView = (GridView) getActivity().findViewById(R.id.gridView);
                gridView.setAdapter(new ImageAdapter(getActivity(), imUrls));
            } else {
                Log.i(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

    }
}
