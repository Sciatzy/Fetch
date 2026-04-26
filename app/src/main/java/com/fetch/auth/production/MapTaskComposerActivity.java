package com.fetch.auth.production;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fetch.auth.production.location.GeofenceManager;
import com.fetch.auth.production.repository.AuthRepository;
import com.fetch.auth.production.repository.PaymentRepository;
import com.fetch.auth.production.repository.TaskRepository;
import com.fetch.auth.production.repository.UserProfileRepository;
import com.fetch.auth.production.validation.TaskValidator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapTaskComposerActivity extends AppCompatActivity {

    private static final String TAG = "MapTaskComposer";

    public static final String EXTRA_PREFILL_TITLE = "prefill_title";
    public static final String EXTRA_PREFILL_DESCRIPTION = "prefill_description";

    private EditText etMapTaskTitle;
    private EditText etMapTaskDescription;
    private TextView chipLookingForRider;
    private TextView chipPickUp;
    private TextView chipFetchNow;
    private Button btnPickup;
    private Button btnDropoff;
    private Button btnCreateTaskWithMap;
    private ProgressBar progressCreateTask;
    private TextView tvRouteInfo;

    private MapView osmMapView;
    private Marker pickupMarker;
    private Marker dropoffMarker;

    private AuthRepository authRepository;
    private UserProfileRepository userProfileRepository;
    private TaskRepository taskRepository;
    private PaymentRepository paymentRepository;
    private GeofenceManager geofenceManager;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isCustomerAuthorized;
    private boolean pickingPickup = true;

    private GeoPoint pickupPoint;
    private GeoPoint dropoffPoint;
    private GeoPoint customerCurrentPoint;
    private String pickupAddress;
    private String dropoffAddress;
    private double estimatedFee;
    private double distanceMeters;
    private boolean isPricingInProgress;

    private static final double MALAYBALAY_LAT = 8.1575;
    private static final double MALAYBALAY_LNG = 125.1278;
    private static final double BASE_FEE_PHP = 50.0;
    private static final double BASE_DISTANCE_KM = 3.0;
    private static final double EXTRA_PER_KM_PHP = 15.0;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    centerMapToCurrentLocation();
                } else {
                    Toast.makeText(this, R.string.map_location_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map_task_composer);

        authRepository = new AuthRepository();
        userProfileRepository = new UserProfileRepository();
        taskRepository = new TaskRepository();
        paymentRepository = new PaymentRepository();
        geofenceManager = new GeofenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        etMapTaskTitle = findViewById(R.id.etMapTaskTitle);
        etMapTaskDescription = findViewById(R.id.etMapTaskDescription);
        chipLookingForRider = findViewById(R.id.chipLookingForRider);
        chipPickUp = findViewById(R.id.chipPickUp);
        chipFetchNow = findViewById(R.id.chipFetchNow);
        btnPickup = findViewById(R.id.btnPickup);
        btnDropoff = findViewById(R.id.btnDropoff);
        btnCreateTaskWithMap = findViewById(R.id.btnCreateTaskWithMap);
        progressCreateTask = findViewById(R.id.progressCreateTask);
        tvRouteInfo = findViewById(R.id.tvRouteInfo);
        osmMapView = findViewById(R.id.osmMapView);

        setupMap();

        chipLookingForRider.setOnClickListener(v -> etMapTaskTitle.setText("Looking for Rider"));
        chipPickUp.setOnClickListener(v -> etMapTaskTitle.setText("Pick up"));
        chipFetchNow.setOnClickListener(v -> etMapTaskTitle.setText("Fetch now"));

        btnPickup.setOnClickListener(v -> {
            pickingPickup = true;
            Toast.makeText(this, R.string.map_pickup, Toast.LENGTH_SHORT).show();
        });

        btnDropoff.setOnClickListener(v -> {
            pickingPickup = false;
            Toast.makeText(this, R.string.map_dropoff, Toast.LENGTH_SHORT).show();
        });

        btnCreateTaskWithMap.setOnClickListener(v -> createTask());

        String prefillTitle = getIntent().getStringExtra(EXTRA_PREFILL_TITLE);
        String prefillDescription = getIntent().getStringExtra(EXTRA_PREFILL_DESCRIPTION);
        if (!TextUtils.isEmpty(prefillTitle)) {
            etMapTaskTitle.setText(prefillTitle);
        }
        if (!TextUtils.isEmpty(prefillDescription)) {
            etMapTaskDescription.setText(prefillDescription);
        }

        if (authRepository.getCurrentUser() == null) {
            Toast.makeText(this, R.string.error_user_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        verifyCustomerRole(authRepository.getCurrentUser().getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (osmMapView != null) {
            osmMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (osmMapView != null) {
            osmMapView.onPause();
        }
        super.onPause();
    }

    private void setupMap() {
        osmMapView.setTileSource(TileSourceFactory.MAPNIK);
        osmMapView.setMultiTouchControls(true);
        osmMapView.getController().setZoom(13.0);
        osmMapView.getController().setCenter(new GeoPoint(MALAYBALAY_LAT, MALAYBALAY_LNG));

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                handleMapTap(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        osmMapView.getOverlays().add(eventsOverlay);

        centerMapToCurrentLocation();
    }

    private void centerMapToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                return;
            }

            GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            customerCurrentPoint = currentPoint;
            osmMapView.getController().setCenter(currentPoint);
            osmMapView.getController().setZoom(15.0);

            // Re-run estimate if rider has already picked a route and customer location arrives later.
            estimateFeeIfReady();
        });
    }

    private void handleMapTap(GeoPoint point) {
        if (pickingPickup) {
            pickupPoint = point;
            pickupAddress = String.format(Locale.US, "%.6f, %.6f", point.getLatitude(), point.getLongitude());
            btnPickup.setText(getString(R.string.map_pickup_selected, point.getLatitude(), point.getLongitude()));

            if (pickupMarker == null) {
                pickupMarker = new Marker(osmMapView);
                pickupMarker.setTitle("Pickup");
                pickupMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_buy));
                osmMapView.getOverlays().add(pickupMarker);
            }
            pickupMarker.setPosition(point);
            pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        } else {
            dropoffPoint = point;
            dropoffAddress = String.format(Locale.US, "%.6f, %.6f", point.getLatitude(), point.getLongitude());
            btnDropoff.setText(getString(R.string.map_dropoff_selected, point.getLatitude(), point.getLongitude()));

            if (dropoffMarker == null) {
                dropoffMarker = new Marker(osmMapView);
                dropoffMarker.setTitle("Drop-off");
                dropoffMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_deliver));
                osmMapView.getOverlays().add(dropoffMarker);
            }
            dropoffMarker.setPosition(point);
            dropoffMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }

        osmMapView.invalidate();
        estimateFeeIfReady();
    }

    private void verifyCustomerRole(String uid) {
        userProfileRepository.getUserRole(uid, new UserProfileRepository.UserRoleCallback() {
            @Override
            public void onSuccess(String role) {
                isCustomerAuthorized = "customer".equalsIgnoreCase(role);
                if (!isCustomerAuthorized) {
                    Toast.makeText(MapTaskComposerActivity.this, R.string.error_customer_access_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MapTaskComposerActivity.this, R.string.error_role_check_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void estimateFeeIfReady() {
        if (pickupPoint == null || dropoffPoint == null) {
            return;
        }

        isPricingInProgress = false;

        double pickupToDropoffMeters = calculateDistanceMeters(
                pickupPoint.getLatitude(),
                pickupPoint.getLongitude(),
                dropoffPoint.getLatitude(),
                dropoffPoint.getLongitude()
        );

        double customerToPickupMeters = 0;
        if (customerCurrentPoint != null) {
            customerToPickupMeters = calculateDistanceMeters(
                    customerCurrentPoint.getLatitude(),
                    customerCurrentPoint.getLongitude(),
                    pickupPoint.getLatitude(),
                    pickupPoint.getLongitude()
            );
        }

        distanceMeters = pickupToDropoffMeters + customerToPickupMeters;

        if (distanceMeters <= 0) {
            estimatedFee = 0;
            tvRouteInfo.setText(R.string.map_fee_estimation_required);
            return;
        }

        double distanceKm = distanceMeters / 1000d;
        estimatedFee = calculateSystemFee(distanceKm);

        String info = getString(R.string.map_distance_label, distanceKm) + "\n"
                + getString(R.string.map_fee_label, estimatedFee) + "\n"
                + getString(R.string.map_pricing_rule_label, BASE_FEE_PHP, BASE_DISTANCE_KM, EXTRA_PER_KM_PHP);

        if (customerCurrentPoint != null) {
            info = info + "\n" + getString(R.string.map_customer_pickup_distance_label, customerToPickupMeters / 1000d);
        }
        tvRouteInfo.setText(info);
    }

    private void createTask() {
        if (!isCustomerAuthorized || authRepository.getCurrentUser() == null) {
            Toast.makeText(this, R.string.error_customer_access_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = TaskValidator.normalizeText(etMapTaskTitle.getText().toString());
        String description = TaskValidator.normalizeText(etMapTaskDescription.getText().toString());

        if (!TaskValidator.isValidTitle(title)) {
            etMapTaskTitle.setError(getString(R.string.error_task_title_required));
            etMapTaskTitle.requestFocus();
            return;
        }
        if (!TaskValidator.isValidDescription(description)) {
            etMapTaskDescription.setError(getString(R.string.error_task_description_required));
            etMapTaskDescription.requestFocus();
            return;
        }
        if (pickupPoint == null || dropoffPoint == null) {
            Toast.makeText(this, R.string.map_missing_locations, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPricingInProgress) {
            Toast.makeText(this, R.string.map_pricing_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        if (distanceMeters <= 0 || estimatedFee <= 0) {
            Toast.makeText(this, R.string.map_fee_estimation_required, Toast.LENGTH_SHORT).show();
            estimateFeeIfReady();
            return;
        }

        setCreateTaskLoading(true);

        Map<String, Object> pickupMap = new HashMap<>();
        pickupMap.put("lat", pickupPoint.getLatitude());
        pickupMap.put("lng", pickupPoint.getLongitude());
        pickupMap.put("placeId", null);
        pickupMap.put("address", pickupAddress);

        Map<String, Object> dropoffMap = new HashMap<>();
        dropoffMap.put("lat", dropoffPoint.getLatitude());
        dropoffMap.put("lng", dropoffPoint.getLongitude());
        dropoffMap.put("placeId", null);
        dropoffMap.put("address", dropoffAddress);

        String uid = authRepository.getCurrentUser().getUid();
        taskRepository.createTaskWithRouteData(
                title,
                description,
                null,
                uid,
                pickupMap,
                dropoffMap,
                distanceMeters,
                estimatedFee,
                new TaskRepository.TaskCreateCallback() {
                    @Override
                    public void onSuccess(String taskId) {
                        createPaymentCheckout(taskId, uid);
                        registerDropoffGeofence(taskId);

                        Intent trackingIntent = new Intent(MapTaskComposerActivity.this, TaskTrackingActivity.class);
                        trackingIntent.putExtra(TaskTrackingActivity.EXTRA_TASK_ID, taskId);
                        trackingIntent.putExtra(TaskTrackingActivity.EXTRA_IS_RIDER, false);
                        startActivity(trackingIntent);

                        Toast.makeText(MapTaskComposerActivity.this, R.string.task_created_success, Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception error) {
                        setCreateTaskLoading(false);
                        String message = error != null && !TextUtils.isEmpty(error.getMessage())
                                ? error.getMessage()
                                : getString(R.string.error_unknown);
                        Toast.makeText(MapTaskComposerActivity.this, getString(R.string.task_created_error, message), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void createPaymentCheckout(String taskId, String uid) {
        if (estimatedFee <= 0) {
            return;
        }

        paymentRepository.createPayMongoCheckout(taskId, uid, estimatedFee, new PaymentRepository.PaymentCallback() {
            @Override
            public void onSuccess(String checkoutUrl, String checkoutId) {
                taskRepository.updatePaymentMetadata(taskId, checkoutId, "checkout_created", new TaskRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        // Payment metadata is optional for demo flow.
                    }

                    @Override
                    public void onError(Exception error) {
                        android.util.Log.w(TAG, "Failed to save payment metadata", error);
                    }
                });

                if (!TextUtils.isEmpty(checkoutUrl)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl)));
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MapTaskComposerActivity.this, R.string.map_payment_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerDropoffGeofence(String taskId) {
        if (dropoffPoint == null) {
            return;
        }

        geofenceManager.registerDropoffGeofence(taskId, dropoffPoint.getLatitude(), dropoffPoint.getLongitude(), new GeofenceManager.Callback() {
            @Override
            public void onSuccess() {
                // Keep non-blocking for classroom demos.
            }

            @Override
            public void onError(Exception error) {
                // Keep task flow non-blocking if geofence registration fails.
                android.util.Log.w(TAG, "Drop-off geofence registration failed", error);
            }
        });
    }

    private double calculateDistanceMeters(double fromLat, double fromLng, double toLat, double toLng) {
        float[] result = new float[1];
        android.location.Location.distanceBetween(fromLat, fromLng, toLat, toLng, result);
        return result[0];
    }

    private void setCreateTaskLoading(boolean loading) {
        btnCreateTaskWithMap.setEnabled(!loading);
        btnCreateTaskWithMap.setText(loading ? R.string.map_creating_task : R.string.submit_task);
        btnCreateTaskWithMap.setAlpha(loading ? 0.7f : 1f);
        progressCreateTask.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private double calculateSystemFee(double distanceKm) {
        if (distanceKm <= BASE_DISTANCE_KM) {
            return BASE_FEE_PHP;
        }

        double excessKm = distanceKm - BASE_DISTANCE_KM;
        return BASE_FEE_PHP + (excessKm * EXTRA_PER_KM_PHP);
    }
}


