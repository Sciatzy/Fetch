package com.fetch.auth.production.pasabuy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.fetch.auth.production.R;
import com.fetch.auth.production.util.StorageBackedImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddPasaBuyPostActivity extends AppCompatActivity {

    private ImageView btnBack, ivAuthorAvatar, ivSelectedImage;
    private TextView tvAuthorName;
    private EditText etCaption;
    private Button btnPost;
    private LinearLayout llImageHints;
    private View btnGallery, btnCamera;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String riderName = "Unknown Rider";
    private String riderAvatarUrl = "";
    
    private Uri selectedImageUri = null;
    private Uri photoURI = null; // for camera
    private String currentPhotoPath = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pasabuy_post);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        btnPost = findViewById(R.id.btnPost);
        ivAuthorAvatar = findViewById(R.id.ivAuthorAvatar);
        tvAuthorName = findViewById(R.id.tvAuthorName);
        etCaption = findViewById(R.id.etCaption);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        llImageHints = findViewById(R.id.llImageHints);
        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);

        btnBack.setOnClickListener(v -> finish());
        
        btnGallery.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> openCamera());
        btnPost.setOnClickListener(v -> uploadPost());

        loadRiderInfo();
    }

    private void loadRiderInfo() {
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        riderName = doc.getString("fullName");
                        if (riderName == null) riderName = doc.getString("name");
                        tvAuthorName.setText(riderName);
                        
                        riderAvatarUrl = doc.getString("profileImage");
                        if (riderAvatarUrl == null || riderAvatarUrl.isEmpty()) {
                            riderAvatarUrl = doc.getString("profileImageUrl");
                        }
                        if (riderAvatarUrl != null && !riderAvatarUrl.isEmpty()) {
                            StorageBackedImageLoader.load(ivAuthorAvatar, riderAvatarUrl, R.drawable.fetch_logo, true);
                        } else {
                            ivAuthorAvatar.setImageResource(R.drawable.fetch_logo);
                        }
                    }
                });
        }
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    displaySelectedImage();
                }
            }
    );

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoURI != null) {
                    selectedImageUri = photoURI;
                    displaySelectedImage();
                }
            }
    );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCameraIntent();
                } else {
                    Toast.makeText(this, "Camera permission is required to post photos", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCameraIntent();
        } else {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCameraIntent() {
        File photoFile = null;
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
            currentPhotoPath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoURI);
        }
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            llImageHints.setVisibility(View.GONE);
            Glide.with(this).load(selectedImageUri).into(ivSelectedImage);
        }
    }

    
    
    
    private String getBase64ImageString(android.net.Uri uri) {
        try {
            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            // Compress heavily to stay under Firestore's 1MB document limit
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] imageBytes = baos.toByteArray();
            return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadPost() {
        String caption = etCaption.getText().toString().trim();
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please add a photo first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (caption.isEmpty()) {
            Toast.makeText(this, "Please write a caption", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing Image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            String base64Image = getBase64ImageString(selectedImageUri);
            
            runOnUiThread(() -> {
                if (base64Image == null || base64Image.isEmpty()) {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to process image.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Format as a data URI so Glide/Picasso can still load it
                String imageUrl = "data:image/jpeg;base64," + base64Image;

                progressDialog.setMessage("Saving Post to Database...");
                savePostToFirestore(imageUrl, caption, progressDialog);
            });
        }).start();
    }

    private void savePostToFirestore(String imageUrl, String caption, ProgressDialog progressDialog) {
        String userId = auth.getCurrentUser().getUid();
        
        Map<String, Object> post = new HashMap<>();
        post.put("riderId", userId);
        post.put("riderName", riderName);
        post.put("riderAvatarUrl", riderAvatarUrl);
        post.put("imageUrl", imageUrl);
        post.put("caption", caption);
        post.put("location", "Nearby"); // Later you can connect this to actual device location
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("likes", new ArrayList<String>());
        post.put("avails", new HashMap<String, Object>());

        db.collection("pasabuy_posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "PasaBuy Posted Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error posting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
