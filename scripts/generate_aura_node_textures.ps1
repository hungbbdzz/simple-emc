# PowerShell script to generate depleted/unlit (_off.png) versions of existing Aura Node PNG textures
Add-Type -AssemblyName System.Drawing

$targetDir = "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\block"
if (-not (Test-Path $targetDir)) {
    Write-Error "Target directory not found: $targetDir"
    exit 1
}

# Find all aura_node_tier_*.png files, skipping any already generated _off / _depleted files
$sourceFiles = Get-ChildItem -Path $targetDir -Filter "aura_node_tier_*.png" | Where-Object {
    $_.Name -notlike "*_off.png" -and $_.Name -notlike "*_depleted.png"
}

if ($sourceFiles.Count -eq 0) {
    Write-Host "No aura_node_tier_*.png files found in $targetDir" -ForegroundColor Red
    exit 1
}

Write-Host "Found $($sourceFiles.Count) aura node texture files to process for depleted/off state..." -ForegroundColor Cyan

foreach ($file in $sourceFiles) {
    $srcBitmap = [System.Drawing.Bitmap]::FromFile($file.FullName)
    $width = $srcBitmap.Width
    $height = $srcBitmap.Height
    $outBitmap = New-Object System.Drawing.Bitmap($width, $height)

    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            $pixel = $srcBitmap.GetPixel($x, $y)

            if ($pixel.A -eq 0) {
                $outBitmap.SetPixel($x, $y, $pixel)
                continue
            }

            # Calculate RGB luminance and saturation
            $maxC = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B))
            $minC = [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B))
            $chroma = $maxC - $minC
            $lum = (0.299 * $pixel.R + 0.587 * $pixel.G + 0.114 * $pixel.B)

            # Determine if pixel is a glowing gem/rune (high saturation or bright accent)
            $isGlowingPixel = ($chroma -gt 35) -or ($maxC -gt 180 -and $chroma -gt 15)

            if ($isGlowingPixel) {
                # Desaturate and heavily dim glowing gems/runes to unlit dormant stone
                $dimmedLum = $lum * 0.40
                $r = [Math]::Min(255, [Math]::Max(0, [int]($dimmedLum * 0.85)))
                $g = [Math]::Min(255, [Math]::Max(0, [int]($dimmedLum * 0.90)))
                $b = [Math]::Min(255, [Math]::Max(0, [int]($dimmedLum * 0.95)))
                $outBitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $r, $g, $b))
            } else {
                # Darken normal stone base slightly for overall dimmed state (70% brightness)
                $r = [Math]::Min(255, [Math]::Max(0, [int]($pixel.R * 0.70)))
                $g = [Math]::Min(255, [Math]::Max(0, [int]($pixel.G * 0.70)))
                $b = [Math]::Min(255, [Math]::Max(0, [int]($pixel.B * 0.70)))
                $outBitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $r, $g, $b))
            }
        }
    }

    # Generate output file name (e.g. aura_node_tier_1_off.png)
    $outName = [System.IO.Path]::GetFileNameWithoutExtension($file.Name) + "_off.png"
    $outPath = Join-Path $targetDir $outName

    $outBitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $outBitmap.Dispose()
    $srcBitmap.Dispose()

    Write-Host "Generated depleted texture: $outName" -ForegroundColor Green
}

Write-Host "All depleted (_off.png) textures generated successfully in $targetDir!" -ForegroundColor Yellow
