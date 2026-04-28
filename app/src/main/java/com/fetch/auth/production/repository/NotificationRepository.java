package com.fetch.auth.production.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fetch.auth.production.network.BackendApiClient;
import com.fetch.auth.production.network.FcmTokenRequest;
import com.fetch.auth.production.network.PasabuyStatusPushRequest;
import com.fetch.auth.production.network.RoleEventPushRequest;
import com.fetch.auth.production.network.TaskStatusPushRequest;
import com.fetch.auth.production.network.UserEventPushRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {

    public void registerFcmToken(@NonNull String uid, @NonNull String token, @NonNull OperationCallback callback) {
        BackendApiClient.service().registerFcmToken(new FcmTokenRequest(uid, token)).enqueue(voidCallback(callback, "Failed to register token."));
    }

    public void sendTaskStatusPush(@NonNull String taskId, @NonNull String status, boolean isRiderActor) {
        sendTaskStatusPush(taskId, status, isRiderActor, noOpCallback());
    }

    public void sendTaskStatusPush(
            @NonNull String taskId,
            @NonNull String status,
            boolean isRiderActor,
            @NonNull OperationCallback callback
    ) {
        BackendApiClient.service()
                .sendTaskStatusPush(new TaskStatusPushRequest(taskId, status, isRiderActor))
                .enqueue(voidCallback(callback, "Failed to send task status push."));
    }

    public void sendPasabuyStatusPush(
            @NonNull String transactionId,
            @NonNull String event,
            boolean isRiderActor,
            @Nullable String preview
    ) {
        sendPasabuyStatusPush(transactionId, event, isRiderActor, preview, noOpCallback());
    }

    public void sendPasabuyStatusPush(
            @NonNull String transactionId,
            @NonNull String event,
            boolean isRiderActor,
            @Nullable String preview,
            @NonNull OperationCallback callback
    ) {
        BackendApiClient.service()
                .sendPasabuyStatusPush(new PasabuyStatusPushRequest(transactionId, event, isRiderActor, preview))
                .enqueue(voidCallback(callback, "Failed to send PasaBuy push."));
    }

    public void sendUserEventPush(@NonNull String targetUid, @NonNull String event, @Nullable String referenceId) {
        sendUserEventPush(targetUid, event, referenceId, noOpCallback());
    }

    public void sendUserEventPush(
            @NonNull String targetUid,
            @NonNull String event,
            @Nullable String referenceId,
            @NonNull OperationCallback callback
    ) {
        BackendApiClient.service()
                .sendUserEventPush(new UserEventPushRequest(targetUid, event, referenceId))
                .enqueue(voidCallback(callback, "Failed to send user event push."));
    }

    public void sendRoleEventPush(@NonNull String targetRole, @NonNull String event, @Nullable String referenceId) {
        sendRoleEventPush(targetRole, event, referenceId, noOpCallback());
    }

    public void sendRoleEventPush(
            @NonNull String targetRole,
            @NonNull String event,
            @Nullable String referenceId,
            @NonNull OperationCallback callback
    ) {
        BackendApiClient.service()
                .sendRoleEventPush(new RoleEventPushRequest(targetRole, event, referenceId))
                .enqueue(voidCallback(callback, "Failed to send role event push."));
    }

    private Callback<Void> voidCallback(@NonNull OperationCallback callback, @NonNull String fallbackMessage) {
        return new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                callback.onError(new IllegalStateException(fallbackMessage));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        };
    }

    private OperationCallback noOpCallback() {
        return new OperationCallback() {
            @Override
            public void onSuccess() {
                // No-op.
            }

            @Override
            public void onError(Exception error) {
                // No-op.
            }
        };
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }
}
