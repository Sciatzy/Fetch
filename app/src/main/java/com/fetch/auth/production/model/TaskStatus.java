package com.fetch.auth.production.model;

public final class TaskStatus {

    private TaskStatus() {
        // Utility class.
    }

    public static final String PENDING = "pending";
    public static final String ACCEPTED = "accepted";
    public static final String IN_PROGRESS = "in_progress";
    public static final String ARRIVED_PICKUP = "arrived_pickup";
    public static final String ARRIVED_DROPOFF = "arrived_dropoff";
    public static final String COMPLETED = "completed";
}

