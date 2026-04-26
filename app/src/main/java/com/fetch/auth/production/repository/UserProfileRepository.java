package com.fetch.auth.production.repository;

import android.text.TextUtils;

import com.fetch.auth.production.model.UserProfile;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;
import java.util.HashMap;

public class UserProfileRepository {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore firestore;

    public UserProfileRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void isProfileComplete(String uid, ProfileStatusCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(isDocumentComplete(document)))
                .addOnFailureListener(callback::onError);
    }

    public void saveUserProfile(String uid, Map<String, Object> userData, OperationCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getUserRole(String uid, UserRoleCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(document -> callback.onSuccess(document.getString("role")))
                .addOnFailureListener(callback::onError);
    }

    public void updateFcmToken(String uid, String fcmToken, OperationCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", fcmToken);

        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(update)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void updateUserProfileFields(String uid, Map<String, Object> updates, OperationCallback callback) {
        if (updates == null || updates.isEmpty()) {
            callback.onSuccess();
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void getUserProfile(String uid, UserProfileDataCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(document);
                    } else {
                        callback.onError(new Exception("User profile not found."));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private boolean isDocumentComplete(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return false;
        }

        String phone = document.getString("phone");
        String role = document.getString("role");
        if (UserProfile.ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }
        return !TextUtils.isEmpty(phone) && !TextUtils.isEmpty(role);
    }

    public interface ProfileStatusCallback {
        void onSuccess(boolean isComplete);
        void onError(Exception error);
    }

    public interface UserRoleCallback {
        void onSuccess(String role);
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public interface UserProfileDataCallback {
        void onSuccess(DocumentSnapshot document);
        void onError(Exception error);
    }
}
