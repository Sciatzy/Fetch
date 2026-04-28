package com.fetch.auth.production;

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

import com.fetch.auth.production.pasabuy.PasaBuyTransactionActivity;
import com.fetch.auth.production.pasabuy.PasaBuyTransactionHistoryAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ROLE = "extra_user_role";
    public static final String EXTRA_FILTER_MODE = "extra_filter_mode";
    public static final String FILTER_CUSTOMER_AVAILS = "customer_avails";
    public static final String FILTER_RIDER_TRANSACTIONS = "rider_transactions";

    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration transactionListener;
    private RecyclerView rvTransactionHistory;
    private View layoutTransactionEmptyState;
    private TextView tvTransactionEmptyTitle;
    private TextView tvTransactionEmptySubtitle;
    private PasaBuyTransactionHistoryAdapter adapter;
    private String activeFilterMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        bindViews();
        resolveAndStartFilter();
    }

    @Override
    protected void onDestroy() {
        if (transactionListener != null) {
            transactionListener.remove();
            transactionListener = null;
        }
        super.onDestroy();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarTransactionHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.transaction_history_title_default);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory);
        layoutTransactionEmptyState = findViewById(R.id.layoutTransactionEmptyState);
        tvTransactionEmptyTitle = findViewById(R.id.tvTransactionEmptyTitle);
        tvTransactionEmptySubtitle = findViewById(R.id.tvTransactionEmptySubtitle);
        rvTransactionHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void resolveAndStartFilter() {
        String explicitFilter = getIntent().getStringExtra(EXTRA_FILTER_MODE);
        if (!TextUtils.isEmpty(explicitFilter)) {
            startTransactionListener(explicitFilter);
            return;
        }

        String role = getIntent().getStringExtra(EXTRA_USER_ROLE);
        if (!TextUtils.isEmpty(role)) {
            startTransactionListener(resolveFilterModeFromRole(role));
            return;
        }

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String userRole = doc != null ? doc.getString("role") : null;
                    startTransactionListener(resolveFilterModeFromRole(userRole));
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Unable to load role. Showing My Avails.", Toast.LENGTH_SHORT).show();
                    startTransactionListener(FILTER_CUSTOMER_AVAILS);
                });
    }

    private String resolveFilterModeFromRole(String role) {
        return "rider".equalsIgnoreCase(role)
                ? FILTER_RIDER_TRANSACTIONS
                : FILTER_CUSTOMER_AVAILS;
    }

    private void startTransactionListener(String filterMode) {
        activeFilterMode = filterMode;
        boolean customerView = FILTER_CUSTOMER_AVAILS.equals(filterMode);
        String filterField = customerView ? "customerId" : "riderId";

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(customerView
                    ? R.string.transaction_history_title_customer
                    : R.string.transaction_history_title_rider);
        }
        tvTransactionEmptyTitle.setText(customerView
                ? R.string.transaction_empty_customer_title
                : R.string.transaction_empty_rider_title);
        tvTransactionEmptySubtitle.setText(customerView
                ? R.string.transaction_empty_customer_subtitle
                : R.string.transaction_empty_rider_subtitle);

        adapter = new PasaBuyTransactionHistoryAdapter(customerView, this::openTransactionChat);
        rvTransactionHistory.setAdapter(adapter);

        if (transactionListener != null) {
            transactionListener.remove();
        }
        transactionListener = db.collection("pasabuy_transactions")
                .whereEqualTo(filterField, currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load transactions: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null) {
                        adapter.setTransactions(new ArrayList<>());
                        showEmptyState(true);
                        return;
                    }

                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    docs.sort(this::compareByUpdatedTimeDesc);
                    adapter.setTransactions(docs);
                    showEmptyState(docs.isEmpty());
                });
    }

    private int compareByUpdatedTimeDesc(DocumentSnapshot left, DocumentSnapshot right) {
        long leftTime = resolveTimestampMillis(left);
        long rightTime = resolveTimestampMillis(right);
        return Long.compare(rightTime, leftTime);
    }

    private long resolveTimestampMillis(DocumentSnapshot doc) {
        Timestamp updated = doc.getTimestamp("updatedAt");
        if (updated != null) {
            return updated.toDate().getTime();
        }
        Timestamp created = doc.getTimestamp("createdAt");
        if (created != null) {
            return created.toDate().getTime();
        }
        return 0L;
    }

    private void showEmptyState(boolean showEmpty) {
        layoutTransactionEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        rvTransactionHistory.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }

    private void openTransactionChat(DocumentSnapshot transactionDoc) {
        String transactionId = transactionDoc.getId();
        if (TextUtils.isEmpty(transactionId)) {
            Toast.makeText(this, "Transaction reference is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PasaBuyTransactionActivity.class);
        intent.putExtra(PasaBuyTransactionActivity.EXTRA_TRANSACTION_ID, transactionId);
        intent.putExtra(EXTRA_FILTER_MODE, activeFilterMode);
        startActivity(intent);
    }
}
