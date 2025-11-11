# 🔍 Firebase Troubleshooting - Self-Diagnostic Guide

## Quick Checks (5 minutes)

Follow these steps in order to identify if the Firebase issue is on your end:

---

## ✅ Step 1: Verify Firebase Console Access

1. **Open Firebase Console:**

   - Go to https://console.firebase.google.com/
   - Login with your Google account

2. **Find Your Project:**

   - Look for project: `ldms-4f84d`
   - Can you see it?
     - ✅ Yes → Continue to Step 2
     - ❌ No → **Issue: You don't have access to this project**

3. **Check Project Status:**
   - Click on the project
   - Look for any warning banners or error messages
   - Is the project active?
     - ✅ Yes → Continue to Step 2
     - ❌ No → **Issue: Project may be disabled or billing issue**

---

## ✅ Step 2: Verify Realtime Database Exists

1. **Navigate to Database:**

   - In Firebase Console → Click "Realtime Database" in left menu
   - Do you see a database?
     - ✅ Yes → Continue to Step 3
     - ❌ No → **Issue: Database not created yet**

2. **Check Database URL:**

   - Look at the database URL at the top
   - Should be: `https://ldms-4f84d-default-rtdb.firebaseio.com/`
   - Does it match?
     - ✅ Yes → Continue to Step 3
     - ❌ No → **Issue: Wrong database or region**

3. **Check Database Contents:**
   - Do you see data in the database?
     - ✅ Yes → Note what you see, continue to Step 3
     - ❌ Empty → **Possible issue: No data uploaded**

---

## ✅ Step 3: Verify Database Rules

1. **Click on "Rules" tab** in Realtime Database

2. **Check Current Rules:**

   **GOOD Rules (Testing):**

   ```json
   {
     "rules": {
       ".read": true,
       ".write": true
     }
   }
   ```

   **GOOD Rules (Production with Auth):**

   ```json
   {
     "rules": {
       ".read": "auth != null",
       ".write": "auth != null"
     }
   }
   ```

   **BAD Rules (Blocking Everything):**

   ```json
   {
     "rules": {
       ".read": false,
       ".write": false
     }
   }
   ```

3. **Test Your Rules:**
   - Are your rules allowing read access?
     - ✅ Yes (`.read: true` or `.read: "auth != null"`) → Continue to Step 4
     - ❌ No (`.read: false`) → **FOUND ISSUE: Rules blocking access**

**How to Fix:**

- Click "Rules" tab
- Replace with testing rules (see above)
- Click "Publish"
- ⚠️ **Remember to secure before production!**

---

## ✅ Step 4: Verify Database Structure

1. **Check Root Level Data:**

   - In "Data" tab, what do you see at the root?

   **CORRECT Structure:**

   ```
   📦 Root
    ├─ 📁 node_1
    ├─ 📁 node_2
    ├─ 📁 node_3
    └─ 📁 users (optional)
   ```

   **INCORRECT Structure:**

   ```
   📦 Root
    ├─ accelX: -0.00117
    ├─ accelY: -0.00542
    ├─ rain: 0
    ├─ 📁 node_1
    └─ 📁 node_2
   ```

   ☝️ _Root-level sensor data is wrong!_

2. **Check node_1 Contents:**

   - Click on `node_1`
   - Do you see these fields?
     - ✅ `name` (String) - e.g., "Sensor Node 1"
     - ✅ `latitude` (Number) - e.g., 37.7749
     - ✅ `longitude` (Number) - e.g., -122.4194
     - ✅ `tilt` (Number)
     - ✅ `rain` (Number)
     - ✅ `soilMoisture` (Number)

3. **Common Structure Issues:**

   **Issue A: Missing "name" field**

   ```
   node_1:
     - nodeName: "Sensor 1"  ❌ WRONG
     - latitude: 37.7749
   ```

   **Fix:** Rename `nodeName` to `name`

   **Issue B: Wrong data types**

   ```
   node_1:
     - name: "Sensor 1"
     - latitude: "37.7749"  ❌ String instead of Number
   ```

   **Fix:** Remove quotes to make it a number

   **Issue C: Missing required fields**

   ```
   node_1:
     - name: "Sensor 1"
     - (no latitude/longitude)  ❌ MISSING
   ```

   **Fix:** Add all required fields

---

## ✅ Step 5: Verify google-services.json

1. **Check File Exists:**

   ```powershell
   Test-Path "d:\MADCW\app\google-services.json"
   ```

   - Returns `True`?
     - ✅ Yes → Continue
     - ❌ No → **FOUND ISSUE: Missing google-services.json**

2. **Check File Contents:**

   ```powershell
   Get-Content "d:\MADCW\app\google-services.json" | Select-String "ldms-4f84d"
   ```

   - Do you see output with "ldms-4f84d"?
     - ✅ Yes → Continue
     - ❌ No → **FOUND ISSUE: Wrong google-services.json file**

3. **Verify Project ID:**
   ```powershell
   Get-Content "d:\MADCW\app\google-services.json" | Select-String "project_id"
   ```
   - Should show: `"project_id": "ldms-4f84d"`
   - Does it match?
     - ✅ Yes → Continue to Step 6
     - ❌ No → **FOUND ISSUE: Wrong project ID in config**

**How to Fix:**

- Go to Firebase Console → Project Settings (gear icon)
- Scroll down to "Your apps"
- Click the Android app
- Click "Download google-services.json"
- Replace the file in `d:\MADCW\app\`

---

## ✅ Step 6: Verify Authentication

1. **Check if Authentication is Enabled:**
   - Firebase Console → Authentication
   - Is "Email/Password" enabled?
     - ✅ Yes → Continue
     - ❌ No → **FOUND ISSUE: Auth not enabled**

**How to Enable:**

- Click "Get started" (if new)
- Click "Sign-in method" tab
- Click "Email/Password"
- Toggle to "Enabled"
- Click "Save"

2. **Check if You Have Users:**

   - Go to "Users" tab
   - Do you see any users?
     - ✅ Yes → Note the user email
     - ❌ No → Need to sign up in app first

3. **Test Login:**
   - Try logging into the app
   - What happens?
     - ✅ Success → Auth is working
     - ❌ Error → Check Logcat for details

---

## ✅ Step 7: Check Network & Connectivity

1. **Test Internet Connection:**

   ```powershell
   Test-NetConnection -ComputerName firebaseio.com -Port 443
   ```

   - Shows "TcpTestSucceeded : True"?
     - ✅ Yes → Internet works
     - ❌ No → **FOUND ISSUE: Network/Firewall blocking Firebase**

2. **Check Emulator/Device Internet:**

   - If using emulator: Can you open browser and visit google.com?
   - If using device: Is WiFi/mobile data enabled?
     - ✅ Yes → Continue
     - ❌ No → **FOUND ISSUE: Device has no internet**

3. **Check Firewall/Antivirus:**
   - Temporarily disable firewall/antivirus
   - Does the app work now?
     - ✅ Yes → **FOUND ISSUE: Firewall blocking Firebase**
     - ❌ No → Continue to Step 8

---

## ✅ Step 8: Run App Validation

1. **Clean and Rebuild:**

   ```powershell
   .\gradlew clean assembleDebug
   ```

   - Build successful?
     - ✅ Yes → Continue
     - ❌ No → Check errors, fix compile issues first

2. **Install and Run:**

   ```powershell
   .\gradlew installDebug
   ```

3. **Check Logcat for Firebase Logs:**

   **In Android Studio:**

   - Open Logcat (bottom panel)
   - Filter by: `FirebaseValidator` or `DashboardActivity`

   **Common Error Messages:**

   **A. Permission Denied:**

   ```
   DatabaseError: Permission denied
   ```

   → **Issue: Database rules are too restrictive**
   → **Fix: See Step 3**

   **B. Network Error:**

   ```
   Failed to connect to Firebase
   Timeout
   ```

   → **Issue: Network/connectivity problem**
   → **Fix: See Step 7**

   **C. Invalid Configuration:**

   ```
   FirebaseApp not initialized
   google-services.json missing
   ```

   → **Issue: Missing or wrong config file**
   → **Fix: See Step 5**

   **D. No Data Found:**

   ```
   Found 0 sensor nodes
   ```

   → **Issue: Database is empty or wrong structure**
   → **Fix: See Step 4**

---

## 🎯 Quick Decision Tree

```
Can you access Firebase Console?
├─ No → You don't have project access (contact project owner)
└─ Yes → Does Realtime Database exist?
    ├─ No → Create database in Firebase Console
    └─ Yes → Are rules set to allow read?
        ├─ No → Change rules to allow read (Step 3)
        └─ Yes → Does database have node_1, node_2, etc?
            ├─ No → Import firebase-database-corrected.json
            └─ Yes → Does node_1 have "name" field (not "nodeName")?
                ├─ No → Rename field from nodeName to name
                └─ Yes → Does google-services.json exist?
                    ├─ No → Download from Firebase Console
                    └─ Yes → Check Logcat for specific errors
```

---

## 📋 Checklist Summary

Copy this and check off as you verify:

```
FIREBASE CONSOLE:
[ ] Can access Firebase Console
[ ] Project "ldms-4f84d" exists and is active
[ ] Realtime Database exists
[ ] Database URL is correct

DATABASE RULES:
[ ] Rules allow read access (either true or auth != null)
[ ] Rules allow write access (for testing, use true)

DATABASE STRUCTURE:
[ ] Has node_1, node_2, node_3 at root level
[ ] No sensor data at root level (accelX, rain, etc.)
[ ] Each node has "name" field (NOT "nodeName")
[ ] Each node has latitude, longitude, tilt, rain, soilMoisture
[ ] All numeric fields are numbers (not strings)

CONFIGURATION:
[ ] google-services.json exists in app folder
[ ] google-services.json has project_id: "ldms-4f84d"
[ ] File is recent (downloaded after any project changes)

AUTHENTICATION:
[ ] Email/Password auth is enabled in Firebase Console
[ ] At least one test user exists
[ ] Can login with test credentials

NETWORK:
[ ] Internet connection works
[ ] Can ping firebaseio.com
[ ] No firewall blocking Firebase
[ ] Device/emulator has internet access

BUILD & APP:
[ ] App builds successfully (no compile errors)
[ ] App installs on device/emulator
[ ] No crashes on startup
[ ] Logcat shows connection attempts
```

---

## 🚨 Most Common Issues (90% of problems)

### 1. **Wrong Database Rules** (40%)

**Symptom:** "Permission denied" in logs
**Quick Fix:**

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

### 2. **Wrong Database Structure** (30%)

**Symptom:** "Found 0 sensor nodes" in logs
**Quick Fix:** Import `firebase-database-corrected.json`

### 3. **Missing/Wrong google-services.json** (15%)

**Symptom:** App can't connect to Firebase at all
**Quick Fix:** Re-download from Firebase Console

### 4. **Field Name Mismatch** (10%)

**Symptom:** Sensors load but show "null" for name
**Quick Fix:** Rename `nodeName` → `name` in database

### 5. **No Internet** (5%)

**Symptom:** Timeout errors
**Quick Fix:** Check device WiFi/mobile data

---

## 🔧 Emergency Quick Test

Run this to quickly test if Firebase is accessible:

```powershell
# Test if Firebase is reachable
curl "https://ldms-4f84d-default-rtdb.firebaseio.com/.json"
```

**Expected Result:**

- You should see JSON data (your database contents)
- If you get an error, Firebase is not accessible from your network

**If it shows "Permission denied":**

- Your rules are blocking access
- Go to Firebase Console → Database → Rules
- Change to allow read/write temporarily

---

## 📞 Next Steps

After going through these checks:

1. **Note which step failed** (if any)
2. **Copy the error message** from that step
3. **Check Logcat** and copy relevant Firebase errors
4. **Share findings** so we can pinpoint the exact issue

Most Firebase issues are one of the items above. Once you identify which step fails, the fix is usually straightforward! 🎯
