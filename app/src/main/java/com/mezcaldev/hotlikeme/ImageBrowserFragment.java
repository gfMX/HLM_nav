package com.mezcaldev.hotlikeme;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class ImageBrowserFragment extends Fragment {

    private static final String TAG = "FacebookLogin";
    private AccessToken accessToken = AccessToken.getCurrentAccessToken();
    private String fieldsParams = "picture";
    private JSONArray nPhotosArray;
    private JSONArray nPhotosId;

    public ImageBrowserFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
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
            GraphRequest request = GraphRequest.newMeRequest(
                    accessToken,
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject jsonObject,
                                GraphResponse response) {
                            // Application code
                            Log.i(TAG, "Results (GraphResponse): " + response.toString()); //Query Results
                            Log.i(TAG, "Results (JSONObject): " + jsonObject.toString()); //Query Results

                            JSONArray photoArray = response.getJSONArray();

                            photoSelection(jsonObject);
                        }
                    });
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
        Log.i(TAG,"Object Length: " + photoObject.length());
        try {
            if (photoObject != null) {

                //Log.i(TAG,"Array Length: " + photoObject.length());
                Log.i(TAG, "Parsing ID: " + photoObject.get("id"));
                //Log.i(TAG, "Parsing Picture: " + photoObject.getJSONObject("picture"));
                //Log.i(TAG, "Parsing Data: " + photoObject.getJSONObject("picture").getJSONObject("data"));
                Log.i(TAG, "Parsing URL: " + photoObject.getJSONObject("picture").getJSONObject("data").get("url"));

                for (int i = 0; i < photoObject.length(); i++) {
                    //JSONObject object1 = pArray.getJSONObject(i);
                    //JSONObject object2 = (JSONObject) object1.get("picture");
                    //System.out.println("Object1: " + object1.toString());
                    //JSONArray  object3 = object2.getJSONArray("data");
                }
            } else {
                Log.i(TAG, "Nothing to do here!");
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

    }
}
