package com.fetch.auth.production.pasabuy;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.TransactionHistoryActivity;
import com.fetch.auth.production.R;
import com.fetch.auth.production.repository.NotificationRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PasaBuyFeedActivity extends AppCompatActivity {

    private boolean isRider;
    private RecyclerView rvPasaBuyFeed;
    private FloatingActionButton fabPostPasaBuy;
    private ExtendedFloatingActionButton fabMyAvails;
    private PasaBuyFeedAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private NotificationRepository notificationRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasabuy_feed);

        Toolbar toolbar = findViewById(R.id.toolbarPasaBuy);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PasaBuy");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String role = getIntent().getStringExtra("USER_ROLE");
        isRider = "rider".equalsIgnoreCase(role);

        rvPasaBuyFeed = findViewById(R.id.rvPasaBuyFeed);
        rvPasaBuyFeed.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();

        adapter = new PasaBuyFeedAdapter(isRider, currentUserId, new PasaBuyFeedAdapter.OnPostInteractionListener() {
            @Override
            public void onAvailClicked(DocumentSnapshot post) {
                handleAvailAction(post);
            }

            @Override
            public void onLikeClicked(DocumentSnapshot post, boolean isLiked) {
                toggleLike(post, isLiked);
            }

            @Override
            public void onViewAvailRequestsClicked(DocumentSnapshot post) {
                openAvailRequests(post);
            }
        });

        rvPasaBuyFeed.setAdapter(adapter);

        fabPostPasaBuy = findViewById(R.id.fabPostPasaBuy);
        fabMyAvails = findViewById(R.id.fabMyAvails);

        if (isRider) {
            fabPostPasaBuy.setVisibility(View.VISIBLE);
            fabPostPasaBuy.setOnClickListener(v -> startActivity(new Intent(this, AddPasaBuyPostActivity.class)));
            fabMyAvails.setText(R.string.pasabuy_menu_my_transactions);
            fabMyAvails.setVisibility(View.VISIBLE);
            moveMyAvailsFabToStart();
        } else {
            fabMyAvails.setText(R.string.pasabuy_menu_my_avails);
            fabMyAvails.setVisibility(View.VISIBLE);
        }

        fabMyAvails.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionHistoryActivity.class);
            intent.putExtra(TransactionHistoryActivity.EXTRA_FILTER_MODE,
                    isRider
                            ? TransactionHistoryActivity.FILTER_RIDER_TRANSACTIONS
                            : TransactionHistoryActivity.FILTER_CUSTOMER_AVAILS);
            startActivity(intent);
        });

        listenForPasaBuyPosts();
    }

    private void moveMyAvailsFabToStart() {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fabMyAvails.getLayoutParams();
        params.gravity = Gravity.BOTTOM | Gravity.START;
        fabMyAvails.setLayoutParams(params);
    }

    private void listenForPasaBuyPosts() {
        db.collection("pasabuy_posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    if (!value.isEmpty()) {
                        adapter.setPosts(value.getDocuments());
                    } else {
                        adapter.setPosts(new ArrayList<>());
                    }
                });
    }

    private void toggleLike(DocumentSnapshot post, boolean shouldLike) {
        post.getReference()
                .update("likes", shouldLike ? FieldValue.arrayUnion(currentUserId) : FieldValue.arrayRemove(currentUserId))
                .addOnFailureListener(error -> Toast.makeText(PasaBuyFeedActivity.this,
                        "Failed to update like: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void handleAvailAction(DocumentSnapshot post) {
        if (isRider) {
            Toast.makeText(this, "Only customers can avail a post.", Toast.LENGTH_SHORT).show();
            return;
        }

        String riderId = post.getString("riderId");
        if (currentUserId.equals(riderId)) {
            Toast.makeText(this, "You cannot avail your own post.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> avails = post.get("avails") instanceof Map<?, ?>
                ? (Map<String, Object>) post.get("avails")
                : new HashMap<>();
        Map<String, Object> myAvail = avails.get(currentUserId) instanceof Map<?, ?>
                ? (Map<String, Object>) avails.get(currentUserId)
                : null;
        String status = myAvail != null && myAvail.get("status") != null ? String.valueOf(myAvail.get("status")) : "";
        String transactionId = myAvail != null && myAvail.get("transactionId") != null
                ? String.valueOf(myAvail.get("transactionId"))
                : "";

        if (myAvail == null || "cancelled".equalsIgnoreCase(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Avail")
                    .setMessage("Do you want to avail this rider's PasaBuy post?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", (dialog, which) -> createAvailRequest(post))
                    .show();
            return;
        }

        if ("pending".equalsIgnoreCase(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Avail")
                    .setMessage("Cancel your avail request for this post?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", (dialog, which) -> cancelAvailRequest(post, transactionId))
                    .show();
            return;
        }

        if (!transactionId.isEmpty()) {
            openTransaction(transactionId);
            return;
        }

        Toast.makeText(this, "Unable to open transaction for this avail.", Toast.LENGTH_SHORT).show();
    }

    private void createAvailRequest(DocumentSnapshot post) {
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String customerName = userDoc.getString("fullName");
                    if (customerName == null || customerName.trim().isEmpty()) {
                        customerName = userDoc.getString("name");
                    }
                    if (customerName == null || customerName.trim().isEmpty()) {
                        customerName = "Customer";
                    }
                    createAvailTransaction(post, customerName);
                })
                .addOnFailureListener(error -> Toast.makeText(PasaBuyFeedActivity.this,
                        "Unable to load your profile: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createAvailTransaction(DocumentSnapshot post, String customerName) {
        DocumentReference transactionRef = db.collection("pasabuy_transactions").document();
        String riderId = post.getString("riderId");
        String riderName = post.getString("riderName");
        String postCaption = post.getString("caption");

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionRef.getId());
        transaction.put("postId", post.getId());
        transaction.put("postCaption", postCaption != null ? postCaption : "");
        transaction.put("riderId", riderId != null ? riderId : "");
        transaction.put("riderName", riderName != null ? riderName : "Rider");
        transaction.put("customerId", currentUserId);
        transaction.put("customerName", customerName);
        transaction.put("status", "pending");
        transaction.put("paymentStatus", "pending");
        transaction.put("latestUpdate", "Awaiting rider approval");
        transaction.put("createdAt", FieldValue.serverTimestamp());
        transaction.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> availEntry = new HashMap<>();
        availEntry.put("customerId", currentUserId);
        availEntry.put("customerName", customerName);
        availEntry.put("transactionId", transactionRef.getId());
        availEntry.put("status", "pending");
        availEntry.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> initialMessage = new HashMap<>();
        initialMessage.put("senderId", currentUserId);
        initialMessage.put("senderName", customerName);
        initialMessage.put("senderRole", "customer");
        initialMessage.put("type", "status");
        initialMessage.put("text", "Customer requested avail.");
        initialMessage.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(transactionRef, transaction);
        batch.update(post.getReference(), "avails." + currentUserId, availEntry);
        batch.set(transactionRef.collection("messages").document(), initialMessage);
        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(PasaBuyFeedActivity.this, "Avail request sent.", Toast.LENGTH_SHORT).show();
                    notificationRepository.sendPasabuyStatusPush(
                            transactionRef.getId(),
                            "avail_requested",
                            false,
                            postCaption
                    );
                    openTransaction(transactionRef.getId());
                })
                .addOnFailureListener(error -> Toast.makeText(PasaBuyFeedActivity.this,
                        "Failed to send avail request: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cancelAvailRequest(DocumentSnapshot post, String transactionId) {
        WriteBatch batch = db.batch();
        batch.update(post.getReference(), "avails." + currentUserId, FieldValue.delete());

        if (transactionId != null && !transactionId.isEmpty()) {
            DocumentReference transactionRef = db.collection("pasabuy_transactions").document(transactionId);
            Map<String, Object> update = new HashMap<>();
            update.put("status", "cancelled");
            update.put("latestUpdate", "Customer cancelled the avail request.");
            update.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(transactionRef, update, SetOptions.merge());

            Map<String, Object> cancelMessage = new HashMap<>();
            cancelMessage.put("senderId", currentUserId);
            cancelMessage.put("senderName", "Customer");
            cancelMessage.put("senderRole", "customer");
            cancelMessage.put("type", "status");
            cancelMessage.put("text", "Customer cancelled the avail request.");
            cancelMessage.put("createdAt", FieldValue.serverTimestamp());
            batch.set(transactionRef.collection("messages").document(), cancelMessage);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(PasaBuyFeedActivity.this, "Avail request cancelled.", Toast.LENGTH_SHORT).show();
                    if (transactionId != null && !transactionId.isEmpty()) {
                        notificationRepository.sendPasabuyStatusPush(
                                transactionId,
                                "avail_cancelled",
                                false,
                                null
                        );
                    }
                })
                .addOnFailureListener(error -> Toast.makeText(PasaBuyFeedActivity.this,
                        "Failed to cancel avail: " + error.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openAvailRequests(DocumentSnapshot post) {
        String riderId = post.getString("riderId");
        if (!currentUserId.equals(riderId)) {
            Toast.makeText(this, "Only the post owner can view avails.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PasaBuyAvailRequestsActivity.class);
        intent.putExtra(PasaBuyAvailRequestsActivity.EXTRA_POST_ID, post.getId());
        intent.putExtra(PasaBuyAvailRequestsActivity.EXTRA_POST_CAPTION, post.getString("caption"));
        startActivity(intent);
    }

    private void openTransaction(String transactionId) {
        Intent intent = new Intent(this, PasaBuyTransactionActivity.class);
        intent.putExtra(PasaBuyTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pasabuy_feed, menu);
        MenuItem action = menu.findItem(R.id.actionMyPasabuyTransactions);
        if (action != null) {
            action.setTitle(isRider
                    ? R.string.pasabuy_menu_my_transactions
                    : R.string.pasabuy_menu_my_avails);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.actionMyPasabuyTransactions) {
            Intent intent = new Intent(this, TransactionHistoryActivity.class);
            intent.putExtra(TransactionHistoryActivity.EXTRA_FILTER_MODE,
                    isRider
                            ? TransactionHistoryActivity.FILTER_RIDER_TRANSACTIONS
                            : TransactionHistoryActivity.FILTER_CUSTOMER_AVAILS);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
