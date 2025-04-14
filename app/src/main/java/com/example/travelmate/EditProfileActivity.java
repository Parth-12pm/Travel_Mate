package com.example.travelmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {
    private CircleImageView profileImage;
    private EditText editName, editSurname;
    private RadioGroup genderRadioGroup;
    private Button saveButton;
    private String profilePictureUrl = "";
    private Uri imageUri;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();
                        uploadImageToCloudinary(imageUri);
                    } else if (imageUri != null) {
                        uploadImageToCloudinary(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        profileImage = findViewById(R.id.profileImage);
        editName = findViewById(R.id.editName);
        editSurname = findViewById(R.id.editSurname);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        saveButton = findViewById(R.id.saveButton);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load current user data
        loadUserData();

        // Set up profile picture change
        profileImage.setOnClickListener(v -> openImagePicker());

        // Set up save button
        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editName.setText(documentSnapshot.getString("name"));
                        editSurname.setText(documentSnapshot.getString("surname"));

                        String gender = documentSnapshot.getString("gender");
                        if (gender != null) {
                            int radioId = gender.equals("Male") ? R.id.maleRadioButton : R.id.femaleRadioButton;
                            genderRadioGroup.check(radioId);
                        }

                        String imageUrl = documentSnapshot.getString("profilePictureUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            profilePictureUrl = imageUrl;
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.account_circle_24px)
                                    .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Toast.makeText(EditProfileActivity.this, "Uploading image...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                profilePictureUrl = (String) resultData.get("url");
                Glide.with(EditProfileActivity.this)
                        .load(profilePictureUrl)
                        .into(profileImage);
                Toast.makeText(EditProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(EditProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveProfileChanges() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        String name = editName.getText().toString();
        String surname = editSurname.getText().toString();
        String gender = genderRadioGroup.getCheckedRadioButtonId() == R.id.maleRadioButton ? "Male" : "Female";

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("surname", surname);
        updates.put("gender", gender);

        if (!profilePictureUrl.isEmpty()) {
            updates.put("profilePictureUrl", profilePictureUrl);
        }

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}