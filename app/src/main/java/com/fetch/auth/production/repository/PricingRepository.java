package com.fetch.auth.production.repository;

import androidx.annotation.NonNull;

import com.fetch.auth.production.network.BackendApiClient;
import com.fetch.auth.production.network.PricingRequest;
import com.fetch.auth.production.network.PricingResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PricingRepository {

    public void estimateFee(
            double pickupLat,
            double pickupLng,
            double dropoffLat,
            double dropoffLng,
            @NonNull PricingCallback callback
    ) {
        PricingRequest request = new PricingRequest(pickupLat, pickupLng, dropoffLat, dropoffLng);
        BackendApiClient.service().estimatePricing(request).enqueue(new Callback<PricingResponse>() {
            @Override
            public void onResponse(@NonNull Call<PricingResponse> call, @NonNull Response<PricingResponse> response) {
                PricingResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    callback.onSuccess(body.distanceMeters, body.estimatedFee);
                    return;
                }
                callback.onError(new IllegalStateException("Pricing API failed."));
            }

            @Override
            public void onFailure(@NonNull Call<PricingResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    public interface PricingCallback {
        void onSuccess(double distanceMeters, double estimatedFee);
        void onError(Exception error);
    }
}

