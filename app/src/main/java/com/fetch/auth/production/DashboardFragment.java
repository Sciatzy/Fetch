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

import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.fetch.auth.production.util.StorageBackedImageLoader;
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
    
    // New Service Shortcuts
    private TextView btnClaimRewards;
    private LinearLayout btnServiceRide;
    private LinearLayout btnServiceBills;
    private LinearLayout btnServiceFood;
    private LinearLayout btnServiceFetch;

    private LinearLayout btnPasaBuy;
    private LinearLayout btnTransactionHistory;
    private LinearLayout btnRewardsHistory;

    private LinearLayout llOnlineRidersContainer;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private String currentRole;

    private MaterialCardView cardRiderStats;
    private TextView tvRiderEarnings;
    private TextView tvRiderCompletedTasks;
    private Button btnViewAnalytics;
    
    private MaterialCardView cardCustomerDashboard;
    private LinearLayout llCustomerOnlineRiders;

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
        tvActiveTaskTitle = view.findViewById(R.id .tvActiveTaskTitle);
        tvActiveTaskDesc = view.findViewById(R.id.tvActiveTaskDesc);
        cardQuickService = view.findViewById(R.id.cardQuickService);
        cardWallet = view.findViewById(R.id.cardWallet);
        tvRewardPoints = view.findViewById(R.id.tvRewardPoints);
        cardPromo = view.findViewById(R.id.cardPromo);
        tvOnlineRidersCount = view.findViewById(R.id.tvOnlineRidersCount);

        cardRiderStats = view.findViewById(R.id.cardRiderStats);
        tvRiderEarnings = view.findViewById(R.id.tvRiderEarnings);
        tvRiderCompletedTasks = view.findViewById(R.id.tvRiderCompletedTasks);
        btnViewAnalytics = view.findViewById(R.id.btnViewAnalytics);
        
        cardCustomerDashboard = view.findViewById(R.id.cardCustomerDashboard);
        llCustomerOnlineRiders = view.findViewById(R.id.llCustomerOnlineRiders);
        
        btnClaimRewards = view.findViewById(R.id.btnClaimRewards);
        btnServiceRide = view.findViewById(R.id.btnServiceRide);
        btnServiceBills = view.findViewById(R.id.btnServiceBills);
        btnServiceFood = view.findViewById(R.id.btnServiceFood);
        btnServiceFetch = view.findViewById(R.id.btnServiceFetch);

        btnPasaBuy = view.findViewById(R.id.btnPasaBuy);
        btnTransactionHistory = view.findViewById(R.id.btnTransactionHistory);
        btnRewardsHistory = view.findViewById(R.id.btnRewardsHistory);

        llOnlineRidersContainer = view.findViewById(R.id.llOnlineRidersContainer);

        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            loadDashboardData(user);
        }

        btnPrimaryAction.setOnClickListener(v -> openPrimaryTaskAction());
        
        if (btnClaimRewards != null) {
            btnClaimRewards.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RewardsFragment())
                    .addToBackStack(null)
                    .commit();
            });
        }
        
        if (btnServiceRide != null) {
            btnServiceRide.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapTaskComposerActivity.class);
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_TITLE, "Find a Ride");
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_DESCRIPTION, "Fetching a ride to my destination.");
                startActivity(intent);
            });
        }
        
        if (btnServiceBills != null) {
            btnServiceBills.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapTaskComposerActivity.class);
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_TITLE, "Pay Bill");
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_DESCRIPTION, "Fetch the cash payment to pay my bill.");
                startActivity(intent);
            });
        }
        
        if (btnServiceFood != null) {
            btnServiceFood.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapTaskComposerActivity.class);
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_TITLE, "Order Food");
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_DESCRIPTION, "Buy and deliver food from my choice of place.");
                startActivity(intent);
            });
        }
        
        if (btnServiceFetch != null) {
            btnServiceFetch.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MapTaskComposerActivity.class);
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_TITLE, "Fetch Item");
                intent.putExtra(MapTaskComposerActivity.EXTRA_PREFILL_DESCRIPTION, "Pickup and drop-off an item.");
                startActivity(intent);
            });
        }
        
        if (btnPasaBuy != null) {
            btnPasaBuy.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.fetch.auth.production.pasabuy.PasaBuyFeedActivity.class);
                intent.putExtra("USER_ROLE", currentRole);
                startActivity(intent);
            });
        }
        
        if (btnTransactionHistory != null) {
            btnTransactionHistory.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TransactionHistoryActivity.class);
                intent.putExtra(TransactionHistoryActivity.EXTRA_USER_ROLE, currentRole);
                startActivity(intent);
            });
        }

        if (btnRewardsHistory != null) {
            btnRewardsHistory.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), RewardsHistoryActivity.class);
                startActivity(intent);
            });
        }

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
                if (TextUtils.isEmpty(profileImage)) {
                    profileImage = document.getString("profileImageUrl");
                }
                
                Double totalEarnings = document.getDouble("totalEarnings");
                Long totalTasks = document.getLong("overallAcceptedTasks");

                tvHomeName.setText(!TextUtils.isEmpty(name)
                        ? getString(R.string.home_hello_name, name)
                        : getString(R.string.home_hello));
                currentRole = !TextUtils.isEmpty(role) ? role : getString(R.string.home_role_unknown);
                
                if (tvRiderEarnings != null && tvRiderCompletedTasks != null) {
                    tvRiderEarnings.setText(String.format("PHP %.2f", totalEarnings != null ? totalEarnings : 0.0));
                    tvRiderCompletedTasks.setText(String.valueOf(totalTasks != null ? totalTasks : 0));
                }

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
        StorageBackedImageLoader.load(ivHomeAvatar, profileImageUrl, R.drawable.fetch_logo, true);
    }

    private void bindPrimaryActionByRole(String role) {
        if (role == null) {
            btnPrimaryAction.setEnabled(false);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
            layoutCarouselHeader.setVisibility(View.GONE);
            svCustomerCarousel.setVisibility(View.GONE);
            cardRiderStats.setVisibility(View.GONE);
            if (cardCustomerDashboard != null) cardCustomerDashboard.setVisibility(View.GONE);
            if (llCustomerOnlineRiders != null) llCustomerOnlineRiders.setVisibility(View.GONE);
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
            cardRiderStats.setVisibility(View.GONE);
            if (cardCustomerDashboard != null) cardCustomerDashboard.setVisibility(View.VISIBLE);
            if (llCustomerOnlineRiders != null) llCustomerOnlineRiders.setVisibility(View.VISIBLE);
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
            cardRiderStats.setVisibility(View.VISIBLE);
            
            if (cardCustomerDashboard != null) cardCustomerDashboard.setVisibility(View.GONE);
            if (llCustomerOnlineRiders != null) llCustomerOnlineRiders.setVisibility(View.GONE);
            
            btnViewAnalytics.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), RiderAnalyticsActivity.class));
            });
            
            cardRiderStats.setOnClickListener(v -> {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                    requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_tasks);
                }
            });
            
            stopListeningForRiders();
        } else {
            btnPrimaryAction.setEnabled(false);
            btnPrimaryAction.setText(R.string.open_tasks);
            tvHomePrimaryHint.setText(R.string.home_subtitle);
            layoutCarouselHeader.setVisibility(View.GONE);
            svCustomerCarousel.setVisibility(View.GONE);
            cardRiderStats.setVisibility(View.GONE);
            
            if (cardCustomerDashboard != null) cardCustomerDashboard.setVisibility(View.GONE);
            if (llCustomerOnlineRiders != null) llCustomerOnlineRiders.setVisibility(View.GONE);
            
            stopListeningForRiders();
        }
    }

    private void calculateRewardPoints() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user == null || tvRewardPoints == null) return;

        FirebaseFirestore.getInstance()
            .collection("tasks")
            .whereEqualTo("customerId", user.getUid())
            .whereEqualTo("status", "completed")
            .addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null || !isAdded() || queryDocumentSnapshots == null) return;
                
                double totalPoints = 0.0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    Double amount = doc.getDouble("estimatedFee");
                    if (amount == null) amount = doc.getDouble("budget");
                    if (amount != null && amount > 0) {
                        totalPoints += Math.floor(amount / 50.0) * 0.5;
                    }
                }
                
                tvRewardPoints.setText(String.format("%.1f Pts", totalPoints));
                
                // Write points dynamically to user's profile to align with Rewards screen natively
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("points", totalPoints);
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
        if (tvOnlineRidersCount != null) {
            tvOnlineRidersCount.setText("Checking...");
        }
        
        ridersListener = FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", "rider")
                .whereEqualTo("isOnline", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (isAdded() && tvOnlineRidersCount != null) tvOnlineRidersCount.setText("Offline");
                        return;
                    }
                    if (value != null && isAdded()) {
                        int activeCount = value.size(); 
                        if (tvOnlineRidersCount != null) {
                            if (activeCount == 0) {
                                tvOnlineRidersCount.setText("No riders online");
                            } else {
                                tvOnlineRidersCount.setText(activeCount + " riders online");
                            }
                        }

                        if (llOnlineRidersContainer != null) {
                            llOnlineRidersContainer.removeAllViews();
                            if (activeCount == 0) {
                                TextView tvEmpty = new TextView(requireContext());
                                tvEmpty.setText("No online riders nearby.");
                                tvEmpty.setTextColor(0xFF757575); // Grey
                                tvEmpty.setPadding(16, 16, 16, 16);
                                llOnlineRidersContainer.addView(tvEmpty);
                            } else {
                                LayoutInflater inflater = LayoutInflater.from(requireContext());
                                for (DocumentSnapshot doc : value.getDocuments()) {
                                    View riderView = inflater.inflate(R.layout.item_online_rider, llOnlineRidersContainer, false);
                                    
                                    TextView tvRiderName = riderView.findViewById(R.id.tvRiderName);
                                    TextView tvRiderLocation = riderView.findViewById(R.id.tvRiderLocation);
                                    ImageView ivRiderAvatar = riderView.findViewById(R.id.ivRiderAvatar);
                                    
                                    String name = doc.getString("name");
                                    String profileImage = doc.getString("profileImage");
                                    if (TextUtils.isEmpty(profileImage)) {
                                        profileImage = doc.getString("profileImageUrl");
                                    }
                                    
                                    tvRiderName.setText(!TextUtils.isEmpty(name) ? name : "Rider");
                                    
                                    // Mock location text
                                    int randomDistance = (int)(Math.random() * 5) + 1;
                                    tvRiderLocation.setText(randomDistance + " km away");
                                    
                                    StorageBackedImageLoader.load(ivRiderAvatar, profileImage, R.drawable.fetch_logo, true);
                                    
                                    llOnlineRidersContainer.addView(riderView);
                                }
                            }
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
