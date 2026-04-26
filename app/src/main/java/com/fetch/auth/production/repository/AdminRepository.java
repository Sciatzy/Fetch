package com.fetch.auth.production.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fetch.auth.production.model.RiderAccountItem;
import com.fetch.auth.production.model.RiderApplicationItem;
import com.fetch.auth.production.model.UserProfile;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String APPLICATION_STATUS_PENDING = "pending";
    private static final String APPLICATION_STATUS_APPROVED = "approved";
    private static final String APPLICATION_STATUS_REJECTED = "rejected";
    private static final String APPLICATION_STATUS_DEACTIVATED = "deactivated";

    private final FirebaseFirestore firestore;

    public AdminRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration listenPendingRiderApplications(
            @NonNull RiderApplicationListCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        return firestore.collection(USERS_COLLECTION)
                .whereEqualTo("application.status", APPLICATION_STATUS_PENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }
                    List<RiderApplicationItem> items = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            items.add(mapApplication(document));
                        }
                    }
                    callback.onSuccess(items);
                });
    }

    public ListenerRegistration listenApprovedRiders(
            @NonNull RiderAccountListCallback callback,
            @NonNull ErrorCallback errorCallback
    ) {
        return firestore.collection(USERS_COLLECTION)
                .whereEqualTo("role", UserProfile.ROLE_RIDER)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorCallback.onError(error);
                        return;
                    }

                    List<RiderAccountItem> items = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            String applicationStatus = safe(document.getString("application.status"));
                            Boolean verified = document.getBoolean("riderDetails.verified");
                            if (!APPLICATION_STATUS_APPROVED.equalsIgnoreCase(applicationStatus)
                                    && !Boolean.TRUE.equals(verified)) {
                                continue;
                            }
                            items.add(mapRider(document));
                        }
                    }
                    callback.onSuccess(items);
                });
    }

    public void approveRiderApplication(@NonNull String userId, @NonNull OperationCallback callback) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
        WriteBatch batch = firestore.batch();

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", UserProfile.ROLE_RIDER);
        updates.put("riderDetails.verified", true);
        updates.put("verificationStatus", "verified");
        updates.put("application.status", APPLICATION_STATUS_APPROVED);
        updates.put("application.reviewedAt", FieldValue.serverTimestamp());

        batch.set(userRef, updates, SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void rejectRiderApplication(@NonNull String userId, @NonNull OperationCallback callback) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
        WriteBatch batch = firestore.batch();

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", UserProfile.ROLE_CUSTOMER);
        updates.put("riderDetails.verified", false);
        updates.put("verificationStatus", "rejected");
        updates.put("application.status", APPLICATION_STATUS_REJECTED);
        updates.put("application.reviewedAt", FieldValue.serverTimestamp());

        batch.set(userRef, updates, SetOptions.merge());
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void deactivateRider(@NonNull String userId, @NonNull OperationCallback callback) {
        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", UserProfile.ROLE_CUSTOMER);
        updates.put("riderDetails.verified", false);
        updates.put("verificationStatus", "deactivated");
        updates.put("application.status", APPLICATION_STATUS_DEACTIVATED);
        updates.put("application.reviewedAt", FieldValue.serverTimestamp());

        userRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private RiderApplicationItem mapApplication(DocumentSnapshot document) {
        String userId = safe(document.getId());
        String name = safe(document.getString("name"));
        String email = safe(document.getString("email"));
        String vehicleType = safe(document.getString("riderDetails.vehicleType"));
        String licenseNumber = safe(document.getString("riderDetails.licenseNumber"));
        String status = safe(document.getString("application.status"));

        String governmentIdUrl = safe(document.getString("application.requirements.governmentIdUrl"));
        String selfieUrl = safe(document.getString("application.requirements.selfieUrl"));
        String requirementsLabel = "Gov ID: " + normalizeRequirement(governmentIdUrl)
                + "\nSelfie: " + normalizeRequirement(selfieUrl);

        return new RiderApplicationItem(
                userId,
                name,
                email,
                vehicleType,
                licenseNumber,
                requirementsLabel,
                status
        );
    }

    private RiderAccountItem mapRider(DocumentSnapshot document) {
        String userId = safe(document.getId());
        String name = safe(document.getString("name"));
        String email = safe(document.getString("email"));
        String vehicleType = safe(document.getString("riderDetails.vehicleType"));
        String licenseNumber = safe(document.getString("riderDetails.licenseNumber"));
        boolean active = UserProfile.ROLE_RIDER.equalsIgnoreCase(safe(document.getString("role")));

        return new RiderAccountItem(userId, name, email, vehicleType, licenseNumber, active);
    }

    private String normalizeRequirement(String value) {
        if (TextUtils.isEmpty(value)) {
            return "not uploaded";
        }
        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public interface RiderApplicationListCallback {
        void onSuccess(List<RiderApplicationItem> items);
    }

    public interface RiderAccountListCallback {
        void onSuccess(List<RiderAccountItem> items);
    }

    public interface OperationCallback {
        void onSuccess();

        void onError(Exception error);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }
}
