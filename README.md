# Bible Verse App

Android app to save, browse, and share Bible verses with images. Built with Jetpack Compose + Firebase.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| Auth | Firebase Auth + Credential Manager |
| Database | Cloud Firestore (realtime) |
| Storage | Firebase Storage |
| Images | Coil |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines |

---

## Setup (Required Before Running)

### 1. Add `google-services.json`
Place your `google-services.json` file into:
```
app/google-services.json
```
Download it from: **Firebase Console → Project Settings → Your apps → google-services.json**

### 2. Set your Web Client ID
Open `app/src/main/res/values/strings.xml` and replace `YOUR_WEB_CLIENT_ID_HERE`:
```xml
<string name="default_web_client_id">123456789-abc...xyz.apps.googleusercontent.com</string>
```
Find it in: **Firebase Console → Authentication → Sign-in method → Google → Web SDK configuration → Web client ID**

### 3. Enable Firebase services
In Firebase Console:
- **Authentication** → Sign-in method → Enable **Google**
- **Firestore** → Create database (start in production mode, apply `firestore.rules`)
- **Storage** → Get started (apply `storage.rules`)

### 4. Apply Security Rules
Upload `firestore.rules` and `storage.rules` via Firebase Console or Firebase CLI:
```bash
firebase deploy --only firestore:rules,storage
```

### 5. Add SHA-1 fingerprint to Firebase
```bash
# Debug keystore
./gradlew signingReport
```
Copy the SHA-1 and add it in: **Firebase Console → Project Settings → Your apps → Add fingerprint**

---

## Project Structure

```
com.raguel0087.myapplication
│
├── MainActivity.kt                  # Entry point, hosts NavGraph
│
├── auth/
│   └── GoogleSignInHelper.kt        # Credential Manager sign-in, Firebase auth
│
├── model/
│   ├── BibleVerse.kt                # Firestore document model
│   └── UserData.kt                  # Signed-in user info
│
├── repository/
│   └── BibleRepository.kt           # All Firebase ops (Firestore + Storage)
│
├── viewmodel/
│   ├── AuthViewModel.kt             # Login/logout state
│   └── MainViewModel.kt             # Verses: load, add, edit, delete, search
│
├── navigation/
│   └── AppNavigation.kt             # NavHost with login/main routes
│
└── ui/
    ├── LoginScreen.kt               # Google Sign-In screen
    ├── MainScreen.kt                # Grid, search, FAB, cards
    ├── ProfileScreen.kt             # Bottom sheet: photo, stats, logout
    ├── Dialogs.kt                   # Add/Edit/Delete dialogs
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## Firestore Schema

**Collection:** `verses`

| Field | Type | Description |
|---|---|---|
| `id` | String (auto) | Document ID |
| `verse` | String | Bible verse text |
| `reference` | String | e.g. "John 3:16" |
| `imageUrl` | String | Firebase Storage download URL |
| `ownerId` | String | Firebase Auth UID |
| `ownerName` | String | Display name at time of creation |
| `createdAt` | Timestamp | Server timestamp |

---

## Features

- ✅ Google Sign-In (Credential Manager API)
- ✅ Auto-restore session on relaunch
- ✅ Realtime Firestore listener (no manual refresh needed)
- ✅ 2-column LazyVerticalGrid
- ✅ Upload image from gallery → Firebase Storage
- ✅ Add / Edit / Delete verse dialogs
- ✅ Edit & Delete restricted to verse owner only
- ✅ Real-time search (reference + verse text)
- ✅ Manual refresh button in TopAppBar
- ✅ Profile bottom sheet (photo, name, email, verse count, logout)
- ✅ Loading states (login, upload, initial fetch)
- ✅ Error state with Retry button
- ✅ Empty state (global + search-specific)
- ✅ Snackbar feedback (add/edit/delete success, upload error)
- ✅ Input validation (empty reference, verse, image all blocked)
- ✅ Delete confirmation AlertDialog
- ✅ Material 3 + Dynamic Color (Android 12+)
