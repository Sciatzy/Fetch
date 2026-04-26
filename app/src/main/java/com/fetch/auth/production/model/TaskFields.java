package com.fetch.auth.production.model;

public final class TaskFields {

    private TaskFields() {
        // Utility class.
    }

    public static final String TASK_ID = "taskId";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String BUDGET = "budget";
    public static final String STATUS = "status";
    public static final String CUSTOMER_ID = "customerId";
    public static final String RIDER_ID = "riderId";
    public static final String CREATED_AT = "createdAt";
    public static final String LOCATION = "location";
    public static final String LAT = "lat";
    public static final String LNG = "lng";

    public static final String PICKUP = "pickup";
    public static final String DROPOFF = "dropoff";
    public static final String PLACE_ID = "placeId";
    public static final String ADDRESS = "address";

    public static final String DISTANCE_METERS = "distanceMeters";
    public static final String ESTIMATED_FEE = "estimatedFee";
    public static final String CURRENCY = "currency";

    public static final String PAYMENT_STATUS = "paymentStatus";
    public static final String PAYMENT_PROVIDER = "paymentProvider";
    public static final String PAYMENT_CHECKOUT_ID = "paymentCheckoutId";
    public static final String PAYMENT_METHOD = "paymentMethod";

    public static final String RIDER_LOCATION = "riderLocation";
    public static final String RIDER_LOCATION_UPDATED_AT = "riderLocationUpdatedAt";

    public static final String COLLECTABLE_AMOUNT = "collectableAmount";
    public static final String COLLECTION_STATUS = "collectionStatus";
    public static final String COLLECTED_AT = "collectedAt";

    public static final String GEOFENCE_STATE = "geofenceState";
    public static final String LAST_STATUS_UPDATED_AT = "lastStatusUpdatedAt";
}

