package com.fetch.auth.production.model;

import java.util.ArrayList;
import java.util.List;

public class CustomerDetails {

    private final List<String> savedLocations;

    public CustomerDetails(List<String> savedLocations) {
        this.savedLocations = savedLocations != null ? savedLocations : new ArrayList<>();
    }

    public List<String> getSavedLocations() {
        return savedLocations;
    }
}

