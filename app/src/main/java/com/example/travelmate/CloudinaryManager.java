package com.example.travelmate;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {
    private static boolean isInitialized = false;

    public static void initialize(Context context) {
        if (!isInitialized) {
            Map config = new HashMap();
            config.put("cloud_name", BuildConfig.CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUD_API_KEY);
            config.put("api_secret", BuildConfig.CLOUD_API_SECRET);
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }
}