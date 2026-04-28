package com.fetch.auth.production.pasabuy;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.util.StorageBackedImageLoader;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PasaBuyMessagesAdapter extends RecyclerView.Adapter<PasaBuyMessagesAdapter.ViewHolder> {

    public interface OnLocationNavigateClickListener {
        void onNavigateToLocation(double lat, double lng, String label);
    }

    private final List<DocumentSnapshot> messages = new ArrayList<>();
    private final String currentUserId;
    private final OnLocationNavigateClickListener locationNavigateClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    public PasaBuyMessagesAdapter(String currentUserId, OnLocationNavigateClickListener locationNavigateClickListener) {
        this.currentUserId = currentUserId;
        this.locationNavigateClickListener = locationNavigateClickListener;
    }

    public void setMessages(List<DocumentSnapshot> docs) {
        messages.clear();
        messages.addAll(docs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pasabuy_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot message = messages.get(position);
        String senderId = message.getString("senderId");
        String senderName = message.getString("senderName");
        String text = message.getString("text");
        String type = message.getString("type");
        String imageUrl = message.getString("imageUrl");
        boolean mine = currentUserId != null && currentUserId.equals(senderId);

        holder.rootMessageRow.setGravity(mine ? Gravity.END : Gravity.START);
        holder.cardMessageBubble.setCardBackgroundColor(mine ? 0xFF4285F4 : 0xFFFFFFFF);
        holder.tvMessageText.setTextColor(mine ? 0xFFFFFFFF : 0xFF212121);
        holder.tvMessageSender.setTextColor(mine ? 0xCCFFFFFF : 0xFF4285F4);
        holder.tvMessageTime.setTextColor(0xFF9E9E9E);
        
        LinearLayout.LayoutParams timeParams = (LinearLayout.LayoutParams) holder.tvMessageTime.getLayoutParams();
        timeParams.gravity = mine ? Gravity.END : Gravity.START;
        holder.tvMessageTime.setLayoutParams(timeParams);

        holder.tvMessageSender.setText(mine ? "You" : (!TextUtils.isEmpty(senderName) ? senderName : "User"));
        holder.tvMessageText.setText(!TextUtils.isEmpty(text) ? text : "");
        holder.tvMessageText.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);

        if (!TextUtils.isEmpty(imageUrl)) {
            holder.ivMessageImage.setVisibility(View.VISIBLE);
            StorageBackedImageLoader.load(holder.ivMessageImage, imageUrl, R.drawable.fetch_logo, false);
        } else {
            holder.ivMessageImage.setVisibility(View.GONE);
        }

        holder.btnMessageAction.setVisibility(View.GONE);
        if ("location".equalsIgnoreCase(type)) {
            Double lat = getDouble(message.get("locationLat"));
            Double lng = getDouble(message.get("locationLng"));
            if (lat != null && lng != null && locationNavigateClickListener != null) {
                holder.btnMessageAction.setVisibility(View.VISIBLE);
                holder.btnMessageAction.setText("Navigate");
                String label = !TextUtils.isEmpty(text) ? text : "Customer location";
                holder.btnMessageAction.setOnClickListener(v -> locationNavigateClickListener.onNavigateToLocation(lat, lng, label));
            }
        }

        Timestamp timestamp = message.getTimestamp("createdAt");
        if (timestamp != null) {
            holder.tvMessageTime.setText(timeFormat.format(timestamp.toDate()));
        } else {
            holder.tvMessageTime.setText("Now");
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private Double getDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rootMessageRow;
        com.google.android.material.card.MaterialCardView cardMessageBubble;
        TextView tvMessageSender;
        TextView tvMessageText;
        ImageView ivMessageImage;
        Button btnMessageAction;
        TextView tvMessageTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rootMessageRow = itemView.findViewById(R.id.rootMessageRow);
            cardMessageBubble = itemView.findViewById(R.id.cardMessageBubble);
            tvMessageSender = itemView.findViewById(R.id.tvMessageSender);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            btnMessageAction = itemView.findViewById(R.id.btnMessageAction);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
