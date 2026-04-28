package com.fetch.auth.production.network;

import androidx.annotation.Nullable;

public class UserEventPushRequest {
    public String targetUid;
    public String event;
    @Nullable
    public String referenceId;

    public UserEventPushRequest(String targetUid, String event, @Nullable String referenceId) {
        this.targetUid = targetUid;
        this.event = event;
        this.referenceId = referenceId;
    }
}
