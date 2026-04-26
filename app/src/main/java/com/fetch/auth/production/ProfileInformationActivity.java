package com.fetch.auth.production;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileInformationActivity extends AppCompatActivity {

    private ImageView ivProfileAvatar;
    private TextView tvProfileEmail;
    private EditText etProfileName;
    private EditText etProfilePhone;
    private Button btnChangePhoto;
    private Button btnSaveProfileChanges;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private String currentUid;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Immediate preview keeps UX responsive while upload runs.
                    bindProfileImage(uri.toString());
                    uploadProfilePhoto(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_information);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfileChanges = findViewById(R.id.btnSaveProfileChanges);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        currentUid = user.getUid();
        tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.home_no_email));
        loadProfile();

        btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSaveProfileChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadProfile() {
        if (TextUtils.isEmpty(currentUid)) {
            return;
        }

        userProfileRepository.getUserProfile(currentUid, new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                String name = document.getString("name");
                String phone = document.getString("phone");
                String profileImage = document.getString("profileImage");

                etProfileName.setText(!TextUtils.isEmpty(name) ? name : "");
                etProfilePhone.setText(!TextUtils.isEmpty(phone) ? phone : "");
                bindProfileImage(profileImage);
            }

            @Override
            public void onError(Exception error) {
                bindProfileImage(null);
            }
        });
    }

    private void bindProfileImage(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            ivProfileAvatar.setImageResource(R.drawable.fetch_logo);
            return;
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.fetch_logo)
                .error(R.drawable.fetch_logo)
                .circleCrop()
                .into(ivProfileAvatar);
    }

    private void uploadProfilePhoto(Uri imageUri) {
        if (TextUtils.isEmpty(currentUid) || imageUri == null) {
            return;
        }

        btnChangePhoto.setEnabled(false);
        Toast.makeText(this, R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show();

        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        StorageReference profileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images")
                .child(currentUid)
                .child(fileName);

        profileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("profileImage", downloadUri.toString());

                            userProfileRepository.updateUserProfileFields(currentUid, updates, new UserProfileRepository.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    btnChangePhoto.setEnabled(true);
                                    bindProfileImage(downloadUri.toString());
                                    Toast.makeText(ProfileInformationActivity.this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception error) {
                                    btnChangePhoto.setEnabled(true);
                                    String details = error != null ? error.getMessage() : getString(R.string.error_unknown);
                                    Toast.makeText(ProfileInformationActivity.this, getString(R.string.profile_photo_upload_failed_detail, details), Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .addOnFailureListener(error -> {
                            btnChangePhoto.setEnabled(true);
                            String details = error.getMessage() != null ? error.getMessage() : getString(R.string.error_unknown);
                            Toast.makeText(ProfileInformationActivity.this, getString(R.string.profile_photo_upload_failed_detail, details), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(error -> {
                    btnChangePhoto.setEnabled(true);
                    String details;
                    if (error instanceof StorageException) {
                        details = "code=" + ((StorageException) error).getErrorCode();
                    } else {
                        details = error.getMessage() != null ? error.getMessage() : getString(R.string.error_unknown);
                    }
                    Toast.makeText(ProfileInformationActivity.this, getString(R.string.profile_photo_upload_failed_detail, details), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileChanges() {
        if (TextUtils.isEmpty(currentUid)) {
            return;
        }

        String name = etProfileName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etProfileName.setError(getString(R.string.error_name_required));
            etProfileName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etProfilePhone.setError(getString(R.string.error_phone_required));
            etProfilePhone.requestFocus();
            return;
        }

        btnSaveProfileChanges.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        userProfileRepository.updateUserProfileFields(currentUid, updates, new UserProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                btnSaveProfileChanges.setEnabled(true);
                Toast.makeText(ProfileInformationActivity.this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                btnSaveProfileChanges.setEnabled(true);
                String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
                Toast.makeText(ProfileInformationActivity.this, getString(R.string.profile_update_failed, message), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

