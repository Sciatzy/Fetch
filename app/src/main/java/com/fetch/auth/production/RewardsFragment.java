package com.fetch.auth.production;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RewardsFragment extends Fragment {

    private TextView tvTotalPoints;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);
        
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints);
        db = FirebaseFirestore.getInstance();
        
        fetchUserPoints();
        
        return view;
    }
    
    private void fetchUserPoints() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null) return;
                Double points = snapshot.getDouble("points");
                if (points != null) {
                    if (tvTotalPoints != null) {
                        tvTotalPoints.setText(String.valueOf(points));
                    }
                } else {
                    if (tvTotalPoints != null) {
                        tvTotalPoints.setText("0.0");
                    }
                }
            });
        }
    }
}