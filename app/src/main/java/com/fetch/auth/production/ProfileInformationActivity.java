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

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.fetch.auth.production.util.ImageDataUriUtil;
import com.fetch.auth.production.util.StorageBackedImageLoader;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

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

        btnChangePhoto.setOnClickListener(v -> showAvatarOptionsDialog());
        btnSaveProfileChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void showAvatarOptionsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        String[] options = {"Upload Photo", "Choose Default Emoji"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                pickImageLauncher.launch("image/*");
            } else {
                showEmojiPicker();
            }
        });
        builder.show();
    }

    private void showEmojiPicker() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Emoji");
        String[] emojis = {"🐶", "🐱", "🐼", "🐻", "🦊", "🦁", "🐰", "🐸", "🚀", "⚡", "⭐", "🥑"};
        builder.setItems(emojis, (dialog, which) -> {
            String selectedEmoji = emojis[which];
            uploadEmojiAsPhoto(selectedEmoji);
        });
        builder.show();
    }

    private void uploadEmojiAsPhoto(String emoji) {
        if (TextUtils.isEmpty(currentUid)) return;
        btnChangePhoto.setEnabled(false);
        Toast.makeText(this, "Generating avatar...", Toast.LENGTH_SHORT).show();

        // Create a bitmap from emoji
        int size = 300;
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        
        android.graphics.Paint bgPaint = new android.graphics.Paint();
        bgPaint.setColor(android.graphics.Color.parseColor("#44000000")); // Darkish transparent or a solid color
        bgPaint.setAntiAlias(true);
        canvas.drawCircle(size/2f, size/2f, size/2f, bgPaint);

        android.graphics.Paint textPaint = new android.graphics.Paint();
        textPaint.setTextSize(size * 0.6f);
        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        
        android.graphics.Paint.FontMetrics fm = textPaint.getFontMetrics();
        float x = size / 2f;
        float y = size / 2f - (fm.descent + fm.ascent) / 2f;
        canvas.drawText(emoji, x, y, textPaint);

        String imageDataUri = ImageDataUriUtil.toPngDataUri(bitmap);
        if (TextUtils.isEmpty(imageDataUri)) {
            btnChangePhoto.setEnabled(true);
            Toast.makeText(ProfileInformationActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImage", imageDataUri);
        userProfileRepository.updateUserProfileFields(currentUid, updates, new UserProfileRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                btnChangePhoto.setEnabled(true);
                bindProfileImage(imageDataUri);
                Toast.makeText(ProfileInformationActivity.this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                btnChangePhoto.setEnabled(true);
                Toast.makeText(ProfileInformationActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
            }
        });
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
                if (TextUtils.isEmpty(profileImage)) {
                    profileImage = document.getString("profileImageUrl");
                }

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
        StorageBackedImageLoader.load(ivProfileAvatar, imageUrl, R.drawable.fetch_logo, true);
    }

    private void uploadProfilePhoto(Uri imageUri) {
        if (TextUtils.isEmpty(currentUid) || imageUri == null) {
            return;
        }

        btnChangePhoto.setEnabled(false);
        Toast.makeText(this, R.string.profile_photo_uploading, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String imageDataUri = ImageDataUriUtil.toJpegDataUri(this, imageUri, 960, 40);
            runOnUiThread(() -> {
                if (TextUtils.isEmpty(imageDataUri)) {
                    btnChangePhoto.setEnabled(true);
                    Toast.makeText(ProfileInformationActivity.this,
                            getString(R.string.profile_photo_upload_failed_detail, getString(R.string.error_unknown)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("profileImage", imageDataUri);
                userProfileRepository.updateUserProfileFields(currentUid, updates, new UserProfileRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        btnChangePhoto.setEnabled(true);
                        bindProfileImage(imageDataUri);
                        Toast.makeText(ProfileInformationActivity.this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception error) {
                        btnChangePhoto.setEnabled(true);
                        String details = error != null ? error.getMessage() : getString(R.string.error_unknown);
                        Toast.makeText(ProfileInformationActivity.this, getString(R.string.profile_photo_upload_failed_detail, details), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }).start();
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
