package com.example.travelmate;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;


import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView tvPlaceName;
    private EditText etNotes;
    private GoogleMap mMap;
    private String tripId;
    private Toolbar toolbar;
    private LinearLayout photosContainer;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            uploadImageToCloudinary(uri);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_details);

        // Initialize views
        tvPlaceName = findViewById(R.id.tvPlaceName);
        etNotes = findViewById(R.id.etNotes);
        toolbar = findViewById(R.id.toolbar);
        photosContainer = findViewById(R.id.photosContainer);

        setSupportActionBar(toolbar);

        // Initialize Cloudinary
        initCloudinary();

        // Get trip ID from intent
        tripId = getIntent().getStringExtra("tripId");
        if (tripId == null) {
            finish();
            return;
        }

        // Load trip details
        loadTripDetails();

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup save button
        findViewById(R.id.saveButton).setOnClickListener(v -> saveJournalEntry());

        // Setup delete button
        findViewById(R.id.deleteButton).setOnClickListener(v -> deleteTrip());

        // Setup add photo button
        findViewById(R.id.btnAddPhoto).setOnClickListener(v -> openImagePicker());

        toolbar.setNavigationOnClickListener(v -> {
            Intent i = new Intent(JournalDetailsActivity.this, HomePageActivity.class);
            startActivity(i);
            finish();
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (tripId != null) {
            FirebaseFirestore.getInstance().collection("trips").document(tripId).get().addOnSuccessListener(documentSnapshot -> {
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

        FirebaseFirestore.getInstance().collection("trips").document(tripId).update("notes", notes).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Journal saved", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
        });
    }

    private void initCloudinary() {
        Map config = new HashMap();
        config.put("cloud_name", BuildConfig.CLOUD_NAME);
        config.put("api_key", BuildConfig.CLOUD_API_SECRET);
        config.put("api_secret", BuildConfig.CLOUD_API_SECRET);
        MediaManager.init(this, config);
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        String requestId = MediaManager.get().upload(imageUri).option("folder", "travel_mate/" + tripId).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                runOnUiThread(() -> Toast.makeText(JournalDetailsActivity.this, "Upload started...", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Update progress if needed
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                runOnUiThread(() -> {
                    String imageUrl = (String) resultData.get("url");
                    addImageToLayout(imageUrl);
                    saveImageUrlToFirestore(imageUrl);
                    Toast.makeText(JournalDetailsActivity.this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                runOnUiThread(() -> Toast.makeText(JournalDetailsActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Retry upload
            }
        }).dispatch();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void addImageToLayout(String imageUrl) {
        // Create card container
        MaterialCardView cardView = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                dpToPx(120), // Fixed width for the card
                LinearLayout.LayoutParams.MATCH_PARENT // Match parent height
        );
        cardParams.setMargins(0, 0, dpToPx(8), 0); // Add right margin for spacing
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(getResources().getDimension(R.dimen.card_corner_radius));
        cardView.setCardElevation(getResources().getDimension(R.dimen.card_elevation));
        cardView.setUseCompatPadding(true);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_surfaceVariant)); // Consistent background

        // Create image view
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Load image
        Glide.with(this)
                .load(imageUrl)
                .into(imageView);

        // Add click listener for fullscreen view
        cardView.setOnClickListener(v -> showFullScreenImage(imageUrl));

        // Add image to card
        cardView.addView(imageView);

        // Add card to the photos container, before the add button
        photosContainer.addView(cardView, photosContainer.getChildCount() - 1);

        androidx.media3.common.util.Log.d("ImageAdded", "Added image: " + imageUrl);
    }


    private void showFullScreenImage(String imageUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView imageView = dialog.findViewById(R.id.fullscreen_image);
        Glide.with(this).load(imageUrl).into(imageView);

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @UnstableApi
    private void loadImages(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot.contains("images")) {
            List<String> images = (List<String>) documentSnapshot.get("images");
            if (images != null && !images.isEmpty()) {  // Check for null or empty list
                for (String imageUrl : images) {
                    addImageToLayout(imageUrl);
                }
            } else {
                androidx.media3.common.util.Log.d("LoadImages", "No images found or images list is empty");
            }
        } else {
            androidx.media3.common.util.Log.d("LoadImages", "No 'images' field found in document");
        }
    }

    private void saveImageUrlToFirestore(String imageUrl) {
        FirebaseFirestore.getInstance().collection("trips").document(tripId).update("images", FieldValue.arrayUnion(imageUrl)).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadTripDetails() {
        FirebaseFirestore.getInstance().collection("trips").document(tripId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String source = documentSnapshot.getString("source");
                String destination = documentSnapshot.getString("destination");
                String date = documentSnapshot.getString("date");
                String notes = documentSnapshot.getString("notes");

                tvPlaceName.setText(source + " to " + destination + " • " + date);
                etNotes.setText(notes != null ? notes : "");

                // Load images if any
                if (documentSnapshot.contains("images")) {
                    List<String> images = (List<String>) documentSnapshot.get("images");
                    for (String imageUrl : images) {
                        addImageToLayout(imageUrl);
                    }
                }

                // Update map if ready
                if (mMap != null) {
                    updateMap(documentSnapshot);
                }
            }
        });
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void deleteTrip() {
        FirebaseFirestore.getInstance().collection("trips").document(tripId).delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to delete trip", Toast.LENGTH_SHORT).show();
        });
    }
}