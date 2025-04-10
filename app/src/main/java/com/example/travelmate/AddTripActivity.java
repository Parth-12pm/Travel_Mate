package com.example.travelmate;

import static com.example.travelmate.BuildConfig.GOOGLE_MAPS_API_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class AddTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String sourceName, destinationName;
    private AutocompleteSupportFragment autocompleteSource, autocompleteDestination;
    private Button btnSelectDate, btnCreateTrip;
    private Toolbar toolbar;
    private TextView tvSelectedDate;
    private LatLng sourceLatLng, destinationLatLng;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Consistent format

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_MAPS_API_KEY);
        }

        // Initialize views
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnCreateTrip = findViewById(R.id.btnCreateTrip);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Initialize the AutocompleteSupportFragments
        setupAutocompleteFragments();

        // Set up map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up date picker
        setupDatePicker();

        // Set up create trip button
        btnCreateTrip.setOnClickListener(v -> createTrip());
        toolbar.setNavigationOnClickListener(v -> { // Use setNavigationOnClickListener
            Intent i = new Intent(AddTripActivity.this, HomePageActivity.class);
            startActivity(i);  // <--- Add this line
            finish(); // Optional: Close the current activity
        });


    }

    private void setupAutocompleteFragments() {
        // Initialize the AutocompleteSupportFragment for source
        autocompleteSource = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_source);

        // Initialize the AutocompleteSupportFragment for destination
        autocompleteDestination = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_destination);

        // Specify the types of place data to return
        autocompleteSource.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Set up place selection listeners
        autocompleteSource.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                sourceLatLng = place.getLatLng();
                sourceName = place.getName();
                updateMap();
            }

            @Override
            public void onError(com.google.android.gms.common.api.Status status) {
                Toast.makeText(AddTripActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        autocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                destinationLatLng = place.getLatLng();
                destinationName = place.getName();
                updateMap();
            }

            @Override
            public void onError(com.google.android.gms.common.api.Status status) {
                Toast.makeText(AddTripActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select trip date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Format the selected date to yyyy-MM-dd
            String formattedDate = dateFormatter.format(new Date(selection));
            tvSelectedDate.setText(formattedDate);
            tvSelectedDate.setTextColor(getResources().getColor(R.color.md_theme_onBackground));
        });

        btnSelectDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void updateMap() {
        if (mMap == null || sourceLatLng == null || destinationLatLng == null) return;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(sourceLatLng).title("Source"));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

        // Create bounds to include both markers
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(sourceLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        // Move camera to show both locations with padding
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        // Add a line between the points
        mMap.addPolyline(new PolylineOptions()
                .add(sourceLatLng, destinationLatLng)
                .width(5f)
                .color(getResources().getColor(R.color.md_theme_primary)));
    }

    private void createTrip() {
        if (sourceLatLng == null) {
            Toast.makeText(this, "Please select source location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destinationLatLng == null) {
            Toast.makeText(this, "Please select destination location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tvSelectedDate.getText().toString().equals("No date selected")) {
            Toast.makeText(this, "Please select trip date", Toast.LENGTH_SHORT).show();
            return;
        }
        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        // Create trip data
        Map<String, Object> trip = new HashMap<>();
        trip.put("source", sourceName);
        trip.put("destination", destinationName);
        trip.put("date", tvSelectedDate.getText().toString());
        trip.put("sourceLat", sourceLatLng.latitude);
        trip.put("sourceLng", sourceLatLng.longitude);
        trip.put("destLat", destinationLatLng.latitude);
        trip.put("destLng", destinationLatLng.longitude);
        trip.put("userId", user.getUid());
        trip.put("createdAt", new Date());
        trip.put("notes", "");
        trip.put("photos", new ArrayList<String>());
        // Save to Firestore
        FirebaseFirestore.getInstance().collection("trips")
                .add(trip)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Trip created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create trip", Toast.LENGTH_SHORT).show();
                });
    }
}