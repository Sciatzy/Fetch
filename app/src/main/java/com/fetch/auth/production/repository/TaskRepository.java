package com.fetch.auth.production.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fetch.auth.production.model.ErrandTask;
import com.fetch.auth.production.model.TaskFields;
import com.fetch.auth.production.model.TaskStatus;
import com.fetch.auth.production.validation.RiderTaskFlowValidator;
import com.fetch.auth.production.validation.TaskValidator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {

    private static final String TASKS_COLLECTION = "tasks";
    private static final long MAX_FEED_RESULTS = 300;

    private final FirebaseFirestore firestore;
    private final NotificationRepository notificationRepository;

    public TaskRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository();
    }

    public void createTask(
            @NonNull String title,
            @NonNull String description,
            Double budget,
            @NonNull String customerId,
            @NonNull TaskCreateCallback callback
    ) {
        String normalizedTitle = TaskValidator.normalizeText(title);
        String normalizedDescription = TaskValidator.normalizeText(description);

        if (TextUtils.isEmpty(customerId)) {
            callback.onError(new IllegalArgumentException("Customer ID is required."));
            return;
        }
        if (!TaskValidator.isValidTitle(normalizedTitle)) {
            callback.onError(new IllegalArgumentException("Task title is invalid."));
            return;
        }
        if (!TaskValidator.isValidDescription(normalizedDescription)) {
            callback.onError(new IllegalArgumentException("Task description is invalid."));
            return;
        }
        if (!TaskValidator.isValidBudget(budget)) {
            callback.onError(new IllegalArgumentException("Budget must be zero or positive."));
            return;
        }

        DocumentReference docRef = firestore.collection(TASKS_COLLECTION).document();

        Map<String, Object> location = new HashMap<>();
        location.put(TaskFields.LAT, 0.0);
        location.put(TaskFields.LNG, 0.0);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put(TaskFields.TASK_ID, docRef.getId());
        taskData.put(TaskFields.TITLE, normalizedTitle);
        taskData.put(TaskFields.DESCRIPTION, normalizedDescription);
        taskData.put(TaskFields.BUDGET, budget);
        taskData.put(TaskFields.STATUS, TaskStatus.PENDING);
        taskData.put(TaskFields.CUSTOMER_ID, customerId);
        taskData.put(TaskFields.RIDER_ID, null);
        taskData.put(TaskFields.CREATED_AT, FieldValue.serverTimestamp());
        taskData.put(TaskFields.LOCATION, location);

        docRef.set(taskData)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(docRef.getId());
                    dispatchTaskStatusPush(docRef.getId(), TaskStatus.PENDING, false);
                })
                .addOnFailureListener(callback::onError);
    }

    public void createTaskWithRouteData(
            @NonNull String title,
            @NonNull String description,
            Double budget,
            @NonNull String customerId,
            @NonNull Map<String, Object> pickup,
            @NonNull Map<String, Object> dropoff,
            double distanceMeters,
            double estimatedFee,
            @NonNull TaskCreateCallback callback
    ) {
        String normalizedTitle = TaskValidator.normalizeText(title);
        String normalizedDescription = TaskValidator.normalizeText(description);

        if (TextUtils.isEmpty(customerId)) {
            callback.onError(new IllegalArgumentException("Customer ID is required."));
            return;
        }
        if (!TaskValidator.isValidTitle(normalizedTitle)) {
            callback.onError(new IllegalArgumentException("Task title is invalid."));
            return;
        }
        if (!TaskValidator.isValidDescription(normalizedDescription)) {
            callback.onError(new IllegalArgumentException("Task description is invalid."));
            return;
        }
        if (!TaskValidator.isValidBudget(budget)) {
            callback.onError(new IllegalArgumentException("Budget must be zero or positive."));
            return;
        }

        DocumentReference docRef = firestore.collection(TASKS_COLLECTION).document();

        double safeDistance = Math.max(0d, distanceMeters);
        double safeEstimatedFee = Math.max(0d, estimatedFee);
        Double resolvedBudget = budget != null ? budget : (safeEstimatedFee > 0 ? safeEstimatedFee : null);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put(TaskFields.TASK_ID, docRef.getId());
        taskData.put(TaskFields.TITLE, normalizedTitle);
        taskData.put(TaskFields.DESCRIPTION, normalizedDescription);
        taskData.put(TaskFields.BUDGET, resolvedBudget);
        taskData.put(TaskFields.STATUS, TaskStatus.PENDING);
        taskData.put(TaskFields.CUSTOMER_ID, customerId);
        taskData.put(TaskFields.RIDER_ID, null);
        taskData.put(TaskFields.CREATED_AT, FieldValue.serverTimestamp());
        taskData.put(TaskFields.PICKUP, pickup);
        taskData.put(TaskFields.DROPOFF, dropoff);
        taskData.put(TaskFields.DISTANCE_METERS, safeDistance);
        taskData.put(TaskFields.ESTIMATED_FEE, safeEstimatedFee);
        taskData.put(TaskFields.CURRENCY, "PHP");
        taskData.put(TaskFields.PAYMENT_STATUS, "pending");
        taskData.put(TaskFields.PAYMENT_PROVIDER, "paymongo");
        taskData.put(TaskFields.COLLECTABLE_AMOUNT, safeEstimatedFee);
        taskData.put(TaskFields.COLLECTION_STATUS, "not_collected");
        taskData.put(TaskFields.GEOFENCE_STATE, "not_started");

        docRef.set(taskData)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(docRef.getId());
                    dispatchTaskStatusPush(docRef.getId(), TaskStatus.PENDING, false);
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenToPendingTasks(
            @NonNull TaskListCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        // Keep query simple to avoid requiring composite indexes for the MVP feed.
        return firestore.collection(TASKS_COLLECTION)
                .whereEqualTo(TaskFields.STATUS, TaskStatus.PENDING)
            .limit(MAX_FEED_RESULTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }

                    List<ErrandTask> tasks = mapTasks(value != null ? value.getDocuments() : null);
                    sortByCreatedAtDesc(tasks);
                    callback.onTasksUpdated(tasks);
                });
    }

    public ListenerRegistration listenToCustomerTasks(
            @NonNull String customerId,
            @NonNull TaskListCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        return firestore.collection(TASKS_COLLECTION)
                .whereEqualTo(TaskFields.CUSTOMER_ID, customerId)
            .limit(MAX_FEED_RESULTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }

                    List<ErrandTask> tasks = mapTasks(value != null ? value.getDocuments() : null);
                    sortByCreatedAtDesc(tasks);
                    callback.onTasksUpdated(tasks);
                });
    }

    public ListenerRegistration listenToRiderTasks(
            @NonNull String riderId,
            @NonNull TaskListCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        return firestore.collection(TASKS_COLLECTION)
                .whereEqualTo(TaskFields.RIDER_ID, riderId)
            .limit(MAX_FEED_RESULTS)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }

                    List<ErrandTask> tasks = mapTasks(value != null ? value.getDocuments() : null);
                    sortByCreatedAtDesc(tasks);
                    callback.onTasksUpdated(tasks);
                });
    }

    public ListenerRegistration listenToTask(
            @NonNull String taskId,
            @NonNull TaskDocumentCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        return firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }
                    callback.onTaskUpdated(snapshot);
                });
    }

    public void acceptTask(
            @NonNull String taskId,
            @NonNull String riderId,
            @NonNull OperationCallback callback
    ) {
        DocumentReference taskRef = firestore.collection(TASKS_COLLECTION).document(taskId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("Task does not exist.");
            }

            String currentStatus = snapshot.getString(TaskFields.STATUS);
            String currentRiderId = snapshot.getString(TaskFields.RIDER_ID);

            if (TaskStatus.ACCEPTED.equals(currentStatus) && riderId.equals(currentRiderId)) {
                return null;
            }
            if (!TaskStatus.PENDING.equals(currentStatus)) {
                throw new IllegalStateException("Task is no longer available.");
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(TaskFields.STATUS, TaskStatus.ACCEPTED);
            updates.put(TaskFields.RIDER_ID, riderId);
            updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());
            transaction.update(taskRef, updates);

            return null;
        }).addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, TaskStatus.ACCEPTED, true);
                })
                .addOnFailureListener(callback::onError);
    }

    public void markArrivedAtPickup(
            @NonNull String taskId,
            @NonNull String riderId,
            @NonNull OperationCallback callback
    ) {
        DocumentReference taskRef = firestore.collection(TASKS_COLLECTION).document(taskId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("Task does not exist.");
            }

            String currentStatus = snapshot.getString(TaskFields.STATUS);
            String assignedRiderId = snapshot.getString(TaskFields.RIDER_ID);
            String geofenceState = snapshot.getString(TaskFields.GEOFENCE_STATE);
            RiderTaskFlowValidator.validateArrivedAtPickup(currentStatus, assignedRiderId, riderId, geofenceState);

            Map<String, Object> updates = new HashMap<>();
            updates.put(TaskFields.STATUS, TaskStatus.ARRIVED_PICKUP);
            updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());
            transaction.update(taskRef, updates);
            return null;
        }).addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, TaskStatus.ARRIVED_PICKUP, true);
                })
                .addOnFailureListener(callback::onError);
    }

    public void markTaskCompleted(
            @NonNull String taskId,
            @NonNull String riderId,
            @NonNull OperationCallback callback
    ) {
        DocumentReference taskRef = firestore.collection(TASKS_COLLECTION).document(taskId);
        DocumentReference riderRef = firestore.collection("users").document(riderId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("Task does not exist.");
            }

            String currentStatus = snapshot.getString(TaskFields.STATUS);
            String assignedRiderId = snapshot.getString(TaskFields.RIDER_ID);
            RiderTaskFlowValidator.validateCompletion(currentStatus, assignedRiderId, riderId);

            Map<String, Object> updates = new HashMap<>();
            updates.put(TaskFields.STATUS, TaskStatus.COMPLETED);
            updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());
            transaction.update(taskRef, updates);

            Double estimatedFee = snapshot.getDouble(TaskFields.ESTIMATED_FEE);
            Double budget = snapshot.getDouble(TaskFields.BUDGET);
            double earnings = RiderTaskFlowValidator.resolveCompletionEarnings(estimatedFee, budget);

            Map<String, Object> riderUpdates = new HashMap<>();
            riderUpdates.put("totalEarnings", FieldValue.increment(earnings));
            riderUpdates.put("overallAcceptedTasks", FieldValue.increment(1));
            transaction.set(riderRef, riderUpdates, com.google.firebase.firestore.SetOptions.merge());

            return null;
        }).addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, TaskStatus.COMPLETED, true);
                })
                .addOnFailureListener(callback::onError);
    }

    public void startTripToDropoff(
            @NonNull String taskId,
            @NonNull String riderId,
            @NonNull OperationCallback callback
    ) {
        DocumentReference taskRef = firestore.collection(TASKS_COLLECTION).document(taskId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("Task does not exist.");
            }

            String currentStatus = snapshot.getString(TaskFields.STATUS);
            String assignedRiderId = snapshot.getString(TaskFields.RIDER_ID);
            RiderTaskFlowValidator.validateStartTrip(currentStatus, assignedRiderId, riderId);

            Map<String, Object> updates = new HashMap<>();
            updates.put(TaskFields.STATUS, TaskStatus.IN_PROGRESS);
            updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());
            transaction.update(taskRef, updates);
            return null;
        }).addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, TaskStatus.IN_PROGRESS, true);
                })
                .addOnFailureListener(callback::onError);
    }

    public void markArrivedAtDropoff(
            @NonNull String taskId,
            @NonNull String riderId,
            @NonNull OperationCallback callback
    ) {
        DocumentReference taskRef = firestore.collection(TASKS_COLLECTION).document(taskId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);

            if (!snapshot.exists()) {
                throw new IllegalStateException("Task does not exist.");
            }

            String currentStatus = snapshot.getString(TaskFields.STATUS);
            String assignedRiderId = snapshot.getString(TaskFields.RIDER_ID);
            RiderTaskFlowValidator.validateArrivedAtDropoff(currentStatus, assignedRiderId, riderId);

            Map<String, Object> updates = new HashMap<>();
            updates.put(TaskFields.STATUS, TaskStatus.ARRIVED_DROPOFF);
            updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());
            transaction.update(taskRef, updates);
            return null;
        }).addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, TaskStatus.ARRIVED_DROPOFF, true);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateGeofenceState(
            @NonNull String taskId,
            @NonNull String geofenceState,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(TaskFields.GEOFENCE_STATE, geofenceState);
        updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());

        firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void updatePaymentMetadata(
            @NonNull String taskId,
            @NonNull String checkoutId,
            @NonNull String paymentStatus,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(TaskFields.PAYMENT_CHECKOUT_ID, checkoutId);
        updates.put(TaskFields.PAYMENT_STATUS, paymentStatus);

        firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, paymentStatus, false);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateTaskStatus(
            @NonNull String taskId,
            @NonNull String status,
            @NonNull OperationCallback callback
    ) {
        updateTaskStatus(taskId, status, false, callback);
    }

    public void updateTaskStatus(
            @NonNull String taskId,
            @NonNull String status,
            boolean isRiderActor,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(TaskFields.STATUS, status);
        updates.put(TaskFields.LAST_STATUS_UPDATED_AT, FieldValue.serverTimestamp());

        firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, status, isRiderActor);
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateRiderLiveLocation(
            @NonNull String taskId,
            @NonNull String riderId,
            double lat,
            double lng,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> riderLocation = new HashMap<>();
        riderLocation.put(TaskFields.LAT, lat);
        riderLocation.put(TaskFields.LNG, lng);

        Map<String, Object> updates = new HashMap<>();
        updates.put(TaskFields.RIDER_ID, riderId);
        updates.put(TaskFields.RIDER_LOCATION, riderLocation);
        updates.put(TaskFields.RIDER_LOCATION_UPDATED_AT, FieldValue.serverTimestamp());

        firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private void dispatchTaskStatusPush(@NonNull String taskId, @NonNull String status, boolean isRiderActor) {
        notificationRepository.sendTaskStatusPush(taskId, status, isRiderActor);
    }

    public void updateCollection(
            @NonNull String taskId,
            double amount,
            @NonNull String paymentMethod,
            @NonNull String collectionStatus,
            @NonNull OperationCallback callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(TaskFields.COLLECTABLE_AMOUNT, amount);
        updates.put(TaskFields.PAYMENT_METHOD, paymentMethod);
        updates.put(TaskFields.COLLECTION_STATUS, collectionStatus);
        if ("collected".equalsIgnoreCase(collectionStatus)) {
            updates.put(TaskFields.COLLECTED_AT, FieldValue.serverTimestamp());
            updates.put(TaskFields.PAYMENT_STATUS, "paid");
        }

        firestore.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess();
                    dispatchTaskStatusPush(taskId, collectionStatus, true);
                })
                .addOnFailureListener(callback::onError);
    }

    private List<ErrandTask> mapTasks(List<DocumentSnapshot> documents) {
        List<ErrandTask> tasks = new ArrayList<>();
        if (documents == null) {
            return tasks;
        }

        for (DocumentSnapshot document : documents) {
            ErrandTask task = ErrandTask.fromDocument(document);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    private void sortByCreatedAtDesc(List<ErrandTask> tasks) {
        Collections.sort(tasks, new Comparator<ErrandTask>() {
            @Override
            public int compare(ErrandTask first, ErrandTask second) {
                if (first.getCreatedAt() == null && second.getCreatedAt() == null) {
                    return 0;
                }
                if (first.getCreatedAt() == null) {
                    return 1;
                }
                if (second.getCreatedAt() == null) {
                    return -1;
                }
                return second.getCreatedAt().compareTo(first.getCreatedAt());
            }
        });
    }

    public interface TaskCreateCallback {
        void onSuccess(String taskId);

        void onError(Exception error);
    }

    public interface TaskListCallback {
        void onTasksUpdated(List<ErrandTask> tasks);
    }

    public interface TaskDocumentCallback {
        void onTaskUpdated(DocumentSnapshot snapshot);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();

        void onError(Exception error);
    }
}
