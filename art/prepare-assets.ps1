$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
function Write-AssetPng([string]$InputPath, [string]$OutputPath, [int]$Size) {
    $source = [Drawing.Bitmap]::new((Join-Path $projectRoot $InputPath))
    $target = [Drawing.Bitmap]::new($Size, $Size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($target)
    try {
        $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.Clear([Drawing.Color]::Transparent)
        $graphics.DrawImage($source, [Drawing.Rectangle]::new(0, 0, $Size, $Size), 0, 0, $source.Width, $source.Height, [Drawing.GraphicsUnit]::Pixel)
        $target.Save((Join-Path $projectRoot $OutputPath), [Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $target.Dispose(); $source.Dispose() }
}
Write-AssetPng 'art/source/soap-ai.png' 'src/main/resources/assets/sum1rem0d/textures/item/soap.png' 32
Write-AssetPng 'art/source/mod-icon-ai.png' 'src/main/resources/assets/sum1rem0d/icon.png' 256
Write-AssetPng 'src/main/resources/assets/sum1rem0d/textures/item/soap.png' 'art/preview/soap-512.png' 512
Write-AssetPng 'art/source/coin-ai.png' 'src/main/resources/assets/sum1rem0d/textures/item/coin.png' 32
Write-AssetPng 'src/main/resources/assets/sum1rem0d/textures/item/coin.png' 'art/preview/coin-512.png' 512
