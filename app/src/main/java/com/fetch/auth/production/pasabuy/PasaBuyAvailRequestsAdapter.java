package com.fetch.auth.production.pasabuy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;

import java.util.ArrayList;
import java.util.List;

public class PasaBuyAvailRequestsAdapter extends RecyclerView.Adapter<PasaBuyAvailRequestsAdapter.ViewHolder> {

    public interface OnRequestClickListener {
        void onOpenTransaction(AvailRequestItem requestItem);
    }

    private final List<AvailRequestItem> requests = new ArrayList<>();
    private final OnRequestClickListener listener;

    public PasaBuyAvailRequestsAdapter(OnRequestClickListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<AvailRequestItem> items) {
        requests.clear();
        requests.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pasabuy_avail_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AvailRequestItem item = requests.get(position);
        holder.tvCustomerName.setText(item.getCustomerName());
        holder.tvStatus.setText("Status: " + item.getStatus());
        holder.tvCustomerId.setText("Customer ID: " + item.getCustomerId());

        boolean hasTransaction = item.getTransactionId() != null && !item.getTransactionId().isEmpty();
        holder.btnOpenTransaction.setEnabled(hasTransaction);
        holder.btnOpenTransaction.setOnClickListener(v -> {
            if (listener != null && hasTransaction) {
                listener.onOpenTransaction(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName;
        TextView tvStatus;
        TextView tvCustomerId;
        Button btnOpenTransaction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvStatus = itemView.findViewById(R.id.tvRequestStatus);
            tvCustomerId = itemView.findViewById(R.id.tvCustomerId);
            btnOpenTransaction = itemView.findViewById(R.id.btnOpenTransaction);
        }
    }
}
