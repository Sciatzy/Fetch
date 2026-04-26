package com.fetch.auth.production.model;

public class RiderDetails {

    private final String vehicleType;
    private final String licenseNumber;
    private final String age;
    private final String birthplace;
    private final boolean verified;

    public RiderDetails(String vehicleType, String licenseNumber, String age, String birthplace, boolean verified) {
        this.vehicleType = vehicleType;
        this.licenseNumber = licenseNumber;
        this.age = age;
        this.birthplace = birthplace;
        this.verified = verified;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getAge() {
        return age;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public boolean isVerified() {
        return verified;
    }
}

