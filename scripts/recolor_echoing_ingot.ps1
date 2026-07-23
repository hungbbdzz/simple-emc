# PowerShell script to recolor echoing_resonant_ingot.png / aetheric_resonant_ingot.png
# to match the Vibrant Cyan/Teal color of the raw chunk & block variant (brighter & metallic).
Add-Type -AssemblyName System.Drawing

$workspaceRoot = "$PSScriptRoot\.."

# Output directories
$outDirs = @("C:\Downloads", "$env:USERPROFILE\Downloads")
foreach ($dir in $outDirs) {
    if (-not (Test-Path $dir)) {
        try { New-Item -ItemType Directory -Force -Path $dir | Out-Null } catch {}
    }
}

function Recolor-EchoingIngot {
    param (
        [string]$ItemName
    )

    $targetFile = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\item\$ItemName"
    
    # Download vanilla iron ingot if target file not found
    if (-not (Test-Path $targetFile)) {
        $vanillaIngotUrl = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/iron_ingot.png"
        $targetFile = Join-Path $env:TEMP "vanilla_iron_ingot.png"
        if (-not (Test-Path $targetFile)) {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Invoke-WebRequest -Uri $vanillaIngotUrl -OutFile $targetFile -UserAgent "Mozilla/5.0"
        }
    }

    Write-Host "Recoloring $ItemName with brighter Echo/Cyan palette..." -ForegroundColor Cyan
    $srcBmp = [System.Drawing.Bitmap]::FromFile($targetFile)
    $width = $srcBmp.Width
    $height = $srcBmp.Height

    $newBmp = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    # Brighter Vibrant Echo Cyan Palette (Matches Raw Ore Chunk & Block Variant)
    # Dark Outline     : #022830
    # Bevel Shadow     : #00505a
    # Deep Teal Body   : #007d8a
    # Vibrant Cyan     : #00abb8
    # Bright Cyan Glow : #1ee2ee
    # Specular Highlight: #7efff7

    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            $c = $srcBmp.GetPixel($x, $y)
            if ($c.A -eq 0) {
                $newBmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                continue
            }

            $brightness = $c.GetBrightness()
            $hex = "#022830"
            if ($brightness -gt 0.85) { $hex = "#7efff7" }
            elseif ($brightness -gt 0.68) { $hex = "#1ee2ee" }
            elseif ($brightness -gt 0.50) { $hex = "#00abb8" }
            elseif ($brightness -gt 0.32) { $hex = "#007d8a" }
            elseif ($brightness -gt 0.15) { $hex = "#00505a" }
            else { $hex = "#022830" }

            $mapped = [System.Drawing.ColorTranslator]::FromHtml($hex)
            $finalColor = [System.Drawing.Color]::FromArgb($c.A, $mapped.R, $mapped.G, $mapped.B)
            $newBmp.SetPixel($x, $y, $finalColor)
        }
    }

    $srcBmp.Dispose()

    # Save to output locations
    foreach ($dir in $outDirs) {
        if (Test-Path $dir) {
            $outPath = Join-Path $dir $ItemName
            $newBmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Host "[SUCCESS] Saved recolored ingot to: $outPath" -ForegroundColor Green

            # 8x Preview
            $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $g = [System.Drawing.Graphics]::FromImage($previewBmp)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
            $g.DrawImage($newBmp, 0, 0, 128, 128)
            $g.Dispose()

            $baseName = [System.IO.Path]::GetFileNameWithoutExtension($ItemName)
            $previewPath = Join-Path $dir "${baseName}_8x_preview.png"
            $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
            $previewBmp.Dispose()
            Write-Host "[SUCCESS] Saved 8x preview to: $previewPath" -ForegroundColor Green
        }
    }

    # Update project texture directly
    $projectItemDir = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\item"
    if (Test-Path $projectItemDir) {
        $projectTexture = Join-Path $projectItemDir $ItemName
        $newBmp.Save($projectTexture, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "[SUCCESS] Updated project texture at: $projectTexture" -ForegroundColor Yellow
    }

    $newBmp.Dispose()
}

Recolor-EchoingIngot -ItemName "echoing_resonant_ingot.png"
Recolor-EchoingIngot -ItemName "aetheric_resonant_ingot.png"

Write-Host "Recoloring complete!" -ForegroundColor Green
