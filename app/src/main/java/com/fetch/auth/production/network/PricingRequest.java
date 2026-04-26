package com.fetch.auth.production.network;

public class PricingRequest {
    public double pickupLat;
    public double pickupLng;
    public double dropoffLat;
    public double dropoffLng;

    public PricingRequest(double pickupLat, double pickupLng, double dropoffLat, double dropoffLng) {
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
    }
}

