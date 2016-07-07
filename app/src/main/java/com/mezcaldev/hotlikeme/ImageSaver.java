package com.mezcaldev.hotlikeme;

import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.facebook.Profile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import java.util.List;

/**
 * Created by developer on 17/06/16.
 */
public class ImageSaver {

    private final String TAG = "Image record: ";

    public ImageSaver() {

    }
    //Create Image as object
    public void iCreateBitmap (final Profile user, final String imageProfileFileName, final Context context){
        if (user != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL imgUrl = new URL("https://graph.facebook.com/"
                                + user.getId() + "/picture?type=large");
                        //Log.d(TAG, "Image URL: " + imgUrl);
                        InputStream inputStream = (InputStream) imgUrl.getContent();
                        Bitmap pImage = BitmapFactory.decodeStream(inputStream);
                        if (pImage != null) {
                            iSaveToInternalStorage(pImage, imageProfileFileName, context);
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
        File directory = new ContextWrapper(context).getDir("imageDir", Context.MODE_PRIVATE);
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
        Log.i(TAG,"Image found at: " + directory.getAbsolutePath());
        return directory.getAbsolutePath();
    }
    //Load Image
    public Bitmap iLoadImageFromStorage(View view, String path, String imageName) {
        try {
            File f=new File(path, imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
            return bitmap;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    //Upload Image to Firebase
    public void iUploadFBImageToFirebase(String path, FirebaseUser user){
        FirebaseStorage  storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
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
                //Log.v(TAG,"URL: " + downloadUrl);
            }
        });
    }
    public void iUploadImagesToFirebase(final List<String> path, final FirebaseUser user, final Context context, final int nUploads, final String bPath){
        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context.getApplicationContext())
                        .setSmallIcon(R.drawable.ic_sync_black_24dp)
                        .setContentTitle("Uploading to HotLikeMe")
                        .setContentText("Images left: " + nUploads);
        final NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = "image" + (nUploads-1) + ".jpg";
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");
                final StorageReference upImageRef = storageRef.child(user.getUid() + bPath + fileName);
                UploadTask uploadTask;

                FirebaseDatabase database = FirebaseDatabase.getInstance();;
                final DatabaseReference databaseReference = database.getReference(user.getUid() + bPath + (nUploads-1));
                final DatabaseReference databaseReference2 = database.getReference(user.getUid() + "/thumbs/" + (nUploads-1));

                try {
                    URL image = new URL(path.get(nUploads-1));
                    Log.i(TAG, "New URL: " + image);
                    try {

                        InputStream inputStream = (InputStream) image.getContent();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                        byte[] data = byteArrayOutputStream.toByteArray();

                        uploadTask = upImageRef.putBytes(data);

                        // Listen for state changes, errors, and completion of the upload.
                        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                if (bPath.equals(ImageBrowser.pathImages)) {
                                    mBuilder.setProgress(100, (int) progress, false);
                                    notificationManager.notify(1, mBuilder.build());
                                }
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
                                if (bPath.equals(ImageBrowser.pathImages)) {
                                    databaseReference.setValue(upImageRef.getPath());

                                    String textNotification;
                                    if (nUploads>1){
                                        textNotification = "Image uploaded";
                                    } else{
                                        textNotification = "All images Uploaded!";
                                    }

                                    mBuilder.setContentText(textNotification).setProgress(0,0,false);
                                    notificationManager.notify(1, mBuilder.build());
                                } else if (bPath.equals(ImageBrowser.pathThumbs)) {
                                    databaseReference2.setValue(upImageRef.getPath());
                                }

                                if (nUploads>1){
                                    int newUploads = nUploads - 1;
                                    iUploadImagesToFirebase(path, user, context, newUploads, bPath);
                                } else {
                                    Log.i(TAG, "All done!");
                                }
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
}
