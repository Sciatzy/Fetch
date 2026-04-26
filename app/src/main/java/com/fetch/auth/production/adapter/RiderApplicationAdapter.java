package com.fetch.auth.production.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fetch.auth.production.R;
import com.fetch.auth.production.model.RiderApplicationItem;

import java.util.ArrayList;
import java.util.List;

public class RiderApplicationAdapter extends RecyclerView.Adapter<RiderApplicationAdapter.ApplicationViewHolder> {

    private final List<RiderApplicationItem> items = new ArrayList<>();
    private final RiderApplicationActionListener actionListener;

    public RiderApplicationAdapter(RiderApplicationActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rider_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        RiderApplicationItem item = items.get(position);
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
        holder.tvRequirements.setText(item.getRequirementsLabel());

        holder.btnApprove.setOnClickListener(v -> actionListener.onApprove(item));
        holder.btnReject.setOnClickListener(v -> actionListener.onReject(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<RiderApplicationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class ApplicationViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        TextView tvEmail;
        TextView tvVehicle;
        TextView tvLicense;
        TextView tvRequirements;
        Button btnApprove;
        Button btnReject;

        ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApplicationName);
            tvEmail = itemView.findViewById(R.id.tvApplicationEmail);
            tvVehicle = itemView.findViewById(R.id.tvApplicationVehicle);
            tvLicense = itemView.findViewById(R.id.tvApplicationLicense);
            tvRequirements = itemView.findViewById(R.id.tvApplicationRequirements);
            btnApprove = itemView.findViewById(R.id.btnApproveApplication);
            btnReject = itemView.findViewById(R.id.btnRejectApplication);
        }
    }

    public interface RiderApplicationActionListener {
        void onApprove(RiderApplicationItem item);

        void onReject(RiderApplicationItem item);
    }
}
