package com.fetch.auth.production.pasabuy;

public class AvailRequestItem {
    private final String customerId;
    private final String customerName;
    private final String status;
    private final String transactionId;
    private final long updatedAtMillis;

    public AvailRequestItem(String customerId, String customerName, String status, String transactionId, long updatedAtMillis) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.status = status;
        this.transactionId = transactionId;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
