# Full verification suite for Matrix.apk
$ErrorActionPreference = "Continue"

$JDK  = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$SDK  = "D:\android-sdk"
$BT   = "$SDK\build-tools\34.0.0"
$AJAR = "$SDK\platforms\android-34\android.jar"
$PROJ = $PSScriptRoot
$APK  = "$PROJ\Matrix.apk"
$fails = 0

# apksigner.bat needs java on PATH or JAVA_HOME set
$env:JAVA_HOME = $JDK

Write-Host "===== 1. APK signature =====" -ForegroundColor Cyan
$sigOut = & "$BT\apksigner.bat" verify --verbose $APK 2>&1
$sigCode = $LASTEXITCODE
$sigOut | Select-String -Pattern "Verifies|v1 scheme|v2 scheme|v3 scheme"
if ($sigCode -ne 0 -or -not ($sigOut -match "Verifies")) {
    Write-Host "  SIGNATURE FAILED (exit=$sigCode)" -ForegroundColor Red; $fails++
}

Write-Host "`n===== 2. Manifest / badging =====" -ForegroundColor Cyan
$badging = & "$BT\aapt2.exe" dump badging $APK 2>&1
$badging | Select-String -Pattern "^package:|^sdkVersion|^targetSdkVersion|^launchable-activity|^application-label:"
$perm = $badging | Select-String -Pattern "uses-permission"
if ($perm) { Write-Host "  permissions: $perm" -ForegroundColor Red; $fails++ }
else { Write-Host "  permissions: NONE (good)" }

$mf = (& "$BT\aapt2.exe" dump xmltree --file AndroidManifest.xml $APK 2>&1) -join "`n"
if ($mf -notmatch 'package="com\.example\.detmatrix"') {
    Write-Host "  PROBLEM: unexpected package name" -ForegroundColor Red; $fails++
}
# aapt2 keeps the source-relative form (".DetActivity"); the package attr resolves it.
foreach ($act in @(".MainActivity", ".DetActivity", ".ShapeStepActivity", ".MatMulActivity",
                   ".StatsActivity", ".WrongShapesActivity", ".WrongListActivity")) {
    $pat = 'android:name\(0x01010003\)="' + [regex]::Escape($act) + '"'
    $c = ([regex]::Matches($mf, $pat)).Count
    if ($c -eq 1) { Write-Host "  declared: $act" }
    else { Write-Host "  PROBLEM: $act declared $c times" -ForegroundColor Red; $fails++ }
}

Write-Host "`n===== 3. View ids per layout =====" -ForegroundColor Cyan
$rjava = Get-Content "$PROJ\build\gen\com\example\detmatrix\R.java" -Raw
$idBlock = [regex]::Match($rjava, 'class id \{(.*?)\}', 'Singleline').Groups[1].Value
$idMap = @{}
foreach ($m in [regex]::Matches($idBlock, 'int\s+(\w+)\s*=\s*(0x[0-9a-fA-F]+)')) {
    $idMap[$m.Groups[1].Value] = $m.Groups[2].Value
}

$layouts = @{
    "res/layout/activity_main.xml"       = @("mode_det", "mode_matmul")
    "res/layout/activity_det.xml"        = @("back", "screen_title", "timer",
                                             "cell00", "cell01", "cell02",
                                             "cell10", "cell11", "cell12",
                                             "cell20", "cell21", "cell22",
                                             "answer", "practice_note",
                                             "next", "show_answer", "mark_wrong",
                                             "wrong_book", "stats")
    "res/layout/activity_matmul.xml"     = @("back", "screen_title", "shape_label", "timer",
                                             "row_ab", "matrix_c", "answer_hint",
                                             "practice_note", "next", "show_answer",
                                             "mark_wrong", "wrong_book", "stats")
    "res/layout/activity_shape_step.xml" = @("back", "step_indicator", "step_title",
                                             "step_hint", "chip_row", "chosen", "confirm")
    "res/layout/activity_stats.xml"      = @("back", "mode_name", "empty_note", "stats_body",
                                             "avg_title", "avg_value", "total_value",
                                             "recent_title", "recent_list")
    "res/layout/activity_wrong_shapes.xml" = @("back", "shape_grid")
    "res/layout/activity_wrong_list.xml"   = @("back", "list_title", "list_subtitle",
                                               "empty_note", "number_grid", "tap_hint")
}

$allSeen = New-Object System.Collections.Generic.HashSet[string]
foreach ($layout in $layouts.Keys) {
    $treeText = ((& "$BT\aapt2.exe" dump xmltree --file $layout $APK 2>&1) -join "`n")
    $hexes = [regex]::Matches($treeText, 'android:id\(0x010100d0\)=@(0x[0-9a-fA-F]+)') |
             ForEach-Object { $_.Groups[1].Value }
    $problem = 0
    foreach ($name in $layouts[$layout]) {
        if (-not $idMap.ContainsKey($name)) { Write-Host "  $name : not in R.java" -ForegroundColor Red; $problem++; continue }
        $h = $idMap[$name]
        $n = ($hexes | Where-Object { $_ -eq $h }).Count
        if ($n -ne 1) { Write-Host "  $name appears $n times in $layout" -ForegroundColor Red; $problem++ }
        [void]$allSeen.Add($name)
    }
    if ($hexes.Count -ne $layouts[$layout].Count) {
        Write-Host "  $layout has $($hexes.Count) id attrs, expected $($layouts[$layout].Count)" -ForegroundColor Red
        $problem++
    }
    if ($problem -eq 0) {
        Write-Host ("  {0,-34} {1,2} ids OK" -f $layout, $layouts[$layout].Count)
    } else { $fails++ }
}
$orphan = $idMap.Keys | Where-Object { -not $allSeen.Contains($_) }
if ($orphan) { Write-Host "  orphan ids declared but unused: $orphan" -ForegroundColor Red; $fails++ }
else { Write-Host "  every declared id is used by exactly one layout: OK" }

Write-Host "`n===== 4. Determinant cell sizing (explicit in layout) =====" -ForegroundColor Cyan
$detTree = ((& "$BT\aapt2.exe" dump xmltree --file res/layout/activity_det.xml $APK 2>&1) -join "`n")
$w78 = ([regex]::Matches($detTree, 'layout_width\(0x010100f4\)=78\.000000dp')).Count
$h78 = ([regex]::Matches($detTree, 'layout_height\(0x010100f5\)=78\.000000dp')).Count
Write-Host "  cells with explicit 78dp width : $w78 (expect 9)"
Write-Host "  cells with explicit 78dp height: $h78 (expect 9)"
if ($w78 -ne 9 -or $h78 -ne 9) { $fails++ }

Write-Host "`n===== 5. Adaptive launcher icon =====" -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($APK)
$names = $zip.Entries | ForEach-Object { $_.FullName }
$zip.Dispose()
foreach ($need in @("res/mipmap-anydpi-v26/ic_launcher.xml",
                    "res/mipmap-xxxhdpi-v4/ic_launcher.png",
                    "res/mipmap-xxxhdpi-v4/ic_launcher_foreground.png",
                    "res/drawable/ic_launcher_background.xml")) {
    if ($names -contains $need) { Write-Host "  present: $need" }
    else { Write-Host "  MISSING: $need" -ForegroundColor Red; $fails++ }
}
foreach ($legacy in @("res/drawable/ic_launcher.xml")) {
    if ($names -contains $legacy) { Write-Host "  old grid icon still present: $legacy" -ForegroundColor Red; $fails++ }
}

Write-Host "`n===== 6. Maths (real APK bytecode) =====" -ForegroundColor Cyan
$testOut = "$PROJ\build\testout"
Remove-Item $testOut -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $testOut | Out-Null
$cp = "$AJAR;$PROJ\build\classes"
& "$JDK\bin\javac.exe" -encoding UTF-8 -nowarn -cp $cp -d $testOut "$PROJ\test\MathTest.java" 2>&1 | Out-Null
& "$JDK\bin\java.exe" -cp "$AJAR;$PROJ\build\classes;$testOut" MathTest
if ($LASTEXITCODE -ne 0) { $fails++ }

Write-Host ""
if ($fails -eq 0) { Write-Host "===== ALL CHECKS PASSED =====" -ForegroundColor Green }
else { Write-Host "===== $fails CHECK GROUP(S) FAILED =====" -ForegroundColor Red; exit 1 }
