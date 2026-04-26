package com.fetch.auth.production.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fetch.auth.production.HomeActivity;
import com.fetch.auth.production.R;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FetchFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FetchFcmService";
    private static final String CHANNEL_ID = "status_updates";
    private static final String EXTRA_NOTIFICATION_TASK_ID = "extra_notification_task_id";
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

        String taskId = null;
        boolean isRider = false;
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            taskId = coalesce(
                    remoteMessage.getData().get("taskId"),
                    remoteMessage.getData().get("task_id"),
                    remoteMessage.getData().get("extra_task_id")
            );
            isRider = "true".equalsIgnoreCase(remoteMessage.getData().get("isRider"));
        }

        Intent homeIntent = new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_NOTIFICATION_SOURCE, NOTIFICATION_SOURCE_FCM)
                .putExtra(EXTRA_NOTIFICATION_IS_RIDER, isRider);
        if (!TextUtils.isEmpty(taskId)) {
            homeIntent.putExtra(EXTRA_NOTIFICATION_TASK_ID, taskId);
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

