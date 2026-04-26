package com.fetch.auth.production;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RoutePreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PICKUP_LAT = "extra_pickup_lat";
    public static final String EXTRA_PICKUP_LNG = "extra_pickup_lng";
    public static final String EXTRA_DROPOFF_LAT = "extra_dropoff_lat";
    public static final String EXTRA_DROPOFF_LNG = "extra_dropoff_lng";
    public static final String EXTRA_PICKUP_ADDRESS = "extra_pickup_address";
    public static final String EXTRA_DROPOFF_ADDRESS = "extra_dropoff_address";
    public static final String EXTRA_ESTIMATED_FEE = "extra_estimated_fee";
    private static final String TAG = "RoutePreviewActivity";
    private static final float ROUTE_STROKE_WIDTH = 12f;

    private MapView previewMapView;
    private Polyline routeLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_route_preview);

        previewMapView = findViewById(R.id.previewMapView);
        TextView tvPreviewAmount = findViewById(R.id.tvPreviewAmount);
        TextView tvPreviewPickup = findViewById(R.id.tvPreviewPickup);
        TextView tvPreviewDropoff = findViewById(R.id.tvPreviewDropoff);
        Button btnPreviewClose = findViewById(R.id.btnPreviewClose);

        double pickupLat = getIntent().getDoubleExtra(EXTRA_PICKUP_LAT, Double.NaN);
        double pickupLng = getIntent().getDoubleExtra(EXTRA_PICKUP_LNG, Double.NaN);
        double dropoffLat = getIntent().getDoubleExtra(EXTRA_DROPOFF_LAT, Double.NaN);
        double dropoffLng = getIntent().getDoubleExtra(EXTRA_DROPOFF_LNG, Double.NaN);
        String pickupAddress = getIntent().getStringExtra(EXTRA_PICKUP_ADDRESS);
        String dropoffAddress = getIntent().getStringExtra(EXTRA_DROPOFF_ADDRESS);
        double estimatedFee = getIntent().getDoubleExtra(EXTRA_ESTIMATED_FEE, 0d);

        tvPreviewAmount.setText(getString(R.string.task_budget_amount, estimatedFee));
        tvPreviewPickup.setText(getString(
                R.string.task_route_pickup,
                !TextUtils.isEmpty(pickupAddress) ? pickupAddress : "-"
        ));
        tvPreviewDropoff.setText(getString(
                R.string.task_route_dropoff,
                !TextUtils.isEmpty(dropoffAddress) ? dropoffAddress : "-"
        ));

        setupMap(pickupLat, pickupLng, dropoffLat, dropoffLng);

        btnPreviewClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (previewMapView != null) {
            previewMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (previewMapView != null) {
            previewMapView.onPause();
        }
        super.onPause();
    }

    private void setupMap(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) {
        previewMapView.setTileSource(TileSourceFactory.MAPNIK);
        previewMapView.setMultiTouchControls(true);

        if (Double.isNaN(pickupLat) || Double.isNaN(pickupLng) || Double.isNaN(dropoffLat) || Double.isNaN(dropoffLng)) {
            previewMapView.getController().setZoom(13.0);
            previewMapView.getController().setCenter(new GeoPoint(8.1575, 125.1278));
            return;
        }

        GeoPoint pickupPoint = new GeoPoint(pickupLat, pickupLng);
        GeoPoint dropoffPoint = new GeoPoint(dropoffLat, dropoffLng);

        Marker pickupMarker = new Marker(previewMapView);
        pickupMarker.setPosition(pickupPoint);
        pickupMarker.setTitle("Pickup");
        pickupMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_buy));
        pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        previewMapView.getOverlays().add(pickupMarker);

        Marker dropoffMarker = new Marker(previewMapView);
        dropoffMarker.setPosition(dropoffPoint);
        dropoffMarker.setTitle("Drop-off");
        dropoffMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_pin_deliver));
        dropoffMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        previewMapView.getOverlays().add(dropoffMarker);

        fetchAndDrawRoadRoute(pickupPoint, dropoffPoint);

        double centerLat = (pickupLat + dropoffLat) / 2d;
        double centerLng = (pickupLng + dropoffLng) / 2d;
        previewMapView.getController().setCenter(new GeoPoint(centerLat, centerLng));
        previewMapView.getController().setZoom(14.0);
        previewMapView.invalidate();
    }

    private void fetchAndDrawRoadRoute(GeoPoint pickupPoint, GeoPoint dropoffPoint) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String routeUrl = String.format(
                        Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        pickupPoint.getLongitude(),
                        pickupPoint.getLatitude(),
                        dropoffPoint.getLongitude(),
                        dropoffPoint.getLatitude()
                );

                connection = (HttpURLConnection) new URL(routeUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("OSRM HTTP error: " + responseCode);
                }

                String responseBody = readResponseBody(connection.getInputStream());
                List<GeoPoint> routePoints = parseOsrmRoutePoints(responseBody);

                runOnUiThread(() -> {
                    if (routePoints.isEmpty()) {
                        drawFallbackStraightRoute(pickupPoint, dropoffPoint);
                        Toast.makeText(this, "No road route found. Showing direct line.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    drawRoutePolyline(routePoints);
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch OSRM route", e);
                runOnUiThread(() -> {
                    drawFallbackStraightRoute(pickupPoint, dropoffPoint);
                    Toast.makeText(this, "Unable to load route. Showing direct line.", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String readResponseBody(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private List<GeoPoint> parseOsrmRoutePoints(String responseBody) throws JSONException {
        List<GeoPoint> points = new ArrayList<>();
        JSONObject root = new JSONObject(responseBody);
        JSONArray routes = root.optJSONArray("routes");
        if (routes == null || routes.length() == 0) {
            return points;
        }

        JSONObject firstRoute = routes.optJSONObject(0);
        if (firstRoute == null) {
            return points;
        }

        JSONObject geometry = firstRoute.optJSONObject("geometry");
        if (geometry == null) {
            return points;
        }

        JSONArray coordinates = geometry.optJSONArray("coordinates");
        if (coordinates == null) {
            return points;
        }

        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray coordinate = coordinates.optJSONArray(i);
            if (coordinate == null || coordinate.length() < 2) {
                continue;
            }
            double lon = coordinate.optDouble(0, Double.NaN);
            double lat = coordinate.optDouble(1, Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                points.add(new GeoPoint(lat, lon));
            }
        }

        return points;
    }

    private void drawRoutePolyline(List<GeoPoint> points) {
        if (routeLine != null) {
            previewMapView.getOverlays().remove(routeLine);
        }
        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.brand_blue));
        routeLine.getOutlinePaint().setStrokeWidth(ROUTE_STROKE_WIDTH);
        previewMapView.getOverlays().add(routeLine);
        previewMapView.invalidate();
    }

    private void drawFallbackStraightRoute(GeoPoint pickupPoint, GeoPoint dropoffPoint) {
        drawRoutePolyline(Arrays.asList(pickupPoint, dropoffPoint));
    }
}

