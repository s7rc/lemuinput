$basePath = "c:\Users\user\Documents\Lemuroid-master\lemuroid-touchinput\src\main\java\com\swordfish\touchinput\radial\layouts\"

# Files with simple Select/Start buttons
$simpleFiles = @('Genesis3.kt', 'PCE.kt', 'Atari7800.kt', 'NGP.kt', 'GG.kt', 'SMS.kt')
foreach ($file in $simpleFiles) {
    $path = Join-Path $basePath $file
    if (Test-Path $path) {
        $content = Get-Content $path -Raw
        $content = $content -replace 'SecondaryButtonSelect\(\)', 'SecondaryButtonSelect(settings = settings)'
        $content = $content -replace 'SecondaryButtonStart\(\)', 'SecondaryButtonStart(settings = settings)'
        Set-Content -Path $path -Value $content -NoNewline
        Write-Host "Updated $file"
    }
}

# Files with L1/L2/R1/R2 buttons
$l1r1Files = @('PSX.kt', 'PSXDualShock.kt', 'DOS.kt')
foreach ($file in $l1r1Files) {
    $path = Join-Path $basePath $file
    if (Test-Path $path) {
        $content = Get-Content $path -Raw
        $content = $content -replace 'SecondaryButtonL1\(\)', 'SecondaryButtonL1(settings)'
        $content = $content -replace 'SecondaryButtonL2\(\)', 'SecondaryButtonL2(settings)'
        $content = $content -replace 'SecondaryButtonR1\(\)', 'SecondaryButtonR1(settings)'
        $content = $content -replace 'SecondaryButtonR2\(\)', 'SecondaryButtonR2(settings)'
        Set-Content -Path $path -Value $content -NoNewline
        Write-Host "Updated $file"
    }
}

# Files with L/R buttons
$lrFiles = @('PSP.kt', 'N64.kt', 'Nintendo3DS.kt', 'Desmume.kt', 'MelonDS.kt')
foreach ($file in $lrFiles) {
    $path = Join-Path $basePath $file
    if (Test-Path $path) {
        $content = Get-Content $path -Raw
        $content = $content -replace 'SecondaryButtonL\(\)', 'SecondaryButtonL(settings)'
        $content = $content -replace 'SecondaryButtonR\(\)', 'SecondaryButtonR(settings)'
        Set-Content -Path $path -Value $content -NoNewline
        Write-Host "Updated $file"
    }
}

# Files with Coin/Start buttons
$coinFiles = @('Arcade4.kt', 'Arcade6.kt', 'Lynx.kt', 'WS.kt')
foreach ($file in $coinFiles) {
    $path = Join-Path $basePath $file
    if (Test-Path $path) {
        $content = Get-Content $path -Raw
        $content = $content -replace 'SecondaryButtonCoin\(\)', 'SecondaryButtonCoin(settings)'
        $content = $content -replace 'SecondaryButtonStart\(\)', 'SecondaryButtonStart(settings = settings)'
        Set-Content -Path $path -Value $content -NoNewline
        Write-Host "Updated $file"
    }
}

# Genesis6 has special buttons
$path = Join-Path $basePath 'Genesis6.kt'
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    $content = $content -replace 'SecondaryButtonSelect\(\)', 'SecondaryButtonSelect(settings = settings)'
    $content = $content -replace 'SecondaryButtonStart\(\)', 'SecondaryButtonStart(settings = settings)'
    Set-Content -Path $path -Value $content -NoNewline
    Write-Host "Updated Genesis6.kt"
}

# Atari2600 has its button calls inline
$path = Join-Path $basePath 'Atari2600.kt'
if (Test-Path $path) {
    $content = Get-Content $path -Raw
    $content = $content -replace 'SecondaryButtonSelect\(\)', 'SecondaryButtonSelect(settings = settings)'
    $content = $content -replace 'SecondaryButtonStart\(\)', 'SecondaryButtonStart(settings = settings)'
    Set-Content -Path $path -Value $content -NoNewline
    Write-Host "Updated Atari2600.kt"
}

Write-Host "All layout files updated successfully!"
