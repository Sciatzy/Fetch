package com.fetch.auth.production.model;

public class UserProfile {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_RIDER = "rider";
    public static final String ROLE_CUSTOMER = "customer";

    private final String name;
    private final String email;
    private final String phone;
    private final String role;
    private final String profileImage;
    private final RiderDetails riderDetails;
    private final CustomerDetails customerDetails;

    public UserProfile(
            String name,
            String email,
            String phone,
            String role,
            String profileImage,
            RiderDetails riderDetails,
            CustomerDetails customerDetails
    ) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.profileImage = profileImage;
        this.riderDetails = riderDetails;
        this.customerDetails = customerDetails;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public RiderDetails getRiderDetails() {
        return riderDetails;
    }

    public CustomerDetails getCustomerDetails() {
        return customerDetails;
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return ROLE_CUSTOMER;
        }
        String normalized = role.trim().toLowerCase();
        if (ROLE_ADMIN.equals(normalized)) {
            return ROLE_ADMIN;
        }
        if (ROLE_RIDER.equals(normalized)) {
            return ROLE_RIDER;
        }
        return ROLE_CUSTOMER;
    }
}

