# PowerShell script to download official Vanilla Smithing Template texture from Mojang and recolor it
Add-Type -AssemblyName System.Drawing

$targetDir = "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\item"
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

$templateUrl = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/netherite_upgrade_smithing_template.png"
$basePath = "$PSScriptRoot\vanilla_smithing_template.png"

if (-not (Test-Path $basePath)) {
    Write-Host "Downloading official Mojang Smithing Template from GitHub..." -ForegroundColor Cyan
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $templateUrl -OutFile $basePath -UserAgent "Mozilla/5.0"
        Write-Host "Downloaded vanilla_smithing_template.png successfully!" -ForegroundColor Green
    } catch {
        Write-Error "Failed to download template texture: $_"
        exit 1
    }
}

function Recolor-Template ($outputName, $primaryColor, $accentColor) {
    $srcBmp = [System.Drawing.Bitmap]::FromFile($basePath)
    $w = $srcBmp.Width
    $h = $srcBmp.Height
    $outBmp = New-Object System.Drawing.Bitmap($w, $h)

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $px = $srcBmp.GetPixel($x, $y)
            if ($px.A -eq 0) { continue }

            $lum = (0.299 * $px.R + 0.587 * $px.G + 0.114 * $px.B) / 255.0
            $isEngraving = ($px.R -gt ($px.G + 15)) -and ($px.R -gt ($px.B + 15))

            if ($isEngraving) {
                $r = [Math]::Min(255, [Math]::Max(0, [int]($accentColor.R * $lum * 1.4)))
                $g = [Math]::Min(255, [Math]::Max(0, [int]($accentColor.G * $lum * 1.4)))
                $b = [Math]::Min(255, [Math]::Max(0, [int]($accentColor.B * $lum * 1.4)))
            } else {
                $r = [Math]::Min(255, [Math]::Max(0, [int]($primaryColor.R * $lum * 1.2)))
                $g = [Math]::Min(255, [Math]::Max(0, [int]($primaryColor.G * $lum * 1.2)))
                $b = [Math]::Min(255, [Math]::Max(0, [int]($primaryColor.B * $lum * 1.2)))
            }

            $outBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($px.A, $r, $g, $b))
        }
    }

    $outPath = Join-Path $targetDir "$outputName.png"
    $outBmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $outBmp.Dispose()
    $srcBmp.Dispose()

    Write-Host "Generated official-based template: $outPath" -ForegroundColor Green
}

# 1. Infernal Ascension Template (Nether Blackstone & Lava Orange Emblem)
Recolor-Template -outputName "infernal_ascension_template" `
                 -primaryColor ([System.Drawing.Color]::FromArgb(255, 65, 55, 68)) `
                 -accentColor ([System.Drawing.Color]::FromArgb(255, 255, 120, 20))

# 2. End Ascension Template (End Stone & Ender Violet Emblem)
Recolor-Template -outputName "end_ascension_template" `
                 -primaryColor ([System.Drawing.Color]::FromArgb(255, 215, 210, 160)) `
                 -accentColor ([System.Drawing.Color]::FromArgb(255, 168, 85, 247))

# 3. Deep Convergence Template (Deepslate & Sculk Soul Aqua Emblem)
Recolor-Template -outputName "deep_convergence_template" `
                 -primaryColor ([System.Drawing.Color]::FromArgb(255, 50, 60, 72)) `
                 -accentColor ([System.Drawing.Color]::FromArgb(255, 20, 184, 166))

Write-Host "All 3 Smithing Templates recolored from official Mojang Vanilla texture!" -ForegroundColor Yellow
