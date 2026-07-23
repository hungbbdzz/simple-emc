# PowerShell script to generate 16x16 PNG pixel-art textures for Smithing Templates & Upgraded Items
Add-Type -AssemblyName System.Drawing

$targetDir = "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\item"
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

function Create-PixelArt ($name, $matrix, $colorMap) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)

    for ($y = 0; $y -lt 16; $y++) {
        $row = $matrix[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $char = $row[$x].ToString()
            if ($colorMap.ContainsKey($char) -and $null -ne $colorMap[$char]) {
                $bmp.SetPixel($x, $y, $colorMap[$char])
            } else {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            }
        }
    }

    $outPath = Join-Path $targetDir "$name.png"
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Generated 16x16 texture: $outPath" -ForegroundColor Green
}

# ==============================================================================
# 1. INFERNAL ASCENSION TEMPLATE
# ==============================================================================
$infernalTemplateMatrix = @(
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSFFFFFSSBH..",
    "..HBSFFFFFSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
)
$infernalTemplateColors = @{
    'H' = [System.Drawing.Color]::FromArgb(255, 28, 24, 30)
    'B' = [System.Drawing.Color]::FromArgb(255, 52, 47, 56)
    'S' = [System.Drawing.Color]::FromArgb(255, 85, 78, 92)
    'F' = [System.Drawing.Color]::FromArgb(255, 249, 115, 22)
}

# ==============================================================================
# 2. END ASCENSION TEMPLATE
# ==============================================================================
$endTemplateMatrix = @(
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSVVEVVVVSBH.",
    "..HBSVEVEVVSBH..",
    "..HBSVEEEEVSBH..",
    "..HBSVEVEVVSBH..",
    "..HBSVVEVVVVSBH.",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
)
$endTemplateColors = @{
    'H' = [System.Drawing.Color]::FromArgb(255, 150, 145, 100)
    'B' = [System.Drawing.Color]::FromArgb(255, 215, 210, 160)
    'S' = [System.Drawing.Color]::FromArgb(255, 245, 240, 195)
    'V' = [System.Drawing.Color]::FromArgb(255, 126, 34, 206)
    'E' = [System.Drawing.Color]::FromArgb(255, 34, 211, 238)
}

# ==============================================================================
# 3. DEEP CONVERGENCE TEMPLATE
# ==============================================================================
$deepTemplateMatrix = @(
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSAAASAAASBH.",
    "..HBSAAAAAAASBH.",
    "..HBSAAASAAASBH.",
    "..HBSAAAAAAASBH.",
    "..HBSAAASAAASBH.",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
)
$deepTemplateColors = @{
    'H' = [System.Drawing.Color]::FromArgb(255, 18, 22, 28)
    'B' = [System.Drawing.Color]::FromArgb(255, 45, 53, 63)
    'S' = [System.Drawing.Color]::FromArgb(255, 80, 92, 108)
    'A' = [System.Drawing.Color]::FromArgb(255, 45, 212, 191)
}

Create-PixelArt "infernal_ascension_template" $infernalTemplateMatrix $infernalTemplateColors
Create-PixelArt "end_ascension_template" $endTemplateMatrix $endTemplateColors
Create-PixelArt "deep_convergence_template" $deepTemplateMatrix $deepTemplateColors

Write-Host "All 3 Smithing Template textures generated successfully in $targetDir!" -ForegroundColor Yellow
