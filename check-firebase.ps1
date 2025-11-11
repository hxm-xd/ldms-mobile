# Firebase Self-Diagnostic Script
# Run this in PowerShell to automatically check common Firebase issues

Write-Host "=== FIREBASE SELF-DIAGNOSTIC SCRIPT ===" -ForegroundColor Cyan
Write-Host ""

$issuesFound = @()
$checksPerformed = 0

# Check 1: google-services.json exists
Write-Host "[1/6] Checking google-services.json..." -ForegroundColor Yellow
$checksPerformed++
if (Test-Path "app\google-services.json") {
    Write-Host "  ✅ google-services.json exists" -ForegroundColor Green
    
    # Check project ID
    $content = Get-Content "app\google-services.json" -Raw
    if ($content -match '"project_id":\s*"ldms-4f84d"') {
        Write-Host "  ✅ Project ID is correct (ldms-4f84d)" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Project ID mismatch or not found" -ForegroundColor Red
        $issuesFound += "Wrong project ID in google-services.json"
    }
} else {
    Write-Host "  ❌ google-services.json NOT FOUND" -ForegroundColor Red
    $issuesFound += "Missing google-services.json in app folder"
}

Write-Host ""

# Check 2: Corrected database file exists
Write-Host "[2/6] Checking firebase-database-corrected.json..." -ForegroundColor Yellow
$checksPerformed++
if (Test-Path "firebase-database-corrected.json") {
    Write-Host "  ✅ Corrected database file exists" -ForegroundColor Green
    Write-Host "  ℹ️  You can import this to Firebase Console" -ForegroundColor Cyan
} else {
    Write-Host "  ⚠️  firebase-database-corrected.json not found" -ForegroundColor Yellow
    Write-Host "  ℹ️  This is optional, but useful for reference" -ForegroundColor Cyan
}

Write-Host ""

# Check 3: Internet connectivity to Firebase
Write-Host "[3/6] Testing Firebase connectivity..." -ForegroundColor Yellow
$checksPerformed++
try {
    $result = Test-NetConnection -ComputerName "firebaseio.com" -Port 443 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($result) {
        Write-Host "  ✅ Can reach Firebase servers" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Cannot reach Firebase servers" -ForegroundColor Red
        $issuesFound += "Network cannot reach firebaseio.com (firewall/network issue)"
    }
} catch {
    Write-Host "  ⚠️  Could not test connection" -ForegroundColor Yellow
}

Write-Host ""

# Check 4: Build configuration
Write-Host "[4/6] Checking build configuration..." -ForegroundColor Yellow
$checksPerformed++
if (Test-Path "build.gradle.kts") {
    Write-Host "  ✅ Root build.gradle.kts exists" -ForegroundColor Green
} else {
    Write-Host "  ❌ Root build.gradle.kts missing" -ForegroundColor Red
    $issuesFound += "Missing root build.gradle.kts"
}

if (Test-Path "app\build.gradle.kts") {
    Write-Host "  ✅ App build.gradle.kts exists" -ForegroundColor Green
    
    # Check for Firebase dependencies
    $appBuild = Get-Content "app\build.gradle.kts" -Raw
    if ($appBuild -match "firebase") {
        Write-Host "  ✅ Firebase dependencies found" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  No Firebase dependencies found" -ForegroundColor Yellow
        $issuesFound += "Firebase dependencies may be missing in app/build.gradle.kts"
    }
} else {
    Write-Host "  ❌ App build.gradle.kts missing" -ForegroundColor Red
    $issuesFound += "Missing app/build.gradle.kts"
}

Write-Host ""

# Check 5: Validator exists
Write-Host "[5/6] Checking FirebaseValidator..." -ForegroundColor Yellow
$checksPerformed++
if (Test-Path "app\src\main\java\com\example\mad_cw\util\FirebaseValidator.kt") {
    Write-Host "  ✅ FirebaseValidator.kt exists" -ForegroundColor Green
    Write-Host "  ℹ️  Validator will run automatically when app launches" -ForegroundColor Cyan
} else {
    Write-Host "  ⚠️  FirebaseValidator.kt not found" -ForegroundColor Yellow
}

Write-Host ""

# Check 6: Quick Firebase database test
Write-Host "[6/6] Testing Firebase Realtime Database access..." -ForegroundColor Yellow
$checksPerformed++
Write-Host "  ℹ️  Attempting to read from database..." -ForegroundColor Cyan

try {
    $dbUrl = "https://ldms-4f84d-default-rtdb.firebaseio.com/.json"
    $response = Invoke-WebRequest -Uri $dbUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        $data = $response.Content | ConvertFrom-Json
        
        # Check for node_* entries
        $nodeCount = 0
        foreach ($key in $data.PSObject.Properties.Name) {
            if ($key -match "^node_\d+$") {
                $nodeCount++
            }
        }
        
        if ($nodeCount -gt 0) {
            Write-Host "  ✅ Database is accessible!" -ForegroundColor Green
            Write-Host "  ✅ Found $nodeCount sensor node(s)" -ForegroundColor Green
            
            # Check structure of first node
            $firstNode = $data.PSObject.Properties | Where-Object { $_.Name -match "^node_\d+$" } | Select-Object -First 1
            if ($firstNode) {
                $nodeData = $firstNode.Value
                $hasName = $nodeData.PSObject.Properties.Name -contains "name"
                $hasLat = $nodeData.PSObject.Properties.Name -contains "latitude"
                $hasLon = $nodeData.PSObject.Properties.Name -contains "longitude"
                
                if ($hasName -and $hasLat -and $hasLon) {
                    Write-Host "  ✅ Node structure looks correct (has name, lat, lon)" -ForegroundColor Green
                } else {
                    Write-Host "  ⚠️  Node structure may be incomplete:" -ForegroundColor Yellow
                    if (-not $hasName) { 
                        Write-Host "    - Missing 'name' field" -ForegroundColor Yellow
                        $issuesFound += "Database nodes missing 'name' field"
                    }
                    if (-not $hasLat) { 
                        Write-Host "    - Missing 'latitude' field" -ForegroundColor Yellow
                        $issuesFound += "Database nodes missing 'latitude' field"
                    }
                    if (-not $hasLon) { 
                        Write-Host "    - Missing 'longitude' field" -ForegroundColor Yellow
                        $issuesFound += "Database nodes missing 'longitude' field"
                    }
                }
            }
        } else {
            Write-Host "  ⚠️  Database is accessible but has NO sensor nodes" -ForegroundColor Yellow
            Write-Host "  ℹ️  You need to import firebase-database-corrected.json" -ForegroundColor Cyan
            $issuesFound += "Database has no node_* entries (empty or wrong structure)"
        }
    }
} catch {
    $errorMsg = $_.Exception.Message
    if ($errorMsg -match "Permission denied" -or $errorMsg -match "401") {
        Write-Host "  ❌ Permission Denied" -ForegroundColor Red
        Write-Host "  ℹ️  Database rules are blocking access" -ForegroundColor Yellow
        $issuesFound += "Database rules blocking access (Permission Denied)"
    } elseif ($errorMsg -match "timeout" -or $errorMsg -match "could not be resolved") {
        Write-Host "  ❌ Cannot reach database" -ForegroundColor Red
        Write-Host "  ℹ️  Network or DNS issue" -ForegroundColor Yellow
        $issuesFound += "Cannot reach Firebase database (network/DNS issue)"
    } else {
        Write-Host "  ⚠️  Error accessing database: $errorMsg" -ForegroundColor Yellow
        $issuesFound += "Database access error: $errorMsg"
    }
}

Write-Host ""
Write-Host "=".PadRight(60, "=") -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "SUMMARY:" -ForegroundColor Cyan
Write-Host "Checks performed: $checksPerformed/6" -ForegroundColor White
Write-Host "Issues found: $($issuesFound.Count)" -ForegroundColor White
Write-Host ""

if ($issuesFound.Count -eq 0) {
    Write-Host "🎉 NO ISSUES FOUND!" -ForegroundColor Green
    Write-Host "Your Firebase configuration looks good." -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Build the app: .\gradlew assembleDebug" -ForegroundColor White
    Write-Host "2. Install: .\gradlew installDebug" -ForegroundColor White
    Write-Host "3. Check Logcat for 'FirebaseValidator' to see detailed validation" -ForegroundColor White
} else {
    Write-Host "⚠️  ISSUES FOUND:" -ForegroundColor Yellow
    Write-Host ""
    for ($i = 0; $i -lt $issuesFound.Count; $i++) {
        Write-Host "  $($i + 1). $($issuesFound[$i])" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "RECOMMENDED FIXES:" -ForegroundColor Cyan
    Write-Host ""
    
    # Provide specific fixes
    foreach ($issue in $issuesFound) {
        if ($issue -match "google-services.json") {
            Write-Host "❯ google-services.json issue:" -ForegroundColor Yellow
            Write-Host "  1. Go to https://console.firebase.google.com/" -ForegroundColor White
            Write-Host "  2. Select project: ldms-4f84d" -ForegroundColor White
            Write-Host "  3. Project Settings → Your apps → Download google-services.json" -ForegroundColor White
            Write-Host "  4. Place in: d:\MADCW\app\" -ForegroundColor White
            Write-Host ""
        }
        if ($issue -match "Permission denied" -or $issue -match "rules blocking") {
            Write-Host "❯ Database rules issue:" -ForegroundColor Yellow
            Write-Host "  1. Go to Firebase Console → Realtime Database → Rules" -ForegroundColor White
            Write-Host "  2. Change to:" -ForegroundColor White
            Write-Host '     { "rules": { ".read": true, ".write": true } }' -ForegroundColor White
            Write-Host "  3. Click 'Publish'" -ForegroundColor White
            Write-Host ""
        }
        if ($issue -match "no node_" -or $issue -match "empty or wrong structure") {
            Write-Host "❯ Database structure issue:" -ForegroundColor Yellow
            Write-Host "  1. Go to Firebase Console → Realtime Database → Data" -ForegroundColor White
            Write-Host "  2. Click ⋮ menu → Import JSON" -ForegroundColor White
            Write-Host "  3. Select: firebase-database-corrected.json" -ForegroundColor White
            Write-Host ""
        }
        if ($issue -match "name.*field" -or $issue -match "latitude.*field" -or $issue -match "longitude.*field") {
            Write-Host "❯ Missing fields issue:" -ForegroundColor Yellow
            Write-Host "  1. Check FIREBASE_VALIDATION_GUIDE.md for required fields" -ForegroundColor White
            Write-Host "  2. Update database to include all required fields" -ForegroundColor White
            Write-Host "  3. Ensure field names are correct (e.g., 'name' not 'nodeName')" -ForegroundColor White
            Write-Host ""
        }
        if ($issue -match "firewall" -or $issue -match "network") {
            Write-Host "❯ Network/Firewall issue:" -ForegroundColor Yellow
            Write-Host "  1. Check your internet connection" -ForegroundColor White
            Write-Host "  2. Temporarily disable firewall/antivirus" -ForegroundColor White
            Write-Host "  3. Try from a different network" -ForegroundColor White
            Write-Host ""
        }
    }
}

Write-Host ""
Write-Host "For detailed troubleshooting, see: FIREBASE_SELF_DIAGNOSTIC.md" -ForegroundColor Cyan
Write-Host ""
