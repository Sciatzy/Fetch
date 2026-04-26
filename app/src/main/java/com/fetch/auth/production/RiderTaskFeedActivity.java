package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.adapter.TaskFeedAdapter;
import com.fetch.auth.production.model.ErrandTask;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.TaskRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class RiderTaskFeedActivity extends AppCompatActivity implements TaskFeedAdapter.TaskActionListener {

    private RecyclerView rvPendingTasks;
    private TextView tvEmptyState;
    private TextView tvErrorState;
    private ProgressBar progressTaskFeed;
    private Button btnRetryFeed;

    private AuthRepository authRepository;
    private TaskRepository taskRepository;
    private UserProfileRepository userProfileRepository;
    private TaskFeedAdapter taskFeedAdapter;

    private ListenerRegistration pendingTaskListener;
    private String riderId;
    private boolean isRiderAuthorized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_task_feed);

        authRepository = new AuthRepository();
        taskRepository = new TaskRepository();
        userProfileRepository = new UserProfileRepository();

        rvPendingTasks = findViewById(R.id.rvPendingTasks);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvErrorState = findViewById(R.id.tvErrorState);
        progressTaskFeed = findViewById(R.id.progressTaskFeed);
        btnRetryFeed = findViewById(R.id.btnRetryFeed);

        taskFeedAdapter = new TaskFeedAdapter(this);
        rvPendingTasks.setLayoutManager(new LinearLayoutManager(this));
        rvPendingTasks.setAdapter(taskFeedAdapter);
        btnRetryFeed.setOnClickListener(v -> subscribeToPendingTasks());

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_user_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        riderId = user.getUid();
        verifyRiderRole(riderId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeToPendingTasks();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pendingTaskListener != null) {
            pendingTaskListener.remove();
            pendingTaskListener = null;
        }
    }

    private void subscribeToPendingTasks() {
        if (TextUtils.isEmpty(riderId) || !isRiderAuthorized) {
            return;
        }

        showLoadingState();

        if (pendingTaskListener != null) {
            pendingTaskListener.remove();
        }

        pendingTaskListener = taskRepository.listenToPendingTasks(
                this::renderTasks,
                error -> {
                    String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
                    showErrorState(getString(R.string.task_feed_error, message));
                }
        );
    }

    private void renderTasks(List<ErrandTask> tasks) {
        List<ErrandTask> safeTasks = tasks != null ? tasks : new ArrayList<>();
        taskFeedAdapter.submitList(safeTasks);

        progressTaskFeed.setVisibility(View.GONE);
        tvErrorState.setVisibility(View.GONE);
        btnRetryFeed.setVisibility(View.GONE);

        boolean isEmpty = safeTasks.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvPendingTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showLoadingState() {
        progressTaskFeed.setVisibility(View.VISIBLE);
        tvErrorState.setVisibility(View.GONE);
        btnRetryFeed.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        rvPendingTasks.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        progressTaskFeed.setVisibility(View.GONE);
        rvPendingTasks.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvErrorState.setText(TextUtils.isEmpty(message)
                ? getString(R.string.task_feed_error_retry)
                : message);
        tvErrorState.setVisibility(View.VISIBLE);
        btnRetryFeed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAcceptTask(ErrandTask task) {
        if (!isRiderAuthorized) {
            Toast.makeText(this, R.string.error_rider_access_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (task == null || task.getTaskId() == null) {
            return;
        }

        taskRepository.acceptTask(task.getTaskId(), riderId, new TaskRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                taskFeedAdapter.clearProcessing(task.getTaskId());
                Toast.makeText(RiderTaskFeedActivity.this, R.string.task_accepted_success, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RiderTaskFeedActivity.this, TaskTrackingActivity.class);
                intent.putExtra(TaskTrackingActivity.EXTRA_TASK_ID, task.getTaskId());
                intent.putExtra(TaskTrackingActivity.EXTRA_IS_RIDER, true);
                intent.putExtra(TaskTrackingActivity.EXTRA_AUTO_NAVIGATE_TO_PICKUP, true);
                startActivity(intent);
            }

            @Override
            public void onError(Exception error) {
                taskFeedAdapter.clearProcessing(task.getTaskId());
                String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
                Toast.makeText(RiderTaskFeedActivity.this, getString(R.string.task_accepted_error, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPreviewRoute(ErrandTask task) {
        if (task == null
                || task.getPickupLat() == null
                || task.getPickupLng() == null
                || task.getDropoffLat() == null
                || task.getDropoffLng() == null) {
            Toast.makeText(this, R.string.task_route_preview_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        openRoutePreview(task);
    }

    private void openRoutePreview(ErrandTask task) {
        if (task == null
                || task.getPickupLat() == null
                || task.getPickupLng() == null
                || task.getDropoffLat() == null
                || task.getDropoffLng() == null) {
            Toast.makeText(this, R.string.task_route_preview_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, RoutePreviewActivity.class);
        intent.putExtra(RoutePreviewActivity.EXTRA_PICKUP_LAT, task.getPickupLat());
        intent.putExtra(RoutePreviewActivity.EXTRA_PICKUP_LNG, task.getPickupLng());
        intent.putExtra(RoutePreviewActivity.EXTRA_DROPOFF_LAT, task.getDropoffLat());
        intent.putExtra(RoutePreviewActivity.EXTRA_DROPOFF_LNG, task.getDropoffLng());
        intent.putExtra(RoutePreviewActivity.EXTRA_PICKUP_ADDRESS, task.getPickupAddress());
        intent.putExtra(RoutePreviewActivity.EXTRA_DROPOFF_ADDRESS, task.getDropoffAddress());
        intent.putExtra(RoutePreviewActivity.EXTRA_ESTIMATED_FEE,
                task.getBudget() != null ? task.getBudget() : (task.getEstimatedFee() != null ? task.getEstimatedFee() : 0d));
        startActivity(intent);
    }

    private void verifyRiderRole(String uid) {
        userProfileRepository.getUserRole(uid, new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                isRiderAuthorized = "rider".equalsIgnoreCase(role);
                if (!isRiderAuthorized) {
                    Toast.makeText(RiderTaskFeedActivity.this, R.string.error_rider_access_required, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                subscribeToPendingTasks();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(RiderTaskFeedActivity.this, R.string.error_role_check_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}


