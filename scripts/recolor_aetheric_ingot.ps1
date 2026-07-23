# PowerShell script to recolor aetheric_resonant_ingot.png to match the Cyan/Teal ore & block variant.
Add-Type -AssemblyName System.Drawing

$workspaceRoot = "$PSScriptRoot\.."
$ingotPath = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\item\aetheric_resonant_ingot.png"

# Target output directories
$outDirs = @("C:\Downloads", "$env:USERPROFILE\Downloads")
foreach ($dir in $outDirs) {
    if (-not (Test-Path $dir)) {
        try { New-Item -ItemType Directory -Force -Path $dir | Out-Null } catch {}
    }
}

# Download vanilla iron ingot base if target file not found
if (-not (Test-Path $ingotPath)) {
    $vanillaIngotUrl = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/iron_ingot.png"
    $ingotPath = Join-Path $env:TEMP "vanilla_iron_ingot.png"
    if (-not (Test-Path $ingotPath)) {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $vanillaIngotUrl -OutFile $ingotPath -UserAgent "Mozilla/5.0"
    }
}

Write-Host "Loading ingot texture from: $ingotPath" -ForegroundColor Cyan
$srcBmp = [System.Drawing.Bitmap]::FromFile($ingotPath)
$width = $srcBmp.Width
$height = $srcBmp.Height

$newBmp = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Cyan/Teal Color Palette matching the Aetheric Ore & Block variant:
# Dark Outline  : #04272c
# Dark Shadow   : #005a60
# Midtone Teal  : #009fa7
# Bright Cyan   : #1de4eb
# Light Glow    : #7ff5fb
# Specular White: #e0ffff

for ($y = 0; $y -lt $height; $y++) {
    for ($x = 0; $x -lt $width; $x++) {
        $c = $srcBmp.GetPixel($x, $y)
        if ($c.A -eq 0) {
            $newBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            continue
        }

        $brightness = $c.GetBrightness()
        $hex = "#04272c"
        if ($brightness -gt 0.88) { $hex = "#e0ffff" }
        elseif ($brightness -gt 0.72) { $hex = "#7ff5fb" }
        elseif ($brightness -gt 0.55) { $hex = "#1de4eb" }
        elseif ($brightness -gt 0.38) { $hex = "#009fa7" }
        elseif ($brightness -gt 0.20) { $hex = "#005a60" }
        else { $hex = "#04272c" }

        $mapped = [System.Drawing.ColorTranslator]::FromHtml($hex)
        $finalColor = [System.Drawing.Color]::FromArgb($c.A, $mapped.R, $mapped.G, $mapped.B)
        $newBmp.SetPixel($x, $y, $finalColor)
    }
}

$srcBmp.Dispose()

# Save output files to C:\Downloads and User Downloads
foreach ($dir in $outDirs) {
    if (Test-Path $dir) {
        $outPath = Join-Path $dir "aetheric_resonant_ingot.png"
        $newBmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "[SUCCESS] Saved recolored ingot to: $outPath" -ForegroundColor Green

        # Save 8x preview for quick inspection
        $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($previewBmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $g.DrawImage($newBmp, 0, 0, 128, 128)
        $g.Dispose()

        $previewPath = Join-Path $dir "aetheric_resonant_ingot_8x_preview.png"
        $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $previewBmp.Dispose()
        Write-Host "[SUCCESS] Saved 8x preview to: $previewPath" -ForegroundColor Green
    }
}

# Update in shared-resources directory
$projectItemDir = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\item"
if (Test-Path $projectItemDir) {
    $projectTexture = Join-Path $projectItemDir "aetheric_resonant_ingot.png"
    $newBmp.Save($projectTexture, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "[SUCCESS] Updated project texture at: $projectTexture" -ForegroundColor Yellow
}

$newBmp.Dispose()
Write-Host "Recoloring complete!" -ForegroundColor Green
