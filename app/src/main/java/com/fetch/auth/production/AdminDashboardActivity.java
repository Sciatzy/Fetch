package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fetch.auth.production.model.UserProfile;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;

public class AdminDashboardActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        TextView tvAdminEmail = findViewById(R.id.tvAdminEmail);
        View btnViewApplications = findViewById(R.id.btnViewApplications);
        View btnViewRiders = findViewById(R.id.btnViewRiders);
        View btnAddRider = findViewById(R.id.btnAddRider);
        Button btnAdminLogout = findViewById(R.id.btnAdminLogout);

        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            routeToLogin();
            return;
        }

        tvAdminEmail.setText(TextUtils.isEmpty(currentUser.getEmail())
                ? getString(R.string.not_available)
                : currentUser.getEmail());

        userProfileRepository.getUserRole(currentUser.getUid(), new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                if (!UserProfile.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(AdminDashboardActivity.this, R.string.admin_access_denied, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                btnViewApplications.setOnClickListener(v ->
                        startActivity(new Intent(AdminDashboardActivity.this, RiderApplicationsActivity.class)));

                btnViewRiders.setOnClickListener(v ->
                        startActivity(new Intent(AdminDashboardActivity.this, RiderListActivity.class)));

                btnAddRider.setOnClickListener(v ->
                        startActivity(new Intent(AdminDashboardActivity.this, AdminAddRiderActivity.class)));

                btnAdminLogout.setOnClickListener(v -> {
                    authRepository.signOut();
                    routeToLogin();
                });
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(AdminDashboardActivity.this, R.string.error_role_check_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void routeToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
