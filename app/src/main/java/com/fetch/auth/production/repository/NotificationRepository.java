package com.fetch.auth.production.repository;

import androidx.annotation.NonNull;

import com.fetch.auth.production.network.BackendApiClient;
import com.fetch.auth.production.network.FcmTokenRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {

    public void registerFcmToken(@NonNull String uid, @NonNull String token, @NonNull OperationCallback callback) {
        BackendApiClient.service().registerFcmToken(new FcmTokenRequest(uid, token)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                callback.onError(new IllegalStateException("Failed to register token."));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }
}

