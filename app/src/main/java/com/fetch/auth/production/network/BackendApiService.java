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

    @POST("v1/fcm/pasabuy-status")
    Call<Void> sendPasabuyStatusPush(@Body PasabuyStatusPushRequest request);

    @POST("v1/fcm/user-event")
    Call<Void> sendUserEventPush(@Body UserEventPushRequest request);

    @POST("v1/fcm/role-event")
    Call<Void> sendRoleEventPush(@Body RoleEventPushRequest request);
}
