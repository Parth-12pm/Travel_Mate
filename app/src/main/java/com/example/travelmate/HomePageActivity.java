package com.example.travelmate;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class HomePageActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout upcomingTripsContainer, recentTripsContainer;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CircleImageView profileIcon;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Adjust format if needed


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

        upcomingTripsContainer.removeAllViews();
        recentTripsContainer.removeAllViews();

        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String source = doc.getString("source");
                        String destination = doc.getString("destination");
                        String dateStr = doc.getString("date"); // Get date as string
                        String tripId = doc.getId();

                        try {
                            Date tripDate = dateFormatter.parse(dateStr);
                            if (tripDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(tripDate);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                tripDate = calendar.getTime();

                                Date today = Calendar.getInstance().getTime();
                                Calendar todayCalendar = Calendar.getInstance();
                                todayCalendar.setTime(today);
                                todayCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                todayCalendar.set(Calendar.MINUTE, 0);
                                todayCalendar.set(Calendar.SECOND, 0);
                                todayCalendar.set(Calendar.MILLISECOND, 0);
                                today = todayCalendar.getTime();

                                MaterialCardView card = createTripCard(source, destination, dateStr);
                                card.setOnClickListener(v -> {
                                    Intent intent = new Intent(HomePageActivity.this, JournalDetailsActivity.class);
                                    intent.putExtra("tripId", tripId);
                                    startActivity(intent);
                                });

                                if (tripDate.after(today)) {
                                    upcomingTripsContainer.addView(card);
                                } else {
                                    recentTripsContainer.addView(card);
                                }
                            }
                        } catch (ParseException e) {
                            // Handle date parsing error (e.g., log it, display an error message)
                            e.printStackTrace();  // Log the error for debugging
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load trips", Toast.LENGTH_SHORT).show();
                });
    }

    private MaterialCardView createTripCard(String source, String destination, String date) {
        // Create card with styling that matches your theme
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16); // Add bottom margin
        card.setLayoutParams(layoutParams);
        card.setCardElevation(4);
        card.setRadius(12);
        card.setCardBackgroundColor(getResources().getColor(R.color.md_theme_surfaceContainerHigh));
        card.setStrokeWidth(1);
        card.setStrokeColor(getResources().getColor(R.color.md_theme_outlineVariant));

        // Create inner layout
        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(24, 24, 24, 24);

        // Create and style route text (source to destination)
        TextView routeText = new TextView(this);
        routeText.setText(source + " → " + destination);
        routeText.setTextSize(18);
        routeText.setTextColor(getResources().getColor(R.color.md_theme_onSurface));
        routeText.setTypeface(null, Typeface.BOLD);

        // Create and style date text
        TextView dateText = new TextView(this);
        try {
            // Format the date to be more readable
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date tripDate = inputFormat.parse(date);
            dateText.setText(outputFormat.format(tripDate));
        } catch (ParseException e) {
            dateText.setText(date); // Fallback to original format if parsing fails
        }
        dateText.setTextSize(14);
        dateText.setTextColor(getResources().getColor(R.color.md_theme_onSurfaceVariant));
        dateText.setPadding(0, 8, 0, 0);

        // Add views to inner layout
        innerLayout.addView(routeText);
        innerLayout.addView(dateText);

        // Add inner layout to card
        card.addView(innerLayout);

        // Add ripple effect using theme attributes
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        card.setForeground(getDrawable(outValue.resourceId));

        return card;
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