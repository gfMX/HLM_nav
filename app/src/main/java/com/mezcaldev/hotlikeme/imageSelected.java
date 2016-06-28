package com.mezcaldev.hotlikeme;


import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

public class imageSelected extends DialogFragment {

    static Uri uriImage;

    private Button btn_ok;
    private Button btn_cancel;

    static imageSelected newInstance(Uri uri) {
        uriImage = uri;
        return new imageSelected();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_selected, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fb_prof_image);

        getDialog().setTitle("Selected Image");

        btn_ok = (Button) view.findViewById(R.id.btn_ok_image);
        btn_cancel = (Button) view.findViewById(R.id.btn_cancel_image);

        Glide
                .with(getActivity())
                .load(uriImage)
                .into(imageView);

        btn_ok.setOnClickListener(settingsButtons);
        btn_cancel.setOnClickListener(settingsButtons);

        return view;
    }

    //Buttons for different settings
    private View.OnClickListener settingsButtons = new View.OnClickListener(){
        public void onClick (View v){
            switch (v.getId()){
                case R.id.btn_ok_image:
                    Toast.makeText(getActivity(), "Ok",
                            Toast.LENGTH_LONG).show();

                    break;
                case  R.id.btn_cancel_image:
                    Toast.makeText(getActivity(), "Bla, bla bla...",
                            Toast.LENGTH_LONG).show();
                    getDialog().dismiss();
                    break;
            }
        }
    };

}
