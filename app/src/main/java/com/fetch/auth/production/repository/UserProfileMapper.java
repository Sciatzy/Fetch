package com.fetch.auth.production.repository;

import com.fetch.auth.production.model.CustomerDetails;
import com.fetch.auth.production.model.ProfileFields;
import com.fetch.auth.production.model.RiderDetails;
import com.fetch.auth.production.model.UserProfile;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public final class UserProfileMapper {

    private UserProfileMapper() {
        // Utility class.
    }

    public static Map<String, Object> toMap(UserProfile profile) {
        Map<String, Object> userData = new HashMap<>();
        userData.put(ProfileFields.NAME, profile.getName());
        userData.put(ProfileFields.EMAIL, profile.getEmail());
        userData.put(ProfileFields.PHONE, profile.getPhone());
        userData.put(ProfileFields.ROLE, profile.getRole());
        userData.put(ProfileFields.PROFILE_IMAGE, profile.getProfileImage());
        userData.put(ProfileFields.CREATED_AT, FieldValue.serverTimestamp());

        Map<String, Object> riderMap = new HashMap<>();
        RiderDetails riderDetails = profile.getRiderDetails();
        if (riderDetails != null) {
            riderMap.put(ProfileFields.VEHICLE_TYPE, riderDetails.getVehicleType());
            riderMap.put(ProfileFields.LICENSE_NUMBER, riderDetails.getLicenseNumber());
            riderMap.put(ProfileFields.VERIFIED, riderDetails.isVerified());
        }
        userData.put(ProfileFields.RIDER_DETAILS, riderMap);

        Map<String, Object> customerMap = new HashMap<>();
        CustomerDetails customerDetails = profile.getCustomerDetails();
        if (customerDetails != null) {
            customerMap.put(ProfileFields.SAVED_LOCATIONS, customerDetails.getSavedLocations());
        }
        userData.put(ProfileFields.CUSTOMER_DETAILS, customerMap);

        return userData;
    }
}

