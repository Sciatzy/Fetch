package com.fetch.auth.production.pasabuy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PasaBuyAvailRequestsActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_POST_CAPTION = "extra_post_caption";

    private FirebaseFirestore db;
    private String postId;
    private String currentUserId;
    private TextView tvEmptyState;
    private PasaBuyAvailRequestsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pasabuy_avail_requests);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        String postCaption = getIntent().getStringExtra(EXTRA_POST_CAPTION);
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Post reference is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarAvailRequests);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Avail Requests");
            if (!TextUtils.isEmpty(postCaption)) {
                getSupportActionBar().setSubtitle(postCaption);
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        tvEmptyState = findViewById(R.id.tvEmptyAvailRequests);
        RecyclerView rvRequests = findViewById(R.id.rvAvailRequests);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PasaBuyAvailRequestsAdapter(this::openTransaction);
        rvRequests.setAdapter(adapter);

        listenToPostAvails();
    }

    private void listenToPostAvails() {
        db.collection("pasabuy_posts")
                .document(postId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load avail requests: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        adapter.setRequests(new ArrayList<>());
                        tvEmptyState.setVisibility(View.VISIBLE);
                        return;
                    }

                    String riderId = snapshot.getString("riderId");
                    if (!TextUtils.equals(currentUserId, riderId)) {
                        Toast.makeText(this, "Only the post owner can view requests.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    List<AvailRequestItem> requestItems = parseAvailRequests(snapshot);
                    adapter.setRequests(requestItems);
                    tvEmptyState.setVisibility(requestItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @SuppressWarnings("unchecked")
    private List<AvailRequestItem> parseAvailRequests(DocumentSnapshot snapshot) {
        Object availsObject = snapshot.get("avails");
        Map<String, Object> avails = availsObject instanceof Map<?, ?>
                ? (Map<String, Object>) availsObject
                : new HashMap<>();

        List<AvailRequestItem> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : avails.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> request = (Map<String, Object>) entry.getValue();
            String customerId = safeString(request.get("customerId"));
            if (customerId.isEmpty()) {
                customerId = entry.getKey();
            }
            String customerName = safeString(request.get("customerName"));
            if (customerName.isEmpty()) {
                customerName = "Customer";
            }
            String status = safeString(request.get("status"));
            if (status.isEmpty()) {
                status = "pending";
            }
            if ("cancelled".equalsIgnoreCase(status)) {
                continue;
            }
            String transactionId = safeString(request.get("transactionId"));

            long updatedAtMillis = 0L;
            Object updatedAtValue = request.get("updatedAt");
            if (updatedAtValue instanceof Timestamp) {
                updatedAtMillis = ((Timestamp) updatedAtValue).toDate().getTime();
            }

            items.add(new AvailRequestItem(customerId, customerName, status, transactionId, updatedAtMillis));
        }

        Collections.sort(items, Comparator.comparingLong(AvailRequestItem::getUpdatedAtMillis).reversed());
        return items;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void openTransaction(AvailRequestItem item) {
        Intent intent = new Intent(this, PasaBuyTransactionActivity.class);
        intent.putExtra(PasaBuyTransactionActivity.EXTRA_TRANSACTION_ID, item.getTransactionId());
        startActivity(intent);
    }
}
