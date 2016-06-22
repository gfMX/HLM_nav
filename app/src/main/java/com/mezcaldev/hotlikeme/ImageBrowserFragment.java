package com.mezcaldev.hotlikeme;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.FacebookSdk;

/**
 * A placeholder fragment containing a simple view.
 */
public class ImageBrowserFragment extends Fragment {

    private static final String TAG = "FacebookLogin";

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

    }
}
