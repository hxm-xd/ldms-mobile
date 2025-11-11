# 🚀 Quick Firebase Diagnostic

## Run This First:

```powershell
.\check-firebase.ps1
```

This automated script will check:

- ✅ google-services.json configuration
- ✅ Network connectivity to Firebase
- ✅ Database accessibility
- ✅ Database structure
- ✅ Build configuration

---

## Common Issues & Quick Fixes

### 1️⃣ Permission Denied

**Symptom:** Can't read from database

**Fix:**

```
Firebase Console → Realtime Database → Rules
Change to: { "rules": { ".read": true, ".write": true } }
Click "Publish"
```

### 2️⃣ Found 0 Sensors

**Symptom:** App loads but no markers on map

**Fix:**

```
Firebase Console → Realtime Database → Data
Click ⋮ menu → Import JSON
Select: firebase-database-corrected.json
```

### 3️⃣ Null Sensor Names

**Symptom:** Sensors load but show "null"

**Fix:**

```
In Firebase, rename field from "nodeName" to "name"
Each node needs: name, latitude, longitude, tilt, rain, soilMoisture
```

### 4️⃣ Can't Connect

**Symptom:** App can't reach Firebase

**Fix:**

```
1. Check internet connection
2. Re-download google-services.json from Firebase Console
3. Place in app/ folder
4. Rebuild: .\gradlew clean assembleDebug
```

---

## Manual Checks

### Firebase Console Quick Check:

1. **Access:** https://console.firebase.google.com/
2. **Project:** ldms-4f84d
3. **Database:** Realtime Database → Data
4. **Should see:** node_1, node_2, node_3

### Database Structure Test:

```powershell
# Test if database is accessible
curl "https://ldms-4f84d-default-rtdb.firebaseio.com/.json"
```

**Good Response:** Shows your JSON data
**Bad Response:** "Permission denied" or timeout

---

## Full Documentation

- 📄 **FIREBASE_SELF_DIAGNOSTIC.md** - Complete step-by-step guide
- 📄 **FIREBASE_VALIDATION_GUIDE.md** - Validation procedures
- 📄 **FIREBASE_CONNECTION_SUMMARY.md** - Setup summary

---

## Get Help

After running diagnostics, if issues persist:

1. ✅ Run `.\check-firebase.ps1`
2. ✅ Note which checks failed
3. ✅ Check Logcat for errors (filter: "FirebaseValidator")
4. ✅ Share the error messages

Most issues are configuration-related and can be fixed quickly! 🎯
