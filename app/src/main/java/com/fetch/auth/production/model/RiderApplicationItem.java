package com.fetch.auth.production.model;

public class RiderApplicationItem {

    private final String userId;
    private final String name;
    private final String email;
    private final String vehicleType;
    private final String licenseNumber;
    private final String requirementsLabel;
    private final String status;

    public RiderApplicationItem(
            String userId,
            String name,
            String email,
            String vehicleType,
            String licenseNumber,
            String requirementsLabel,
            String status
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.requirementsLabel = requirementsLabel;
        this.status = status;
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

    public String getRequirementsLabel() {
        return requirementsLabel;
    }

    public String getStatus() {
        return status;
    }
}
