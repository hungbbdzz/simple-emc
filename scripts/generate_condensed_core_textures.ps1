Add-Type -AssemblyName System.Drawing

$assetRoot = Join-Path $PSScriptRoot '..\shared-resources\assets\velorise_emc\textures'
$itemRoot = Join-Path $assetRoot 'item'
$blockRoot = Join-Path $assetRoot 'block'

function New-Bitmap16([bool]$transparent = $true) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    if ($transparent) { $bitmap.MakeTransparent() }
    return $bitmap
}

function Color([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Pixel($bitmap, [int]$x, [int]$y, [string]$hex) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
        $bitmap.SetPixel($x, $y, (Color $hex))
    }
}

function Save-Png($bitmap, [string]$path) {
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Draw-Core([string]$path, [string[]]$palette) {
    $b = New-Bitmap16
    $rows = @{
        2=@(7,8); 3=@(5,10); 4=@(4,11); 5=@(3,12); 6=@(2,13);
        7=@(2,13); 8=@(2,13); 9=@(3,12); 10=@(3,12); 11=@(4,11);
        12=@(5,10); 13=@(7,8)
    }
    foreach ($entry in $rows.GetEnumerator()) {
        for ($x=$entry.Value[0]; $x -le $entry.Value[1]; $x++) { Pixel $b $x $entry.Key $palette[0] }
    }
    for ($y=4; $y -le 11; $y++) {
        $left = if ($y -le 6) { 5-$y+4 } elseif ($y -ge 10) { $y-7 } else { 3 }
        $right = 15-$left
        for ($x=$left; $x -le $right; $x++) { Pixel $b $x $y $palette[2] }
    }
    # Large diagonal facets deliberately avoid a centered plus/cross silhouette.
    foreach ($p in @(@(4,5),@(5,5),@(6,5),@(4,6),@(5,6),@(6,6),@(7,6),@(4,7),@(5,7),@(6,7),@(8,4),@(9,4),@(10,5))) { Pixel $b $p[0] $p[1] $palette[4] }
    foreach ($p in @(@(3,8),@(4,8),@(4,9),@(5,9),@(5,10),@(6,10),@(6,11))) { Pixel $b $p[0] $p[1] $palette[3] }
    foreach ($p in @(@(9,5),@(10,6),@(11,6),@(9,7),@(10,7),@(11,7),@(12,7),@(8,8),@(9,8),@(10,8),@(11,8),@(8,9),@(9,9),@(10,9),@(9,10))) { Pixel $b $p[0] $p[1] $palette[1] }
    foreach ($p in @(@(5,4),@(4,5),@(4,6),@(3,7),@(6,4))) { Pixel $b $p[0] $p[1] $palette[5] }
    foreach ($p in @(@(7,12),@(8,12),@(9,11),@(10,10),@(11,9),@(12,8))) { Pixel $b $p[0] $p[1] $palette[0] }
    Save-Png $b $path
}

function Draw-CoreBlock([string]$path, [string[]]$palette, [int]$tier) {
    $b = New-Bitmap16 $false
    for ($y=0; $y -lt 16; $y++) {
        for ($x=0; $x -lt 16; $x++) {
            $shade = (($x*3 + $y*5 + $tier) % 7)
            Pixel $b $x $y $(if ($shade -lt 2) {$palette[0]} elseif ($shade -lt 5) {$palette[1]} else {$palette[2]})
        }
    }
    for ($i=0; $i -lt 16; $i++) { Pixel $b $i 0 $palette[0]; Pixel $b $i 15 $palette[0]; Pixel $b 0 $i $palette[0]; Pixel $b 15 $i $palette[0] }
    foreach ($p in @(@(7,2),@(8,2),@(5,3),@(6,3),@(7,3),@(8,3),@(9,3),@(10,3),@(4,4),@(5,4),@(6,4),@(7,4),@(8,4),@(9,4),@(10,4),@(11,4),
        @(3,5),@(4,5),@(5,5),@(6,5),@(7,5),@(8,5),@(9,5),@(10,5),@(11,5),@(12,5),@(3,6),@(4,6),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(11,6),@(12,6),
        @(3,7),@(4,7),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),@(11,7),@(12,7),@(3,8),@(4,8),@(5,8),@(6,8),@(7,8),@(8,8),@(9,8),@(10,8),@(11,8),@(12,8),
        @(4,9),@(5,9),@(6,9),@(7,9),@(8,9),@(9,9),@(10,9),@(11,9),@(4,10),@(5,10),@(6,10),@(7,10),@(8,10),@(9,10),@(10,10),@(11,10),
        @(5,11),@(6,11),@(7,11),@(8,11),@(9,11),@(10,11),@(7,12),@(8,12))) { Pixel $b $p[0] $p[1] $palette[3] }
    foreach ($p in @(@(5,5),@(6,5),@(7,5),@(5,6),@(6,6),@(7,6),@(4,7),@(5,7),@(6,7),@(9,4),@(10,5))) { Pixel $b $p[0] $p[1] $palette[5] }
    foreach ($p in @(@(9,6),@(10,6),@(11,7),@(8,7),@(9,7),@(10,7),@(8,8),@(9,8),@(10,8),@(7,9),@(8,9),@(9,9),@(8,10))) { Pixel $b $p[0] $p[1] $palette[4] }
    Save-Png $b $path
}

$palettes = @(
    @('#062b30','#09545a','#087d77','#0aa99a','#18d5bb','#9cfff0'),
    @('#351206','#692109','#9b3508','#d55209','#f47f12','#ffd46a'),
    @('#210c38','#421568','#652098','#8e31c4','#bf50e8','#f0a8ff'),
    @('#03191d','#06333b','#075866','#087c87','#12aeb5','#78fff4')
)

for ($tier=1; $tier -le 4; $tier++) {
    $suffix = @('i','ii','iii','iv')[$tier-1]
    Draw-Core (Join-Path $itemRoot "condensed_core_$suffix.png") $palettes[$tier-1]
    Draw-CoreBlock (Join-Path $blockRoot "condensed_core_block_$suffix.png") $palettes[$tier-1] $tier
}

# Infernal Resonant Ore: dense blackstone/netherrack clusters with intentional molten seams.
$ore = New-Bitmap16 $false
$base = @('#190d0d','#241112','#301719','#3b1d1f')
for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) { Pixel $ore $x $y $base[(($x*5 + $y*3 + ($x*$y)) % 4)] }
}
foreach ($p in @(@(0,4),@(1,4),@(1,5),@(2,5),@(2,6),@(3,6),@(3,7),@(4,7),@(4,8),@(5,8),@(5,9),@(6,9),@(6,10),
    @(9,0),@(9,1),@(8,2),@(8,3),@(7,3),@(7,4),@(6,4),@(6,5),@(5,5),@(12,5),@(12,6),@(11,6),@(11,7),@(10,7),@(10,8),@(9,8),
    @(15,11),@(14,11),@(14,12),@(13,12),@(13,13),@(12,13),@(12,14),@(11,14),@(11,15),@(3,14),@(4,14),@(4,13),@(5,13),@(5,12))) { Pixel $ore $p[0] $p[1] '#7a0d0a' }
foreach ($p in @(@(1,5),@(2,6),@(3,7),@(4,8),@(5,9),@(8,2),@(7,3),@(6,4),@(11,6),@(10,7),@(14,12),@(13,13),@(12,14),@(4,13),@(5,12))) { Pixel $ore $p[0] $p[1] '#d3260c' }
foreach ($p in @(@(3,7),@(7,3),@(10,7),@(13,13),@(5,12))) { Pixel $ore $p[0] $p[1] '#ff8a16' }
foreach ($p in @(@(7,3),@(13,13))) { Pixel $ore $p[0] $p[1] '#ffd45a' }
Save-Png $ore (Join-Path $blockRoot 'infernal_resonant_ore.png')
