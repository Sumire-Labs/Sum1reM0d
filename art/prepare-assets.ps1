$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
function Write-AssetPng([string]$InputPath, [string]$OutputPath, [int]$Size, [int[]]$Bounds = @()) {
    $source = [Drawing.Bitmap]::new((Join-Path $projectRoot $InputPath))
    $target = [Drawing.Bitmap]::new($Size, $Size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($target)
    try {
        $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.Clear([Drawing.Color]::Transparent)
        if ($Bounds.Count -eq 4) {
            $ratio = 14.0 / [Math]::Max($Bounds[2],$Bounds[3])
            $drawWidth = [int][Math]::Round($Bounds[2]*$ratio)
            $drawHeight = [int][Math]::Round($Bounds[3]*$ratio)
            $destination = [Drawing.Rectangle]::new([int][Math]::Floor((16-$drawWidth)/2),[int][Math]::Floor((16-$drawHeight)/2),$drawWidth,$drawHeight)
            $graphics.DrawImage($source,$destination,$Bounds[0],$Bounds[1],$Bounds[2],$Bounds[3],[Drawing.GraphicsUnit]::Pixel)
        } else {
            $graphics.DrawImage($source, [Drawing.Rectangle]::new(0, 0, $Size, $Size), 0, 0, $source.Width, $source.Height, [Drawing.GraphicsUnit]::Pixel)
        }
        if ($Size -eq 16) {
            # Native item silhouettes use a binary cutout mask; remove faint generation fringes.
            for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
                $pixel = $target.GetPixel($x,$y)
                $alpha = if ($pixel.A -ge 128) { 255 } else { 0 }
                $target.SetPixel($x,$y,[Drawing.Color]::FromArgb($alpha,$pixel.R,$pixel.G,$pixel.B))
            } }
        }
        $target.Save((Join-Path $projectRoot $OutputPath), [Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $target.Dispose(); $source.Dispose() }
}
$assetLayout = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'layout-16x16.json') -Raw | ConvertFrom-Json
foreach ($assetName in @('soap','coin','coin_roll','mud_ball')) {
    Write-AssetPng "art/source/16x16-redesign/$assetName-ai.png" "src/main/resources/assets/sum1rem0d/textures/item/$assetName.png" 16 @($assetLayout.$assetName.source_bounds)
    Write-AssetPng "src/main/resources/assets/sum1rem0d/textures/item/$assetName.png" "art/preview/$assetName-512.png" 512
}
Write-AssetPng 'art/source/16x16-redesign/icon-ai.png' 'src/main/resources/assets/sum1rem0d/icon.png' 16 @($assetLayout.icon.source_bounds)
