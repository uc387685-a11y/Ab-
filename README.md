# Hello Umesh AI (Complete Native Android Project)

A complete, modern personal AI assistant built natively with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Google Gemini 3.8 Flash** via **Firebase AI Logic**. Designed with full multilingual voice support (Hindi, Nepali, English) and optimized for Redmi/Android smartphones.

---

## 🚀 How to Export & Build the APK for Free (Without Google Play)

You can build and install this app on your phone **100% free** without needing a Google Play Developer account, without paying any fees, and without publishing to the store.

### Method 1: Export as ZIP from Google AI Studio
1. In the **Google AI Studio Build** window, look at the top right or sidebar menu.
2. Click the **Export** / **Settings** icon (or three dots `⋮`).
3. Select **Download Project (ZIP)** or **Push to GitHub**.
4. Extract the ZIP file to a folder on your computer (e.g. `C:\Projects\HelloUmeshAI` or `~/HelloUmeshAI`).

---

### Method 2: Build the APK using Android Studio (Recommended)

#### Step 1: Install Android Studio
- Download and install the latest free version of **Android Studio** (Ladybug / Iguana or later) from [developer.android.com/studio](https://developer.android.com/studio).

#### Step 2: Open the Project
1. Open Android Studio.
2. Click **Open** (or `File > Open`).
3. Browse to and select the extracted folder.
4. Android Studio will automatically recognize the Gradle files and start syncing dependencies.

#### Step 3: Build the APK
1. In the top menu bar, click:
   ```
   Build  ➔  Build Bundle(s) / APK(s)  ➔  Build APK(s)
   ```
2. Android Studio will compile the app in 1–2 minutes.
3. When finished, a popup notification will appear at the bottom right:
   ```
   APK(s) generated successfully for 1 module: 'app'
   [ locate ]
   ```
4. Click **locate**. It will open the folder containing your APK:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Method 3: Build using Command Line (Terminal / CMD)

If you have Java and Android SDK installed, open Terminal or CMD inside the project root and run:

**On Windows:**
```bat
gradlew.bat assembleDebug
```

**On Mac / Linux:**
```bash
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 How to Install the APK on Your Redmi / Android Smartphone

1. Transfer `app-debug.apk` to your phone using:
   - USB cable
   - Google Drive or WhatsApp / Telegram (send file to yourself)
   - Nearby Share / Quick Share / Bluetooth
2. On your phone, tap `app-debug.apk` in your File Manager or Downloads.
3. If prompted: **"Install unknown apps"**, toggle **Allow from this source**.
4. On Redmi / Xiaomi phones (MIUI / HyperOS):
   - A security scan will check the APK.
   - Tap **Install**.
5. Once installed, tap **Open** to launch **Hello Umesh AI**!

---

## 📁 Complete Project Structure & File Guide

```
├── .env.example                               # Environment config template
├── settings.gradle.kts                        # Root project definition (Hello Umesh AI)
├── build.gradle.kts                           # Top-level Gradle plugin declarations
├── gradle/
│   ├── libs.versions.toml                     # Version Catalog (Dependencies & plugins)
│   └── wrapper/
│       ├── gradle-wrapper.properties          # Gradle distribution configuration
│       └── gradle-wrapper.jar
├── gradlew & gradlew.bat                      # Executable Gradle build scripts
├── app/
│   ├── build.gradle.kts                       # App module build configuration, dependencies, signing
│   ├── google-services.json                   # Firebase AI Logic & App Check configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml            # Permissions, Service, and Package Visibility
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt            # App Check initialization, theme, navigation
│       │   │   ├── core/
│       │   │   │   ├── AppCheckConfig.kt      # Firebase App Check debug & release provider setup
│       │   │   │   ├── NetworkMonitor.kt      # Real-time online/offline connectivity callback flow
│       │   │   │   └── PreferencesManager.kt  # Jetpack DataStore user settings (language, greeting, voice)
│       │   │   ├── data/local/
│       │   │   │   ├── Entities.kt            # Room entities (ConversationEntity, MessageEntity)
│       │   │   │   ├── ChatDao.kt             # SQLite Room DAO for message operations
│       │   │   │   └── AppDatabase.kt         # Room database singleton
│       │   │   ├── ai/
│       │   │   │   ├── GeminiAssistant.kt     # Firebase AI Logic (Gemini 3.8 Flash, streaming, context)
│       │   │   │   └── GeminiAppBuilder.kt    # Interactive AI Android Kotlin Compose app code builder
│       │   │   ├── voice/
│       │   │   │   ├── SpeechManager.kt       # Speech-to-text with audio RMS visualizer & wake-word detection
│       │   │   │   ├── TtsManager.kt          # Text-to-speech with pitch, speed, and language controls
│       │   │   │   └── AudioRecordManager.kt  # MediaRecorder & audio presets (Robot, Titan, Helium)
│       │   │   ├── actions/
│       │   │   │   ├── AppLauncherHelper.kt   # Voice launcher for YouTube, Facebook, WhatsApp, etc.
│       │   │   │   ├── ContactCallerHelper.kt # Search contacts by voice with explicit call confirmation
│       │   │   │   ├── AlarmTimerHelper.kt    # Hindi/English natural language timer and alarm parser
│       │   │   │   ├── DeviceControlHelper.kt # System volume adjustment & settings shortcuts
│       │   │   │   ├── WebSearchHelper.kt     # Google, YouTube, Facebook, Instagram search queries
│       │   │   │   └── SocialShareHelper.kt   # Social comment drafter & photo sharing
│       │   │   ├── service/
│       │   │   │   └── WakeWordService.kt     # Foreground service for screen-off wake phrase detection
│       │   │   └── ui/
│       │   │       ├── MainScreen.kt          # Bottom navigation scaffold & back handler
│       │   │       ├── ChatViewModel.kt       # MVVM StateFlow coordinator
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt           # Modern electric sky cyan, amber, slate palette
│       │   │       │   ├── Theme.kt           # Dark/Light Material 3 theme implementation
│       │   │       │   └── Type.kt            # Typography scaling
│       │   │       └── screens/
│       │   │           ├── ChatScreen.kt          # Real-time chat, audio visualizer, confirmation cards
│       │   │           ├── PhotoGeneratorScreen.kt# AI photo generator with style pills & photo picker
│       │   │           ├── VideoGeneratorScreen.kt# AI video prompt studio with storyboard & preview
│       │   │           ├── AppBuilderScreen.kt    # Full Android Kotlin code generator & copy tabs
│       │   │           ├── VoiceChangerScreen.kt  # Assistant voice tuning & user voice conversion
│       │   │           ├── SettingsScreen.kt      # Languages, wake word, Redmi guide, permissions
│       │   │           └── ChatHistorySheet.kt    # Modal bottom sheet for saved conversations
│       │   └── res/
│       │       ├── values/strings.xml         # App name and user strings
│       │       ├── values/firebase_applet_config.xml # ReCAPTCHA Enterprise site key
│       │       ├── drawable/                  # Vector assets & generated launcher icon
│       │       └── mipmap-*/                  # Custom adaptive icons across mdpi..xxxhdpi
│       └── test/java/com/example/             # Local JVM and Robolectric unit tests
```

---

## ⚙️ Redmi & Xiaomi (MIUI / HyperOS) Phone Tips

For continuous background wake-word listening on Redmi smartphones:
1. Open **Settings ➔ Apps ➔ Manage apps ➔ Hello Umesh AI**.
2. Enable **Autostart** (ऑटोस्टार्ट).
3. Tap **Battery saver** (बैटरी सेवर) and select **No restrictions** (कोई प्रतिबंध नहीं).
4. Lock the app in your Recent Apps screen by holding down its preview card and tapping the Lock icon.
