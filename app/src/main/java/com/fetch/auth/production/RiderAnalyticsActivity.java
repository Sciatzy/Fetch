package com.fetch.auth.production;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.adapter.TaskFeedAdapter;
import com.fetch.auth.production.model.ErrandTask;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RiderAnalyticsActivity extends AppCompatActivity {
    private static final String TAG = "RiderAnalytics";
    private TextView tvEarnings;
    private TextView tvTasks;
    private TextView tvEmptyHistory;
    private RecyclerView rvTaskHistory;
    private TaskFeedAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_analytics);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEarnings = findViewById(R.id.tvAnalyticsEarnings);
        tvTasks = findViewById(R.id.tvAnalyticsTasks);
        rvTaskHistory = findViewById(R.id.rvTaskHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        rvTaskHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskFeedAdapter(new TaskFeedAdapter.TaskActionListener() {
            @Override
            public void onAcceptTask(ErrandTask task) {}
            @Override
            public void onPreviewRoute(ErrandTask task) {}
        });
        rvTaskHistory.setAdapter(adapter);

        if (user != null) {
            loadRiderAnalytics();
            loadTaskHistory();
        }
    }

    private void loadRiderAnalytics() {
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    Double earnings = document.getDouble("totalEarnings");
                    Long tasks = document.getLong("overallAcceptedTasks");
                    tvEarnings.setText(String.format(java.util.Locale.US, "PHP %.2f", earnings != null ? earnings : 0.0));
                    tvTasks.setText("Tasks Completed: " + (tasks != null ? tasks : 0));
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error fetching stats", e));
    }

    private void loadTaskHistory() {
        // Find tasks assigned to this rider that are specifically completed
        db.collection("tasks")
            .whereEqualTo("riderId", user.getUid())
            .whereEqualTo("status", "completed")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50) // reasonable analytics window for a list
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ErrandTask> fetchedTasks = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    ErrandTask task = ErrandTask.fromDocument(doc);
                    if (task != null) {
                        fetchedTasks.add(task);
                    }
                }
                if (fetchedTasks.isEmpty()) {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    rvTaskHistory.setVisibility(View.GONE);
                } else {
                    tvEmptyHistory.setVisibility(View.GONE);
                    rvTaskHistory.setVisibility(View.VISIBLE);
                    adapter.submitList(fetchedTasks);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching history", e);
                Toast.makeText(this, "Could not load history.", Toast.LENGTH_SHORT).show();
            });
    }
}




