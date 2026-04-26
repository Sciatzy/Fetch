package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DashboardFragment extends Fragment {

    private TextView tvHomeName;
    private TextView tvHomePrimaryHint;
    private Button btnPrimaryAction;
    private ImageView ivHomeAvatar;

    // Customer Carousel
    private LinearLayout layoutCarouselHeader;
    private TextView tvSwipeHint;
    private HorizontalScrollView svCustomerCarousel;
    private MaterialCardView cardActiveTask;
    private TextView tvActiveTaskTitle;
    private TextView tvActiveTaskDesc;
    private String activeTaskId = null;

    private MaterialCardView cardQuickService;
    private MaterialCardView cardWallet;
    private TextView tvRewardPoints;
    private MaterialCardView cardPromo;
    private TextView tvOnlineRidersCount;
    private ListenerRegistration ridersListener;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private String currentRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();

        tvHomeName = view.findViewById(R.id.tvHomeName);
        tvHomePrimaryHint = view.findViewById(R.id.tvHomePrimaryHint);
        btnPrimaryAction = view.findViewById(R.id.btnPrimaryAction);
        ivHomeAvatar = view.findViewById(R.id.ivHomeAvatar);

        layoutCarouselHeader = view.findViewById(R.id.layoutCarouselHeader);
        tvSwipeHint = view.findViewById(R.id.tvSwipeHint);
        svCustomerCarousel = view.findViewById(R.id.svCustomerCarousel);
        cardActiveTask = view.findViewById(R.id.cardActiveTask);
        tvActiveTaskTitle = view.findViewById(R.id.tvActiveTaskTitle);
        tvActiveTaskDesc = view.findViewById(R.id.tvActiveTaskDesc);
        cardQuickService = view.findViewById(R.id.cardQuickService);
        cardWallet = view.findViewById(R.id.cardWallet);
        tvRewardPoints = view.findViewById(R.id.tvRewardPoints);
        cardPromo = view.findViewById(R.id.cardPromo);
        tvOnlineRidersCount = view.findViewById(R.id.tvOnlineRidersCount);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            loadDashboardData(user);
        }

        btnPrimaryAction.setOnClickListener(v -> openPrimaryTaskAction());

        return view;
    }

    private void loadDashboardData(FirebaseUser user) {
        userProfileRepository.getUserProfile(user.getUid(), new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                if (!isAdded()) return;

                String name = document.getString("name");
                String role = document.getString("role");
                String profileImage = document.getString("profileImage");

                tvHomeName.setText(!TextUtils.isEmpty(name)
                        ? getString(R.string.home_hello_name, name)
                        : getString(R.string.home_hello));
                currentRole = !TextUtils.isEmpty(role) ? role : getString(R.string.home_role_unknown);
                bindPrimaryActionByRole(currentRole);
                bindAvatar(profileImage);
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                tvHomeName.setText(R.string.home_hello);
                bindPrimaryActionByRole(null);
                bindAvatar(null);
            }
        });
    }

    private void bindAvatar(String profileImageUrl) {
        if (!isAdded()) return;

        if (TextUtils.isEmpty(profileImageUrl)) {
            ivHomeAvatar.setImageResource(R.drawable.fetch_logo);
            return;
        }

        Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.fetch_logo)
                .error(R.drawable.fetch_logo)
                .circleCrop()
                .into(ivHomeAvatar);
    }

    private void bindPrimaryActionByRole(String role) {
        if (role == null) {
            btnPrimaryAction.setEnabled(false);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
            layoutCarouselHeader.setVisibility(View.GONE);
            svCustomerCarousel.setVisibility(View.GONE);
            stopListeningForRiders();
            return;
        }

        if ("customer".equalsIgnoreCase(role)) {
            btnPrimaryAction.setEnabled(true);
            btnPrimaryAction.setText(R.string.create_task_cta);
            tvHomePrimaryHint.setText(R.string.home_primary_hint_customer);
            
            // Carousel Setup
            layoutCarouselHeader.setVisibility(View.VISIBLE);
            svCustomerCarousel.setVisibility(View.VISIBLE);
            svCustomerCarousel.setAlpha(1f); // Ensuring it is fully visible immediately
            
            // Give a visual bounce to hint the swipe function to users
            svCustomerCarousel.postDelayed(() -> {
                if (isAdded()) {
                    ObjectAnimator animator = ObjectAnimator.ofInt(svCustomerCarousel, "scrollX", 0, 100, 0);
                    animator.setDuration(1200);
                    animator.setInterpolator(new DecelerateInterpolator());
                    animator.start();
                }
            }, 500);

            // Bouncing text indicator
            ObjectAnimator swipeHintAnim = ObjectAnimator.ofFloat(tvSwipeHint, "translationX", 0f, 15f, 0f);
            swipeHintAnim.setRepeatCount(ValueAnimator.INFINITE);
            swipeHintAnim.setDuration(1500);
            swipeHintAnim.start();
            
            cardActiveTask.setOnClickListener(v -> {
                if (activeTaskId != null) {
                    Intent intent = new Intent(requireContext(), TaskTrackingActivity.class);
                    intent.putExtra(TaskTrackingActivity.EXTRA_TASK_ID, activeTaskId);
                    intent.putExtra(TaskTrackingActivity.EXTRA_IS_RIDER, false);
                    startActivity(intent);
                } else {
                    // Create new task if no active ones exist
                    startActivity(new Intent(requireContext(), MapTaskComposerActivity.class));
                }
            });

            cardQuickService.setOnClickListener(v -> 
                startActivity(new Intent(requireContext(), MapTaskComposerActivity.class))
            );

            cardWallet.setOnClickListener(v -> 
                Toast.makeText(requireContext(), "Redeeming points for shipping fee...", Toast.LENGTH_SHORT).show()
            );

            cardPromo.setOnClickListener(v -> 
                Toast.makeText(requireContext(), "Applying Promo FETCH20...", Toast.LENGTH_SHORT).show()
            );

            startListeningForRiders();
            listenForActiveCustomerTask();
            calculateRewardPoints();

        } else if ("rider".equalsIgnoreCase(role)) {
            btnPrimaryAction.setEnabled(true);
            btnPrimaryAction.setText(R.string.view_pending_tasks_cta);
            tvHomePrimaryHint.setText(R.string.home_primary_hint_rider);
            layoutCarouselHeader.setVisibility(View.GONE);
            svCustomerCarousel.setVisibility(View.GONE);
            stopListeningForRiders();
        } else {
            btnPrimaryAction.setEnabled(false);
            btnPrimaryAction.setText(R.string.open_tasks);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
            layoutCarouselHeader.setVisibility(View.GONE);
            svCustomerCarousel.setVisibility(View.GONE);
            stopListeningForRiders();
        }
    }

    private void calculateRewardPoints() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null || tvRewardPoints == null) return;

        FirebaseFirestore.getInstance()
            .collection("tasks")
            .whereEqualTo("customerId", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                int taskCount = queryDocumentSnapshots.size();
                double points = taskCount * 0.5;
                tvRewardPoints.setText(String.format("%.1f Pts", points));
            });
    }

    private void listenForActiveCustomerTask() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("tasks")
            .whereEqualTo("customerId", user.getUid())
            .whereEqualTo("status", "accepted")
            .limit(1)
            .addSnapshotListener((value, error) -> {
                if (error != null || !isAdded()) return;
                
                if (value != null && !value.isEmpty()) {
                    DocumentSnapshot taskDoc = value.getDocuments().get(0);
                    activeTaskId = taskDoc.getId();
                    tvActiveTaskTitle.setText("In Transit");
                    tvActiveTaskDesc.setText("Your rider is on the way");
                } else {
                    activeTaskId = null;
                    tvActiveTaskTitle.setText("No Active Task");
                    tvActiveTaskDesc.setText("Tap to create a delivery");
                }
            });
    }

    private void startListeningForRiders() {
        if (ridersListener != null) return;
        tvOnlineRidersCount.setText("Checking...");
        
        // Mock logic to read actual online riders or just active users that have role rider.
        // Assuming there is a field that marks real-time online capability, but falling back to checking how many riders exist.
        ridersListener = FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", "rider")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (isAdded()) tvOnlineRidersCount.setText("Offline");
                        return;
                    }
                    if (value != null && isAdded()) {
                        int activeCount = value.size(); // Simplified approximation of available riders. 
                        if (activeCount == 0) {
                            tvOnlineRidersCount.setText("No riders online");
                        } else {
                            tvOnlineRidersCount.setText(activeCount + " riders online");
                        }
                    }
                });
    }

    private void stopListeningForRiders() {
        if (ridersListener != null) {
            ridersListener.remove();
            ridersListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListeningForRiders();
    }

    private void openPrimaryTaskAction() {
        if (currentRole == null) {
            Toast.makeText(requireContext(), R.string.error_role_unknown, Toast.LENGTH_SHORT).show();
            return;
        }

        if ("customer".equalsIgnoreCase(currentRole)) {
            startActivity(new Intent(requireContext(), MapTaskComposerActivity.class));
        } else if ("rider".equalsIgnoreCase(currentRole)) {
            startActivity(new Intent(requireContext(), RiderTaskFeedActivity.class));
        } else {
            Toast.makeText(requireContext(), R.string.error_role_unknown, Toast.LENGTH_SHORT).show();
        }
    }
}

