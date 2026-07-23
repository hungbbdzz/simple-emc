$srcDir = "C:\Users\hunga\Downloads\temp_texture"
$destDir = Join-Path $PSScriptRoot "..\shared-resources\assets\velorise_emc\textures\item"

if (-not (Test-Path $srcDir)) {
    Write-Error "Khong tim thay thư mục nguồn: $srcDir"
    exit 1
}

if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
}

$pngFiles = Get-ChildItem -Path $srcDir -Filter "*.png" | Where-Object { $_.Name -notlike "*_preview_8x.png" }

Write-Host "Dang sao chep $( $pngFiles.Count ) file texture tu Downloads sang project..." -ForegroundColor Yellow

foreach ($file in $pngFiles) {
    $targetPath = Join-Path $destDir $file.Name
    Copy-Item -Path $file.FullName -Destination $targetPath -Force
    Write-Host " -> [Đã thay thế] $($file.Name) -> $targetPath" -ForegroundColor Green
}

Write-Host "`nHOAN THANH! Da copy toan bo texture gậy mới vào project!" -ForegroundColor Cyan
