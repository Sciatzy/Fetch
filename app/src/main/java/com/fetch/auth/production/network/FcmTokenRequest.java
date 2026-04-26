package com.fetch.auth.production.network;

public class FcmTokenRequest {
    public String uid;
    public String token;

    public FcmTokenRequest(String uid, String token) {
        this.uid = uid;
        this.token = token;
    }
}

