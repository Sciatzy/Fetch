package com.fetch.auth.production.pasabuy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.util.StorageBackedImageLoader;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PasaBuyFeedAdapter extends RecyclerView.Adapter<PasaBuyFeedAdapter.ViewHolder> {

    private boolean isRider;
    private String currentUserId;
    private List<DocumentSnapshot> posts = new ArrayList<>();
    private OnPostInteractionListener listener;

    public interface OnPostInteractionListener {
        void onAvailClicked(DocumentSnapshot post);
        void onLikeClicked(DocumentSnapshot post, boolean isLiked);
        void onViewAvailRequestsClicked(DocumentSnapshot post);
    }

    public PasaBuyFeedAdapter(boolean isRider, String currentUserId, OnPostInteractionListener listener) {
        this.isRider = isRider;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setPosts(List<DocumentSnapshot> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pasabuy_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot post = posts.get(position);
        
        String riderName = post.getString("riderName");
        String location = post.getString("location");
        String caption = post.getString("caption");
        String postImageUrl = post.getString("imageUrl");
        String riderAvatarUrl = post.getString("riderAvatarUrl");
        
        List<String> likes = (List<String>) post.get("likes");
        int likeCount = likes != null ? likes.size() : 0;
        boolean isLikedByMe = likes != null && likes.contains(currentUserId);
        
        Map<String, Object> avails = toMap(post.get("avails"));
        int availCount = countActiveAvails(avails);
        Map<String, Object> myAvailEntry = toMap(avails.get(currentUserId));
        String myAvailStatus = myAvailEntry != null ? safeString(myAvailEntry.get("status")) : "";

        holder.tvRiderName.setText(riderName != null ? riderName : "Unknown Rider");
        holder.tvCaptionName.setText(riderName != null ? riderName : "Unknown Rider");
        holder.tvLocation.setText(location != null ? location : "Unknown Location");
        holder.tvCaption.setText(caption != null ? caption : "");
        holder.tvLikesCount.setText(likeCount + " likes");
        holder.tvTime.setText("Just now"); // Can format actual timestamp if available
        
        if (availCount > 0) {
            holder.tvAvailCount.setText("View all " + availCount + " avail requests");
        } else {
            holder.tvAvailCount.setText("No avail requests yet");
        }
        
        if (postImageUrl != null && !postImageUrl.isEmpty()) {
            StorageBackedImageLoader.load(holder.ivPostImage, postImageUrl, R.drawable.fetch_logo, false);
        } else {
            holder.ivPostImage.setImageResource(R.drawable.fetch_logo);
        }
        
        if (riderAvatarUrl != null && !riderAvatarUrl.isEmpty()) {
            StorageBackedImageLoader.load(holder.ivRiderAvatar, riderAvatarUrl, R.drawable.fetch_logo, true);
        } else {
            holder.ivRiderAvatar.setImageResource(R.drawable.fetch_logo);
        }
        
        // Setup Icon state
        holder.btnLike.setColorFilter(isLikedByMe ? 0xFFD32F2F : 0xFF000000); // Red if liked

        if (isRider) {
            holder.btnAvail.setVisibility(View.GONE);
            holder.btnLike.setVisibility(View.VISIBLE);
            String riderId = post.getString("riderId");
            boolean isMyPost = riderId != null && riderId.equals(currentUserId);
            holder.tvAvailCount.setOnClickListener(null);
            if (isMyPost) {
                holder.tvAvailCount.setTextColor(0xFF1976D2);
                holder.tvAvailCount.setOnClickListener(v -> {
                    if (listener != null) listener.onViewAvailRequestsClicked(post);
                });
            } else {
                holder.tvAvailCount.setTextColor(0xFF757575);
            }
        } else {
            holder.btnAvail.setVisibility(View.VISIBLE);
            holder.btnAvail.setEnabled(true);
            String actionLabel = "Avail";
            if ("pending".equalsIgnoreCase(myAvailStatus)) {
                actionLabel = "Cancel Avail";
            } else if ("approved".equalsIgnoreCase(myAvailStatus)
                    || "completed".equalsIgnoreCase(myAvailStatus)
                    || "in_progress".equalsIgnoreCase(myAvailStatus)) {
                actionLabel = "Open Transaction";
            }
            holder.btnAvail.setText(actionLabel);
            holder.btnAvail.setOnClickListener(v -> {
                if (listener != null) listener.onAvailClicked(post);
            });
            holder.tvAvailCount.setTextColor(0xFF757575);
        }

        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClicked(post, !isLikedByMe);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRiderName, tvLocation, tvTime, tvCaption, tvAvailCount, tvCaptionName, tvLikesCount;
        ImageView ivPostImage, ivRiderAvatar, btnLike;
        Button btnAvail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRiderName = itemView.findViewById(R.id.tvRiderName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            tvAvailCount = itemView.findViewById(R.id.tvAvailCount);
            tvCaptionName = itemView.findViewById(R.id.tvCaptionName);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivRiderAvatar = itemView.findViewById(R.id.ivRiderAvatar);
            btnAvail = itemView.findViewById(R.id.btnAvail);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return new java.util.HashMap<>();
    }

    private int countActiveAvails(Map<String, Object> avails) {
        int total = 0;
        for (Object value : avails.values()) {
            Map<String, Object> entry = toMap(value);
            String status = safeString(entry.get("status"));
            if (!"cancelled".equalsIgnoreCase(status)) {
                total++;
            }
        }
        return total;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
