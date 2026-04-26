package com.fetch.auth.production.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.model.ErrandTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskFeedAdapter extends RecyclerView.Adapter<TaskFeedAdapter.TaskViewHolder> {

    private final List<ErrandTask> tasks = new ArrayList<>();
    private final Set<String> processingTaskIds = new HashSet<>();
    private final TaskActionListener taskActionListener;

    public TaskFeedAdapter(TaskActionListener taskActionListener) {
        this.taskActionListener = taskActionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ErrandTask task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        bindRouteDetails(holder, task);

        if (task.getBudget() != null) {
            holder.tvBudget.setText(holder.itemView.getContext().getString(R.string.task_budget_amount, task.getBudget()));
        } else {
            holder.tvBudget.setText(R.string.task_budget_not_set);
        }

        boolean isProcessing = processingTaskIds.contains(task.getTaskId());
        holder.btnAccept.setEnabled(!isProcessing);
        holder.btnAccept.setText(isProcessing
                ? holder.itemView.getContext().getString(R.string.accepting_task)
                : holder.itemView.getContext().getString(R.string.accept_task));

        holder.btnAccept.setOnClickListener(v -> {
            if (task.getTaskId() == null || processingTaskIds.contains(task.getTaskId())) {
                return;
            }
            processingTaskIds.add(task.getTaskId());
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPosition);
            }
            taskActionListener.onAcceptTask(task);
        });

        holder.btnPreviewRoute.setOnClickListener(v -> taskActionListener.onPreviewRoute(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void submitList(List<ErrandTask> newTasks) {
        tasks.clear();
        if (newTasks != null) {
            tasks.addAll(newTasks);
        }
        notifyDataSetChanged();
    }

    public void clearProcessing(String taskId) {
        if (taskId == null) {
            return;
        }

        if (processingTaskIds.remove(taskId)) {
            int index = findIndexByTaskId(taskId);
            if (index >= 0) {
                notifyItemChanged(index);
            }
        }
    }

    private int findIndexByTaskId(String taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (taskId.equals(tasks.get(i).getTaskId())) {
                return i;
            }
        }
        return -1;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvDescription;
        TextView tvBudget;
        TextView tvRouteEstimate;
        TextView tvPickupAddress;
        TextView tvDropoffAddress;
        Button btnPreviewRoute;
        Button btnAccept;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvBudget = itemView.findViewById(R.id.tvTaskBudget);
            tvRouteEstimate = itemView.findViewById(R.id.tvRouteEstimate);
            tvPickupAddress = itemView.findViewById(R.id.tvPickupAddress);
            tvDropoffAddress = itemView.findViewById(R.id.tvDropoffAddress);
            btnPreviewRoute = itemView.findViewById(R.id.btnPreviewRoute);
            btnAccept = itemView.findViewById(R.id.btnAcceptTask);
        }
    }

    private void bindRouteDetails(@NonNull TaskViewHolder holder, @NonNull ErrandTask task) {
        if (task.getDistanceMeters() != null && task.getDistanceMeters() > 0) {
            double distanceKm = task.getDistanceMeters() / 1000d;
            int minutes = Math.max(1, (int) Math.ceil(distanceKm / 0.4d));
            holder.tvRouteEstimate.setText(holder.itemView.getContext().getString(
                    R.string.task_route_estimate,
                    distanceKm,
                    minutes
            ));
        } else {
            holder.tvRouteEstimate.setText(R.string.task_route_unknown);
        }

        String pickup = task.getPickupAddress();
        String dropoff = task.getDropoffAddress();
        holder.tvPickupAddress.setText(holder.itemView.getContext().getString(
                R.string.task_route_pickup,
                pickup != null && !pickup.trim().isEmpty() ? pickup : "-"
        ));
        holder.tvDropoffAddress.setText(holder.itemView.getContext().getString(
                R.string.task_route_dropoff,
                dropoff != null && !dropoff.trim().isEmpty() ? dropoff : "-"
        ));

        boolean hasRouteCoordinates = task.getPickupLat() != null
                && task.getPickupLng() != null
                && task.getDropoffLat() != null
                && task.getDropoffLng() != null;
        holder.btnPreviewRoute.setEnabled(hasRouteCoordinates);
        holder.btnPreviewRoute.setAlpha(hasRouteCoordinates ? 1f : 0.5f);
    }

    public interface TaskActionListener {
        void onAcceptTask(ErrandTask task);
        void onPreviewRoute(ErrandTask task);
    }
}


