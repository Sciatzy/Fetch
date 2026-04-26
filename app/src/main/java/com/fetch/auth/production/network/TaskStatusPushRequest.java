package com.fetch.auth.production.network;

public class TaskStatusPushRequest {
    public String taskId;
    public String status;
    public boolean isRider;

    public TaskStatusPushRequest(String taskId, String status, boolean isRider) {
        this.taskId = taskId;
        this.status = status;
        this.isRider = isRider;
    }
}
