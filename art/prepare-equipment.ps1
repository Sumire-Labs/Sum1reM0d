param([string]$MinecraftJar=(Join-Path (Split-Path -Parent $PSScriptRoot) '.gradle/isolated-home/caches/fabric-loom/26.3/minecraft-client.jar'))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
if(-not ('TextureExport' -as [type])){Add-Type -Path (Join-Path $PSScriptRoot 'TextureExport.cs')}
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskZip=[IO.Compression.ZipFile]::OpenRead($MinecraftJar)
$taskManifest=[Collections.Generic.List[object]]::new()
function Read-Palette([string]$Path,[string]$Family){
 $taskImage=[Drawing.Bitmap]::new($Path);$taskLock=$taskImage.LockBits([Drawing.Rectangle]::new(0,0,$taskImage.Width,$taskImage.Height),[Drawing.Imaging.ImageLockMode]::ReadOnly,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 try{$taskData=[byte[]]::new($taskLock.Stride*$taskLock.Height);[Runtime.InteropServices.Marshal]::Copy($taskLock.Scan0,$taskData,0,$taskData.Length);return [TextureExport]::Palette($taskData,$taskImage.Width,$taskImage.Height,$taskLock.Stride,$Family)}finally{$taskImage.UnlockBits($taskLock);$taskImage.Dispose()}
}
function Write-Colored([string]$Native,[string]$Output,[int[]]$Palette,[bool]$PreserveWood){
 $taskEntry=$taskZip.GetEntry("assets/minecraft/$Native");if($null -eq $taskEntry){return}
 $taskStream=$taskEntry.Open();$taskSource=[Drawing.Bitmap]::new($taskStream);$taskImage=[Drawing.Bitmap]::new($taskSource.Width,$taskSource.Height,[Drawing.Imaging.PixelFormat]::Format32bppArgb);$taskGraphics=[Drawing.Graphics]::FromImage($taskImage)
 try{$taskGraphics.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy;$taskGraphics.DrawImageUnscaled($taskSource,0,0);$taskLock=$taskImage.LockBits([Drawing.Rectangle]::new(0,0,$taskImage.Width,$taskImage.Height),[Drawing.Imaging.ImageLockMode]::ReadWrite,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
  try{$taskData=[byte[]]::new($taskLock.Stride*$taskLock.Height);[Runtime.InteropServices.Marshal]::Copy($taskLock.Scan0,$taskData,0,$taskData.Length);[TextureExport]::Material($taskData,$taskImage.Width,$taskImage.Height,$taskLock.Stride,$Palette,$PreserveWood);[Runtime.InteropServices.Marshal]::Copy($taskData,0,$taskLock.Scan0,$taskData.Length)}finally{$taskImage.UnlockBits($taskLock)}
  $taskOutput=Join-Path $taskRoot "src/main/resources/assets/sum1rem0d/$Output";New-Item -ItemType Directory -Force (Split-Path -Parent $taskOutput) | Out-Null;$taskImage.Save($taskOutput,[Drawing.Imaging.ImageFormat]::Png)
  $taskManifest.Add(@{path=$Output;native=$Native;palette=$Palette;width=$taskImage.Width;height=$taskImage.Height;sha256=(Get-FileHash -LiteralPath $taskOutput -Algorithm SHA256).Hash.ToLowerInvariant();method='GPT material palette projected onto unchanged vanilla pixels'})
  Write-Output "Exported $Output"
 }finally{$taskGraphics.Dispose();$taskImage.Dispose();$taskSource.Dispose();$taskStream.Dispose()}
}
try{
 foreach($taskFamily in @('emerald','redstone','lapis','manyullyn')){
  $taskTemplate=if($taskFamily -eq 'manyullyn'){'netherite'}else{'iron'}
  foreach($taskPart in @('sword','pickaxe','axe','shovel','hoe','helmet','chestplate','leggings','boots')){
   $taskSource=Join-Path $PSScriptRoot "source/vanilla-color-2026/${taskFamily}_${taskPart}-ai.png";if(!(Test-Path -LiteralPath $taskSource)){continue}
   $taskPalette=@(Read-Palette $taskSource $taskFamily)
   Write-Colored "textures/item/${taskTemplate}_${taskPart}.png" "textures/item/${taskFamily}_${taskPart}.png" $taskPalette ($taskPart -notin @('helmet','chestplate','leggings','boots'))
  }
  $taskArmorSource=Join-Path $PSScriptRoot "source/vanilla-color-2026/${taskFamily}_chestplate-ai.png"
  if(Test-Path -LiteralPath $taskArmorSource){$taskArmorPalette=@(Read-Palette $taskArmorSource $taskFamily);foreach($taskLayer in @('humanoid','humanoid_leggings','humanoid_baby')){Write-Colored "textures/entity/equipment/$taskLayer/$taskTemplate.png" "textures/entity/equipment/$taskLayer/$taskFamily.png" $taskArmorPalette $false}}
  if($taskFamily -eq 'manyullyn' -and (Test-Path -LiteralPath $taskArmorSource)){
   foreach($taskPart in @('horse_armor','nautilus_armor','spear','spear_in_hand')){Write-Colored "textures/item/netherite_$taskPart.png" "textures/item/manyullyn_$taskPart.png" $taskArmorPalette ($taskPart -like 'spear*')}
   foreach($taskLayer in @('horse_body','nautilus_body')){Write-Colored "textures/entity/equipment/$taskLayer/netherite.png" "textures/entity/equipment/$taskLayer/manyullyn.png" $taskArmorPalette $false}
  }
 }
 $taskManifest | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $PSScriptRoot 'manifest-vanilla-color-2026.json')
}finally{$taskZip.Dispose()}
