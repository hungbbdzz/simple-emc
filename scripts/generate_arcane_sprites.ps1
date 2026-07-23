Add-Type -AssemblyName System.Drawing

$assetDir = Join-Path $PSScriptRoot '..\shared-resources\assets\velorise_emc\textures\item'

function Color([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Point([int]$x, [int]$y) {
    return [System.Drawing.Point]::new($x, $y)
}

function New-Sprite([string]$name, [scriptblock]$paint) {
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    & $paint $graphics
    $graphics.Dispose()
    $path = Join-Path $assetDir ($name + '.png')
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Draw-Staff($g, [string]$dark, [string]$mid, [string]$light) {
    $g.DrawLine([System.Drawing.Pen]::new((Color $dark), 3), 2, 14, 10, 4)
    $g.DrawLine([System.Drawing.Pen]::new((Color $mid), 1), 3, 13, 10, 4)
    $g.DrawLine([System.Drawing.Pen]::new((Color $light), 1), 4, 12, 6, 10)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color $dark)), 1, 13, 4, 2)
    $g.SetClip([System.Drawing.Rectangle]::new(0, 0, 16, 16))
}

New-Sprite 'dragonfire_scepter' {
    param($g)
    Draw-Staff $g '#160d22' '#54227a' '#c94bff'
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#160d22')), [System.Drawing.Point[]]@(
        (Point 8 1), (Point 11 1), (Point 12 2), (Point 14 2), (Point 15 4),
        (Point 14 6), (Point 11 6), (Point 9 5), (Point 7 6), (Point 8 3)))
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#5e2381')), [System.Drawing.Point[]]@(
        (Point 9 2), (Point 12 2), (Point 13 3), (Point 15 3), (Point 14 5), (Point 11 5), (Point 9 4)))
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#ff5b20')), 12, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#ffb02e')), 14, 5, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#a92aff')), 8, 0, 1, 2)
}

New-Sprite 'ghastfire_staff' {
    param($g)
    Draw-Staff $g '#35191a' '#9b3d20' '#ff8b2d'
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#4b3330')), 8, 1, 7, 6)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#e8d8bd')), 9, 1, 5, 5)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#8b5b4b')), 10, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#8b5b4b')), 13, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#301818')), 11, 5, 2, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#e8d8bd')), 9, 6, 1, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#e8d8bd')), 11, 6, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#e8d8bd')), 13, 6, 1, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#ff6a16')), 14, 5, 2, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#ffd15a')), 15, 5, 1, 1)
}

New-Sprite 'wither_scepter' {
    param($g)
    Draw-Staff $g '#090a0d' '#353947' '#8d94aa'
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#090a0d')), 7, 2, 8, 5)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#353947')), 8, 1, 3, 4)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#353947')), 12, 2, 3, 4)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#b6c1db')), 9, 2, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#b6c1db')), 13, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#11131a')), 9, 4, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#11131a')), 13, 5, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#7651ac')), 10, 6, 1, 1)
}

New-Sprite 'shulker_seeker' {
    param($g)
    Draw-Staff $g '#23122e' '#663078' '#d36be8'
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#24102e')), 8, 1, 7, 6)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#8f4aa5')), 8, 1, 6, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#bc70ca')), 9, 4, 6, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#e9c6ef')), 10, 3, 3, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#24102e')), 11, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#d36be8')), 14, 2, 1, 3)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#f8a4ff')), 15, 3, 1, 1)
}

New-Sprite 'guardian_lens' {
    param($g)
    Draw-Staff $g '#082f36' '#167b82' '#58e6df'
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#082f36')), [System.Drawing.Point[]]@(
        (Point 8 2), (Point 11 0), (Point 14 1), (Point 15 4), (Point 13 7), (Point 9 6), (Point 7 4)))
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#238c8d')), [System.Drawing.Point[]]@(
        (Point 9 2), (Point 11 1), (Point 14 2), (Point 14 4), (Point 12 6), (Point 9 5)))
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#f5e7b0')), 11, 3, 3, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#173d45')), 12, 3, 1, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#77fff4')), 15, 3, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#167b82')), 8, 1, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#167b82')), 13, 6, 1, 1)
}

New-Sprite 'void_translocator' {
    param($g)
    # Heavy obsidian shaft with a violet energy coil.
    $g.DrawLine([System.Drawing.Pen]::new((Color '#10091d'), 4), 2, 14, 10, 5)
    $g.DrawLine([System.Drawing.Pen]::new((Color '#34205f'), 2), 3, 13, 10, 5)
    $g.DrawLine([System.Drawing.Pen]::new((Color '#a940ed'), 1), 3, 12, 9, 6)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#10091d')), 1, 12, 5, 3)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#5b2a91')), 2, 12, 3, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#c85cff')), 4, 10, 2, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#c85cff')), 6, 8, 2, 1)
    # Oversized Ender portal eye at the head, pointing to the right.
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#10091d')), [System.Drawing.Point[]]@(
        (Point 8 1), (Point 12 0), (Point 15 2), (Point 15 6),
        (Point 12 8), (Point 8 7), (Point 7 4)))
    $g.FillPolygon([System.Drawing.SolidBrush]::new((Color '#552783')), [System.Drawing.Point[]]@(
        (Point 9 2), (Point 12 1), (Point 14 2), (Point 14 5),
        (Point 12 7), (Point 9 6), (Point 8 4)))
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#0a3238')), 10, 2, 4, 4)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#159c91')), 11, 2, 3, 4)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#6dffe2')), 12, 3, 2, 2)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#dba5ff')), 9, 1, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#a940ed')), 14, 1, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#a940ed')), 15, 4, 1, 1)
    $g.FillRectangle([System.Drawing.SolidBrush]::new((Color '#a940ed')), 13, 7, 1, 1)
}

New-Sprite 'alchemical_hourglass' {
    param($g)
    $dark = [System.Drawing.SolidBrush]::new((Color '#3b2558'))
    $frame = [System.Drawing.SolidBrush]::new((Color '#8755ba'))
    $glass = [System.Drawing.SolidBrush]::new((Color '#9cebf0'))
    $sand = [System.Drawing.SolidBrush]::new((Color '#f3bd43'))
    $shine = [System.Drawing.SolidBrush]::new((Color '#e3ffff'))
    $g.FillRectangle($dark, 3, 1, 10, 2)
    $g.FillRectangle($frame, 4, 1, 8, 1)
    $g.FillRectangle($dark, 3, 13, 10, 2)
    $g.FillRectangle($frame, 4, 13, 8, 1)
    $g.FillRectangle($dark, 4, 3, 2, 3)
    $g.FillRectangle($dark, 10, 3, 2, 3)
    $g.FillRectangle($dark, 6, 6, 4, 4)
    $g.FillRectangle($dark, 4, 10, 2, 3)
    $g.FillRectangle($dark, 10, 10, 2, 3)
    $g.FillPolygon($glass, [System.Drawing.Point[]]@((Point 6 3),(Point 10 3),(Point 9 6),(Point 7 6)))
    $g.FillPolygon($glass, [System.Drawing.Point[]]@((Point 7 10),(Point 9 10),(Point 10 13),(Point 6 13)))
    $g.FillRectangle($sand, 7, 4, 2, 2)
    $g.FillRectangle($sand, 7, 7, 2, 1)
    $g.FillRectangle($sand, 6, 11, 4, 2)
    $g.FillRectangle($shine, 6, 3, 1, 2)
}
