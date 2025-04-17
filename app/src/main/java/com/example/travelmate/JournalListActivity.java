package com.example.travelmate;

import static com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class JournalListActivity extends AppCompatActivity {
    private LinearLayout emptyState;
    private Toolbar toolbar;
    private RecyclerView tripsRecyclerView;
    private List<Trip> tripData = new ArrayList<>();
    private List<Trip> filteredTripData = new ArrayList<>();
    private TripAdapter tripAdapter;
    private AutoCompleteTextView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);


        tripsRecyclerView = findViewById(R.id.rvJournalEntries);
        emptyState = findViewById(R.id.emptyState);
        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.actvSearch);


        setupToolbar();
        setupRecyclerView();
        setupSearchFunctionality();
        loadTrips();
    }


    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, HomePageActivity.class));
            finish();
        });
    }

    private void setupRecyclerView() {
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripAdapter = new TripAdapter(filteredTripData, this);
        tripsRecyclerView.setAdapter(tripAdapter);
    }

    private void setupSearchFunctionality() {
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrips(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set up clear text functionality
        TextInputLayout searchInputLayout = findViewById(R.id.searchInputLayout);
        searchInputLayout.setEndIconMode(END_ICON_CLEAR_TEXT);
        searchInputLayout.setEndIconOnClickListener(v -> {
            searchView.setText("");
            filterTrips("");
        });
    }

    private void filterTrips(String searchText) {
        filteredTripData.clear();
        if (searchText.isEmpty()) {
            filteredTripData.addAll(tripData);
        } else {
            String searchLower = searchText.toLowerCase();
            for (Trip trip : tripData) {
                if (tripMatchesSearch(trip, searchLower)) {
                    filteredTripData.add(trip);
                }
            }
        }

        updateEmptyState();
        tripAdapter.notifyDataSetChanged();
    }

    private boolean tripMatchesSearch(Trip trip, String searchLower) {
        return trip.source.toLowerCase().contains(searchLower) ||
                trip.destination.toLowerCase().contains(searchLower) ||
                trip.date.toLowerCase().contains(searchLower);
    }

    private void updateEmptyState() {
        emptyState.setVisibility(filteredTripData.isEmpty() ? View.VISIBLE : View.GONE);
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
                    tripData.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        tripData.add(new Trip(
                                doc.getString("source"),
                                doc.getString("destination"),
                                doc.getString("date"),
                                doc.getId()
                        ));
                    }

                    filteredTripData.addAll(tripData);
                    tripAdapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading trips", Toast.LENGTH_SHORT).show();
                });

    }
}