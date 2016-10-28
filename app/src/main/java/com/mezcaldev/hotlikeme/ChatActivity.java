/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mezcaldev.hotlikeme;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.mezcaldev.hotlikeme.FireConnection.databaseGlobal;

public class ChatActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    //Encrypted Message
    boolean flagBottom = true;
    DecryptOnBackground decryptOnBackground;

    protected String myKey; // = "iojdsf290skdjaf823IU8R3SAD9023UJSFAD82934jsfakl";
    private SecureMessage secureMessage;
    //private String encryptedMessage;
    private String encryptedMessageToSend;
    private String decryptedMessage;

    Handler waitForNewMessageSent;
    Runnable waitForNewMessageSentRunnable;

    private static final String TAG = "HLM Chat";
    String MESSAGES_CHILD = "messages";
    String MESSAGES_RESUME = "chats_resume";
    private static final int REQUEST_INVITE = 1;
    int MESSAGE_LIMIT = 20;
    int UPDATE_VIEW_DELAY = 500;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 110;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    String mUsername;
    String mUserChatId;
    String mPhotoUrl;
    SharedPreferences mSharedPreferences;

    private Query recentMessages;
    private FloatingActionButton fab_send;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<ChatMessageModel, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    //private FirebaseDatabase database;
    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference databaseReferenceLastMessages;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser mFirebaseUser;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_hlm);

        /*getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);*/

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();

        if (bundle.getString("userChat")!= null) {
            mUserChatId = bundle.getString("userChat");
            MESSAGES_CHILD = "/chats/" + mUserChatId;
            MESSAGES_RESUME = "/chats_resume/" + mUserChatId;

            /*String myFutureKey = new StringBuilder(mUserChatId.replace("chat_", "")).reverse().toString();
            Log.i(TAG, "-------> KEY For Encryption: " + myFutureKey);*/

            myKey = FireConnection.getInstance().genHashKey(mUserChatId);
            secureMessage = new SecureMessage(myKey);
        }
        if (bundle.getString("userName")!= null) {
            setTitle(bundle.getString("userName"));
        }

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, HLMActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();

        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshChat);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                hideSoftKeyboard(ChatActivity.this);
            }
        });
        mMessageRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                hideSoftKeyboard(ChatActivity.this);
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                hideSoftKeyboard(ChatActivity.this);

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = databaseGlobal.getReference();
        databaseReferenceLastMessages = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        recentMessages = databaseReferenceLastMessages.limitToLast(MESSAGE_LIMIT);

        updateFireBaseRecyclerAdapter();

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (/*flagBottom && */ lastVisiblePosition == -1 ||
                        (/*flagBottom && */ positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mLinearLayoutManager.setStackFromEnd(true);
                    mMessageRecyclerView.scrollToPosition(positionStart);
                    mFirebaseAdapter.notifyDataSetChanged();
                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final int currentMessagePosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                MESSAGE_LIMIT += 10;
                Log.i(TAG, "New Message Limit: " + MESSAGE_LIMIT);
                recentMessages = databaseReferenceLastMessages.limitToLast(MESSAGE_LIMIT);

                mProgressBar.setVisibility(View.VISIBLE);

                flagBottom = false;
                mLinearLayoutManager.setStackFromEnd(false);
                //mLinearLayoutManager.scrollToPosition(0);
                updateFireBaseRecyclerAdapter();

                Handler handlerRefreshSign = new Handler();
                handlerRefreshSign.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout.isRefreshing()){
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },750);

                Handler handlerShowOldMessages = new Handler();
                handlerShowOldMessages.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mLinearLayoutManager.scrollToPosition(currentMessagePosition);
                        //mLinearLayoutManager.setStackFromEnd(true);
                    }
                },2000);


            }
        });

        // Initialize Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", 160L);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(ChatRemotePreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mLinearLayoutManager.setStackFromEnd(true);
                mLinearLayoutManager.scrollToPosition(mLinearLayoutManager.getItemCount()-1);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.toString().trim().length() > 0) {
                    fab_send.setClickable(true);
                    fab_send.setEnabled(true);

                } else {
                    fab_send.setClickable(false);
                    fab_send.setEnabled(false);
                    fab_send.setBackgroundColor(Color.GRAY);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        fab_send = (FloatingActionButton) findViewById(R.id.fab_send);
        fab_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //encryptedMessage = secureMessage.encrypt(mMessageEditText.getText().toString());
                checkNetworkAccess();
                encryptedMessageToSend = secureMessage.EncryptToFinalTransferText(mMessageEditText.getText().toString());

                ChatMessageModel chatMessageModel = new ChatMessageModel(encryptedMessageToSend, mUsername,
                        mPhotoUrl, timeStamp(), mFirebaseUser.getUid(), false);

                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(chatMessageModel);
                mFirebaseDatabaseReference.child(MESSAGES_RESUME).setValue(chatMessageModel);

                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);

                // Go to last message
                waitForNewMessageSent = new Handler();
                waitForNewMessageSentRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mLinearLayoutManager.scrollToPosition(mLinearLayoutManager.getItemCount()-1);
                    }
                };
                waitForNewMessageSent.postDelayed(waitForNewMessageSentRunnable, 500);

            }
        });

        //Check if Network is available
        checkNetworkAccess();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " + e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }

    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong("friendly_msg_length");
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, "HLM Message Length is: " + friendly_msg_length);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private String timeStamp(){
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();

        return String.valueOf(calendar.getTimeInMillis());
    }

    private String dateFormatter (String millis) {

        Long currentDateTime = Long.parseLong(millis);
        Date currentDate = new Date(currentDateTime);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm", Locale.US);
        //Log.v(TAG, sdf.format(currentDate));

        return sdf.format(currentDate);
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);

    }

    class DecryptParameters {
        MessageViewHolder messageViewHolder;
        String chatMessageModel;
        int position;

        DecryptParameters(MessageViewHolder messageViewHolder, String chatMessageModel, int position){
            this.messageViewHolder = messageViewHolder;
            this.chatMessageModel = chatMessageModel;
            this.position = position;
        }
    }

    private void updateFireBaseRecyclerAdapter(){

        if (mFirebaseAdapter != null){
            mFirebaseAdapter.cleanup();
        }

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessageModel, MessageViewHolder>(
                ChatMessageModel.class,
                R.layout.item_message_left,
                MessageViewHolder.class,
                recentMessages) {

            private static final int RIGHT_MSG = 0;
            private static final int LEFT_MSG = 1;

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view;
                if (viewType == RIGHT_MSG){
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_right,parent,false);
                    return new MessageViewHolder(view);
                } else {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_left,parent,false);
                    //Set Last Message from the Other User to Read It!
                    //mFirebaseDatabaseReference.child(MESSAGES_RESUME).child("readIt").setValue(true);
                    return new MessageViewHolder(view);
                }
            }

            @Override
            public int getItemViewType(int position) {
                ChatMessageModel model = getItem(position);

                //System.out.println("------------------------------------------------------------");
                //System.out.println(model.getUserId() + " = " + mFirebaseUser.getUid());

                if (model.getName().equals(mFirebaseUser.getDisplayName())
                        || model.getUserId().equals(mFirebaseUser.getUid())){
                    //System.out.println("Message Right");
                    return RIGHT_MSG;
                }else{
                    //System.out.println("Message Left");
                    //Set Last Message from the Other User to Read It!
                    mFirebaseDatabaseReference.child(MESSAGES_RESUME).child("readIt").setValue(true);
                    return LEFT_MSG;
                }
            }

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              final ChatMessageModel chatMessageModel, int position) {

                DecryptParameters decryptParameters = new DecryptParameters(viewHolder, chatMessageModel.getText(), position);

                decryptOnBackground = new DecryptOnBackground();
                decryptOnBackground.execute(decryptParameters);


                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                String messengerText = dateFormatter(chatMessageModel.getTimeStamp());
                //viewHolder.messageTextView.setText(chatMessageModel.getText());
                viewHolder.messengerTextView.setText(messengerText);

                if (chatMessageModel.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(ChatActivity.this,
                            R.drawable.ic_account_circle_black_24dp));
                } else {
                    Glide.with(ChatActivity.this)
                            .load(chatMessageModel.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };

        flagBottom = true;

        /*mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (flagBottom && lastVisiblePosition == -1 ||
                            (flagBottom && positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mLinearLayoutManager.setStackFromEnd(true);
                    mMessageRecyclerView.scrollToPosition(positionStart);
                    mFirebaseAdapter.notifyDataSetChanged();
                }
            }
        }); */

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        Handler reloadView = new Handler();
        reloadView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFirebaseAdapter.notifyDataSetChanged();
            }
        }, UPDATE_VIEW_DELAY);

    }

    class DecryptOnBackground extends AsyncTask <DecryptParameters, Void, Void>{
        MessageViewHolder v;
        String c;
        int p;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(DecryptParameters ... params) {

            v = params[0].messageViewHolder;
            c = params[0].chatMessageModel;
            p = params[0].position;

            decryptedMessage = secureMessage.decrypt(c);    //<--

            return null;
        }

        @Override
        protected void onPostExecute (Void result){
            v.messageTextView.setText(decryptedMessage);              //<--
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkNetworkAccess (){
        Snackbar snackNetworkRequired = Snackbar.make(this.getWindow().getDecorView(),
                getResources().getString(R.string.text_network_access_required),
                Snackbar.LENGTH_INDEFINITE);

        if (!isNetworkAvailable()) {
            snackNetworkRequired.show();
        } else if (snackNetworkRequired.isShown()) {
            snackNetworkRequired.dismiss();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkAccess();
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkAccess();
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            waitForNewMessageSent.removeCallbacks(waitForNewMessageSentRunnable);
            waitForNewMessageSent.removeCallbacksAndMessages(null);
        } catch (NullPointerException e){
            Log.e(TAG, "No handler to remove");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}
