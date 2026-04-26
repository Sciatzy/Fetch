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

import com.fetch.auth.production.adapter.RiderAccountAdapter;
import com.fetch.auth.production.model.RiderAccountItem;
import com.fetch.auth.production.model.UserProfile;
import com.fetch.auth.production.repository.AdminRepository;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class RiderListActivity extends AppCompatActivity implements RiderAccountAdapter.RiderActionListener {

    private AdminRepository adminRepository;
    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private RiderAccountAdapter adapter;

    private RecyclerView rvRiders;
    private TextView tvEmptyRiders;
    private ProgressBar progressRiders;

    private ListenerRegistration ridersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_list);

        adminRepository = new AdminRepository();
        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        rvRiders = findViewById(R.id.rvRiders);
        tvEmptyRiders = findViewById(R.id.tvEmptyRiders);
        progressRiders = findViewById(R.id.progressRiders);
        Button btnBack = findViewById(R.id.btnBackRiderList);

        adapter = new RiderAccountAdapter(this);
        rvRiders.setLayoutManager(new LinearLayoutManager(this));
        rvRiders.setAdapter(adapter);

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
        if (ridersListener != null) {
            ridersListener.remove();
            ridersListener = null;
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
                    Toast.makeText(RiderListActivity.this, R.string.admin_access_denied, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                subscribeToRiders();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(RiderListActivity.this, R.string.error_role_check_failed, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void subscribeToRiders() {
        progressRiders.setVisibility(View.VISIBLE);
        tvEmptyRiders.setVisibility(View.GONE);
        rvRiders.setVisibility(View.GONE);

        if (ridersListener != null) {
            ridersListener.remove();
            ridersListener = null;
        }

        ridersListener = adminRepository.listenApprovedRiders(
                this::renderRiders,
                error -> {
                    progressRiders.setVisibility(View.GONE);
                    Toast.makeText(
                            RiderListActivity.this,
                            getString(R.string.admin_riders_load_failed, error != null ? error.getMessage() : getString(R.string.error_unknown)),
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void renderRiders(List<RiderAccountItem> items) {
        progressRiders.setVisibility(View.GONE);
        adapter.submitList(items);

        boolean isEmpty = items == null || items.isEmpty();
        tvEmptyRiders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvRiders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDeactivate(RiderAccountItem item) {
        if (item == null) {
            return;
        }

        adminRepository.deactivateRider(item.getUserId(), new AdminRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RiderListActivity.this, R.string.admin_rider_deactivated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(
                        RiderListActivity.this,
                        getString(R.string.admin_action_failed, error != null ? error.getMessage() : getString(R.string.error_unknown)),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}
