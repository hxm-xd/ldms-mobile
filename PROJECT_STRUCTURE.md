# 🗂️ MADCW Project Structure

## 📁 Clean Project Organization

This project has been cleaned and organized. Here's what each file/folder does:

---

## 🔧 Essential Build Files

| File/Folder           | Purpose                          | Keep?   |
| --------------------- | -------------------------------- | ------- |
| `build.gradle.kts`    | Root project build configuration | ✅ KEEP |
| `settings.gradle.kts` | Gradle settings                  | ✅ KEEP |
| `gradle.properties`   | Gradle properties                | ✅ KEEP |
| `gradlew`             | Gradle wrapper (Unix)            | ✅ KEEP |
| `gradlew.bat`         | Gradle wrapper (Windows)         | ✅ KEEP |
| `gradle/`             | Gradle wrapper JAR               | ✅ KEEP |
| `local.properties`    | Local SDK paths & API keys       | ✅ KEEP |

---

## 🔥 Firebase Configuration

| File                               | Purpose                              | Keep?   |
| ---------------------------------- | ------------------------------------ | ------- |
| `firebase-database-corrected.json` | Correct Firebase structure to import | ✅ KEEP |
| `database.rules.json`              | Firebase security rules              | ✅ KEEP |
| `FIREBASE_SETUP.md`                | Firebase setup instructions          | ✅ KEEP |
| `FIREBASE_VALIDATION_GUIDE.md`     | How to validate Firebase connection  | ✅ KEEP |
| `FIREBASE_CONNECTION_SUMMARY.md`   | Quick reference for Firebase         | ✅ KEEP |

---

## 📱 Application Source

| Folder                     | Purpose                           | Keep?   |
| -------------------------- | --------------------------------- | ------- |
| `app/`                     | Main application module           | ✅ KEEP |
| `app/src/main/`            | Source code                       | ✅ KEEP |
| `app/build.gradle.kts`     | App-level build config            | ✅ KEEP |
| `app/google-services.json` | Firebase configuration            | ✅ KEEP |
| `app/proguard-rules.pro`   | ProGuard rules for release builds | ✅ KEEP |

---

## 📄 Documentation

| File                                                       | Purpose          | Keep?   |
| ---------------------------------------------------------- | ---------------- | ------- |
| `Proposal - Landslide Detection and Monitoring System.pdf` | Project proposal | ✅ KEEP |

---

## 🗑️ Files REMOVED (No Longer Needed)

These files have been removed to clean up the project:

| File                                  | Why Removed                                              |
| ------------------------------------- | -------------------------------------------------------- |
| `ldms-4f84d-default-rtdb-export.json` | Old Firebase export with incorrect structure             |
| `firebase-structure-example.json`     | Duplicate (replaced by firebase-database-corrected.json) |
| `REBUILD_SUMMARY.md`                  | Outdated, superseded by current Firebase guides          |
| `SETUP_CHECKLIST.md`                  | Redundant, covered in Firebase guides                    |
| `TROUBLESHOOTING.md`                  | Redundant, covered in Firebase validation guide          |
| `.gradle/`                            | Build cache (auto-regenerated)                           |
| `.kotlin/`                            | Kotlin cache (auto-regenerated)                          |
| `app/build/`                          | Build artifacts (auto-regenerated)                       |

---

## 🚫 Git-Ignored Items

These folders exist but are not tracked in Git:

| Folder       | Purpose                             |
| ------------ | ----------------------------------- |
| `.git/`      | Git version control data            |
| `.idea/`     | Android Studio IDE settings         |
| `.vscode/`   | VS Code IDE settings                |
| `.gradle/`   | Gradle cache (regenerated on build) |
| `.kotlin/`   | Kotlin compiler cache               |
| `app/build/` | Build output (regenerated on build) |

---

## 📊 Current Project Size

After cleanup:

- **Essential files only**: Config, source code, and documentation
- **No build artifacts**: All caches removed
- **No duplicate docs**: Single source of truth for Firebase setup

---

## 🔄 Regenerating Build Files

If you need to rebuild after cleanup:

```powershell
# Clean build (regenerates all caches)
.\gradlew clean

# Build debug APK
.\gradlew assembleDebug

# Install on device
.\gradlew installDebug
```

---

## 📝 Important Notes

1. **Never delete:**

   - `google-services.json` (Firebase config)
   - `local.properties` (API keys)
   - `gradle/wrapper/` (needed for builds)

2. **Safe to delete anytime:**

   - `.gradle/` folder
   - `.kotlin/` folder
   - `app/build/` folder
   - These regenerate automatically

3. **Version control:**
   - `local.properties` is git-ignored (contains secrets)
   - `google-services.json` should be git-ignored if it contains sensitive data
   - Check `.gitignore` to see what's excluded

---

## ✅ Project is Now Clean!

Your project structure is now optimized with:

- ✅ No duplicate files
- ✅ No outdated documentation
- ✅ No unnecessary build artifacts
- ✅ Clear, organized documentation
- ✅ Only essential files remain

Ready for development! 🚀
