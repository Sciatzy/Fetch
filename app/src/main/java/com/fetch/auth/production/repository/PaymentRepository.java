package com.fetch.auth.production.repository;

import androidx.annotation.NonNull;

import com.fetch.auth.production.network.BackendApiClient;
import com.fetch.auth.production.network.PaymentCheckoutRequest;
import com.fetch.auth.production.network.PaymentCheckoutResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentRepository {

    public void createPayMongoCheckout(
            @NonNull String taskId,
            @NonNull String customerId,
            double amount,
            @NonNull PaymentCallback callback
    ) {
        PaymentCheckoutRequest request = new PaymentCheckoutRequest(taskId, customerId, amount, "PHP");
        BackendApiClient.service().createCheckout(request).enqueue(new Callback<PaymentCheckoutResponse>() {
            @Override
            public void onResponse(@NonNull Call<PaymentCheckoutResponse> call, @NonNull Response<PaymentCheckoutResponse> response) {
                PaymentCheckoutResponse body = response.body();
                if (response.isSuccessful() && body != null && body.checkoutUrl != null) {
                    callback.onSuccess(body.checkoutUrl, body.checkoutId);
                    return;
                }
                callback.onError(new IllegalStateException("Payment API failed."));
            }

            @Override
            public void onFailure(@NonNull Call<PaymentCheckoutResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    public interface PaymentCallback {
        void onSuccess(String checkoutUrl, String checkoutId);
        void onError(Exception error);
    }
}

