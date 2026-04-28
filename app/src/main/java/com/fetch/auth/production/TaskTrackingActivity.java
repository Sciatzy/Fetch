package com.fetch.auth.production;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fetch.auth.production.model.TaskFields;
import com.fetch.auth.production.model.TaskStatus;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.TaskRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Map;

public class TaskTrackingActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_IS_RIDER = "extra_is_rider";
    public static final String EXTRA_AUTO_NAVIGATE_TO_PICKUP = "extra_auto_navigate_to_pickup";

    private MapView trackingMapView;
    private TextView tvTrackingTitle;
    private TextView tvTrackingStatus;
    private TextView tvCollectHint;
    private Button btnNavigatePickup;
    private Button btnNavigateDropoff;
    private Button btnArrivedDestination;
    private Button btnCompleteAndCollect;
    private Button btnCallCustomer;
    private Button btnCancelTask;

    private TaskRepository taskRepository;
    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private ListenerRegistration taskListener;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Marker pickupMarker;
    private Marker dropoffMarker;
    private Marker riderMarker;

    private String taskId;
    private String riderId;
    private boolean isRiderMode;
    private boolean shouldAutoNavigateToPickup;
    private boolean hasAutoNavigatedToPickup;
    private boolean hasAutoNavigatedToDropoff;
    private String currentTaskStatus;
    private String selectedPaymentMethod;
    private double collectAmount;
    private GeoPoint pickupPoint;
    private GeoPoint dropoffPoint;
    private boolean hasCenteredRoute;
    private boolean isInPickupZone;
    private String lastTaskStatus;
    private String lastPickupZoneState;
    private String currentCustomerId;
    private String customerPhone;
    private boolean isCompletingTask;

    private static final double MALAYBALAY_LAT = 8.1575;
    private static final double MALAYBALAY_LNG = 125.1278;
    private static final float PICKUP_ZONE_RADIUS_METERS = 120f;
    private static final String GEOFENCE_ENTERED_PICKUP = "entered_pickup";
    private static final String GEOFENCE_EXITED_PICKUP = "exited_pickup";

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRiderLocationUpdates();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_task_tracking);

        taskRepository = new TaskRepository();
        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        trackingMapView = findViewById(R.id.trackingMapView);
        tvTrackingTitle = findViewById(R.id.tvTrackingTitle);
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus);
        tvCollectHint = findViewById(R.id.tvCollectHint);
        btnNavigatePickup = findViewById(R.id.btnNavigatePickup);
        btnNavigateDropoff = findViewById(R.id.btnNavigateDropoff);
        btnArrivedDestination = findViewById(R.id.btnArrivedDestination);
        btnCompleteAndCollect = findViewById(R.id.btnCompleteAndCollect);
        btnCallCustomer = findViewById(R.id.btnCallCustomer);
        btnCancelTask = findViewById(R.id.btnCancelTask);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        isRiderMode = getIntent().getBooleanExtra(EXTRA_IS_RIDER, false);
        shouldAutoNavigateToPickup = getIntent().getBooleanExtra(EXTRA_AUTO_NAVIGATE_TO_PICKUP, false);
        riderId = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUid() : null;

        if (TextUtils.isEmpty(taskId)) {
            finish();
            return;
        }

        if (tvTrackingTitle != null) {
            tvTrackingTitle.setText(isRiderMode ? R.string.tracking_title_rider : R.string.tracking_title);
        }

        setupMap();
        setupActions();
        applyModeUi(isRiderMode);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeToTask();

        if (isRiderMode) {
            ensureLocationPermissionAndStart();
        }
    }

    @Override
    protected void onStop() {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (trackingMapView != null) {
            trackingMapView.onPause();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (trackingMapView != null) {
            trackingMapView.onResume();
        }
    }

    private void setupMap() {
        trackingMapView.setTileSource(TileSourceFactory.MAPNIK);
        trackingMapView.setMultiTouchControls(true);
        trackingMapView.getController().setZoom(13.0);
        trackingMapView.getController().setCenter(new GeoPoint(MALAYBALAY_LAT, MALAYBALAY_LNG));
    }

    private void setupActions() {
        btnNavigatePickup.setOnClickListener(v -> {
            if (!isRiderMode || pickupPoint == null) return;
            openExternalNavigation(pickupPoint);
        });

        btnNavigateDropoff.setOnClickListener(v -> {
            if (!isRiderMode || dropoffPoint == null) return;
            openExternalNavigation(dropoffPoint);
        });

        btnArrivedDestination.setOnClickListener(v -> {
            if (!isRiderMode) return;
            handlePrimaryRiderAction();
        });

        btnCompleteAndCollect.setOnClickListener(v -> {
            if (!isRiderMode) return;

            if (isCompletingTask) {
                return;
            }

            if (TextUtils.isEmpty(selectedPaymentMethod)) {
                showCollectDialog();
                return;
            }

            completeTaskAndCollect(selectedPaymentMethod);
        });

        btnCallCustomer.setOnClickListener(v -> {
            if (!isRiderMode) {
                return;
            }
            if (TextUtils.isEmpty(customerPhone)) {
                Toast.makeText(this, R.string.tracking_customer_phone_unavailable, Toast.LENGTH_SHORT).show();
                return;
            }
            openDialer(customerPhone);
        });

        btnCancelTask.setOnClickListener(v -> handleCancelTask());
    }

    private void handleCancelTask() {
        new AlertDialog.Builder(this)
            .setTitle("Cancel Task")
            .setMessage("Are you sure you want to cancel this task?")
            .setPositiveButton("Yes", (dialog, which) -> {
                taskRepository.updateTaskStatus(taskId, TaskStatus.CANCELLED, new TaskRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(TaskTrackingActivity.this, "Task canceled successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(TaskTrackingActivity.this, "Failed to cancel task.", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void subscribeToTask() {
        taskListener = taskRepository.listenToTask(taskId, this::renderTask, error -> {
            String message = error != null ? error.getMessage() : getString(R.string.error_unknown);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void renderTask(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return;
        }

        String assignedRiderId = snapshot.getString(TaskFields.RIDER_ID);
        boolean isAssignedRider = !TextUtils.isEmpty(riderId)
                && !TextUtils.isEmpty(assignedRiderId)
                && riderId.equals(assignedRiderId);
        if (isAssignedRider != isRiderMode) {
            isRiderMode = isAssignedRider;
            applyModeUi(isRiderMode);
        }

        String status = snapshot.getString(TaskFields.STATUS);
        currentTaskStatus = status;
        tvTrackingStatus.setText(getString(R.string.tracking_status_format, status != null ? status : getString(R.string.home_role_unknown)));

        maybeCacheCustomerPhone(snapshot.getString(TaskFields.CUSTOMER_ID));
        maybeNotifyCustomerRiderArrived(status);

        Number budgetNumber = (Number) snapshot.get(TaskFields.BUDGET);
        Number estimatedFeeNumber = (Number) snapshot.get(TaskFields.ESTIMATED_FEE);
        if (budgetNumber != null) {
            collectAmount = budgetNumber.doubleValue();
        } else if (estimatedFeeNumber != null) {
            collectAmount = estimatedFeeNumber.doubleValue();
        }

        if (isRiderMode) {
            if (collectAmount > 0) {
                tvCollectHint.setText(getString(R.string.tracking_collect_amount, collectAmount));
            } else {
                tvCollectHint.setText(R.string.tracking_collect_amount_pending);
            }
        } else {
            if (collectAmount > 0) {
                tvCollectHint.setText(getString(R.string.tracking_customer_amount_hint, collectAmount));
            } else {
                tvCollectHint.setText(R.string.tracking_customer_hint);
            }
        }

        renderStaticMarkers(snapshot);
        renderRiderMarker(snapshot);
        refreshPickupZone(snapshot, status);
        maybeAutoLaunchNavigation(status);
        updateRiderActionState(status);

        if (!isRiderMode) {
            if (TaskStatus.PENDING.equals(status)) {
                btnCancelTask.setVisibility(View.VISIBLE);
            } else {
                btnCancelTask.setVisibility(View.GONE);
            }
        }

        btnNavigatePickup.setEnabled(isRiderMode && pickupPoint != null);
        btnNavigateDropoff.setEnabled(isRiderMode && dropoffPoint != null);
    }

    private void applyModeUi(boolean riderMode) {
        if (tvTrackingTitle != null) {
            tvTrackingTitle.setText(riderMode ? R.string.tracking_title_rider : R.string.tracking_title);
        }

        btnNavigatePickup.setVisibility(riderMode ? View.VISIBLE : View.GONE);
        btnNavigateDropoff.setVisibility(riderMode ? View.VISIBLE : View.GONE);

        if (!riderMode) {
            btnArrivedDestination.setVisibility(View.GONE);
            btnCompleteAndCollect.setVisibility(View.GONE);
            btnCallCustomer.setVisibility(View.GONE);
            tvCollectHint.setText(R.string.tracking_customer_hint);
            return;
        }

        btnCallCustomer.setVisibility(View.VISIBLE);
    }

    private void updateRiderActionState(String status) {
        if (!isRiderMode) {
            return;
        }

        if (TaskStatus.ACCEPTED.equals(status)) {
            btnArrivedDestination.setVisibility(View.VISIBLE);
            btnArrivedDestination.setText(R.string.tracking_arrived_pickup);
            btnArrivedDestination.setEnabled(isInPickupZone);
            btnCompleteAndCollect.setVisibility(View.GONE);
            if (!isInPickupZone) {
                tvCollectHint.setText(R.string.tracking_pickup_zone_required);
            }
            return;
        }

        if (TaskStatus.ARRIVED_PICKUP.equals(status)) {
            btnArrivedDestination.setVisibility(View.VISIBLE);
            btnArrivedDestination.setText(R.string.tracking_start_trip);
            btnArrivedDestination.setEnabled(true);
            btnCompleteAndCollect.setVisibility(View.GONE);
            return;
        }

        if (TaskStatus.IN_PROGRESS.equals(status)) {
            btnArrivedDestination.setVisibility(View.VISIBLE);
            btnArrivedDestination.setText(R.string.tracking_arrived_destination);
            btnArrivedDestination.setEnabled(true);
            btnCompleteAndCollect.setVisibility(View.GONE);
            return;
        }

        if (TaskStatus.ARRIVED_DROPOFF.equals(status)) {
            btnArrivedDestination.setVisibility(View.GONE);
            btnCompleteAndCollect.setVisibility(View.VISIBLE);
            btnCompleteAndCollect.setEnabled(true);
            return;
        }

        if (TaskStatus.COMPLETED.equals(status)) {
            btnArrivedDestination.setVisibility(View.GONE);
            btnCompleteAndCollect.setVisibility(View.GONE);
            return;
        }

        // Safe fallback for unknown states.
        btnArrivedDestination.setVisibility(View.VISIBLE);
        btnArrivedDestination.setText(R.string.tracking_arrived_destination);
        btnCompleteAndCollect.setVisibility(View.GONE);
    }

    private void handlePrimaryRiderAction() {
        if (TaskStatus.ACCEPTED.equals(currentTaskStatus)) {
            taskRepository.markArrivedAtPickup(taskId, riderId, new TaskRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TaskTrackingActivity.this, R.string.tracking_arrived_pickup_saved, Toast.LENGTH_SHORT).show();
                    Toast.makeText(TaskTrackingActivity.this, R.string.tracking_customer_notified, Toast.LENGTH_SHORT).show();
                    if (dropoffPoint != null) {
                        Toast.makeText(TaskTrackingActivity.this, R.string.tracking_auto_nav_dropoff, Toast.LENGTH_SHORT).show();
                        openExternalNavigation(dropoffPoint);
                    }
                }

                @Override
                public void onError(Exception error) {
                    String message = error != null && !TextUtils.isEmpty(error.getMessage())
                            ? error.getMessage()
                            : getString(R.string.error_unknown);
                    Toast.makeText(TaskTrackingActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (TaskStatus.ARRIVED_PICKUP.equals(currentTaskStatus)) {
            taskRepository.startTripToDropoff(taskId, riderId, new TaskRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(TaskTrackingActivity.this, R.string.tracking_trip_started, Toast.LENGTH_SHORT).show();
                    if (dropoffPoint != null) {
                        Toast.makeText(TaskTrackingActivity.this, R.string.tracking_auto_nav_dropoff, Toast.LENGTH_SHORT).show();
                        openExternalNavigation(dropoffPoint);
                    }
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(TaskTrackingActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (TaskStatus.IN_PROGRESS.equals(currentTaskStatus)) {
            taskRepository.markArrivedAtDropoff(taskId, riderId, new TaskRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    taskRepository.updateCollection(taskId, collectAmount, selectedPaymentMethod != null ? selectedPaymentMethod : "", "awaiting_collection", new TaskRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(TaskTrackingActivity.this, R.string.tracking_arrival_saved, Toast.LENGTH_SHORT).show();
                            showCollectDialog();
                        }

                        @Override
                        public void onError(Exception error) {
                            Toast.makeText(TaskTrackingActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(TaskTrackingActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
    }

    private void maybeAutoLaunchNavigation(String status) {
        if (!isRiderMode || TextUtils.isEmpty(status)) {
            return;
        }

        if (shouldAutoNavigateToPickup
                && !hasAutoNavigatedToPickup
                && TaskStatus.ACCEPTED.equals(status)
                && pickupPoint != null) {
            hasAutoNavigatedToPickup = true;
            Toast.makeText(this, R.string.tracking_auto_nav_pickup, Toast.LENGTH_SHORT).show();
            openExternalNavigation(pickupPoint);
            return;
        }

        boolean canNavigateDropoff = TaskStatus.IN_PROGRESS.equals(status)
                || TaskStatus.ARRIVED_PICKUP.equals(status);
        if (!hasAutoNavigatedToDropoff && canNavigateDropoff && dropoffPoint != null) {
            hasAutoNavigatedToDropoff = true;
            Toast.makeText(this, R.string.tracking_auto_nav_dropoff, Toast.LENGTH_SHORT).show();
            openExternalNavigation(dropoffPoint);
        }
    }

    private void renderStaticMarkers(DocumentSnapshot snapshot) {
        Map<String, Object> pickup = (Map<String, Object>) snapshot.get(TaskFields.PICKUP);
        Map<String, Object> dropoff = (Map<String, Object>) snapshot.get(TaskFields.DROPOFF);

        pickupPoint = null;
        dropoffPoint = null;

        if (pickup != null) {
            GeoPoint point = geoPointFromMap(pickup);
            if (point != null) {
                pickupPoint = point;
                if (pickupMarker == null) {
                    pickupMarker = new Marker(trackingMapView);
                    pickupMarker.setTitle("Pickup");
                    pickupMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_buy));
                    pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    trackingMapView.getOverlays().add(pickupMarker);
                }
                pickupMarker.setPosition(point);
            }
        }

        if (dropoff != null) {
            GeoPoint point = geoPointFromMap(dropoff);
            if (point != null) {
                dropoffPoint = point;
                if (dropoffMarker == null) {
                    dropoffMarker = new Marker(trackingMapView);
                    dropoffMarker.setTitle("Drop-off");
                    dropoffMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_deliver));
                    dropoffMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    trackingMapView.getOverlays().add(dropoffMarker);
                }
                dropoffMarker.setPosition(point);
            }
        }

        centerToRouteIfNeeded();

        trackingMapView.invalidate();
    }

    private void renderRiderMarker(DocumentSnapshot snapshot) {
        Map<String, Object> riderLocation = (Map<String, Object>) snapshot.get(TaskFields.RIDER_LOCATION);
        if (riderLocation == null) {
            return;
        }

        GeoPoint point = geoPointFromMap(riderLocation);
        if (point == null) {
            return;
        }

        if (riderMarker == null) {
            riderMarker = new Marker(trackingMapView);
            riderMarker.setTitle("Rider");
            trackingMapView.getOverlays().add(riderMarker);
        }

        riderMarker.setPosition(point);
        if (!hasCenteredRoute) {
            trackingMapView.getController().setCenter(point);
            trackingMapView.getController().setZoom(15.0);
            hasCenteredRoute = true;
        } else {
            trackingMapView.getController().animateTo(point);
        }
        trackingMapView.invalidate();
    }

    private GeoPoint geoPointFromMap(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Object latObj = data.get(TaskFields.LAT);
        Object lngObj = data.get(TaskFields.LNG);
        if (!(latObj instanceof Number) || !(lngObj instanceof Number)) {
            return null;
        }
        return new GeoPoint(((Number) latObj).doubleValue(), ((Number) lngObj).doubleValue());
    }

    private void centerToRouteIfNeeded() {
        if (hasCenteredRoute) {
            return;
        }

        if (pickupPoint != null && dropoffPoint != null) {
            double centerLat = (pickupPoint.getLatitude() + dropoffPoint.getLatitude()) / 2d;
            double centerLng = (pickupPoint.getLongitude() + dropoffPoint.getLongitude()) / 2d;
            trackingMapView.getController().setCenter(new GeoPoint(centerLat, centerLng));
            trackingMapView.getController().setZoom(14.0);
            hasCenteredRoute = true;
            return;
        }

        if (pickupPoint != null) {
            trackingMapView.getController().setCenter(pickupPoint);
            trackingMapView.getController().setZoom(15.0);
            hasCenteredRoute = true;
            return;
        }

        if (dropoffPoint != null) {
            trackingMapView.getController().setCenter(dropoffPoint);
            trackingMapView.getController().setZoom(15.0);
            hasCenteredRoute = true;
        }
    }

    private void openExternalNavigation(@NonNull GeoPoint destinationPoint) {
        Uri navUri = Uri.parse("google.navigation:q="
                + destinationPoint.getLatitude() + "," + destinationPoint.getLongitude());
        Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
        navIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(navIntent);
        } catch (ActivityNotFoundException notFound) {
            Uri fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + destinationPoint.getLatitude() + "," + destinationPoint.getLongitude());
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
            try {
                startActivity(fallbackIntent);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.tracking_navigation_unavailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCollectDialog() {
        if (collectAmount <= 0) {
            Toast.makeText(this, R.string.tracking_collect_amount_pending, Toast.LENGTH_SHORT).show();
            return;
        }

        String message = getString(R.string.tracking_collect_dialog_message, collectAmount);
        new AlertDialog.Builder(this)
                .setTitle(R.string.tracking_collect_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.payment_method_cash, (dialog, which) -> {
                    selectedPaymentMethod = "cash";
                    completeTaskAndCollect(selectedPaymentMethod);
                })
                .setNegativeButton(R.string.payment_method_digital, (dialog, which) -> {
                    selectedPaymentMethod = "digital";
                    completeTaskAndCollect(selectedPaymentMethod);
                })
                .show();
    }

    private void completeTaskAndCollect(@NonNull String paymentMethod) {
        if (isCompletingTask) {
            return;
        }

        isCompletingTask = true;
        btnCompleteAndCollect.setEnabled(false);

        taskRepository.updateCollection(taskId, collectAmount, paymentMethod, "collected", new TaskRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                taskRepository.markTaskCompleted(taskId, riderId, new TaskRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(TaskTrackingActivity.this, R.string.tracking_completion_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception error) {
                        isCompletingTask = false;
                        btnCompleteAndCollect.setEnabled(true);
                        Toast.makeText(TaskTrackingActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                isCompletingTask = false;
                btnCompleteAndCollect.setEnabled(true);
                Toast.makeText(TaskTrackingActivity.this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ensureLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startRiderLocationUpdates();
            return;
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void startRiderLocationUpdates() {
        if (TextUtils.isEmpty(riderId)) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() == null) {
                    return;
                }

                double lat = locationResult.getLastLocation().getLatitude();
                double lng = locationResult.getLastLocation().getLongitude();
                taskRepository.updateRiderLiveLocation(taskId, riderId, lat, lng, new TaskRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        // No-op.
                    }

                    @Override
                    public void onError(Exception error) {
                        // No-op for live updates.
                    }
                });

                // Keep pickup-zone state fresh so arrival is only possible near pickup point.
                if (pickupPoint != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            lat,
                            lng,
                            pickupPoint.getLatitude(),
                            pickupPoint.getLongitude(),
                            results
                    );
                    boolean currentlyInPickupZone = results[0] <= PICKUP_ZONE_RADIUS_METERS;
                    String desiredState = currentlyInPickupZone ? GEOFENCE_ENTERED_PICKUP : GEOFENCE_EXITED_PICKUP;
                    if (!desiredState.equals(lastPickupZoneState)) {
                        lastPickupZoneState = desiredState;
                        isInPickupZone = currentlyInPickupZone;
                        updateRiderActionState(currentTaskStatus);
                        taskRepository.updateGeofenceState(taskId, desiredState, new TaskRepository.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                // No-op.
                            }

                            @Override
                            public void onError(Exception error) {
                                // No-op for geofence refresh.
                            }
                        });
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    private void maybeCacheCustomerPhone(String customerId) {
        if (!isRiderMode || TextUtils.isEmpty(customerId)) {
            return;
        }
        if (customerId.equals(currentCustomerId) && !TextUtils.isEmpty(customerPhone)) {
            return;
        }

        currentCustomerId = customerId;
        userProfileRepository.getUserProfile(customerId, new UserProfileRepository.UserProfileDataCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                customerPhone = document.getString("phone");
            }

            @Override
            public void onError(Exception error) {
                customerPhone = null;
            }
        });
    }

    private void refreshPickupZone(DocumentSnapshot snapshot, String status) {
        if (!isRiderMode || !TaskStatus.ACCEPTED.equals(status)) {
            return;
        }

        Map<String, Object> riderLocation = (Map<String, Object>) snapshot.get(TaskFields.RIDER_LOCATION);
        GeoPoint riderPoint = geoPointFromMap(riderLocation);
        if (riderPoint == null || pickupPoint == null) {
            return;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                riderPoint.getLatitude(),
                riderPoint.getLongitude(),
                pickupPoint.getLatitude(),
                pickupPoint.getLongitude(),
                results
        );
        isInPickupZone = results[0] <= PICKUP_ZONE_RADIUS_METERS;
    }

    private void maybeNotifyCustomerRiderArrived(String status) {
        if (isRiderMode) {
            lastTaskStatus = status;
            return;
        }

        boolean statusChanged = !TextUtils.equals(lastTaskStatus, status);
        if (statusChanged && TaskStatus.ARRIVED_PICKUP.equals(status)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.tracking_rider_arrived_title)
                    .setMessage(R.string.tracking_rider_arrived_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        lastTaskStatus = status;
    }

    private void openDialer(String rawPhone) {
        String sanitized = rawPhone == null ? "" : rawPhone.replaceAll("[^0-9+]", "");
        if (TextUtils.isEmpty(sanitized)) {
            Toast.makeText(this, R.string.tracking_customer_phone_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sanitized));
        try {
            startActivity(dialIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.tracking_call_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}

