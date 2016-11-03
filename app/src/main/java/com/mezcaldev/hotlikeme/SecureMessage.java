package com.mezcaldev.hotlikeme;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * PASSWORD BASED AES256 Encryption/Decryption
 *
 * Create an instance of SecureMyText using the constructor. Use encrypt method to encrypt the
 * plain text or use EncryptToTransferText method to build the final string with encrypted text,
 * randomly generated salt, Initialisation vector(IV)(all Base64 encoded). Same password to be used
 * for decryption. Iteration count is fixed to be 2000. Can be changed. Password can be hardcoded with
 * some logic or asked from user.
 *
 * A hacker needs to have the PIN, salt, iteration count, IV in each instance to successfully decrypt
 * the text being sent. Since the salt and IV change for each encryption this can be a robust method
 * of encryption in the long run where password and iteration count can be changed in case one transmission
 * is hacked.
 */


class SecureMessage {
    final String TAG = "SecureMessage";
    private static Cipher eCipher;
    private static byte[] salt = new byte[8];
    private static int iterationCount = 2000;
    private static String pass;
    private static byte[] iv;
    private SecretKey secret;

    SecureMessage (String passPhrase) {
        try {

            //password saved in global variable to initialise decrypting cipher too
            pass=passPhrase;

            //random salt generated for each encryption case
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);


            SecretKeyFactory factory = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1");
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount,
                    256);
            SecretKey secretKey = factory.generateSecret(keySpec);
            secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            eCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = eCipher.getParameters();

            //IV extracted from cipher to be sent with the encrypted text
            iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setIterationCount(int n){
        iterationCount=n;
    }

    public String encrypt(String str) {
        try {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");

            // Encrypt
            byte[] enc = eCipher.doFinal(utf8);

            // Encode bytes to base64 to get a string]

            return Base64.encodeToString(enc, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String EncryptToFinalTransferText(String str) {
        try {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");

            // Encrypt
            byte[] enc = eCipher.doFinal(utf8);

            // Encode bytes to base64 to get a string
            //returning encrypted text+salt+IV. Salt and IV will be used to decrypt it back.

            return Base64.encodeToString(enc, Base64.DEFAULT)+Base64.encodeToString(salt, Base64.DEFAULT)+Base64.encodeToString(iv, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String decrypt(String str) {
        //separate encrypted text from salt and IV
        String text = str.substring(0, (str.length() - 38));

        //separate salt
        String RSalt = str.substring((str.length() - 38), (str.length() - 25));

        //separate IV
        String RIV = str.substring((str.length() - 25));

        byte[] saltt = Base64.decode(RSalt, Base64.DEFAULT);
        byte[] IVV = Base64.decode(RIV, Base64.DEFAULT);


        try {
            SecretKeyFactory factory2 = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1");

            //generating same keyspec with same password and recieved salt
            KeySpec keySpec2 = new PBEKeySpec(pass.toCharArray(), saltt, iterationCount, 256);
            SecretKey secretKey2 = factory2.generateSecret(keySpec2);
            SecretKeySpec secret2 = new SecretKeySpec(secretKey2.getEncoded(), "AES");
            Cipher dCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            //initialising decrypt cipher with received IV
            dCipher.init(Cipher.DECRYPT_MODE, secret2, new IvParameterSpec(IVV));

            // Decode base64 to get bytes
            byte[] dec = Base64.decode(text, Base64.DEFAULT);

            // Decrypt
            byte[] utf8 = dCipher.doFinal(dec);

            // Decode using utf-8
            return new String(utf8, "UTF8");
        } catch (BadPaddingException e) {
            Log.e(TAG, "WRONG KEY!");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class BackgroundDecrypt extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String ... params) {
            //separate encrypted text from salt and IV
            String str = params[0];

            String text = str.substring(0, (str.length() - 38));

            //separate salt
            String RSalt = str.substring((str.length() - 38), (str.length() - 25));

            //separate IV
            String RIV = str.substring((str.length() - 25));

            byte[] saltt = Base64.decode(RSalt, Base64.DEFAULT);
            byte[] IVV = Base64.decode(RIV, Base64.DEFAULT);


            try {
                SecretKeyFactory factory2 = SecretKeyFactory
                        .getInstance("PBKDF2WithHmacSHA1");

                //generating same keyspec with same password and recieved salt
                KeySpec keySpec2 = new PBEKeySpec(pass.toCharArray(), saltt, iterationCount, 256);
                SecretKey secretKey2 = factory2.generateSecret(keySpec2);
                SecretKeySpec secret2 = new SecretKeySpec(secretKey2.getEncoded(), "AES");
                Cipher dCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                //initialising decrypt cipher with received IV
                dCipher.init(Cipher.DECRYPT_MODE, secret2, new IvParameterSpec(IVV));

                // Decode base64 to get bytes
                byte[] dec = Base64.decode(text, Base64.DEFAULT);

                // Decrypt
                byte[] utf8 = dCipher.doFinal(dec);

                // Decode using utf-8
                return new String(utf8, "UTF8");
            } catch (BadPaddingException e) {
                Log.e(TAG, "WRONG KEY!");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){

        }
    }

}
