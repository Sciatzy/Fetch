package com.fetch.auth.production.validation;

import com.fetch.auth.production.model.TaskStatus;

public final class RiderTaskFlowValidator {

    public static final String GEOFENCE_ENTERED_PICKUP = "entered_pickup";

    private RiderTaskFlowValidator() {
        // Utility class.
    }

    public static void validateArrivedAtPickup(
            String currentStatus,
            String assignedRiderId,
            String riderId,
            String geofenceState
    ) {
        if (!TaskStatus.ACCEPTED.equals(currentStatus)) {
            throw new IllegalStateException("Task must be accepted before arrival at pickup.");
        }
        validateAssignedRider(assignedRiderId, riderId, "Only the assigned rider can mark pickup arrival.");
        if (!GEOFENCE_ENTERED_PICKUP.equals(geofenceState)) {
            throw new IllegalStateException("You must be in the pickup zone to continue.");
        }
    }

    public static void validateStartTrip(
            String currentStatus,
            String assignedRiderId,
            String riderId
    ) {
        if (!TaskStatus.ARRIVED_PICKUP.equals(currentStatus)) {
            throw new IllegalStateException("Task must be at pickup before starting trip.");
        }
        validateAssignedRider(assignedRiderId, riderId, "Only the assigned rider can start this trip.");
    }

    public static void validateArrivedAtDropoff(
            String currentStatus,
            String assignedRiderId,
            String riderId
    ) {
        if (!TaskStatus.IN_PROGRESS.equals(currentStatus)) {
            throw new IllegalStateException("Task must be in progress before arrival at drop-off.");
        }
        validateAssignedRider(assignedRiderId, riderId, "Only the assigned rider can mark drop-off arrival.");
    }

    public static void validateCompletion(
            String currentStatus,
            String assignedRiderId,
            String riderId
    ) {
        if (!TaskStatus.ARRIVED_DROPOFF.equals(currentStatus)) {
            throw new IllegalStateException("Only active tasks can be completed.");
        }
        validateAssignedRider(assignedRiderId, riderId, "Only the assigned rider can complete this task.");
    }

    public static double resolveCompletionEarnings(Double estimatedFee, Double budget) {
        if (estimatedFee != null) {
            return estimatedFee;
        }
        if (budget != null) {
            return budget;
        }
        return 0.0;
    }

    private static void validateAssignedRider(String assignedRiderId, String riderId, String message) {
        if (assignedRiderId == null || !assignedRiderId.equals(riderId)) {
            throw new IllegalStateException(message);
        }
    }
}