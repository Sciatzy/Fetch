package com.fetch.auth.production.pasabuy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PasaBuyTransactionHistoryAdapter extends RecyclerView.Adapter<PasaBuyTransactionHistoryAdapter.ViewHolder> {

    public interface OnTransactionClickListener {
        void onOpenTransaction(DocumentSnapshot transactionDoc);
    }

    private final List<DocumentSnapshot> transactions = new ArrayList<>();
    private final boolean customerView;
    private final OnTransactionClickListener clickListener;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.US);

    public PasaBuyTransactionHistoryAdapter(boolean customerView, OnTransactionClickListener clickListener) {
        this.customerView = customerView;
        this.clickListener = clickListener;
    }

    public void setTransactions(List<DocumentSnapshot> docs) {
        transactions.clear();
        transactions.addAll(docs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_history_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = transactions.get(position);
        String counterpart = customerView ? doc.getString("riderName") : doc.getString("customerName");
        if (counterpart == null || counterpart.trim().isEmpty()) {
            counterpart = holder.itemView.getContext().getString(R.string.transaction_counterpart_fallback);
        }
        holder.tvCounterpart.setText(counterpart);

        String caption = doc.getString("postCaption");
        holder.tvCaption.setText((caption == null || caption.trim().isEmpty())
                ? holder.itemView.getContext().getString(R.string.transaction_caption_fallback)
                : caption);

        String status = safe(doc.getString("status"), "pending");
        String paymentStatus = safe(doc.getString("paymentStatus"), "pending");
        holder.tvStatusLine.setText(holder.itemView.getContext()
                .getString(R.string.transaction_status_payment_format, status, paymentStatus));

        String latestUpdate = safe(doc.getString("latestUpdate"),
                holder.itemView.getContext().getString(R.string.transaction_update_fallback));
        holder.tvLastUpdate.setText(latestUpdate);

        Timestamp updated = doc.getTimestamp("updatedAt");
        if (updated == null) {
            updated = doc.getTimestamp("createdAt");
        }
        if (updated != null) {
            holder.tvLastUpdate.setText(holder.itemView.getContext().getString(
                    R.string.transaction_update_with_time,
                    latestUpdate,
                    dateTimeFormat.format(updated.toDate())));
        }

        holder.btnOpenChat.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onOpenTransaction(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounterpart;
        TextView tvCaption;
        TextView tvStatusLine;
        TextView tvLastUpdate;
        Button btnOpenChat;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCounterpart = itemView.findViewById(R.id.tvTransactionCounterpart);
            tvCaption = itemView.findViewById(R.id.tvTransactionCaption);
            tvStatusLine = itemView.findViewById(R.id.tvTransactionStatusLine);
            tvLastUpdate = itemView.findViewById(R.id.tvTransactionLastUpdate);
            btnOpenChat = itemView.findViewById(R.id.btnOpenTransactionChat);
        }
    }
}
