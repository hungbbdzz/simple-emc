# PowerShell script to download official Vanilla Glowstone Dust texture from Mojang and recolor alchemical_dust.png
Add-Type -AssemblyName System.Drawing

$targetDir = "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\item"
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

$dustUrl = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/glowstone_dust.png"
$basePath = "$PSScriptRoot\vanilla_glowstone_dust.png"

if (-not (Test-Path $basePath)) {
    Write-Host "Downloading official Mojang Glowstone Dust texture from GitHub..." -ForegroundColor Cyan
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $dustUrl -OutFile $basePath -UserAgent "Mozilla/5.0"
        Write-Host "Downloaded vanilla_glowstone_dust.png successfully!" -ForegroundColor Green
    } catch {
        Write-Error "Failed to download glowstone dust texture: $_"
        exit 1
    }
}

$srcBmp = [System.Drawing.Bitmap]::FromFile($basePath)
$w = $srcBmp.Width
$h = $srcBmp.Height
$outBmp = New-Object System.Drawing.Bitmap($w, $h)

# Colors matching ingredients: Nether Quartz (White/Cream) + Philosopher's Stone (Ruby Red)
$shadowR = 130; $shadowG = 15; $shadowB = 30
$rubyR = 220;   $rubyG = 38;   $rubyB = 38
$quartzR = 250; $quartzG = 242; $quartzB = 235

for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $px = $srcBmp.GetPixel($x, $y)
        if ($px.A -eq 0) { continue }

        $lum = (0.299 * $px.R + 0.587 * $px.G + 0.114 * $px.B) / 255.0

        if ($lum -lt 0.45) {
            $t = $lum / 0.45
            $r = [int]($shadowR * (1 - $t) + $rubyR * $t)
            $g = [int]($shadowG * (1 - $t) + $rubyG * $t)
            $b = [int]($shadowB * (1 - $t) + $rubyB * $t)
        } else {
            $t = ($lum - 0.45) / 0.55
            $r = [int]($rubyR * (1 - $t) + $quartzR * $t)
            $g = [int]($rubyG * (1 - $t) + $quartzR * $t)
            $b = [int]($rubyB * (1 - $t) + $quartzB * $t)
        }

        $r = [Math]::Min(255, [Math]::Max(0, $r))
        $g = [Math]::Min(255, [Math]::Max(0, $g))
        $b = [Math]::Min(255, [Math]::Max(0, $b))

        $outBmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($px.A, $r, $g, $b))
    }
}

$outPath = Join-Path $targetDir "alchemical_dust.png"
$outBmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$outBmp.Dispose()
$srcBmp.Dispose()

Write-Host "Generated official Glowstone Dust based alchemical_dust.png: $outPath" -ForegroundColor Green
