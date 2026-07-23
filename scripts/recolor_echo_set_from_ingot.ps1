# PowerShell script to sample the color palette from echoing_resonant_ingot.png
# and recolor raw_echoing_resonant.png, echoing_resonant_ore.png, and echoing_resonant_block.png
# while preserving 100% of their original texture details, shapes, and transparency.
# Output is saved to C:\Downloads without modifying any workspace files.

Add-Type -AssemblyName System.Drawing

$workspaceRoot = "$PSScriptRoot\.."

# Output directories
$outDirs = @("C:\Downloads", "$env:USERPROFILE\Downloads")
foreach ($dir in $outDirs) {
    if (-not (Test-Path $dir)) {
        try { New-Item -ItemType Directory -Force -Path $dir | Out-Null } catch {}
    }
}

# Path to original Echo Ingot texture (palette source)
$ingotPath = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\item\echoing_resonant_ingot.png"

if (-not (Test-Path $ingotPath)) {
    Write-Error "File echoing_resonant_ingot.png not found at: $ingotPath"
    exit 1
}

Write-Host "Sampling color palette from Echo Ingot: $ingotPath..." -ForegroundColor Cyan

# 1. Extract color palette from Echo Ingot
$ingotBmp = [System.Drawing.Bitmap]::FromFile($ingotPath)
$paletteList = [System.Collections.Generic.List[PSObject]]::new()

for ($y = 0; $y -lt $ingotBmp.Height; $y++) {
    for ($x = 0; $x -lt $ingotBmp.Width; $x++) {
        $c = $ingotBmp.GetPixel($x, $y)
        if ($c.A -gt 10) {
            $b = $c.GetBrightness()
            $paletteList.Add([PSCustomObject]@{
                Brightness = $b
                Color      = $c
            })
        }
    }
}
$ingotBmp.Dispose()

# Sort palette by brightness
$sortedPalette = $paletteList | Sort-Object Brightness

if ($sortedPalette.Count -eq 0) {
    Write-Error "No valid color pixels found in ingot texture."
    exit 1
}

# Helper function to map a brightness value (0.0 to 1.0) to the ingot color palette
function Get-MappedIngotColor {
    param ([double]$Brightness)

    if ($Brightness -le $sortedPalette[0].Brightness) {
        return $sortedPalette[0].Color
    }
    if ($Brightness -ge $sortedPalette[-1].Color.GetBrightness()) {
        return $sortedPalette[-1].Color
    }

    # Find interpolating pair
    for ($i = 0; $i -lt ($sortedPalette.Count - 1); $i++) {
        $p1 = $sortedPalette[$i]
        $p2 = $sortedPalette[$i + 1]
        if ($Brightness -ge $p1.Brightness -and $Brightness -le $p2.Brightness) {
            $span = $p2.Brightness - $p1.Brightness
            if ($span -lt 0.0001) { return $p1.Color }

            $t = ($Brightness - $p1.Brightness) / $span
            $r = [int]($p1.Color.R * (1 - $t) + $p2.Color.R * $t)
            $g = [int]($p1.Color.G * (1 - $t) + $p2.Color.G * $t)
            $b = [int]($p1.Color.B * (1 - $t) + $p2.Color.B * $t)
            return [System.Drawing.Color]::FromArgb($r, $g, $b)
        }
    }

    return $sortedPalette[-1].Color
}

# 2. Target files to recolor using ingot palette
$targets = @(
    @{ Name = "raw_echoing_resonant.png"; Type = "item" },
    @{ Name = "echoing_resonant_ore.png"; Type = "block" },
    @{ Name = "echoing_resonant_block.png"; Type = "block" }
)

Write-Host "Recoloring Echo textures using Ingot palette..." -ForegroundColor Cyan

foreach ($target in $targets) {
    $name = $target.Name
    $sub = $target.Type
    $srcPath = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\$sub\$name"

    if (-not (Test-Path $srcPath)) {
        Write-Host "[SKIP] File not found: $srcPath" -ForegroundColor Yellow
        continue
    }

    $srcBmp = [System.Drawing.Bitmap]::FromFile($srcPath)
    $w = $srcBmp.Width
    $h = $srcBmp.Height
    $outBmp = [System.Drawing.Bitmap]::new($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $px = $srcBmp.GetPixel($x, $y)
            if ($px.A -eq 0) {
                $outBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                continue
            }

            $b = $px.GetBrightness()
            $mappedColor = Get-MappedIngotColor -Brightness $b
            $finalColor = [System.Drawing.Color]::FromArgb($px.A, $mappedColor.R, $mappedColor.G, $mappedColor.B)
            $outBmp.SetPixel($x, $y, $finalColor)
        }
    }

    $srcBmp.Dispose()

    # Save outputs to Downloads directories
    foreach ($dir in $outDirs) {
        if (Test-Path $dir) {
            $outPath = Join-Path $dir $name
            $outBmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Host "[SUCCESS] Exported recolored texture -> $outPath" -ForegroundColor Green

            # 8x Preview
            $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $g = [System.Drawing.Graphics]::FromImage($previewBmp)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
            $g.DrawImage($outBmp, 0, 0, 128, 128)
            $g.Dispose()

            $baseName = [System.IO.Path]::GetFileNameWithoutExtension($name)
            $previewPath = Join-Path $dir "${baseName}_8x_preview.png"
            $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
            $previewBmp.Dispose()
            Write-Host "[SUCCESS] Exported 8x preview      -> $previewPath" -ForegroundColor Cyan
        }
    }

    $outBmp.Dispose()
}

# Also export original echoing_resonant_ingot.png for comparison
foreach ($dir in $outDirs) {
    if (Test-Path $dir) {
        $destIngot = Join-Path $dir "echoing_resonant_ingot.png"
        Copy-Item -Path $ingotPath -Destination $destIngot -Force
        Write-Host "[SUCCESS] Exported reference ingot -> $destIngot" -ForegroundColor Green
    }
}

Write-Host "All Echo textures recolored and exported to Downloads! Workspace project files left untouched." -ForegroundColor Green
