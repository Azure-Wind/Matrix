# Builds DetMatrix.apk using the Android SDK build-tools directly
# (aapt2 link -> javac -> d8 -> zipalign -> apksigner).
# Native tools (keytool/d8/aapt2) legitimately write progress to stderr, which
# PowerShell 5.1 would otherwise turn into a terminating error. So keep this at
# Continue and rely on explicit $LASTEXITCODE checks after every native call.
$ErrorActionPreference = "Continue"

$JDK  = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$SDK  = "D:\android-sdk"
$BT   = "$SDK\build-tools\34.0.0"
$AJAR = "$SDK\platforms\android-34\android.jar"
# Project dir = the folder this script lives in, so the project can be moved
# or checked out anywhere. NOTE: keep these scripts ASCII-only -- PowerShell 5.1
# decodes BOM-less UTF-8 as ANSI and non-ASCII bytes can corrupt parsing.
$PROJ = $PSScriptRoot
$BUILD = "$PROJ\build"

$env:JAVA_HOME = $JDK
$javac    = "$JDK\bin\javac.exe"
$jar      = "$JDK\bin\jar.exe"
$keytool  = "$JDK\bin\keytool.exe"

$PKG = "com\example\detmatrix"

Write-Host "== 0. clean ==" -ForegroundColor Cyan
if (Test-Path $BUILD) { Remove-Item $BUILD -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$BUILD\gen", "$BUILD\classes", "$BUILD\dex" | Out-Null

Write-Host "== 1. aapt2 compile (resources) ==" -ForegroundColor Cyan
& "$BT\aapt2.exe" compile --dir "$PROJ\res" -o "$BUILD\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host "== 2. aapt2 link (base apk + R.java) ==" -ForegroundColor Cyan
& "$BT\aapt2.exe" link `
    -o "$BUILD\base.apk" `
    -I $AJAR `
    --manifest "$PROJ\AndroidManifest.xml" `
    -R "$BUILD\res.zip" `
    --java "$BUILD\gen" `
    --auto-add-overlay `
    --min-sdk-version 21 `
    --target-sdk-version 34 `
    --version-code 1 `
    --version-name "1.0"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "== 3. javac ==" -ForegroundColor Cyan
$sources = @("$PROJ\java\com\example\detmatrix\MainActivity.java")
$sources += (Get-ChildItem "$BUILD\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 -source 8 -target 8 -nowarn `
    -bootclasspath $AJAR -d "$BUILD\classes" $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "== 4. d8 (dex) ==" -ForegroundColor Cyan
$classes = (Get-ChildItem "$BUILD\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& "$BT\d8.bat" --release --min-api 21 --lib $AJAR --output "$BUILD\dex" @classes
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "== 5. add classes.dex into apk ==" -ForegroundColor Cyan
& $jar uf "$BUILD\base.apk" -C "$BUILD\dex" classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar update failed" }

Write-Host "== 6. zipalign ==" -ForegroundColor Cyan
& "$BT\zipalign.exe" -f -p 4 "$BUILD\base.apk" "$BUILD\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "== 7. keystore ==" -ForegroundColor Cyan
$ks = "$PROJ\debug.keystore"
if (-not (Test-Path $ks)) {
    & $keytool -genkeypair -keystore $ks -storepass android -keypass android `
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

Write-Host "== 8. apksigner ==" -ForegroundColor Cyan
$out = "$PROJ\DetMatrix.apk"
if (Test-Path $out) { Remove-Item $out -Force }
& "$BT\apksigner.bat" sign `
    --ks $ks --ks-pass pass:android --key-pass pass:android `
    --ks-key-alias androiddebugkey `
    --v1-signing-enabled true --v2-signing-enabled true `
    --out $out "$BUILD\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "== 9. verify ==" -ForegroundColor Cyan
& "$BT\apksigner.bat" verify --verbose --print-certs $out
Write-Host ""
Write-Host "APK: $out  ($([math]::Round((Get-Item $out).Length/1KB,1)) KB)" -ForegroundColor Green
