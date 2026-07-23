Add-Type -AssemblyName System.Drawing

$width = 16
$height = 16
$bmp = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Palette
$colors = @{
    ' ' = [System.Drawing.Color]::FromArgb(0, 0, 0, 0) # Transparent
    'K' = [System.Drawing.Color]::FromArgb(255, 20, 16, 29)    # #14101D Shadow Base / Outline
    'D' = [System.Drawing.Color]::FromArgb(255, 37, 27, 46)    # #251B2E Dark Chitin
    'M' = [System.Drawing.Color]::FromArgb(255, 61, 45, 75)    # #3D2D4B Medium Chitin
    'H' = [System.Drawing.Color]::FromArgb(255, 89, 69, 107)   # #59456B Chitin Highlight
    'L' = [System.Drawing.Color]::FromArgb(255, 118, 93, 140)  # #765D8C Light Chitin Highlight
    
    'r' = [System.Drawing.Color]::FromArgb(255, 90, 0, 18)     # #5A0012 Deep Red Shadow
    'R' = [System.Drawing.Color]::FromArgb(255, 168, 13, 38)   # #A80D26 Crimson Red
    'G' = [System.Drawing.Color]::FromArgb(255, 230, 25, 60)   # #E6193C Glowing Ruby
    'E' = [System.Drawing.Color]::FromArgb(255, 255, 30, 64)   # #FF1E40 Spider Eye Red
    'S' = [System.Drawing.Color]::FromArgb(255, 255, 89, 117)  # #FF5975 Core Highlight
    'P' = [System.Drawing.Color]::FromArgb(255, 255, 168, 182) # #FFA8B6 Core Spark
    
    'v' = [System.Drawing.Color]::FromArgb(255, 40, 160, 72)   # #28A048 Dark Venom
    'V' = [System.Drawing.Color]::FromArgb(255, 57, 211, 98)   # #39D362 Glowing Venom
    'N' = [System.Drawing.Color]::FromArgb(255, 118, 240, 155) # #76F09B Bright Venom Spark
    
    'w' = [System.Drawing.Color]::FromArgb(180, 176, 196, 222) # #B0C4DE Semitransparent Web
    'W' = [System.Drawing.Color]::FromArgb(230, 200, 216, 232) # #C8D8E8 Soft Light Web
    'Y' = [System.Drawing.Color]::FromArgb(255, 234, 244, 252) # #EAF4FC Bright Web Fiber
}

$grid = @(
    "            D W ",
    "          M D G S P ",
    "         H E R G S D",
    "        D D R P G M N",
    "       W M K r D V v",
    "      M H D Y M D   ",
    "     D M H W        ",
    "    K M H Y         ",
    "    K M H W         ",
    "   Y K M H          ",
    "  W K M H           ",
    " K D M              ",
    "K D H               ",
    "K D                 ",
    "                    ",
    "                    "
)

for ($y = 0; $y -lt 16; $y++) {
    $row = $grid[$y].PadRight(16, ' ')
    for ($x = 0; $x -lt 16; $x++) {
        $char = [string]$row[$x]
        $color = $colors[$char]
        if ($null -eq $color) { $color = $colors[' '] }
        $bmp.SetPixel($x, $y, $color)
    }
}

$paths = @(
    "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\item\spider_staff.png",
    "$PSScriptRoot\..\art-reference\spider_staff.png",
    "C:\Users\hunga\.gemini\antigravity-ide\brain\9ec0f1a4-4d7b-4899-acf3-9af801f581a9\spider_staff.png"
)

foreach ($path in $paths) {
    $dir = [System.IO.Path]::GetDirectoryName($path)
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Saved 16x16 texture to: $path"
}
