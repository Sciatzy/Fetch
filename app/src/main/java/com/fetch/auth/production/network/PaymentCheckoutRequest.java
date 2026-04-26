package com.fetch.auth.production.network;

public class PaymentCheckoutRequest {
    public String taskId;
    public String customerId;
    public double amount;
    public String currency;

    public PaymentCheckoutRequest(String taskId, String customerId, double amount, String currency) {
        this.taskId = taskId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
    }
}

