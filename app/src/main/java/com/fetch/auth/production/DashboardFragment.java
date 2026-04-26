package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class DashboardFragment extends Fragment {

    private TextView tvHomeName;
    private TextView tvHomePrimaryHint;
    private Button btnPrimaryAction;
    private ImageView ivHomeAvatar;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private String currentRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        tvHomeName = view.findViewById(R.id.tvHomeName);
        tvHomePrimaryHint = view.findViewById(R.id.tvHomePrimaryHint);
        btnPrimaryAction = view.findViewById(R.id.btnPrimaryAction);
        ivHomeAvatar = view.findViewById(R.id.ivHomeAvatar);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            loadDashboardData(user);
        }

        btnPrimaryAction.setOnClickListener(v -> openPrimaryTaskAction());

        return view;
    }

    private void loadDashboardData(FirebaseUser user) {
        userProfileRepository.getUserProfile(user.getUid(), new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (!isAdded()) return;

                String name = document.getString("name");
                String role = document.getString("role");
                String profileImage = document.getString("profileImage");

                tvHomeName.setText(!TextUtils.isEmpty(name)
                        ? getString(R.string.home_hello_name, name)
                        : getString(R.string.home_hello));
                currentRole = !TextUtils.isEmpty(role) ? role : getString(R.string.home_role_unknown);
                bindPrimaryActionByRole(currentRole);
                bindAvatar(profileImage);
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                tvHomeName.setText(R.string.home_hello);
                bindPrimaryActionByRole(null);
                bindAvatar(null);
            }
        });
    }

    private void bindAvatar(String profileImageUrl) {
        if (!isAdded()) return;

        if (TextUtils.isEmpty(profileImageUrl)) {
            ivHomeAvatar.setImageResource(R.drawable.fetch_logo);
            return;
        }

        Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.fetch_logo)
                .error(R.drawable.fetch_logo)
                .circleCrop()
                .into(ivHomeAvatar);
    }

    private void bindPrimaryActionByRole(String role) {
        if (role == null) {
            btnPrimaryAction.setEnabled(false);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
            return;
        }

        if ("customer".equalsIgnoreCase(role)) {
            btnPrimaryAction.setEnabled(true);
            btnPrimaryAction.setText(R.string.create_task_cta);
            tvHomePrimaryHint.setText(R.string.home_primary_hint_customer);
        } else if ("rider".equalsIgnoreCase(role)) {
            btnPrimaryAction.setEnabled(true);
            btnPrimaryAction.setText(R.string.view_pending_tasks_cta);
            tvHomePrimaryHint.setText(R.string.home_primary_hint_rider);
        } else {
            btnPrimaryAction.setEnabled(false);
            btnPrimaryAction.setText(R.string.open_tasks);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
        }
    }

    private void openPrimaryTaskAction() {
        if (currentRole == null) {
            Toast.makeText(requireContext(), R.string.error_role_unknown, Toast.LENGTH_SHORT).show();
            return;
        }

        if ("customer".equalsIgnoreCase(currentRole)) {
            startActivity(new Intent(requireContext(), MapTaskComposerActivity.class));
        } else if ("rider".equalsIgnoreCase(currentRole)) {
            startActivity(new Intent(requireContext(), RiderTaskFeedActivity.class));
        } else {
            Toast.makeText(requireContext(), R.string.error_role_unknown, Toast.LENGTH_SHORT).show();
        }
    }
}

