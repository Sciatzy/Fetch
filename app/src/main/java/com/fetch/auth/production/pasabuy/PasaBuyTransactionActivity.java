package com.fetch.auth.production.pasabuy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.util.ImageDataUriUtil;
import com.fetch.auth.production.repository.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PasaBuyTransactionActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";

    private FirebaseFirestore db;
    private String currentUserId;
    private String transactionId;
    private DocumentSnapshot currentTransaction;
    private boolean isRider;
    private NotificationRepository notificationRepository;

    private TextView tvTransactionStatus;
    private TextView tvPaymentStatus;
    private EditText etMessage;
    private android.widget.ImageView btnSendMessage;
    private Button btnApproveAvail;
    private Button btnPaymentReceived;
    private Button btnOnWay;
    private Button btnAboutToDeliver;
    private Button btnCompleteTransaction;
    private Button btnCancelAvail;
    private Button btnShareLocation;
    private Button btnUploadProof;
    private Button btnNavigateCustomer;

    private PasaBuyMessagesAdapter messagesAdapter;

    private final ActivityResultLauncher<String> pickProofImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadPaymentProof(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    shareCurrentLocation();
                } else {
                    Toast.makeText(this, "Location permission is required to share your location.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasabuy_transaction);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        transactionId = getIntent().getStringExtra(EXTRA_TRANSACTION_ID);
        if (TextUtils.isEmpty(transactionId)) {
            Toast.makeText(this, "Transaction reference is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        setupToolbar();
        setupViews();
        listenToTransaction();
        listenToMessages();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarPasaBuyTransaction);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PasaBuy Transaction");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        tvTransactionStatus = findViewById(R.id.tvTransactionStatus);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        etMessage = findViewById(R.id.etTransactionMessage);
        btnSendMessage = findViewById(R.id.btnSendTransactionMessage);
        btnApproveAvail = findViewById(R.id.btnApproveAvail);
        btnPaymentReceived = findViewById(R.id.btnPaymentReceived);
        btnOnWay = findViewById(R.id.btnOnWay);
        btnAboutToDeliver = findViewById(R.id.btnAboutToDeliver);
        btnCompleteTransaction = findViewById(R.id.btnCompleteTransaction);
        btnCancelAvail = findViewById(R.id.btnCancelAvail);
        btnShareLocation = findViewById(R.id.btnShareLocation);
        btnUploadProof = findViewById(R.id.btnUploadPaymentProof);
        btnNavigateCustomer = findViewById(R.id.btnNavigateCustomer);

        RecyclerView rvMessages = findViewById(R.id.rvTransactionMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new PasaBuyMessagesAdapter(currentUserId, this::openNavigationTo);
        rvMessages.setAdapter(messagesAdapter);

        btnSendMessage.setOnClickListener(v -> sendTextMessage());
        btnApproveAvail.setOnClickListener(v -> approveAvailRequest());
        btnPaymentReceived.setOnClickListener(v -> markPaymentReceived());
        btnOnWay.setOnClickListener(v -> sendRiderStatusUpdate("On the way", "Rider is on the way."));
        btnAboutToDeliver.setOnClickListener(v -> sendRiderStatusUpdate("About to deliver", "Rider is about to deliver your order."));
        btnCompleteTransaction.setOnClickListener(v -> confirmAndCompleteTransaction());
        btnCancelAvail.setOnClickListener(v -> confirmAndCancelAvail());
        btnShareLocation.setOnClickListener(v -> shareLocationWithPermissionCheck());
        btnUploadProof.setOnClickListener(v -> pickProofImageLauncher.launch("image/*"));
        btnNavigateCustomer.setOnClickListener(v -> navigateToCustomer());

        btnUploadProof.setEnabled(false);
    }

    private void listenToTransaction() {
        db.collection("pasabuy_transactions")
                .document(transactionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load transaction: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Transaction not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String riderId = snapshot.getString("riderId");
                    String customerId = snapshot.getString("customerId");
                    if (!TextUtils.equals(currentUserId, riderId) && !TextUtils.equals(currentUserId, customerId)) {
                        Toast.makeText(this, "You are not allowed to open this transaction.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentTransaction = snapshot;
                    isRider = TextUtils.equals(currentUserId, riderId);
                    bindTransactionHeader(snapshot);
                    bindRoleControls(snapshot);
                });
    }

    private void listenToMessages() {
        db.collection("pasabuy_transactions")
                .document(transactionId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }
                    messagesAdapter.setMessages(snapshot.getDocuments());
                    RecyclerView rvMessages = findViewById(R.id.rvTransactionMessages);
                    if (messagesAdapter.getItemCount() > 0) {
                        rvMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);
                    }
                });
    }

    private void bindTransactionHeader(DocumentSnapshot transaction) {
        String status = safeValue(transaction.getString("status"), "pending");
        String paymentStatus = safeValue(transaction.getString("paymentStatus"), "pending");
        String counterpartName = isRider
                ? safeValue(transaction.getString("customerName"), "Customer")
                : safeValue(transaction.getString("riderName"), "Rider");

        tvTransactionStatus.setText("Transaction: " + status);
        tvPaymentStatus.setText("Payment: " + paymentStatus);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(counterpartName);
        }
    }

    private void bindRoleControls(DocumentSnapshot transaction) {
        String status = safeValue(transaction.getString("status"), "pending");
        boolean isClosed = isTransactionClosed(status);

        btnSendMessage.setEnabled(!isClosed);
        etMessage.setEnabled(!isClosed);

        if (isRider) {
            btnApproveAvail.setVisibility(View.VISIBLE);
            btnPaymentReceived.setVisibility(View.VISIBLE);
            btnOnWay.setVisibility(View.VISIBLE);
            btnAboutToDeliver.setVisibility(View.VISIBLE);
            btnCompleteTransaction.setVisibility(View.VISIBLE);
            btnNavigateCustomer.setVisibility(View.VISIBLE);

            btnCancelAvail.setVisibility(View.GONE);
            btnShareLocation.setVisibility(View.GONE);
            btnUploadProof.setVisibility(View.GONE);

            btnApproveAvail.setEnabled("pending".equalsIgnoreCase(status));
            btnPaymentReceived.setEnabled(!isClosed);
            btnOnWay.setEnabled(!isClosed && ("approved".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status)));
            btnAboutToDeliver.setEnabled(!isClosed && ("approved".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status)));
            btnCompleteTransaction.setEnabled(!isClosed && !"pending".equalsIgnoreCase(status));
            btnNavigateCustomer.setEnabled(hasCustomerLocation(transaction));
        } else {
            btnApproveAvail.setVisibility(View.GONE);
            btnPaymentReceived.setVisibility(View.GONE);
            btnOnWay.setVisibility(View.GONE);
            btnAboutToDeliver.setVisibility(View.GONE);
            btnCompleteTransaction.setVisibility(View.GONE);
            btnNavigateCustomer.setVisibility(View.GONE);

            btnCancelAvail.setVisibility(View.VISIBLE);
            btnShareLocation.setVisibility(View.VISIBLE);
            btnUploadProof.setVisibility(View.VISIBLE);

            btnCancelAvail.setEnabled("pending".equalsIgnoreCase(status));
            btnShareLocation.setEnabled(!isClosed);
            btnUploadProof.setEnabled(!isClosed);
        }
    }

    private void sendTextMessage() {
        if (currentTransaction == null) return;
        if (isTransactionClosed(safeValue(currentTransaction.getString("status"), "pending"))) {
            Toast.makeText(this, "Transaction is closed.", Toast.LENGTH_SHORT).show();
            return;
        }
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }
        sendMessage("text", messageText, null, null, null, null);
        etMessage.setText("");
    }

    private void approveAvailRequest() {
        if (!isRider || currentTransaction == null) return;
        updateTransactionAndPost("approved", "Rider approved the avail request.", "approved", "avail_approved");
    }

    private void markPaymentReceived() {
        if (!isRider || currentTransaction == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentStatus", "received");
        updates.put("latestUpdate", "Payment received by rider.");
        updates.put("updatedAt", FieldValue.serverTimestamp());
        commitTransactionUpdateWithMessage(updates, "Payment received by rider.", "payment_received");
    }

    private void sendRiderStatusUpdate(String statusLabel, String message) {
        if (!isRider || currentTransaction == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "in_progress");
        updates.put("latestUpdate", statusLabel);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        commitTransactionUpdateWithMessage(updates, message, "rider_status_update");
        updatePostAvailStatus("approved");
    }

    private void confirmAndCompleteTransaction() {
        if (currentTransaction == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Complete Transaction")
                .setMessage("Mark this transaction as completed and close chat?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) ->
                        updateTransactionAndPost("completed", "Transaction completed by rider.", "completed", "transaction_completed"))
                .show();
    }

    private void confirmAndCancelAvail() {
        if (isRider || currentTransaction == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Cancel Avail")
                .setMessage("Cancel this avail request?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> cancelAvailRequest())
                .show();
    }

    private void cancelAvailRequest() {
        if (currentTransaction == null) return;
        String customerId = safeValue(currentTransaction.getString("customerId"), "");
        String postId = safeValue(currentTransaction.getString("postId"), "");
        if (!TextUtils.equals(currentUserId, customerId)) {
            Toast.makeText(this, "Only the customer can cancel this request.", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference transactionRef = db.collection("pasabuy_transactions").document(transactionId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("latestUpdate", "Customer cancelled the avail request.");
        updates.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(transactionRef, updates, SetOptions.merge());

        if (!postId.isEmpty()) {
            batch.update(db.collection("pasabuy_posts").document(postId), "avails." + customerId, FieldValue.delete());
        }

        Map<String, Object> message = buildMessageMap("status", "Customer cancelled the avail request.", null, null, null);
        batch.set(transactionRef.collection("messages").document(), message);
        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Avail request cancelled.", Toast.LENGTH_SHORT).show();
                    dispatchPasabuyEvent("avail_cancelled", null);
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Failed to cancel avail: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void shareLocationWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            shareCurrentLocation();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void shareCurrentLocation() {
        if (currentTransaction == null) return;
        if (isTransactionClosed(safeValue(currentTransaction.getString("status"), "pending"))) {
            Toast.makeText(this, "Transaction is closed.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, "Location service unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> providers = locationManager.getProviders(true);
        Location latest = null;
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (latest == null || location.getAccuracy() < latest.getAccuracy())) {
                latest = location;
            }
        }

        if (latest == null) {
            Toast.makeText(this, "Unable to get your location right now.", Toast.LENGTH_SHORT).show();
            return;
        }

        double lat = latest.getLatitude();
        double lng = latest.getLongitude();
        String label = String.format(Locale.US, "Shared location: %.5f, %.5f", lat, lng);
        sendMessage("location", label, null, lat, lng, null);

        Map<String, Object> updates = new HashMap<>();
        updates.put("customerLocationLat", lat);
        updates.put("customerLocationLng", lng);
        updates.put("customerLocationLabel", label);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("pasabuy_transactions").document(transactionId)
                .set(updates, SetOptions.merge());
    }

    private void uploadPaymentProof(Uri imageUri) {
        if (currentTransaction == null) {
            Toast.makeText(this, "Transaction details are still loading. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTransactionClosed(safeValue(currentTransaction.getString("status"), "pending"))) {
            Toast.makeText(this, "Transaction is closed.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUploadProof.setEnabled(false);
        new Thread(() -> {
            String imageDataUri = ImageDataUriUtil.toJpegDataUri(this, imageUri, 960, 40);
            runOnUiThread(() -> {
                btnUploadProof.setEnabled(true);
                if (TextUtils.isEmpty(imageDataUri)) {
                    Toast.makeText(this, "Failed to upload proof: image processing failed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("paymentStatus", "proof_uploaded");
                updates.put("latestUpdate", "Customer uploaded payment proof.");
                updates.put("updatedAt", FieldValue.serverTimestamp());
                db.collection("pasabuy_transactions").document(transactionId).set(updates, SetOptions.merge());
                sendMessage("image", "Payment proof uploaded.", imageDataUri, null, null, "proof_uploaded");
            });
        }).start();
    }

    private void navigateToCustomer() {
        if (currentTransaction == null) return;
        Double lat = getDouble(currentTransaction.get("customerLocationLat"));
        Double lng = getDouble(currentTransaction.get("customerLocationLng"));
        if (lat == null || lng == null) {
            Toast.makeText(this, "Customer location has not been shared yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String label = safeValue(currentTransaction.getString("customerLocationLabel"), "Customer location");
        openNavigationTo(lat, lng, label);
    }

    private void openNavigationTo(double lat, double lng, String label) {
        Uri navUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
        navIntent.setPackage("com.google.android.apps.maps");
        if (navIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(navIntent);
            return;
        }

        Uri fallbackUri = Uri.parse("geo:0,0?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")");
        startActivity(new Intent(Intent.ACTION_VIEW, fallbackUri));
    }

    private void updateTransactionAndPost(String status, String message, String postAvailStatus, String event) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("latestUpdate", message);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        commitTransactionUpdateWithMessage(updates, message, event);
        updatePostAvailStatus(postAvailStatus);
    }

    private void commitTransactionUpdateWithMessage(Map<String, Object> transactionUpdates, String statusMessage, String event) {
        DocumentReference transactionRef = db.collection("pasabuy_transactions").document(transactionId);
        WriteBatch batch = db.batch();
        batch.set(transactionRef, transactionUpdates, SetOptions.merge());
        batch.set(transactionRef.collection("messages").document(), buildMessageMap("status", statusMessage, null, null, null));
        batch.commit()
                .addOnSuccessListener(unused -> dispatchPasabuyEvent(event, statusMessage))
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Failed to update transaction: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendMessage(String type, String text, @Nullable String imageUrl, @Nullable Double lat, @Nullable Double lng, @Nullable String paymentStatus) {
        DocumentReference transactionRef = db.collection("pasabuy_transactions").document(transactionId);
        Map<String, Object> message = buildMessageMap(type, text, imageUrl, lat, lng);

        WriteBatch batch = db.batch();
        batch.set(transactionRef.collection("messages").document(), message);

        Map<String, Object> updates = new HashMap<>();
        updates.put("updatedAt", FieldValue.serverTimestamp());
        updates.put("lastMessage", text);
        if (paymentStatus != null) {
            updates.put("paymentStatus", paymentStatus);
        }
        batch.set(transactionRef, updates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> {
                    String preview = "text".equalsIgnoreCase(type) ? text : null;
                    dispatchPasabuyEvent("message_" + type.toLowerCase(Locale.US), preview);
                })
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Failed to send message: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void dispatchPasabuyEvent(String event, @Nullable String preview) {
        if (notificationRepository == null || TextUtils.isEmpty(transactionId)) {
            return;
        }
        notificationRepository.sendPasabuyStatusPush(transactionId, event, isRider, preview);
    }

    private Map<String, Object> buildMessageMap(String type, String text, @Nullable String imageUrl, @Nullable Double lat, @Nullable Double lng) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderRole", isRider ? "rider" : "customer");
        message.put("senderName", resolveSenderName());
        message.put("type", type);
        message.put("text", text);
        message.put("createdAt", FieldValue.serverTimestamp());
        if (imageUrl != null) {
            message.put("imageUrl", imageUrl);
        }
        if (lat != null && lng != null) {
            message.put("locationLat", lat);
            message.put("locationLng", lng);
        }
        return message;
    }

    private String resolveSenderName() {
        if (currentTransaction == null) {
            return isRider ? "Rider" : "Customer";
        }
        if (isRider) {
            return safeValue(currentTransaction.getString("riderName"), "Rider");
        }
        return safeValue(currentTransaction.getString("customerName"), "Customer");
    }

    private void updatePostAvailStatus(String status) {
        if (currentTransaction == null) return;
        String postId = safeValue(currentTransaction.getString("postId"), "");
        String customerId = safeValue(currentTransaction.getString("customerId"), "");
        if (postId.isEmpty() || customerId.isEmpty()) return;

        db.collection("pasabuy_posts")
                .document(postId)
                .update(
                        "avails." + customerId + ".status", status,
                        "avails." + customerId + ".updatedAt", FieldValue.serverTimestamp()
                );
    }

    private boolean hasCustomerLocation(DocumentSnapshot transaction) {
        return getDouble(transaction.get("customerLocationLat")) != null
                && getDouble(transaction.get("customerLocationLng")) != null;
    }

    private boolean isTransactionClosed(String status) {
        return "completed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
    }

    private String safeValue(@Nullable String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    @Nullable
    private Double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
