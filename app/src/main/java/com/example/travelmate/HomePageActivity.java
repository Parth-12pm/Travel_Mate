package com.example.travelmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.Glide;
import java.util.Objects;


public class HomePageActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout upcomingTripsContainer, recentTripsContainer;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CircleImageView profileIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        topAppBar = findViewById(R.id.topAppBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        upcomingTripsContainer = findViewById(R.id.upcomingTripsContainer);
        recentTripsContainer = findViewById(R.id.recentTripsContainer);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        profileIcon = findViewById(R.id.profileIcon);

        // Set up Top App Bar
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        // Set up Profile Icon Click
        profileIcon.setOnClickListener(v -> {

            Intent intent = new Intent(HomePageActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Set up Bottom Navigation
        bottomNavigation.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        // Set up Navigation View
        navigationView.setNavigationItemSelectedListener(this::onNavItemSelected);

        // Load trips from Firestore
        loadTrips();
        loadProfilePicture();
    }

    private void loadTrips() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Clear existing views
        upcomingTripsContainer.removeAllViews();
        recentTripsContainer.removeAllViews();

        // Load trips from Firestore
        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Get trip data directly from document
                        String source = doc.getString("source");
                        String destination = doc.getString("destination");
                        String date = doc.getString("date");
                        String tripId = doc.getId();

                        // Create trip card
                        MaterialCardView card = new MaterialCardView(this);
                        card.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        card.setCardElevation(4);
                        card.setRadius(8);
                        card.setContentPadding(16, 16, 16, 16);
                        TextView textView = new TextView(this);
                        textView.setText(source + " to " + destination + "\n" + date);
                        textView.setTextSize(16);
                        textView.setPadding(8, 8, 8, 8);
                        card.addView(textView);

                        // Add to appropriate container (you can add date comparison logic here)
                        upcomingTripsContainer.addView(card);

                        // Set click listener
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(HomePageActivity.this, JournalDetailsActivity.class);
                            intent.putExtra("tripId", tripId);
                            startActivity(intent);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load trips", Toast.LENGTH_SHORT).show();
                });
    }
    private void addTripCard(LinearLayout container, String tripDetails) {
        MaterialCardView card = new MaterialCardView(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        card.setCardElevation(4);
        card.setRadius(8);
        card.setContentPadding(16, 16, 16, 16);
        TextView textView = new TextView(this);
        textView.setText(tripDetails);
        textView.setTextSize(16);
        textView.setPadding(8, 8, 8, 8);

        card.addView(textView);
        container.addView(card);
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.menu_toggle_dark_mode) {
            // Toggle dark mode
            boolean isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
            return true;
        } else if (id == R.id.menu_logout) {
            // Logout the user
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Close the HomePageActivity
            return true;
        }
        return false;
    }



    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.homeButton) {
            // Already on HomePage
            return true;
        } else if (id == R.id.searchButton) {
            startActivity(new Intent(this, JournalListActivity.class));
            return true;
        } else if (id == R.id.addButton) {
            startActivity(new Intent(this, AddTripActivity.class));
            return true;
        } else if (id == R.id.journalButton) {
            startActivity(new Intent(this, JournalDetailsActivity.class));
            return true;
        }
        return false;
    }

    private void loadProfilePicture() {
        // Get the current user's UID
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Fetch the user's profile data from Firestore
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the profile picture URL
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");

                        // Load the profile picture into the CircleImageView using Glide
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profilePictureUrl) // Set the profile picture URL
                                    .placeholder(R.drawable.account_circle_24px) // Default placeholder
                                    .error(R.drawable.account_circle_24px) // Default image if loading fails
                                    .into(profileIcon); // Load into CircleImageView
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile picture", Toast.LENGTH_SHORT).show();
                });
    }}