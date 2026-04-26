package com.fetch.auth.production;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.adapter.RiderApplicationAdapter;
import com.fetch.auth.production.model.RiderApplicationItem;
import com.fetch.auth.production.model.UserProfile;
import com.fetch.auth.production.repository.AdminRepository;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RiderApplicationsActivity extends AppCompatActivity implements RiderApplicationAdapter.RiderApplicationActionListener {

    private AdminRepository adminRepository;
    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private RiderApplicationAdapter adapter;

    private RecyclerView rvApplications;
    private TextView tvEmptyApplications;
    private ProgressBar progressApplications;

    private ListenerRegistration applicationsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_applications);

        adminRepository = new AdminRepository();
        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        rvApplications = findViewById(R.id.rvRiderApplications);
        tvEmptyApplications = findViewById(R.id.tvEmptyApplications);
        progressApplications = findViewById(R.id.progressApplications);
        Button btnBack = findViewById(R.id.btnBackApplications);

        adapter = new RiderApplicationAdapter(this);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        verifyAdminAndSubscribe();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (applicationsListener != null) {
            applicationsListener.remove();
            applicationsListener = null;
        }
    }

    private void verifyAdminAndSubscribe() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        userProfileRepository.getUserRole(user.getUid(), new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                if (!UserProfile.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    Toast.makeText(RiderApplicationsActivity.this, R.string.admin_access_denied, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                subscribeToApplications();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(RiderApplicationsActivity.this, R.string.error_role_check_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void subscribeToApplications() {
        progressApplications.setVisibility(View.VISIBLE);
        tvEmptyApplications.setVisibility(View.GONE);
        rvApplications.setVisibility(View.GONE);

        if (applicationsListener != null) {
            applicationsListener.remove();
            applicationsListener = null;
        }

        applicationsListener = adminRepository.listenPendingRiderApplications(
                this::renderApplications,
                error -> {
                    progressApplications.setVisibility(View.GONE);
                    Toast.makeText(
                            RiderApplicationsActivity.this,
                            getString(R.string.admin_applications_load_failed, error != null ? error.getMessage() : getString(R.string.error_unknown)),
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void renderApplications(List<RiderApplicationItem> items) {
        progressApplications.setVisibility(View.GONE);
        adapter.submitList(items);

        boolean isEmpty = items == null || items.isEmpty();
        tvEmptyApplications.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvApplications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onApprove(RiderApplicationItem item) {
        if (item == null) {
            return;
        }
        adminRepository.approveRiderApplication(item.getUserId(), new AdminRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RiderApplicationsActivity.this, R.string.admin_application_approved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(
                        RiderApplicationsActivity.this,
                        getString(R.string.admin_action_failed, error != null ? error.getMessage() : getString(R.string.error_unknown)),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    public void onReject(RiderApplicationItem item) {
        if (item == null) {
            return;
        }
        adminRepository.rejectRiderApplication(item.getUserId(), new AdminRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RiderApplicationsActivity.this, R.string.admin_application_rejected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(
                        RiderApplicationsActivity.this,
                        getString(R.string.admin_action_failed, error != null ? error.getMessage() : getString(R.string.error_unknown)),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
