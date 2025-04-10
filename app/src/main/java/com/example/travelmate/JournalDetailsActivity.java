package com.example.travelmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class JournalDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView tvPlaceName;
    private EditText etNotes;
    private GoogleMap mMap;
    private String tripId;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_details);

        // Initialize views
        tvPlaceName = findViewById(R.id.tvPlaceName);
        etNotes = findViewById(R.id.etNotes);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Get trip ID from intent
        tripId = getIntent().getStringExtra("tripId");
        if (tripId == null) {
            finish();
            return;
        }

        // Load trip details
        loadTripDetails();

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup save button
        findViewById(R.id.saveButton).setOnClickListener(v -> saveJournalEntry());

        toolbar.setNavigationOnClickListener(v -> { // Use setNavigationOnClickListener
            Intent i = new Intent(JournalDetailsActivity.this, HomePageActivity.class);
            startActivity(i);  // <--- Add this line
            finish(); // Optional: Close the current activity
        });
    }

    private void loadTripDetails() {
        FirebaseFirestore.getInstance().collection("trips").document(tripId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String source = documentSnapshot.getString("source");
                        String destination = documentSnapshot.getString("destination");
                        String date = documentSnapshot.getString("date");
                        String notes = documentSnapshot.getString("notes");

                        tvPlaceName.setText(source + " to " + destination + " • " + date);
                        etNotes.setText(notes != null ? notes : "");

                        // Update map if ready
                        if (mMap != null) {
                            updateMap(documentSnapshot);
                        }
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (tripId != null) {
            FirebaseFirestore.getInstance().collection("trips").document(tripId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            updateMap(documentSnapshot);
                        }
                    });
        }
    }

    private void updateMap(DocumentSnapshot documentSnapshot) {
        Double sourceLat = documentSnapshot.getDouble("sourceLat");
        Double sourceLng = documentSnapshot.getDouble("sourceLng");
        Double destLat = documentSnapshot.getDouble("destLat");
        Double destLng = documentSnapshot.getDouble("destLng");

        if (sourceLat == null || sourceLng == null || destLat == null || destLng == null) return;

        LatLng source = new LatLng(sourceLat, sourceLng);
        LatLng dest = new LatLng(destLat, destLng);

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(source).title("Source"));
        mMap.addMarker(new MarkerOptions().position(dest).title("Destination"));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(source);
        builder.include(dest);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private void saveJournalEntry() {
        String notes = etNotes.getText().toString();

        FirebaseFirestore.getInstance().collection("trips").document(tripId)
                .update("notes", notes)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Journal saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                });
    }
}