package com.fetch.auth.production.network;

import androidx.annotation.Nullable;

public class PasabuyStatusPushRequest {
    public String transactionId;
    public String event;
    public boolean isRider;
    @Nullable
    public String preview;

    public PasabuyStatusPushRequest(String transactionId, String event, boolean isRider, @Nullable String preview) {
        this.transactionId = transactionId;
        this.event = event;
        this.isRider = isRider;
        this.preview = preview;
    }
}
