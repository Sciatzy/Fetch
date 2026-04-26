package com.fetch.auth.production.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fetch.auth.production.repository.TaskRepository;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) {
            return;
        }

        int transition = event.getGeofenceTransition();
        List<Geofence> geofences = event.getTriggeringGeofences();
        if (geofences == null || geofences.isEmpty()) {
            return;
        }

        TaskRepository taskRepository = new TaskRepository();
        String state = transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "entered_dropoff" : "exited_dropoff";

        for (Geofence geofence : geofences) {
            String taskId = geofence.getRequestId();
            if (taskId == null || taskId.isEmpty()) {
                continue;
            }

            taskRepository.updateGeofenceState(taskId, state, new TaskRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    // No-op.
                }

                @Override
                public void onError(Exception error) {
                    // No-op.
                }
            });
        }
    }
}

