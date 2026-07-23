# PowerShell script to restore git textures and export 100% original Echo files to C:\Downloads
Add-Type -AssemblyName System.Drawing

$workspaceRoot = "$PSScriptRoot\.."

# Step 1: Restore original textures from git tracking to guarantee clean untampered files
Write-Host "Restoring original textures from git repository..." -ForegroundColor Cyan
try {
    Push-Location $workspaceRoot
    git checkout -- "shared-resources/assets/velorise_emc/textures/" 2>$null
    Pop-Location
} catch {}

# Output directories
$outDirs = @("C:\Downloads", "$env:USERPROFILE\Downloads")
foreach ($dir in $outDirs) {
    if (-not (Test-Path $dir)) {
        try { New-Item -ItemType Directory -Force -Path $dir | Out-Null } catch {}
    }
}

$originalFiles = @(
    @{ Name = "raw_echoing_resonant.png"; Type = "item" },
    @{ Name = "echoing_resonant_ingot.png"; Type = "item" },
    @{ Name = "echoing_resonant_ore.png"; Type = "block" },
    @{ Name = "echoing_resonant_block.png"; Type = "block" }
)

Write-Host "Exporting 100% ORIGINAL Echo texture files..." -ForegroundColor Green

foreach ($item in $originalFiles) {
    $name = $item.Name
    $sub = $item.Type
    $srcPath = Join-Path $workspaceRoot "shared-resources\assets\velorise_emc\textures\$sub\$name"

    if (-not (Test-Path $srcPath)) {
        Write-Host "[SKIP] Original file not found: $srcPath" -ForegroundColor Yellow
        continue
    }

    foreach ($dir in $outDirs) {
        if (Test-Path $dir) {
            # Copy original PNG directly
            $destPath = Join-Path $dir $name
            Copy-Item -Path $srcPath -Destination $destPath -Force
            Write-Host "[EXPORTED] Original File -> $destPath" -ForegroundColor Green

            # Export 8x enlarged preview
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
                Write-Host "[EXPORTED] 8x Preview   -> $previewPath" -ForegroundColor Cyan
            } catch {}
        }
    }
}

Write-Host "Export of original files completed!" -ForegroundColor Green
