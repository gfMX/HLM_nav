package com.mezcaldev.hotlikeme;

import android.os.AsyncTask;
import android.os.Bundle;
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
    private JSONObject jsonPhotos;

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
                            Log.i(TAG, "Results: " + jsonObject.toString());
                            jsonPhotos = jsonObject;
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "picture");
            request.setParameters(parameters);
            request.executeAsync();

            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            photoSelection(jsonPhotos);
        }

    }
    public void photoSelection (JSONObject photosObject){
        try {
            JSONArray jsonPhotoId = photosObject.getJSONArray("id");
            JSONArray jsonPhotoUrl = photosObject.getJSONArray("url");
        } catch (JSONException e){
            e.printStackTrace();
        }

    }
}
