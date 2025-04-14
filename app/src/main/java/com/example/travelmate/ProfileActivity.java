package com.example.travelmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private CircleImageView profileImage;
    private TextView userName, userEmail, fullName, gender, memberSince;
    private TextView tripsCount, countriesCount, citiesCount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        fullName = findViewById(R.id.fullName);
        gender = findViewById(R.id.gender);
        memberSince = findViewById(R.id.memberSince);
        tripsCount = findViewById(R.id.tripsCount);
        countriesCount = findViewById(R.id.countriesCount);
        citiesCount = findViewById(R.id.citiesCount);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        // Add edit button to toolbar
        toolbar.inflateMenu(R.menu.profile_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
                return true;
            }
            return false;
        });


        // Load user data
        loadUserData();
        loadTravelStats();

    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Set email (from Firebase Auth)
        userEmail.setText(user.getEmail());

        // Get additional user data from Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set name
                        String name = documentSnapshot.getString("name");
                        String surname = documentSnapshot.getString("surname");
                        String fullNameStr = name + " " + surname;
                        userName.setText(fullNameStr);
                        fullName.setText(fullNameStr);

                        // Set gender
                        gender.setText(documentSnapshot.getString("gender"));

                        // Set member since (creation date from Firebase Auth)
                        if (user.getMetadata() != null) {
                            long creationTimestamp = user.getMetadata().getCreationTimestamp();
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                            memberSince.setText(sdf.format(new Date(creationTimestamp)));
                        }

                        // Load profile picture
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .placeholder(R.drawable.account_circle_24px)
                                    .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTravelStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Count trips
        db.collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int tripCount = queryDocumentSnapshots.size();
                    tripsCount.setText(String.valueOf(tripCount));

                    // Count unique countries and cities
                    Set<String> countries = new HashSet<>();
                    Set<String> cities = new HashSet<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String destination = doc.getString("destination");
                        if (destination != null) {
                            // Simple parsing - you might need more sophisticated logic
                            String[] parts = destination.split(",");
                            if (parts.length > 0) {
                                cities.add(parts[0].trim());
                            }
                            if (parts.length > 1) {
                                countries.add(parts[parts.length - 1].trim());
                            }
                        }
                    }

                    countriesCount.setText(String.valueOf(countries.size()));
                    citiesCount.setText(String.valueOf(cities.size()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load travel stats", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from EditProfileActivity
        loadUserData();
    }
}