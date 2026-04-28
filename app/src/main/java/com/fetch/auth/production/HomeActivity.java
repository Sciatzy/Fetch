package com.fetch.auth.production;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fetch.auth.production.pasabuy.PasaBuyTransactionActivity;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.NotificationRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private static final String EXTRA_NOTIFICATION_TASK_ID = "extra_notification_task_id";
    private static final String EXTRA_NOTIFICATION_TRANSACTION_ID = "extra_notification_transaction_id";
    private static final String EXTRA_NOTIFICATION_IS_RIDER = "extra_notification_is_rider";
    private static final String EXTRA_NOTIFICATION_SOURCE = "extra_notification_source";
    private static final String NOTIFICATION_SOURCE_FCM = "fcm";

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private NotificationRepository notificationRepository;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // No-op. Token sync and in-app flows continue even when user denies notifications.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();
        notificationRepository = new NotificationRepository();

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        requestNotificationPermissionIfNeeded();
        syncFcmToken(user.getUid());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new ActivityFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default selection
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String source = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCE);
        if (!NOTIFICATION_SOURCE_FCM.equals(source)) {
            return;
        }

        String taskId = intent.getStringExtra(EXTRA_NOTIFICATION_TASK_ID);
        String transactionId = intent.getStringExtra(EXTRA_NOTIFICATION_TRANSACTION_ID);
        boolean isRider = intent.getBooleanExtra(EXTRA_NOTIFICATION_IS_RIDER, false);
        consumeNotificationExtras(intent);

        if (!TextUtils.isEmpty(transactionId)) {
            showPasabuyPrompt(transactionId);
            return;
        }

        if (TextUtils.isEmpty(taskId)) {
            return;
        }

        showTrackingPrompt(taskId, isRider);
    }

    private void showTrackingPrompt(String taskId, boolean isRider) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.tracking_redirect_prompt_title)
                .setMessage(R.string.tracking_redirect_prompt_message)
                .setPositiveButton(R.string.tracking_redirect_open, (dialog, which) -> {
                    Intent trackingIntent = new Intent(HomeActivity.this, TaskTrackingActivity.class);
                    trackingIntent.putExtra(TaskTrackingActivity.EXTRA_TASK_ID, taskId);
                    trackingIntent.putExtra(TaskTrackingActivity.EXTRA_IS_RIDER, isRider);
                    startActivity(trackingIntent);
                })
                .setNegativeButton(R.string.tracking_redirect_stay, null)
                .show();
    }

    private void consumeNotificationExtras(Intent intent) {
        intent.removeExtra(EXTRA_NOTIFICATION_SOURCE);
        intent.removeExtra(EXTRA_NOTIFICATION_TASK_ID);
        intent.removeExtra(EXTRA_NOTIFICATION_TRANSACTION_ID);
        intent.removeExtra(EXTRA_NOTIFICATION_IS_RIDER);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void showPasabuyPrompt(String transactionId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_pasabuy_prompt_title)
                .setMessage(R.string.notif_pasabuy_prompt_message)
                .setPositiveButton(R.string.notif_pasabuy_prompt_open, (dialog, which) -> {
                    Intent intent = new Intent(this, PasaBuyTransactionActivity.class);
                    intent.putExtra(PasaBuyTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.notif_pasabuy_prompt_later, null)
                .show();
    }

    private void syncFcmToken(String uid) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.isEmpty()) {
                return;
            }

            userProfileRepository.updateFcmToken(uid, token, new UserProfileRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    // No-op.
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Failed to sync FCM token to user profile", error);
                }
            });

            notificationRepository.registerFcmToken(uid, token, new NotificationRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    // No-op.
                }

                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "Failed to register FCM token in backend", error);
                }
            });
        });
    }
}
