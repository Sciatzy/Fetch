package com.fetch.auth.production.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ErrandTask {

    private String taskId;
    private String title;
    private String description;
    private Double budget;
    private String status;
    private String customerId;
    private String riderId;
    private Double distanceMeters;
    private Double estimatedFee;
    private String pickupAddress;
    private String dropoffAddress;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;

    @ServerTimestamp
    private Date createdAt;

    public ErrandTask() {
        // Needed for Firebase serialization.
    }

    public ErrandTask(
            String taskId,
            String title,
            String description,
            Double budget,
            String status,
            String customerId,
            String riderId,
            Double distanceMeters,
            Double estimatedFee,
            String pickupAddress,
            String dropoffAddress,
            Double pickupLat,
            Double pickupLng,
            Double dropoffLat,
            Double dropoffLng,
            Date createdAt
    ) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.budget = budget;
        this.status = status;
        this.customerId = customerId;
        this.riderId = riderId;
        this.distanceMeters = distanceMeters;
        this.estimatedFee = estimatedFee;
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.createdAt = createdAt;
    }

    public static ErrandTask fromDocument(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        Number budgetNumber = document.getDouble(TaskFields.BUDGET);
        Double budgetValue = budgetNumber != null ? budgetNumber.doubleValue() : null;

        Number distanceMetersNumber = document.getDouble(TaskFields.DISTANCE_METERS);
        Double distanceMeters = distanceMetersNumber != null ? distanceMetersNumber.doubleValue() : null;

        Number estimatedFeeNumber = document.getDouble(TaskFields.ESTIMATED_FEE);
        Double estimatedFee = estimatedFeeNumber != null ? estimatedFeeNumber.doubleValue() : null;

        String pickupAddress = null;
        String dropoffAddress = null;
        Double pickupLat = null;
        Double pickupLng = null;
        Double dropoffLat = null;
        Double dropoffLng = null;

        Object pickupRaw = document.get(TaskFields.PICKUP);
        if (pickupRaw instanceof java.util.Map) {
            java.util.Map<?, ?> pickupMap = (java.util.Map<?, ?>) pickupRaw;
            Object pickupAddressRaw = pickupMap.get(TaskFields.ADDRESS);
            pickupAddress = pickupAddressRaw instanceof String ? (String) pickupAddressRaw : null;
            Object pickupLatRaw = pickupMap.get(TaskFields.LAT);
            if (pickupLatRaw instanceof Number) pickupLat = ((Number) pickupLatRaw).doubleValue();
            Object pickupLngRaw = pickupMap.get(TaskFields.LNG);
            if (pickupLngRaw instanceof Number) pickupLng = ((Number) pickupLngRaw).doubleValue();
        }

        Object dropoffRaw = document.get(TaskFields.DROPOFF);
        if (dropoffRaw instanceof java.util.Map) {
            java.util.Map<?, ?> dropoffMap = (java.util.Map<?, ?>) dropoffRaw;
            Object dropoffAddressRaw = dropoffMap.get(TaskFields.ADDRESS);
            dropoffAddress = dropoffAddressRaw instanceof String ? (String) dropoffAddressRaw : null;
            Object dropoffLatRaw = dropoffMap.get(TaskFields.LAT);
            if (dropoffLatRaw instanceof Number) dropoffLat = ((Number) dropoffLatRaw).doubleValue();
            Object dropoffLngRaw = dropoffMap.get(TaskFields.LNG);
            if (dropoffLngRaw instanceof Number) dropoffLng = ((Number) dropoffLngRaw).doubleValue();
        }

        return new ErrandTask(
                document.getString(TaskFields.TASK_ID),
                document.getString(TaskFields.TITLE),
                document.getString(TaskFields.DESCRIPTION),
                budgetValue,
                document.getString(TaskFields.STATUS),
                document.getString(TaskFields.CUSTOMER_ID),
                document.getString(TaskFields.RIDER_ID),
                distanceMeters,
                estimatedFee,
                pickupAddress,
                dropoffAddress,
                pickupLat,
                pickupLng,
                dropoffLat,
                dropoffLng,
                document.getDate(TaskFields.CREATED_AT)
        );
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Double getBudget() {
        return budget;
    }

    public String getStatus() {
        return status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRiderId() {
        return riderId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public Double getEstimatedFee() {
        return estimatedFee;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public Double getPickupLat() {
        return pickupLat;
    }

    public Double getPickupLng() {
        return pickupLng;
    }

    public Double getDropoffLat() {
        return dropoffLat;
    }

    public Double getDropoffLng() {
        return dropoffLng;
    }
}

