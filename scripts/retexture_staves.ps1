Add-Type -AssemblyName System.Drawing

# Target output directory: C:\Users\hunga\Downloads\temp_texture ONLY
$outDir = "C:\Users\hunga\Downloads\temp_texture"
if (-not (Test-Path -Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

function HexColor([string]$hex) {
    if ($hex -eq 'transparent' -or $hex -eq '.') { return [System.Drawing.Color]::Transparent }
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Brush([string]$hex) {
    return [System.Drawing.SolidBrush]::new((HexColor $hex))
}

# Renders a 16-row ASCII grid string array into a 16x16 bitmap
function Render-Grid([hashtable]$palette, [string[]]$grid) {
    $bmp = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)

    $brushes = @{}
    foreach ($key in $palette.Keys) {
        $brushes[$key] = Brush $palette[$key]
    }

    for ($y = 0; $y -lt 16; $y++) {
        $line = $grid[$y]
        for ($x = 0; $x -lt 16; $x++) {
            $char = [string]$line[$x]
            if ($char -ne '.' -and $brushes.ContainsKey($char)) {
                $g.FillRectangle($brushes[$char], $x, $y, 1, 1)
            }
        }
    }

    foreach ($b in $brushes.Values) { $b.Dispose() }
    $g.Dispose()
    return $bmp
}

# Save 16x16 PNG and an 8x preview (128x128) ONLY in C:\Users\hunga\Downloads\temp_texture
function Save-Texture([string]$name, [System.Drawing.Bitmap]$bmp16) {
    # 1. 16x16 PNG
    $path16 = Join-Path $outDir ($name + ".png")
    $bmp16.Save($path16, [System.Drawing.Imaging.ImageFormat]::Png)

    # 2. 128x128 8x preview
    $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $pg = [System.Drawing.Graphics]::FromImage($previewBmp)
    $pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $pg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $pg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $pg.DrawImage($bmp16, 0, 0, 128, 128)
    $pg.Dispose()

    $previewPath = Join-Path $outDir ($name + "_preview_8x.png")
    $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $previewBmp.Dispose()
}

function Build-Staff([string]$name, [hashtable]$palette, [string[]]$grid) {
    $bmp = Render-Grid $palette $grid
    Save-Texture $name $bmp
    $bmp.Dispose()
    Write-Host " -> [Vẽ mới Texture] $name.png" -ForegroundColor Green
}

Write-Host "VẼ LẠI WITHER SCEPTER MÀU ĐEN TRẮNG CHUẨN OFFICAL WITHER MINECRAFT..." -ForegroundColor Yellow

# ==============================================================================
# WITHER SCEPTER (Tông Màu Đen Đen Charcoal & Trắng Xám Chuẩn Minecraft Wither)
# ==============================================================================
Build-Staff 'wither_scepter' @{
    'o' = '#0c0c0e'  # Dark Wither Outline
    'a' = '#141417'  # Charcoal Wither Black Dark
    'b' = '#212126'  # Charcoal Wither Black Mid
    'c' = '#33333b'  # Charcoal Wither Light
    'd' = '#4a4a54'  # Charcoal Wither Highlight
    'w' = '#e6e6e6'  # Wither Eye Socket & Teeth White-Grey
    'i' = '#ffffff'  # Wither Eye Spark White
} @(
    ".......oooooo...",
    "....ooocdddddooo",
    "...ocdowiwiiwddo",
    "...obbowwwwdobbo",
    "....ooocdddddooo",
    ".......oabco....",
    ".......oabco....",
    ".......oabco....",
    ".......oabco....",
    ".......oabco....",
    "......oabco.....",
    "......oabco.....",
    "......oabco.....",
    ".....ooao.......",
    "................",
    "................"
)

Write-Host "`nHOAN THANH! Đã xuất duy nhất wither_scepter.png chuẩn tông màu Đen - Trắng Xám của Wither Minecraft!" -ForegroundColor Green
Write-Host "File duoc xuat CHI CHUAN tai: C:\Users\hunga\Downloads\temp_texture" -ForegroundColor Cyan