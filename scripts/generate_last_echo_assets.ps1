Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$itemDir = Join-Path $root 'shared-resources\assets\velorise_emc\textures\item'
$entityDir = Join-Path $root 'shared-resources\assets\velorise_emc\textures\entity'
New-Item -ItemType Directory -Force -Path $entityDir | Out-Null

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Sprite([string]$name, [scriptblock]$draw) {
    $b = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    & $draw $b
    $b.Save((Join-Path $itemDir ($name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
}
function Px($b, [int]$x, [int]$y, [string]$color) { $b.SetPixel($x, $y, (C $color)) }

Sprite 'eye_of_the_last_echo' {
    param($b)
    foreach ($p in @(@(7,1),@(8,1),@(5,2),@(6,2),@(9,2),@(10,2),@(3,3),@(4,3),@(11,3),@(12,3),@(2,4),@(13,4),@(1,5),@(14,5),@(1,6),@(14,6),@(2,7),@(13,7),@(3,8),@(12,8),@(4,9),@(11,9),@(5,10),@(10,10),@(6,11),@(9,11),@(7,12),@(8,12))) { Px $b $p[0] $p[1] '#151321' }
    foreach ($p in @(@(5,4),@(6,3),@(7,3),@(8,3),@(9,3),@(10,4),@(4,5),@(11,5),@(4,6),@(11,6),@(5,7),@(10,7),@(6,8),@(7,8),@(8,8),@(9,8))) { Px $b $p[0] $p[1] '#6331a8' }
    foreach ($p in @(@(6,5),@(7,4),@(8,4),@(9,5),@(6,6),@(9,6),@(7,7),@(8,7))) { Px $b $p[0] $p[1] '#21b9cf' }
    Px $b 7 5 '#bafaff'; Px $b 8 5 '#bafaff'; Px $b 7 6 '#5b2a91'; Px $b 8 6 '#5b2a91'
    for ($i=0; $i -lt 5; $i++) { Px $b (5+$i) (12+$i%2) '#24243a' }
}

Sprite 'heart_of_the_last_echo' {
    param($b)
    foreach ($y in 3..12) { foreach ($x in 2..13) { if (([Math]::Abs($x-7.5)+($y-3)*0.65) -lt 7 -and !($y -eq 3 -and ($x -eq 7 -or $x -eq 8))) { Px $b $x $y '#171522' } } }
    foreach ($p in @(@(3,4),@(4,3),@(5,3),@(6,4),@(9,4),@(10,3),@(11,3),@(12,4),@(4,5),@(11,5),@(5,7),@(10,7),@(6,9),@(9,9),@(7,11),@(8,11))) { Px $b $p[0] $p[1] '#7130ae' }
    foreach ($p in @(@(7,4),@(8,4),@(7,5),@(8,5),@(6,6),@(9,6),@(7,7),@(8,7),@(7,8),@(8,8),@(7,9),@(8,9))) { Px $b $p[0] $p[1] '#20c0d4' }
    Px $b 7 5 '#c9ffff'; Px $b 8 7 '#8cf6ff'
}

Sprite 'echobound_dragon_scale' {
    param($b)
    foreach ($y in 2..13) { $half = [Math]::Min(5, [Math]::Floor(($y-1)/2)); foreach ($x in (8-$half)..(8+$half)) { Px $b $x $y '#171924' } }
    foreach ($p in @(@(8,2),@(7,4),@(8,4),@(6,6),@(7,6),@(8,6),@(5,8),@(6,8),@(7,8),@(4,10),@(5,10),@(6,10),@(7,10))) { Px $b $p[0] $p[1] '#593091' }
    foreach ($p in @(@(9,3),@(9,5),@(8,7),@(9,7),@(7,9),@(8,9),@(6,11),@(7,11),@(8,11))) { Px $b $p[0] $p[1] '#18aeca' }
    Px $b 9 3 '#a8f8ff'; Px $b 8 7 '#64e8f4'
}

$jar = Join-Path $env:USERPROFILE '.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar'
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$entry = $zip.GetEntry('assets/minecraft/textures/entity/phantom.png')
$stream = $entry.Open()
$source = [System.Drawing.Bitmap]::new($stream)
$target = [System.Drawing.Bitmap]::new($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($y=0; $y -lt $source.Height; $y++) { for ($x=0; $x -lt $source.Width; $x++) {
    $s = $source.GetPixel($x,$y); if ($s.A -eq 0) { continue }
    $lum = [int](($s.R+$s.G+$s.B)/3)
    if ((($x*3+$y*5) % 17) -lt 2) { $r=18; $g=[Math]::Min(255,120+$lum); $bl=[Math]::Min(255,145+$lum) }
    elseif ((($x+$y*2) % 13) -lt 3) { $r=[Math]::Min(255,55+$lum); $g=22; $bl=[Math]::Min(255,90+$lum) }
    else { $r=[Math]::Min(80,8+[int]($lum*0.22)); $g=[Math]::Min(80,10+[int]($lum*0.22)); $bl=[Math]::Min(95,18+[int]($lum*0.27)) }
    $target.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($s.A,$r,$g,$bl))
} }
$target.Save((Join-Path $entityDir 'echobound_dragon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$target.Dispose(); $source.Dispose(); $stream.Dispose(); $zip.Dispose()

function Recolor-Echobound([string]$sourcePath, [string]$targetPath) {
    $src = [System.Drawing.Bitmap]::new($sourcePath)
    $dst = [System.Drawing.Bitmap]::new($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y=0; $y -lt $src.Height; $y++) { for ($x=0; $x -lt $src.Width; $x++) {
        $p=$src.GetPixel($x,$y); if ($p.A -eq 0) { continue }
        $lum=[int](($p.R+$p.G+$p.B)/3)
        if ((($x*5+$y*3) % 19) -lt 2) { $r=20; $g=[Math]::Min(255,105+$lum); $bl=[Math]::Min(255,135+$lum) }
        elseif ((($x+$y*4) % 17) -lt 3) { $r=[Math]::Min(255,65+$lum); $g=25; $bl=[Math]::Min(255,115+$lum) }
        else { $r=[Math]::Min(82,10+[int]($lum*.24)); $g=[Math]::Min(78,12+[int]($lum*.22)); $bl=[Math]::Min(98,22+[int]($lum*.28)) }
        $dst.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($p.A,$r,$g,$bl))
    } }
    $dst.Save($targetPath,[System.Drawing.Imaging.ImageFormat]::Png)
    $dst.Dispose(); $src.Dispose()
}

foreach ($piece in @('helmet','chestplate','leggings','boots')) {
    Recolor-Echobound (Join-Path $itemDir ("echoing_resonant_"+$piece+".png")) (Join-Path $itemDir ("echobound_"+$piece+".png"))
}
$armorDir = Join-Path $root 'shared-resources\assets\velorise_emc\textures\models\armor'
Recolor-Echobound (Join-Path $armorDir 'echoing_resonant_layer_1.png') (Join-Path $armorDir 'echobound_layer_1.png')
Recolor-Echobound (Join-Path $armorDir 'echoing_resonant_layer_2.png') (Join-Path $armorDir 'echobound_layer_2.png')
