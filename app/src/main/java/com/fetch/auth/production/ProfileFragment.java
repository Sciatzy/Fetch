package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.fetch.auth.production.util.StorageBackedImageLoader;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileFragment extends Fragment {

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        ImageView ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        LinearLayout btnProfileInformation = view.findViewById(R.id.btnProfileInformation);
        LinearLayout btnPersonalInfo = view.findViewById(R.id.btnPersonalInfo);
        LinearLayout btnVerification = view.findViewById(R.id.btnVerification);
        LinearLayout btnPaymentMethods = view.findViewById(R.id.btnPaymentMethods);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.home_no_email));

            userProfileRepository.getUserProfile(user.getUid(), new UserProfileRepository.UserProfileDataCallback() {
                @Override
                public void onSuccess(DocumentSnapshot document) {
                    if (!isAdded()) return;
                    String name = document.getString("name");
                    String profileImage = document.getString("profileImage");
                    if (TextUtils.isEmpty(profileImage)) {
                        profileImage = document.getString("profileImageUrl");
                    }

                    tvProfileName.setText(!TextUtils.isEmpty(name) ? name : getString(R.string.profile_name_fallback));
                    bindAvatar(ivProfileAvatar, profileImage);
                }

                @Override
                public void onError(Exception error) {
                    if (!isAdded()) return;
                    tvProfileName.setText(getString(R.string.profile_name_fallback));
                    bindAvatar(ivProfileAvatar, null);
                }
            });
        }

        btnProfileInformation.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileInformationActivity.class)));
        btnPersonalInfo.setOnClickListener(v -> startActivity(new Intent(requireContext(), PersonalInfoActivity.class)));
        btnVerification.setOnClickListener(v -> startActivity(new Intent(requireContext(), VerificationActivity.class)));

        btnPaymentMethods.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.payment_methods_coming_soon, Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            btnLogout.setEnabled(false);
            authRepository.signOutAllProviders(requireContext(), new AuthRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), R.string.logged_out, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onError(Exception error) {
                    if (!isAdded()) return;
                    btnLogout.setEnabled(true);
                    String details = error != null ? error.getMessage() : getString(R.string.error_unknown);
                    Toast.makeText(requireContext(), getString(R.string.logout_failed, details), Toast.LENGTH_LONG).show();
                }
            });
        });

        return view;
    }

    private void bindAvatar(ImageView imageView, String imageUrl) {
        if (!isAdded()) return;
        StorageBackedImageLoader.load(imageView, imageUrl, R.drawable.fetch_logo, true);
    }
}
