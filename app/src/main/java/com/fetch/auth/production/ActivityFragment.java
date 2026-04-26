package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.adapter.TaskActivityAdapter;
import com.fetch.auth.production.model.ErrandTask;
import com.fetch.auth.production.model.TaskStatus;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.TaskRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityFragment extends Fragment implements TaskActivityAdapter.TaskClickListener {

    private RecyclerView rvActivityTasks;
    private TextView tvActivityEmpty;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private TaskRepository taskRepository;
    private TaskActivityAdapter taskActivityAdapter;

    private ListenerRegistration activityTaskListener;
    private String uid;
    private boolean isRider;
    private final Map<String, String> lastTaskStatusById = new HashMap<>();
    private final Set<String> promptedTaskIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        rvActivityTasks = view.findViewById(R.id.rvActivityTasks);
        tvActivityEmpty = view.findViewById(R.id.tvActivityEmpty);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();
        taskRepository = new TaskRepository();

        taskActivityAdapter = new TaskActivityAdapter(this);
        rvActivityTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvActivityTasks.setAdapter(taskActivityAdapter);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            return view;
        }

        uid = user.getUid();
        resolveRoleAndSubscribe();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!TextUtils.isEmpty(uid) && activityTaskListener == null) {
            resolveRoleAndSubscribe();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (activityTaskListener != null) {
            activityTaskListener.remove();
            activityTaskListener = null;
        }
    }

    private void resolveRoleAndSubscribe() {
        if (TextUtils.isEmpty(uid)) {
            return;
        }

        userProfileRepository.getUserRole(uid, new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                isRider = "rider".equalsIgnoreCase(role);
                subscribeToMyTasks();
            }

            @Override
            public void onError(Exception error) {
                subscribeToMyTasks();
            }
        });
    }

    private void subscribeToMyTasks() {
        if (TextUtils.isEmpty(uid)) {
            return;
        }

        if (activityTaskListener != null) {
            activityTaskListener.remove();
        }

        if (isRider) {
            activityTaskListener = taskRepository.listenToRiderTasks(uid, this::renderTasks, this::onTaskListenerError);
        } else {
            activityTaskListener = taskRepository.listenToCustomerTasks(uid, this::renderTasks, this::onTaskListenerError);
        }
    }

    private void renderTasks(List<ErrandTask> tasks) {
        List<ErrandTask> safeTasks = tasks != null ? tasks : new ArrayList<>();
        Collections.sort(safeTasks, new Comparator<ErrandTask>() {
            @Override
            public int compare(ErrandTask first, ErrandTask second) {
                if (first.getCreatedAt() == null && second.getCreatedAt() == null) return 0;
                if (first.getCreatedAt() == null) return 1;
                if (second.getCreatedAt() == null) return -1;
                return second.getCreatedAt().compareTo(first.getCreatedAt());
            }
        });
        taskActivityAdapter.submitList(safeTasks);
        maybePromptCustomerTrackingRedirect(safeTasks);

        boolean empty = safeTasks.isEmpty();
        tvActivityEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvActivityTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void maybePromptCustomerTrackingRedirect(List<ErrandTask> tasks) {
        if (isRider || !isAdded() || tasks == null || tasks.isEmpty()) {
            return;
        }

        for (ErrandTask task : tasks) {
            if (task == null || TextUtils.isEmpty(task.getTaskId())) {
                continue;
            }

            String taskId = task.getTaskId();
            String currentStatus = task.getStatus();
            String previousStatus = lastTaskStatusById.get(taskId);
            lastTaskStatusById.put(taskId, currentStatus);

            if (promptedTaskIds.contains(taskId)) {
                continue;
            }

            boolean isActiveForTracking = isTrackableStatus(currentStatus);
            boolean becameTrackable = !isTrackableStatus(previousStatus) && isActiveForTracking;
            if (!becameTrackable) {
                continue;
            }

            promptedTaskIds.add(taskId);
            showTrackingRedirectDialog(task);
            break;
        }
    }

    private boolean isTrackableStatus(String status) {
        return TaskStatus.ACCEPTED.equals(status)
                || TaskStatus.ARRIVED_PICKUP.equals(status)
                || TaskStatus.IN_PROGRESS.equals(status)
                || TaskStatus.ARRIVED_DROPOFF.equals(status);
    }

    private void showTrackingRedirectDialog(ErrandTask task) {
        if (!isAdded()) {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tracking_redirect_prompt_title)
                .setMessage(R.string.tracking_redirect_prompt_message)
                .setPositiveButton(R.string.tracking_redirect_open, (dialog, which) -> openTaskTracking(task))
                .setNegativeButton(R.string.tracking_redirect_stay, null)
                .show();
    }

    private void openTaskTracking(ErrandTask task) {
        if (task == null || TextUtils.isEmpty(task.getTaskId()) || !isAdded()) {
            return;
        }

        boolean shouldOpenAsRider = isRider;
        if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(task.getRiderId()) && uid.equals(task.getRiderId())) {
            shouldOpenAsRider = true;
        }

        Intent intent = new Intent(requireContext(), TaskTrackingActivity.class);
        intent.putExtra(TaskTrackingActivity.EXTRA_TASK_ID, task.getTaskId());
        intent.putExtra(TaskTrackingActivity.EXTRA_IS_RIDER, shouldOpenAsRider);
        startActivity(intent);
    }

    private void onTaskListenerError(Exception error) {
        if (!isAdded()) {
            return;
        }
        String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
        Toast.makeText(requireContext(), getString(R.string.task_feed_error, message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTrackTask(ErrandTask task) {
        openTaskTracking(task);
    }
}
