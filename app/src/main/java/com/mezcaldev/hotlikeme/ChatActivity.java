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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.mezcaldev.hotlikeme.FireConnection.ONE_SECOND;
import static com.mezcaldev.hotlikeme.FireConnection.chatIsInFront;
import static com.mezcaldev.hotlikeme.FireConnection.databaseGlobal;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLength;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLimit;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageLimitDefault;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageOld;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessageOldDefault;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessagesMax;
import static com.mezcaldev.hotlikeme.FireConnection.fireConfigMessagesMaxDefault;

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
    boolean flagRunOnce = false;
    boolean flagMessagesDecrypted = false;
    boolean isInFront;

    int positionMessages;
    List<String> decryptedMessages = new ArrayList<>();

    protected String myKey;
    private SecureMessage secureMessage;
    private DecryptOnBackground decryptOnBackground;
    private String encryptedMessageToSend;
    String decryptedMessage;
    long totalMessages;


    Handler waitForNewMessageSent;
    Runnable waitForNewMessageSentRunnable;

    //Delays:
    int bigDelay = ONE_SECOND * 2;

    private static final String TAG = "HLM Chat";
    String MESSAGES_CHILD = "messages";
    String MESSAGES_RESUME = "chats_resume";

    Handler handlerUpdateView;
    Runnable runnableUpdateView;

    int UPDATE_VIEW_DELAY = 500;
    private static final int REQUEST_INVITE = 1;

    int MESSAGE_LIMIT = fireConfigMessageLimitDefault;
    int ADD_OLD_MESSAGES = fireConfigMessageOldDefault;
    int MSG_LENGTH_LIMIT = fireConfigMessageLimitDefault;
    int MAX_MESSAGES_DECRYPTED = fireConfigMessagesMaxDefault;

    public static final String ANONYMOUS = "anonymous";
    //private static final String MESSAGE_SENT_EVENT = "message_sent";
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
    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference databaseReferenceLastMessages;
    FirebaseAuth mFirebaseAuth;
    FirebaseUser mFirebaseUser;
    private EditText mMessageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_hlm);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

            myKey = FireConnection.getInstance().genHashKey(mUserChatId);
            secureMessage = new SecureMessage(myKey);
        }
        if (bundle.getString("userName")!= null) {
            setTitle(bundle.getString("userName"));
        }
        if (fireConfigMessageLimit > 0) {
            MESSAGE_LIMIT = fireConfigMessageLimit;
        }
        if (fireConfigMessageOld > 0) {
            ADD_OLD_MESSAGES = fireConfigMessageOld;
        }
        if (fireConfigMessageLength > 0) {
            MSG_LENGTH_LIMIT = fireConfigMessageLength;
        }
        if (fireConfigMessagesMax > 0) {
            MAX_MESSAGES_DECRYPTED = fireConfigMessagesMax;
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
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            } else {
                mPhotoUrl = null;
            }
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        fab_send = (FloatingActionButton) findViewById(R.id.fab_send);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshChat);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //hideSoftKeyboard(ChatActivity.this);
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

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final int currentMessagePosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                MESSAGE_LIMIT += ADD_OLD_MESSAGES;
                Log.i(TAG, "New Message Limit: " + MESSAGE_LIMIT);
                Toast.makeText(getApplicationContext(), "Loading Old Messages", Toast.LENGTH_LONG).show();

                recentMessages = databaseReferenceLastMessages.limitToLast(MESSAGE_LIMIT);

                mProgressBar.setVisibility(View.VISIBLE);
                mLinearLayoutManager.setStackFromEnd(false);
                updateFireBaseRecyclerAdapter();
                //preloadDecryptedMessages();

                Handler handlerRefreshSign = new Handler();
                handlerRefreshSign.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout.isRefreshing()){
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, ONE_SECOND);

                Handler handlerShowOldMessages = new Handler();
                handlerShowOldMessages.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mMessageRecyclerView.smoothScrollToPosition(currentMessagePosition + (ADD_OLD_MESSAGES/2));
                    }
                }, bigDelay);
            }
        });

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(ChatRemotePreferences.FRIENDLY_MSG_LENGTH, MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mLinearLayoutManager.setStackFromEnd(true);
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

        fab_send.setClickable(false);
        fab_send.setEnabled(false);
        fab_send.setBackgroundColor(Color.GRAY);

        fab_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkNetworkAccess();

                if (mLinearLayoutManager.getItemCount()-1 >= 0){
                    mMessageRecyclerView.smoothScrollToPosition(mLinearLayoutManager.getItemCount()-1);
                }

                mFirebaseDatabaseReference.child(MESSAGES_RESUME).child("readIt").setValue(false);          //Remove after finding the TRUE leak...
                encryptedMessageToSend = secureMessage.EncryptToFinalTransferText(mMessageEditText.getText().toString());

                ChatMessageModel chatMessageModel = new ChatMessageModel(encryptedMessageToSend, mUsername,
                        mPhotoUrl, timeStamp(), mFirebaseUser.getUid(), false);

                mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(chatMessageModel);
                mFirebaseDatabaseReference.child(MESSAGES_RESUME).setValue(chatMessageModel);

                mMessageEditText.setText("");

                // Go to last message
                waitForNewMessageSent = new Handler();
                waitForNewMessageSentRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mLinearLayoutManager.scrollToPosition(mLinearLayoutManager.getItemCount()-1);
                        if (mFirebaseAdapter != null) {
                            mFirebaseAdapter.notifyDataSetChanged();
                        }
                    }
                };
                waitForNewMessageSent.postDelayed(waitForNewMessageSentRunnable, 300);

            }
        });

        //Check if Network is available
        checkNetworkAccess();
        preloadDecryptedMessages();
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
            case R.id.action_reloadChat:
                if (mFirebaseAdapter != null){
                    mFirebaseAdapter.notifyDataSetChanged();
                }
                return true;
            case R.id.invite_menu:
                sendInvitation();
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
                //mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
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

        if (activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }

    }

    private void preloadDecryptedMessages(){
        if (!flagMessagesDecrypted) {
            Query preloadMessages = databaseReferenceLastMessages.limitToLast(MAX_MESSAGES_DECRYPTED);
            positionMessages = 0;
            decryptedMessages.clear();

            mProgressBar.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), "Loading Messages", Toast.LENGTH_LONG).show();

            Handler handlerShowLoadingBar = new Handler();
            Runnable runnableShowLoadingBar = new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    if (mFirebaseAdapter != null) {
                        mFirebaseAdapter.notifyDataSetChanged();
                    }
                }
            };
            handlerShowLoadingBar.postDelayed(runnableShowLoadingBar, bigDelay * 2);

            preloadMessages.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    totalMessages = dataSnapshot.getChildrenCount();
                    Log.i(TAG, "Childrens: " + totalMessages);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            preloadMessages.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    if (!flagRunOnce) {
                        decryptedMessages.add(dataSnapshot.child("text").getValue().toString());
                    } else if (!swipeRefreshLayout.isRefreshing()) {
                        decryptedMessages.add(secureMessage.decrypt(dataSnapshot.child("text").getValue().toString()));
                    }

                    if (positionMessages == totalMessages - 1) {
                        Log.e(TAG, "Updating the adapter on: " + positionMessages + "<---");

                        Handler handlerShowMessages = new Handler();
                        Runnable runnableShowMessages = new Runnable() {
                            @Override
                            public void run() {
                                updateFireBaseRecyclerAdapter();
                            }
                        };
                        handlerShowMessages.postDelayed(runnableShowMessages, bigDelay);

                        decryptOnBackground = new DecryptOnBackground();
                        decryptOnBackground.execute();
                        flagMessagesDecrypted = true;

                    } else if (totalMessages == 0) {
                        updateFireBaseRecyclerAdapter();
                    }
                    positionMessages++;
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { }
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) { }
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) { }
                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        } else {
            updateFireBaseRecyclerAdapter();
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
                    return new MessageViewHolder(view);
                }
            }

            @Override
            public int getItemViewType(int position) {
                ChatMessageModel model = getItem(position);

                if (model.getUserId().equals(mFirebaseUser.getUid())){
                    return RIGHT_MSG;
                }else{

                    return LEFT_MSG;
                }
            }

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              final ChatMessageModel chatMessageModel, int position) {

                String messengerText = dateFormatter(chatMessageModel.getTimeStamp());

                //System.out.println("SIZE -------> " + positionMessages);

                int positionToLoadMessage;
                if (positionMessages - MESSAGE_LIMIT >= 0) {
                    positionToLoadMessage = position + (positionMessages - MESSAGE_LIMIT);
                } else {
                    positionToLoadMessage = position;
                }

                viewHolder.messageTextView.setText(decryptedMessages.get(positionToLoadMessage));
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

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition >= -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {

                    mLinearLayoutManager.setStackFromEnd(true);
                    mLinearLayoutManager.scrollToPosition(positionStart);

                    mFirebaseAdapter.notifyDataSetChanged();
                }
                Handler handlerSetAsRead = new Handler();
                Runnable runnableSetAsRead = new Runnable() {
                    @Override
                    public void run() {
                        if (isInFront && !mFirebaseAdapter.getItem(mFirebaseAdapter.getItemCount()-1).getUserId().equals(mFirebaseUser.getUid())) {
                            //Log.i(TAG, "====> Item count: " + getItemCount() + " Item Position: " + position);
                            mFirebaseDatabaseReference.child(MESSAGES_RESUME).child("readIt").setValue(true);
                        }
                    }
                };
                handlerSetAsRead.postDelayed(runnableSetAsRead, 2000);
            }
        });


        Handler reloadView = new Handler();
        reloadView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFirebaseAdapter.notifyDataSetChanged();
                //new DecryptOnBackground().execute();
            }
        }, UPDATE_VIEW_DELAY);
    }

    class DecryptOnBackground extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<String> messagesToDecrypt = decryptedMessages;

            if (!flagRunOnce) {
                int nMessages = messagesToDecrypt.size();
                Log.i(TAG, "--> Decrypting <-- Size: " + nMessages);

                for (int i = nMessages - 1; i >= 0; i--) {
                    decryptedMessage = secureMessage.decrypt(messagesToDecrypt.get(i));
                    decryptedMessages.set(i, decryptedMessage);
                }

                flagRunOnce = true;
            } else {
                Log.i(TAG, "Messages already decrypted");
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result){
            Log.i(TAG, "Decrypting: Updating Adapter");
            if (mFirebaseAdapter != null) {
                mFirebaseAdapter.notifyDataSetChanged();
            }
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

    private void updateViewWithDelay(int delay){
        if (mFirebaseAdapter != null){
            handlerUpdateView = new Handler();
            runnableUpdateView = new Runnable() {
                @Override
                public void run() {
                    mFirebaseAdapter.notifyDataSetChanged();
                }
            };
            handlerUpdateView.postDelayed(runnableUpdateView, delay);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatIsInFront = true;
        checkNetworkAccess();
    }
    @Override
    protected void onResume() {
        super.onResume();
        chatIsInFront = true;
        checkNetworkAccess();
        updateViewWithDelay(bigDelay * 2);
    }
    @Override
    protected void onPause(){
        super.onPause();
        chatIsInFront = false;
    }
    @Override
    protected void onStop() {
        super.onStop();
        chatIsInFront = false;
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
        chatIsInFront = false;
        if (decryptOnBackground != null) {
            decryptOnBackground.cancel(true);
        }
        try {
            handlerUpdateView.removeCallbacks(runnableUpdateView);
            handlerUpdateView.removeCallbacksAndMessages(null);
        }catch(NullPointerException e){
            Log.e(TAG, "No Callbacks to Remove");
        }
    }

}
