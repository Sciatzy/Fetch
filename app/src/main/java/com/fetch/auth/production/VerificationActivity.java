package com.fetch.auth.production;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;

    private TextView tvVerificationStatus;
    private TextView tvVerificationRole;
    private TextView tvVerificationNote;
    private Button btnSubmitVerification;

    private String currentUid;
    private String currentStatus = "not_submitted";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvVerificationRole = findViewById(R.id.tvVerificationRole);
        tvVerificationNote = findViewById(R.id.tvVerificationNote);
        btnSubmitVerification = findViewById(R.id.btnSubmitVerification);
        Button btnBack = findViewById(R.id.btnBackVerification);

        btnBack.setOnClickListener(v -> finish());
        btnSubmitVerification.setOnClickListener(v -> submitVerificationRequest());

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUid = user.getUid();
        loadVerificationStatus();
    }

    private void loadVerificationStatus() {
        userProfileRepository.getUserProfile(currentUid, new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                String role = safeValue(document.getString("role"));
                String status = safeValue(document.getString("verificationStatus"));
                Boolean riderVerified = document.getBoolean("riderDetails.verified");

                if (TextUtils.isEmpty(status)) {
                    status = "not_submitted";
                }
                if (Boolean.TRUE.equals(riderVerified)) {
                    status = "verified";
                }

                currentStatus = status;
                tvVerificationRole.setText(formatRole(role));
                tvVerificationStatus.setText(formatStatus(status));
                tvVerificationNote.setText(buildStatusNote(role, status));
                btnSubmitVerification.setEnabled(!"pending".equals(status) && !"verified".equals(status));
            }

            @Override
            public void onError(Exception error) {
                String details = error != null ? error.getMessage() : getString(R.string.error_unknown);
                Toast.makeText(VerificationActivity.this, getString(R.string.verification_load_failed, details), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitVerificationRequest() {
        if (TextUtils.isEmpty(currentUid)) {
            return;
        }
        if ("pending".equals(currentStatus) || "verified".equals(currentStatus)) {
            return;
        }

        btnSubmitVerification.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("verificationStatus", "pending");
        updates.put("verificationSubmittedAt", FieldValue.serverTimestamp());
        updates.put("application.status", "pending");
        updates.put("application.submittedAt", FieldValue.serverTimestamp());

        userProfileRepository.updateUserProfileFields(currentUid, updates, new UserProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(VerificationActivity.this, R.string.verification_submit_success, Toast.LENGTH_SHORT).show();
                loadVerificationStatus();
            }

            @Override
            public void onError(Exception error) {
                btnSubmitVerification.setEnabled(true);
                String details = error != null ? error.getMessage() : getString(R.string.error_unknown);
                Toast.makeText(VerificationActivity.this, getString(R.string.verification_submit_failed, details), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return getString(R.string.not_available);
        }
        return role.substring(0, 1).toUpperCase(Locale.US) + role.substring(1);
    }

    private String formatStatus(String status) {
        switch (status) {
            case "pending":
                return getString(R.string.verification_status_pending);
            case "verified":
                return getString(R.string.verification_status_verified);
            case "rejected":
                return getString(R.string.verification_status_rejected);
            default:
                return getString(R.string.verification_status_not_submitted);
        }
    }

    private String buildStatusNote(String role, String status) {
        boolean isRider = "rider".equalsIgnoreCase(role);
        if ("verified".equals(status)) {
            return getString(R.string.verification_note_verified);
        }
        if ("pending".equals(status)) {
            return getString(R.string.verification_note_pending);
        }
        if ("rejected".equals(status)) {
            return getString(R.string.verification_note_rejected);
        }
        return isRider
                ? getString(R.string.verification_note_rider_required)
                : getString(R.string.verification_note_customer_optional);
    }
}

