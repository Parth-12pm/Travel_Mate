package com.example.travelmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    private List<Trip> tripList;
    private Context context;

    public TripAdapter(List<Trip> tripList, Context context) {
        this.tripList = tripList;
        this.context = context;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trip_item, parent, false);
        return new TripViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip);

        // Set click listener here in onBindViewHolder
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, JournalDetailsActivity.class);
            intent.putExtra("tripId", trip.tripId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTripDetails;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripDetails = itemView.findViewById(R.id.tvTripDetails);
        }

        public void bind(Trip trip) {
            tvTripDetails.setText(trip.source + " to " + trip.destination + "\n" + trip.date);
        }
    }
}