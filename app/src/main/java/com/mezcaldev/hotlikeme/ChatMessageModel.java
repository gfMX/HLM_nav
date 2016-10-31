/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mezcaldev.hotlikeme;

import android.os.AsyncTask;

public class ChatMessageModel {

    private String id;
    private String text;
    private String name;
    private String userId;
    private String photoUrl;
    private String timeStamp;
    private String userChatId;
    private Boolean readIt;

    private SecureMessage secureMessage;

    public ChatMessageModel() {

    }

    ChatMessageModel(String text, String name, String photoUrl, String timeStamp, String userId, Boolean readIt, String userChatId) {
        this.text = text;
        this.name = name;
        this.userId = userId;
        this.photoUrl = photoUrl;
        this.timeStamp = timeStamp;
        this.readIt = readIt;

        this.userChatId = userChatId;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserChatId() {
        return userChatId;
    }

    public void setUserChatId(String userChatId) {
        this.userChatId = userChatId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setReadIt(Boolean read) {
        this.readIt = read;
    }

    public Boolean getReadIt(){
        return readIt;
    }

    public String getDecryptedText(){
        String mKey = FireConnection.getInstance().genHashKey(userChatId);
        secureMessage = new SecureMessage(mKey);

        return secureMessage.decrypt(getText());
    }
}
