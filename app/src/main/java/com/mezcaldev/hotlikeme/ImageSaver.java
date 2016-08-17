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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.UUID;

/**
 * Methods and functions for general use
 */
public class ImageSaver {

    private final String TAG = "Image record: ";
    final Integer compressRatio = 85;

    String pathImages = "/images/";
    String pathThumbs = "/images/thumbs/";

    final FirebaseUser firebaseUser = FireConnection.getInstance().getUser();
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage  storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

    public ImageSaver() {

    }

    //Save Image
    public String iSaveToInternalStorage(Bitmap bitmapImage, String imageName, Context context){
        File directory = new ContextWrapper(context).getDir("imageDir", Context.MODE_PRIVATE);
        File imPath=new File(directory,imageName);
        FileOutputStream fOut;

        try {
            fOut = new FileOutputStream(imPath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, compressRatio, fOut);
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG,"Image found at: " + directory.getAbsolutePath());
        return directory.getAbsolutePath();
    }

    //Load Image
    public Bitmap iLoadImageFromStorage(String path, String imageName) {
        try {
            File file = new File(path, imageName);
            return BitmapFactory.decodeStream(new FileInputStream(file));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //Upload Image to Firebase
    public void iUploadProfileImageToFirebase(String path, FirebaseUser user){
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
                exception.printStackTrace();

            }

        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.v(TAG,"Image uploaded to Firebase");
            }
        });
    }

    public void uploadToFirebase (List<String> listImages,
                                  List<String> listThumbs,
                                  FirebaseUser user,
                                  Context context,
                                  int nUploads){

        String uniqueID = UUID.randomUUID().toString();

        iUploadThumbsToFirebase(listThumbs, user, nUploads, pathThumbs, uniqueID);
        iUploadImagesToFirebase(listImages, listThumbs, user, context, nUploads, pathImages, uniqueID);
    }

    public void iUploadImagesToFirebase(final List<String> pathImages,
                                        final List<String> pathThumbs,
                                        final FirebaseUser user,
                                        final Context context,
                                        final int nUploads,
                                        final String bPath,
                                        final String uniqueID){

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
                String fName = "image_" + uniqueID;
                String fileName = fName + ".jpg";
                final StorageReference upImageRef = storageRef.child(user.getUid() + bPath + fileName);
                final DatabaseReference databaseReferenceImages =
                        database.getReference().child("users").child(user.getUid()).child(bPath).child(uniqueID);

                UploadTask uploadTask;


                try {
                    URL image = new URL(pathImages.get(nUploads-1));
                    //Log.i(TAG, "New URL: " + image);
                    try {

                        InputStream inputStream = (InputStream) image.getContent();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressRatio, byteArrayOutputStream);
                        byte[] data = byteArrayOutputStream.toByteArray();

                        uploadTask = upImageRef.putBytes(data);

                        // Listen for state changes, errors, and completion of the upload.
                        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                mBuilder.setProgress(100, (int) progress, false);
                                notificationManager.notify(1, mBuilder.build());
                                System.out.println("Upload is " + progress + "% done");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                exception.printStackTrace();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                databaseReferenceImages.setValue(upImageRef.getPath());
                                //databaseRefURLImages.setValue(downloadUrl);

                                String textNotification;
                                if (nUploads>1){
                                    textNotification = context.getResources().getString(R.string.text_image_uploaded);
                                } else{
                                    textNotification = context.getResources().getString(R.string.text_images_uploaded);
                                    //Updates the total images on Firebase
                                    updateTotalImagesOnFire();
                                }

                                mBuilder.setContentText(textNotification).setProgress(0,0,false);
                                notificationManager.notify(1, mBuilder.build());

                                if (nUploads>1){
                                    int newUploads = nUploads - 1;
                                    uploadToFirebase(pathImages, pathThumbs, user, context, newUploads);
                                } else {
                                    Log.i(TAG, "Images: All done!");
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

    public void iUploadThumbsToFirebase(final List<String> path,
                                        final FirebaseUser user,
                                        final int nUploads,
                                        final String bPath,
                                        final String uniqueID){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                String fName = "thumb_" + uniqueID;
                String fileName = fName + ".jpg";
                final StorageReference upImageRef = storageRef.child(user.getUid() + bPath + fileName);
                final DatabaseReference databaseReferenceThumbs =
                        database.getReference().child("users").child(user.getUid()).child("/thumbs/").child(uniqueID);

                UploadTask uploadTask;

                try {
                    URL image = new URL(path.get(nUploads-1));
                    //Log.i(TAG, "New URL: " + image);
                    try {

                        InputStream inputStream = (InputStream) image.getContent();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, compressRatio, byteArrayOutputStream);
                        byte[] data = byteArrayOutputStream.toByteArray();

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
                                exception.printStackTrace();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                databaseReferenceThumbs.setValue(upImageRef.getPath());
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
    public void updateTotalImagesOnFire () {
        String userId = firebaseUser.getUid();
        final DatabaseReference dbTotalImagesRef =
                database.getReference().child("users").child(userId).child("/total_images");

        database.getReference().child("/users/").child(userId).addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        int nImages = (int) dataSnapshot.child("images").getChildrenCount();
                        int nThumbs = (int) dataSnapshot.child("thumbs").getChildrenCount();

                        Log.i(TAG, "Total Images: " + nImages + " Total Thumbs: " + nThumbs);
                        dbTotalImagesRef.setValue(nImages);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "Cancelled: ",databaseError.toException());
                    }
                }
        );
    }

}
