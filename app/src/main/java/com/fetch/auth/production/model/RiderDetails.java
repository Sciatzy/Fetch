package com.fetch.auth.production.model;

public class RiderDetails {

    private final String vehicleType;
    private final String licenseNumber;
    private final boolean verified;

    public RiderDetails(String vehicleType, String licenseNumber, boolean verified) {
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.verified = verified;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public boolean isVerified() {
        return verified;
    }
}

