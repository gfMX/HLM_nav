package com.mezcaldev.hotlikeme;


import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatPreferenceActivity {

    final static String TAG = "Settings: ";

    FirebaseUser firebaseUser = FireConnection.getInstance().getUser();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReferenceZero = database.getReference();
    DatabaseReference databaseReference = database.getReference().child("users").child(firebaseUser.getUid());

    String gender;
    SharedPreferences sharedPreferences;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gender = sharedPreferences.getString("gender", "Not defined").toLowerCase();

        System.out.println("Shared Preferences: " + sharedPreferences.getAll());
        updateFireSettings();

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenceSettingsFragment())
                .commit();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_profile_settings) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*@Override
    public void onBackPressed() {
        startActivity(new Intent(this, HLMSlidePagerActivity.class));
        finish();
    }*/
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            //startActivity(new Intent(this, HLMSlidePagerActivity.class));
            //finish();
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    protected void updateFireSettings(){
        if (sharedPreferences.getAll() != null){
            databaseReference.child("/preferences/alias/")
                    .setValue(sharedPreferences.getString("alias", "None"));

            FirebaseUser userToUpdate = FirebaseAuth.getInstance().getCurrentUser();
            if (userToUpdate != null) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(sharedPreferences.getString("alias", userToUpdate.getDisplayName()))
                        .build();

                userToUpdate.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User profile updated.");
                                }
                            }
                        });
            }

            databaseReference.child("/preferences/description/")
                    .setValue(sharedPreferences.getString("description", "None"));
            databaseReference.child("/preferences/looking_for/")
                    .setValue(sharedPreferences.getString("looking_for", "both"));
            databaseReference.child("/preferences/sync_freq/")
                    .setValue(Integer.valueOf(sharedPreferences.getString("sync_frequency", "1000")));
            databaseReference.child("/preferences/sync_distance/")
                    .setValue(Integer.valueOf(sharedPreferences.getString("sync_distance", "250")));

            databaseReference.child("/preferences/visible/")
                    .setValue(sharedPreferences.getBoolean("visible_switch", true));
            databaseReference.child("/preferences/gps_enabled/")
                    .setValue(sharedPreferences.getBoolean("gps_enabled", false));
        }
        if (sharedPreferences.getBoolean("visible_switch", false)){
            System.out.println("User Visible");
            databaseReferenceZero.child("groups").child(gender).child(firebaseUser.getUid()).setValue(true);
            databaseReferenceZero.child("groups").child("both").child(firebaseUser.getUid()).setValue(true);
        } else {
            System.out.println("User Not Visible");
            databaseReferenceZero.child("groups").child(gender).child(firebaseUser.getUid()).setValue(null);
            databaseReferenceZero.child("groups").child("both").child(firebaseUser.getUid()).setValue(null);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferenceSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            bindPreferenceSummaryToValue(findPreference("alias"));
            bindPreferenceSummaryToValue(findPreference("looking_for"));
            bindPreferenceSummaryToValue(findPreference("description"));

            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
            bindPreferenceSummaryToValue(findPreference("sync_distance"));

            //bindPreferenceSummaryToValue(findPreference("notifications_new_discovery_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateFireSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateFireSettings();
    }
}
