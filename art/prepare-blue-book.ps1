param([string]$MinecraftJar=(Join-Path (Split-Path -Parent $PSScriptRoot) '.gradle/isolated-home/caches/fabric-loom/26.3/minecraft-client.jar'))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::OpenRead($MinecraftJar)
try {
 $taskStream=$taskZip.GetEntry('assets/minecraft/textures/item/writable_book.png').Open()
 $taskNative=[Drawing.Bitmap]::new($taskStream)
 $taskOutput=[Drawing.Bitmap]::new(16,16,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 try {
  # GPT blue palette, projected onto the native grid without changing brightness or alpha.
  for($taskY=0;$taskY -lt 16;$taskY++){for($taskX=0;$taskX -lt 16;$taskX++){
   $taskPixel=$taskNative.GetPixel($taskX,$taskY)
   $taskMax=[Math]::Max($taskPixel.R,[Math]::Max($taskPixel.G,$taskPixel.B))
   $taskMin=[Math]::Min($taskPixel.R,[Math]::Min($taskPixel.G,$taskPixel.B))
   $taskColor=$taskPixel
   if($taskPixel.A -gt 0 -and $taskPixel.R -gt $taskPixel.B -and ($taskMax-$taskMin) -gt $taskMax*0.30){
    $taskColor=[Drawing.Color]::FromArgb([int]$taskPixel.A,[int][Math]::Round($taskMax*0.14),[int][Math]::Round($taskMax*0.36),[int]$taskMax)
   }
   $taskOutput.SetPixel($taskX,$taskY,$taskColor)
  }}
  $taskOutput.Save((Join-Path (Split-Path -Parent $PSScriptRoot) 'src/main/resources/assets/sum1rem0d/textures/item/tamed_book.png'),[Drawing.Imaging.ImageFormat]::Png)
 } finally {$taskOutput.Dispose();$taskNative.Dispose();$taskStream.Dispose()}
} finally {$taskZip.Dispose()}
