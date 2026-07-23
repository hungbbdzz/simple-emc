Add-Type -AssemblyName System.Drawing

# 1. Restore original texture shape from git repository
$targetPath = Join-Path $PSScriptRoot "..\shared-resources\assets\velorise_emc\textures\item\resonant_echo.png"
$tempDir = "C:\Users\hunga\Downloads\temp_texture"

if (-not (Test-Path $tempDir)) { New-Item -ItemType Directory -Force -Path $tempDir | Out-Null }

# Restore original image from git if tracked
git checkout -- $targetPath 2>$null

if (-not (Test-Path $targetPath)) {
    Write-Error "Không tìm thấy file $targetPath"
    exit 1
}

# 2. Load original bitmap
$srcBmp = [System.Drawing.Bitmap]::FromFile($targetPath)
$width = $srcBmp.Width
$height = $srcBmp.Height

$newBmp = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# 3. Sculk Color Mapping based on pixel brightness (keeping exact original shape & alpha)
for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $c = $srcBmp.GetPixel($x, $y)
        
        if ($c.A -eq 0) {
            $newBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            continue
        }

        # Calculate luminance
        $brightness = $c.GetBrightness()

        # Sculk Palette Mapping:
        # Darkest -> Sculk Deep Black Teal (#06111a, #0c1a24)
        # Mid      -> Sculk Catalyst Teal (#0e4a52, #106570)
        # Bright   -> Sculk Sensor Cyan Glow (#00b8cc, #2bfaee, #79ffff)
        $hex = "#06111a"
        if ($brightness -gt 0.90) { $hex = "#ffffff" }
        elseif ($brightness -gt 0.75) { $hex = "#79ffff" }
        elseif ($brightness -gt 0.60) { $hex = "#2bfaee" }
        elseif ($brightness -gt 0.45) { $hex = "#00b8cc" }
        elseif ($brightness -gt 0.32) { $hex = "#106570" }
        elseif ($brightness -gt 0.18) { $hex = "#0e4a52" }
        else { $hex = "#0c1a24" }

        $mappedColor = [System.Drawing.ColorTranslator]::FromHtml($hex)
        $finalColor = [System.Drawing.Color]::FromArgb($c.A, $mappedColor.R, $mappedColor.G, $mappedColor.B)

        $newBmp.SetPixel($x, $y, $finalColor)
    }
}

$srcBmp.Dispose()

# 4. Save recolored image directly over target files
$newBmp.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)

$tempPath = Join-Path $tempDir "resonant_echo.png"
$newBmp.Save($tempPath, [System.Drawing.Imaging.ImageFormat]::Png)

# 5. Save 8x preview
$previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$pg = [System.Drawing.Graphics]::FromImage($previewBmp)
$pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$pg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$pg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$pg.DrawImage($newBmp, 0, 0, 128, 128)
$pg.Dispose()
$previewPath = Join-Path $tempDir "resonant_echo_preview_8x.png"
$previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$previewBmp.Dispose()

$newBmp.Dispose()

Write-Host "HOÀN THÀNH! Đã đổi màu Sculk cho resonant_echo.png giữ nguyên 100% hình vẽ nguyên bản!" -ForegroundColor Green
Write-Host " -> Dự án: $targetPath" -ForegroundColor Cyan
Write-Host " -> Temp: $tempPath" -ForegroundColor Cyan
