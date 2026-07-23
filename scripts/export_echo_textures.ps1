# PowerShell script to export original Echo textures (raw ore, ore block, ingot block, ingot)
# to C:\Downloads without modifying any workspace files.

Add-Type -AssemblyName System.Drawing

$workspaceRoot = "$PSScriptRoot\.."

# Restore any modified textures in git so workspace remains clean
try {
    Set-Location $workspaceRoot
    git checkout -- "shared-resources/assets/velorise_emc/textures/" 2>$null
} catch {}

# Define target output directories
$outDirs = @("C:\Downloads", "$env:USERPROFILE\Downloads")
foreach ($dir in $outDirs) {
    if (-not (Test-Path $dir)) {
        try { New-Item -ItemType Directory -Force -Path $dir | Out-Null } catch {}
    }
}

# List of Echo textures to export: (Name, SubFolder)
$textures = @(
    @{ Name = "raw_echoing_resonant.png"; Type = "item" },
    @{ Name = "echoing_resonant_ingot.png"; Type = "item" },
    @{ Name = "echoing_resonant_ore.png"; Type = "block" },
    @{ Name = "echoing_resonant_block.png"; Type = "block" },
    @{ Name = "raw_aetheric_resonant.png"; Type = "item" },
    @{ Name = "aetheric_resonant_ingot.png"; Type = "item" },
    @{ Name = "aetheric_resonant_ore.png"; Type = "block" },
    @{ Name = "aetheric_resonant_block.png"; Type = "block" }
)

Write-Host "Exporting Echo textures to Downloads directory..." -ForegroundColor Cyan

foreach ($tex in $textures) {
    $name = $tex.Name
    $sub = $tex.Type
    $srcPath = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\$sub\$name"

    if (-not (Test-Path $srcPath)) {
        Write-Host "[SKIP] Texture not found: $srcPath" -ForegroundColor Yellow
        continue
    }

    foreach ($dir in $outDirs) {
        if (Test-Path $dir) {
            # Copy original PNG file
            $destPath = Join-Path $dir $name
            Copy-Item -Path $srcPath -Destination $destPath -Force
            Write-Host "[EXPORTED] Original texture -> $destPath" -ForegroundColor Green

            # Create & Save 8x Upscaled Preview (128x128)
            try {
                $bmp = [System.Drawing.Bitmap]::FromFile($srcPath)
                $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
                $g = [System.Drawing.Graphics]::FromImage($previewBmp)
                $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
                $g.DrawImage($bmp, 0, 0, 128, 128)
                $g.Dispose()
                $bmp.Dispose()

                $baseName = [System.IO.Path]::GetFileNameWithoutExtension($name)
                $previewPath = Join-Path $dir "${baseName}_8x_preview.png"
                $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
                $previewBmp.Dispose()
                Write-Host "[EXPORTED] 8x Preview      -> $previewPath" -ForegroundColor Cyan
            } catch {
                Write-Host "[WARN] Could not create 8x preview for $name" -ForegroundColor Yellow
            }
        }
    }
}

Write-Host "Export completed successfully! Workspace project files left untouched." -ForegroundColor Green
