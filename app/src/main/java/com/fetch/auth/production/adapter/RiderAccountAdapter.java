package com.fetch.auth.production.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.model.RiderAccountItem;

import java.util.ArrayList;
import java.util.List;

public class RiderAccountAdapter extends RecyclerView.Adapter<RiderAccountAdapter.RiderViewHolder> {

    private final List<RiderAccountItem> items = new ArrayList<>();
    private final RiderActionListener actionListener;

    public RiderAccountAdapter(RiderActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public RiderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rider_account, parent, false);
        return new RiderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiderViewHolder holder, int position) {
        RiderAccountItem item = items.get(position);
        holder.tvName.setText(item.getName());
        holder.tvEmail.setText(item.getEmail());
        holder.tvVehicle.setText(holder.itemView.getContext().getString(
                R.string.admin_vehicle_format,
                item.getVehicleType()
        ));
        holder.tvLicense.setText(holder.itemView.getContext().getString(
                R.string.admin_license_format,
                item.getLicenseNumber()
        ));
        holder.tvStatus.setText(item.isActive()
                ? R.string.admin_rider_status_active
                : R.string.admin_rider_status_inactive);

        holder.btnDeactivate.setOnClickListener(v -> actionListener.onDeactivate(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<RiderAccountItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class RiderViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvEmail;
        TextView tvVehicle;
        TextView tvLicense;
        TextView tvStatus;
        Button btnDeactivate;

        RiderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRiderName);
            tvEmail = itemView.findViewById(R.id.tvRiderEmail);
            tvVehicle = itemView.findViewById(R.id.tvRiderVehicle);
            tvLicense = itemView.findViewById(R.id.tvRiderLicense);
            tvStatus = itemView.findViewById(R.id.tvRiderStatus);
            btnDeactivate = itemView.findViewById(R.id.btnDeactivateRider);
        }
    }

    public interface RiderActionListener {
        void onDeactivate(RiderAccountItem item);
    }
}
