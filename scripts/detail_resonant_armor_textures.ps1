Add-Type -AssemblyName System.Drawing

$root = Join-Path $PSScriptRoot '..\shared-resources\assets\velorise_emc\textures'
$styles = @{
    verdant  = @{ Edge = [System.Drawing.Color]::FromArgb(255, 16, 82, 58); Accent = [System.Drawing.Color]::FromArgb(255, 139, 255, 183); EdgeRate = 7; AccentRate = 29 }
    infernal = @{ Edge = [System.Drawing.Color]::FromArgb(255, 38, 19, 27); Accent = [System.Drawing.Color]::FromArgb(255, 255, 174, 20); EdgeRate = 3; AccentRate = 13 }
    aetheric = @{ Edge = [System.Drawing.Color]::FromArgb(255, 34, 16, 65); Accent = [System.Drawing.Color]::FromArgb(255, 235, 83, 255); EdgeRate = 3; AccentRate = 13 }
    echoing  = @{ Edge = [System.Drawing.Color]::FromArgb(255, 4, 24, 31); Accent = [System.Drawing.Color]::FromArgb(255, 24, 213, 206); EdgeRate = 3; AccentRate = 13 }
}

function Test-EdgePixel([System.Drawing.Bitmap]$image, [int]$x, [int]$y) {
    foreach ($offset in @(@(-1,0), @(1,0), @(0,-1), @(0,1))) {
        $nx = $x + $offset[0]; $ny = $y + $offset[1]
        if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $image.Width -or $ny -ge $image.Height) { return $true }
        if ($image.GetPixel($nx, $ny).A -eq 0) { return $true }
    }
    return $false
}

function Add-ArmorDetail([string]$path, [hashtable]$style) {
    $source = [System.Drawing.Bitmap]::FromFile($path)
    try {
        $output = New-Object System.Drawing.Bitmap($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                if ($pixel.A -eq 0) { $output.SetPixel($x, $y, $pixel); continue }
                $hash = (($x * 17) + ($y * 31) + ($x * $y * 3))
                if ((Test-EdgePixel $source $x $y) -and (($hash % $style.EdgeRate) -ne 1)) {
                    $output.SetPixel($x, $y, $style.Edge)
                } elseif (($hash % $style.AccentRate) -eq 0) {
                    $output.SetPixel($x, $y, $style.Accent)
                } else {
                    $output.SetPixel($x, $y, $pixel)
                }
            }
        }
    } finally { $source.Dispose() }
    try { $output.Save($path, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $output.Dispose() }
}

foreach ($tier in $styles.Keys) {
    $targets = @(
        (Join-Path $root "models\armor\${tier}_resonant_layer_1.png"),
        (Join-Path $root "models\armor\${tier}_resonant_layer_2.png"),
        (Join-Path $root "item\${tier}_resonant_helmet.png"),
        (Join-Path $root "item\${tier}_resonant_chestplate.png"),
        (Join-Path $root "item\${tier}_resonant_leggings.png"),
        (Join-Path $root "item\${tier}_resonant_boots.png")
    )
    foreach ($target in $targets) {
        if (Test-Path -LiteralPath $target) { Add-ArmorDetail $target $styles[$tier] }
    }
}
