package com.fetch.auth.production.network;

import androidx.annotation.Nullable;

public class RoleEventPushRequest {
    public String targetRole;
    public String event;
    @Nullable
    public String referenceId;

    public RoleEventPushRequest(String targetRole, String event, @Nullable String referenceId) {
        this.targetRole = targetRole;
        this.event = event;
        this.referenceId = referenceId;
    }
}
