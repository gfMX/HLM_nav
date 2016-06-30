package com.mezcaldev.hotlikeme;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.facebook.Profile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by developer on 17/06/16.
 */
public class imageSaver {

    private final String TAG = "Image record: ";

    public imageSaver() {

    }
    //Create Image as object
    private void iCreateBitmap (final Profile user, final String imageProfileFileName, final Context context){
        if (user != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL imgUrl = new URL("https://graph.facebook.com/"
                                + user.getId() + "/picture?type=large");
                        Log.d(TAG, "Image URL: " + imgUrl);
                        InputStream inputStream = (InputStream) imgUrl.getContent();
                        Bitmap pImage = BitmapFactory.decodeStream(inputStream);
                        if (pImage != null) {
                            //i_saveToInternalStorage(pImage, imageProfileFileName, context);
                        }
                        Log.v(TAG, "Everything Ok in here! We got the Image");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    //Save Image
    public String iSaveToInternalStorage(Bitmap bitmapImage, String imageName, Context context){
        File directory = new ContextWrapper(context).getDir("imageDir",
                Context.MODE_PRIVATE);
        File imPath=new File(directory,imageName);
        FileOutputStream fOut;

        try {
            fOut = new FileOutputStream(imPath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG,"Image found at: " + directory.getAbsolutePath());
        return directory.getAbsolutePath();
    }
    //Load Image
    public void iLoadImageFromStorage(View view, String path, String imageName) {
        try {
            File f=new File(path, imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
            ImageView img = (ImageView) view.findViewById(R.id.hlm_image);
            img.setImageBitmap(bitmap);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
    //Upload Image to Firebase
    public void iUploadFBImageToFirebase(String path, FirebaseUser user){
        FirebaseStorage  storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://hot-like-me.appspot.com");
        UploadTask uploadTask;

        Uri file = Uri.fromFile(new File(path));
        StorageReference upImageRef = storageRef.child(user.getUid() + "/profile_pic/" + file.getLastPathSegment());
        uploadTask = upImageRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                System.out.println("Upload is " + progress + "% done");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.v(TAG,"Image uploaded to Firebase");
                Log.v(TAG,"URL: " + downloadUrl);
            }
        });
    }
    public void iUploadImagesToFirebase(final List<String> path, final FirebaseUser user){
        for (int i = 0; i<path.size(); i++) {
            final int o = i;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String fileName = "image" + o + ".jpg";
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://hot-like-me.appspot.com");
                    StorageReference upImageRef = storageRef.child(user.getUid() + "/images/" + fileName);
                    UploadTask uploadTask;

                    //Uri file = Uri.fromFile(new File(path.get(0)));
                    try {
                        URL image = new URL(path.get(o));
                        Log.i(TAG, "New URL: " + image);
                        try {
                            InputStream inputStream = (InputStream) image.getContent();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                            byte[] data = byteArrayOutputStream.toByteArray();

                            //final InputStream stream = new FileInputStream(new File(file.toString()));
                            //uploadTask = upImageRef.putStream(stream);
                            uploadTask = upImageRef.putBytes(data);
                            // Listen for state changes, errors, and completion of the upload.
                            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                    System.out.println("Upload is " + progress + "% done");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (MalformedURLException mu) {
                        mu.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        Log.i(TAG, "All Images uploaded!");
    }
}
