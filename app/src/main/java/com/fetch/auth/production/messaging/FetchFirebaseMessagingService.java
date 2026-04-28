package com.fetch.auth.production.messaging;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.fetch.auth.production.HomeActivity;
import com.fetch.auth.production.R;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.NotificationRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FetchFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FetchFcmService";
    private static final String CHANNEL_ID = "status_updates";
    private static final String EXTRA_NOTIFICATION_TASK_ID = "extra_notification_task_id";
    private static final String EXTRA_NOTIFICATION_TRANSACTION_ID = "extra_notification_transaction_id";
    private static final String EXTRA_NOTIFICATION_IS_RIDER = "extra_notification_is_rider";
    private static final String EXTRA_NOTIFICATION_SOURCE = "extra_notification_source";
    private static final String NOTIFICATION_SOURCE_FCM = "fcm";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        AuthRepository authRepository = new AuthRepository();
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            return;
        }

        UserProfileRepository repository = new UserProfileRepository();
        repository.updateFcmToken(user.getUid(), token, new UserProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                // No-op for background token sync.
            }

            @Override
            public void onError(Exception error) {
                Log.w(TAG, "Failed to update FCM token", error);
            }
        });

        NotificationRepository notificationRepository = new NotificationRepository();
        notificationRepository.registerFcmToken(user.getUid(), token, new NotificationRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                // No-op.
            }

            @Override
            public void onError(Exception error) {
                Log.w(TAG, "Failed to register FCM token in backend", error);
            }
        });
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        ensureChannel();

        String title = getString(R.string.notif_default_title);
        String body = getString(R.string.notif_default_body);

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            String dataTitle = remoteMessage.getData().get("title");
            String dataBody = remoteMessage.getData().get("body");
            if (!TextUtils.isEmpty(dataTitle)) {
                title = dataTitle;
            }
            if (!TextUtils.isEmpty(dataBody)) {
                body = dataBody;
            }
        }

        String taskId = null;
        String transactionId = null;
        boolean isRider = false;
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            taskId = coalesce(
                    remoteMessage.getData().get("taskId"),
                    remoteMessage.getData().get("task_id"),
                    remoteMessage.getData().get("extra_task_id")
            );
            transactionId = coalesce(
                    remoteMessage.getData().get("transactionId"),
                    remoteMessage.getData().get("pasabuyTransactionId"),
                    remoteMessage.getData().get("transaction_id"),
                    remoteMessage.getData().get("pasabuy_transaction_id"),
                    remoteMessage.getData().get("extra_transaction_id")
            );
            String riderFlag = coalesce(
                    remoteMessage.getData().get("isRider"),
                    remoteMessage.getData().get("is_rider"),
                    remoteMessage.getData().get("isRiderActor"),
                    remoteMessage.getData().get("is_rider_actor")
            );
            isRider = "true".equalsIgnoreCase(riderFlag) || "1".equals(riderFlag);
        }

        Intent homeIntent = new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_NOTIFICATION_SOURCE, NOTIFICATION_SOURCE_FCM)
                .putExtra(EXTRA_NOTIFICATION_IS_RIDER, isRider);
        if (!TextUtils.isEmpty(taskId)) {
            homeIntent.putExtra(EXTRA_NOTIFICATION_TASK_ID, taskId);
        }
        if (!TextUtils.isEmpty(transactionId)) {
            homeIntent.putExtra(EXTRA_NOTIFICATION_TRANSACTION_ID, transactionId);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private String coalesce(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_status_updates),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.notif_channel_status_updates_desc));
        manager.createNotificationChannel(channel);
    }
}
