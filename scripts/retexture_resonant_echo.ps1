Add-Type -AssemblyName System.Drawing

# Directories
$tempDir = "C:\Users\hunga\Downloads\temp_texture"
$projectDir = Join-Path $PSScriptRoot "..\shared-resources\assets\velorise_emc\textures\item"

if (-not (Test-Path $tempDir)) { New-Item -ItemType Directory -Force -Path $tempDir | Out-Null }
if (-not (Test-Path $projectDir)) { New-Item -ItemType Directory -Force -Path $projectDir | Out-Null }

function HexColor([string]$hex) {
    if ($hex -eq 'transparent' -or $hex -eq '.') { return [System.Drawing.Color]::Transparent }
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Brush([string]$hex) {
    return [System.Drawing.SolidBrush]::new((HexColor $hex))
}

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

function Save-Texture([string]$name, [System.Drawing.Bitmap]$bmp16) {
    # 1. Save to temp_texture
    $tempPath = Join-Path $tempDir ($name + ".png")
    $bmp16.Save($tempPath, [System.Drawing.Imaging.ImageFormat]::Png)

    # 2. Save 8x preview
    $previewBmp = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $pg = [System.Drawing.Graphics]::FromImage($previewBmp)
    $pg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $pg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $pg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $pg.DrawImage($bmp16, 0, 0, 128, 128)
    $pg.Dispose()
    $previewPath = Join-Path $tempDir ($name + "_preview_8x.png")
    $previewBmp.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $previewBmp.Dispose()

    # 3. Direct overwrite into project asset directory
    $projectPath = Join-Path $projectDir ($name + ".png")
    $bmp16.Save($projectPath, [System.Drawing.Imaging.ImageFormat]::Png)

    Write-Host " -> [Đã xuất Temp] $tempPath" -ForegroundColor Green
    Write-Host " -> [Đã cập nhật Project] $projectPath" -ForegroundColor Green
}

Write-Host "VẼ LẠI RESONANT ECHO VỚI TÔNG MÀU SCULK MA MỊ & NĂNG LƯỢNG CYAN THỦY TỘC..." -ForegroundColor Yellow

# ==============================================================================
# RESONANT ECHO (Tông Màu Sculk Warden Teal Dark & Sculk Sensor Cyan Glow)
# ==============================================================================
$palette = @{
    'o' = '#05101a'  # Sculk Deep Black Rim
    'a' = '#0d1921'  # Sculk Dark Teal Base
    'b' = '#0e4a52'  # Sculk Mid Teal
    'c' = '#106570'  # Sculk Light Teal
    'v' = '#007888'  # Sculk Catalyst Cyan Dark
    's' = '#00b8cc'  # Sculk Sensor Cyan Bright
    'u' = '#2bfaee'  # Sculk Sonic Glow Cyan
    'i' = '#79ffff'  # Sculk White-Cyan Core Glare
}

$grid = @(
    "...........ouuo.",
    "........ouuo..uo",
    ".......osiiu...u",
    "......osiiiuso..",
    ".....ovsiisov...",
    "......oooooo....",
    "......oabco.....",
    ".....oabco......",
    "....oabco.......",
    "...oabco........",
    "..oabco.........",
    ".oabco..........",
    "ooao............",
    "................",
    "................",
    "................"
)

$bmp = Render-Grid $palette $grid
Save-Texture "resonant_echo" $bmp
$bmp.Dispose()

Write-Host "`nHOAN THANH! Đã nhúng màu Sculk chuẩn vào resonant_echo.png thành công!" -ForegroundColor Cyan
