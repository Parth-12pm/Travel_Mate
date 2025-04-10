package com.example.travelmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class JournalListActivity extends AppCompatActivity {
    private LinearLayout tripsContainer;
    private LinearLayout emptyState;

    private Toolbar toolbar;
    private RecyclerView tripsRecyclerView; // Change tripsContainer to tripsRecyclerView
    private List<Trip> tripData = new ArrayList<>(); // Add a list to store the trip data
    private TripAdapter tripAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        tripsRecyclerView = findViewById(R.id.rvJournalEntries); // Use tripsRecyclerView
        emptyState = findViewById(R.id.emptyState);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Intent i = new Intent(JournalListActivity.this, HomePageActivity.class);
            startActivity(i);
            finish();
        });

        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(tripData, this); // Initialize the adapter
        tripsRecyclerView.setAdapter(tripAdapter);

        loadTrips();
    }
    private void loadTrips() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tripData.clear(); // Clear the previous data
                    if (queryDocumentSnapshots.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        tripAdapter.notifyDataSetChanged(); // Notify adapter of empty list
                        return;
                    }

                    emptyState.setVisibility(View.GONE);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String source = doc.getString("source");
                        String destination = doc.getString("destination");
                        String date = doc.getString("date");
                        String tripId = doc.getId();

                        Trip trip = new Trip(source, destination, date, tripId);
                        tripData.add(trip);
                    }
                    tripAdapter.notifyDataSetChanged(); // Notify adapter of the new data
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading trips", Toast.LENGTH_SHORT).show();
                });
    }

}