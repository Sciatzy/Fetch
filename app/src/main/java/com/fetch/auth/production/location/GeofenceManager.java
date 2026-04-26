package com.fetch.auth.production.location;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;

public class GeofenceManager {

    private static final float DEFAULT_RADIUS_METERS = 120f;

    private final Context context;
    private final GeofencingClient geofencingClient;

    public GeofenceManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    public void registerDropoffGeofence(
            @NonNull String taskId,
            double lat,
            double lng,
            @NonNull Callback callback
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onError(new SecurityException("Location permission is required for geofencing."));
            return;
        }

        Geofence geofence = new Geofence.Builder()
                .setRequestId(taskId)
                .setCircularRegion(lat, lng, DEFAULT_RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(Collections.singletonList(geofence))
                .build();

        geofencingClient.addGeofences(request, geofencePendingIntent())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void removeGeofence(@NonNull String taskId, @NonNull Callback callback) {
        geofencingClient.removeGeofences(Collections.singletonList(taskId))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private PendingIntent geofencePendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }

    public interface Callback {
        void onSuccess();
        void onError(Exception error);
    }
}

