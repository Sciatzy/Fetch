package com.fetch.auth.production.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BackendApiService {

    @POST("v1/pricing/estimate")
    Call<PricingResponse> estimatePricing(@Body PricingRequest request);

    @POST("v1/payments/paymongo/checkout")
    Call<PaymentCheckoutResponse> createCheckout(@Body PaymentCheckoutRequest request);

    @POST("v1/fcm/register")
    Call<Void> registerFcmToken(@Body FcmTokenRequest request);

    @POST("v1/fcm/task-status")
    Call<Void> sendTaskStatusPush(@Body TaskStatusPushRequest request);
}

