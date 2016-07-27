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

import com.facebook.Profile;
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

    final FirebaseUser firebaseUser = MainActivityFragment.user;
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    final FirebaseStorage  storage = FirebaseStorage.getInstance();
    final StorageReference storageRef = storage.getReferenceFromUrl("gs://project-6344486298585531617.appspot.com");

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
            }

        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                //Log.i(TAG, "Download URL: " + downloadUrl);
                Log.v(TAG,"Image uploaded to Firebase");
                //Log.v(TAG,"URL: " + downloadUrl);
            }
        });
    }

    public void iUploadImagesToFirebase(final List<String> path,
                                        final FirebaseUser user,
                                        final Context context,
                                        final int nUploads,
                                        final String bPath,
                                        final int existentImages){

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

                int imageNumber = existentImages + nUploads-1;
                String uniqueID = UUID.randomUUID().toString();
                String fileName = "image_" + uniqueID + ".jpg";

                final StorageReference upImageRef = storageRef.child(user.getUid() + bPath + fileName);

                final DatabaseReference databaseReferenceImages = database.getReference(user.getUid() + bPath + imageNumber);
                //final DatabaseReference databaseRefURLImages = database.getReference(user.getUid() + "/URLImage/" + imageNumber);

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
                                exception.printStackTrace();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                //Log.i(TAG, "Download URL: " + downloadUrl);
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
                                    iUploadImagesToFirebase(path, user, context, newUploads, bPath, existentImages);
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
                                        final int existentImages){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                int imageNumber = existentImages + nUploads-1;
                String uniqueID = UUID.randomUUID().toString();
                String fileName = "thumb_" + uniqueID + ".jpg";

                final StorageReference upImageRef = storageRef.child(user.getUid() + bPath + fileName);

                final DatabaseReference databaseReferenceThumbs = database.getReference(user.getUid() + "/thumbs/" + imageNumber);
                //final DatabaseReference databaseRefURLThumbs = database.getReference(user.getUid() + "/URLThumb/" + imageNumber);

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
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                                //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                //Log.i(TAG, "Download URL: " + downloadUrl);
                                databaseReferenceThumbs.setValue(upImageRef.getPath());
                                //databaseRefURLThumbs.setValue(downloadUrl);

                                if (nUploads>1){
                                    int newUploads = nUploads - 1;
                                    iUploadThumbsToFirebase(path, user, newUploads, bPath, existentImages);
                                } else {
                                    Log.i(TAG, "Thumbs: All done!");
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
    public void updateTotalImagesOnFire () {
        String userId = firebaseUser.getUid();
        final DatabaseReference dbTotalImagesRef = database.getReference(userId + "/total_images");

        database.getReference(userId).addValueEventListener(
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
    public void DeleteImagesOnFire (List <Integer> deleteList) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // We're working on this (:
                StorageReference fileToDelete;

            }
        });
        thread.start();
    }

}
