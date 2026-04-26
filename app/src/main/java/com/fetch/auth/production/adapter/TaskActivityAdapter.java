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
import com.fetch.auth.production.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskActivityAdapter extends RecyclerView.Adapter<TaskActivityAdapter.TaskActivityViewHolder> {

    private final List<ErrandTask> tasks = new ArrayList<>();
    private final TaskClickListener listener;

    public TaskActivityAdapter(TaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_task, parent, false);
        return new TaskActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskActivityViewHolder holder, int position) {
        ErrandTask task = tasks.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());

        String status = task.getStatus() != null ? task.getStatus() : holder.itemView.getContext().getString(R.string.home_role_unknown);
        holder.tvStatus.setText(formatStatusLabel(status));

        if (task.getBudget() != null) {
            holder.tvBudget.setText(holder.itemView.getContext().getString(R.string.task_budget_amount, task.getBudget()));
        } else {
            holder.tvBudget.setText(R.string.task_budget_not_set);
        }

        holder.btnTrack.setText(resolveTrackButtonLabel(status, holder));
        holder.btnTrack.setOnClickListener(v -> listener.onTrackTask(task));
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

    private String resolveTrackButtonLabel(String status, TaskActivityViewHolder holder) {
        if (TaskStatus.COMPLETED.equals(status)) {
            return holder.itemView.getContext().getString(R.string.activity_view_task_details);
        }
        return holder.itemView.getContext().getString(R.string.activity_track_live_task);
    }

    private String formatStatusLabel(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String cleaned = rawStatus.trim().replace('_', ' ');
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            String lower = word.toLowerCase(Locale.US);
            sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return sb.toString();
    }

    static class TaskActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvStatus;
        TextView tvBudget;
        Button btnTrack;

        TaskActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvActivityTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvActivityTaskDescription);
            tvStatus = itemView.findViewById(R.id.tvActivityTaskStatus);
            tvBudget = itemView.findViewById(R.id.tvActivityTaskBudget);
            btnTrack = itemView.findViewById(R.id.btnTrackTask);
        }
    }

    public interface TaskClickListener {
        void onTrackTask(ErrandTask task);
    }
}

