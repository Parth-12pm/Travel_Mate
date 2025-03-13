# Travel Mate - Your Personal Travel Companion

## Overview
Travel Mate is a modern and user-friendly Android app designed to assist travelers in planning, organizing, and documenting their trips seamlessly. The app allows users to create travel itineraries, manage journals with notes and media, and access weather forecasts and navigation assistance using Google Maps API.

## Features
- **User Authentication** (Firebase Authentication)
- **Trip Planning** (Google Maps API for source/destination selection)
- **Travel Journal** (Add notes, images, videos, and locations)
- **Weather Forecast Widget** (Fetches real-time weather data)
- **Offline Access** (Stores data locally using Firebase Firestore caching)
- **Profile & Settings** (User preferences and account management)
- **Simple & Intuitive UI** (Dual-tone design with Material UI components)

## Tech Stack
- **Programming Language:** Java (Android Studio)
- **Database:** Firebase Firestore
- **Authentication:** Firebase Authentication
- **Cloud Storage:** Firebase Storage (for media uploads)
- **APIs Used:**
  - Google Maps API (Navigation & Location)
  - OpenWeather API (Weather Forecasts)
  - Firebase Firestore (Database)

## Minimum Requirements
- **Android 8.0 (API Level 26)**
- **Android Studio (Latest Version)**
- **Google Play Services Installed**

## Setup & Installation
1. **Clone the repository:**
   ```sh
   git clone https://github.com/yourusername/TravelMate.git
   cd TravelMate
   ```
2. **Open the project in Android Studio**
3. **Configure Firebase:**
   - Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
   - Download `google-services.json` and place it in `app/` directory.
4. **Enable Google Maps API:**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Enable "Maps SDK for Android"
   - Get API Key and add it to `AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_API_KEY" />
     ```
5. **Run the project:**
   - Connect an Android device or use an emulator.
   - Click **Run** ▶️ in Android Studio.

## Screenshots (Coming Soon)

## Contributing
- Feel free to fork the repo and submit pull requests!
- Report issues or suggest new features in the **Issues** section.

## License
This project is licensed under the **MIT License**.

---
🚀 Happy Traveling with Travel Mate! 🚀

