$assetDir = Join-Path $PSScriptRoot '..\shared-resources\assets\velorise_emc\textures\item'
$targetDir = "C:\Users\hunga\Downloads\temp_texture"
$backupDir = "C:\Users\hunga\Downloads\original_textures"

if (-not (Test-Path -Path $targetDir)) { New-Item -ItemType Directory -Force -Path $targetDir | Out-Null }
if (-not (Test-Path -Path $backupDir)) { New-Item -ItemType Directory -Force -Path $backupDir | Out-Null }

$staves = @(
    "dragonfire_scepter",
    "evoker_fang_staff",
    "frostbolt_wand",
    "ghastfire_staff",
    "guardian_lens",
    "inferno_rod",
    "shulker_seeker",
    "stormcaller",
    "void_translocator",
    "webcaster",
    "wither_scepter"
)

Write-Host "Khôi phục lại toàn bộ ảnh GỐC ban đầu vào Downloads..." -ForegroundColor Yellow

foreach ($name in $staves) {
    $src = Join-Path $assetDir ($name + ".png")
    if (Test-Path -Path $src) {
        Copy-Item -Path $src -Destination (Join-Path $targetDir ($name + ".png")) -Force
        Copy-Item -Path $src -Destination (Join-Path $backupDir ($name + ".png")) -Force
        Write-Host " -> Da khoi phuc file goc: $name.png" -ForegroundColor Green
    }
}

Write-Host "`nHOAN THANH! Toan bo anh GOC ban dau da duoc khoi phuc tai:" -ForegroundColor Cyan
Write-Host "1. C:\Users\hunga\Downloads\temp_texture" -ForegroundColor Yellow
Write-Host "2. C:\Users\hunga\Downloads\original_textures" -ForegroundColor Yellow
