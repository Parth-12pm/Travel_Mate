package com.example.travelmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String profilePictureUrl = "";

    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        // Image selected from gallery
                        imageUri = data.getData();
                        uploadImageToCloudinary(imageUri);
                    } else {
                        // Image captured from camera
                        if (imageUri != null) {
                            uploadImageToCloudinary(imageUri);
                        } else {
                            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "diozithos"); // Replace with your Cloudinary cloud name
        config.put("api_key", "191271234992971"); // Replace with your Cloudinary API key
        config.put("api_secret", "6E-sjMWsjvHMf8MXH-mDeAMXfhs"); // Replace with your Cloudinary API secret
        MediaManager.init(this, config);


        EditText signUpName = findViewById(R.id.signUpName);
        EditText signUpSurname = findViewById(R.id.signUpSurname);
        EditText signUpEmail = findViewById(R.id.signUpEmail);
        EditText signUpPassword = findViewById(R.id.signUpPassword);
        RadioGroup genderRadioGroup = findViewById(R.id.genderRadioGroup);
        Button uploadProfilePictureButton = findViewById(R.id.uploadProfilePictureButton);
        Button registerButton = findViewById(R.id.registerButton);

        // Handle profile picture upload (camera or gallery)
        uploadProfilePictureButton.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // Create a file to save the camera image
            File photoFile = createImageFile();
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                // Create a chooser dialog for the user to select camera or gallery
                Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Picture");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
                imagePickerLauncher.launch(chooserIntent);
            } else {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle registration
        registerButton.setOnClickListener(v -> {
            String name = signUpName.getText().toString();
            String surname = signUpSurname.getText().toString();
            String email = signUpEmail.getText().toString();
            String password = signUpPassword.getText().toString();
            String gender = genderRadioGroup.getCheckedRadioButtonId() == R.id.maleRadioButton ? "Male" : "Female";

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || surname.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user with email and password
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save user details to Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name);
                                userData.put("surname", surname);
                                userData.put("email", email);
                                userData.put("gender", gender);
                                userData.put("profilePictureUrl", profilePictureUrl); // Cloudinary URL

                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            finish(); // Close the activity
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            if (task.getException() != null) {
                                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Toast.makeText(SignUpActivity.this, "Upload started...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Upload in progress
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                profilePictureUrl = (String) resultData.get("url");
                Toast.makeText(SignUpActivity.this, "Profile picture uploaded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(SignUpActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Upload rescheduled
            }
        }).dispatch();
    }
}