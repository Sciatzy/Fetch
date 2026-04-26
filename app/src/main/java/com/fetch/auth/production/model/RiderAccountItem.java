package com.fetch.auth.production.model;

public class RiderAccountItem {

    private final String userId;
    private final String name;
    private final String email;
    private final String vehicleType;
    private final String licenseNumber;
    private final boolean active;

    public RiderAccountItem(
            String userId,
            String name,
            String email,
            String vehicleType,
            String licenseNumber,
            boolean active
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.active = active;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public boolean isActive() {
        return active;
    }
}
