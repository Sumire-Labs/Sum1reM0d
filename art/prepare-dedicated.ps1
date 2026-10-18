param([string[]]$Names=@())
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
if(-not ('TextureExport' -as [type])){Add-Type -Path (Join-Path $PSScriptRoot 'TextureExport.cs')}
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskFiles=Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'source/dedicated-2026') -Filter '*-ai.png'
$taskTiles=@('mixed_wool','mixed_concrete','ardite_ore','manyullyn_block','piston_base_side','piston_base_front','bed_wood','glow_wire_dot','glow_wire_line','delivery_box_front','dust','auto_clicker_front')
foreach($taskFile in $taskFiles){
 $taskName=$taskFile.BaseName -replace '-ai$',''
 if($taskName -match '^(emerald|redstone|lapis|manyullyn)_(sword|pickaxe|axe|shovel|hoe|helmet|chestplate|leggings|boots)$'){continue}
 if($Names.Count -gt 0 -and $taskName -notin $Names){continue}
 $taskSource=[Drawing.Bitmap]::new($taskFile.FullName)
 $taskTarget=[Drawing.Bitmap]::new(16,16,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $taskGraphics=[Drawing.Graphics]::FromImage($taskTarget)
 try {
  $taskLocked=$taskSource.LockBits([Drawing.Rectangle]::new(0,0,$taskSource.Width,$taskSource.Height),[Drawing.Imaging.ImageLockMode]::ReadOnly,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
  try{$taskBytes=[byte[]]::new($taskLocked.Stride*$taskLocked.Height);[Runtime.InteropServices.Marshal]::Copy($taskLocked.Scan0,$taskBytes,0,$taskBytes.Length);$taskBounds=[TextureExport]::Bounds($taskBytes,$taskSource.Width,$taskSource.Height,$taskLocked.Stride)}finally{$taskSource.UnlockBits($taskLocked)}
  $taskMinX=$taskBounds[0];$taskMinY=$taskBounds[1];$taskMaxX=$taskBounds[2];$taskMaxY=$taskBounds[3]
  if($taskMaxX -lt 0){throw "Empty generated asset $taskName"}
  $taskW=$taskMaxX-$taskMinX+1;$taskH=$taskMaxY-$taskMinY+1
  if($taskName -in $taskTiles){$taskMinX=0;$taskMinY=0;$taskW=$taskSource.Width;$taskH=$taskSource.Height}
  $taskRatio=$(if($taskName -in $taskTiles){16.0}else{14.0})/[Math]::Max($taskW,$taskH);$taskDrawW=[int][Math]::Round($taskW*$taskRatio);$taskDrawH=[int][Math]::Round($taskH*$taskRatio)
  $taskGraphics.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy
  $taskGraphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $taskGraphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
  $taskGraphics.Clear([Drawing.Color]::Transparent)
  $taskGraphics.DrawImage($taskSource,[Drawing.Rectangle]::new([int][Math]::Floor((16-$taskDrawW)/2),[int][Math]::Floor((16-$taskDrawH)/2),$taskDrawW,$taskDrawH),$taskMinX,$taskMinY,$taskW,$taskH,[Drawing.GraphicsUnit]::Pixel)
  for($taskY=0;$taskY -lt 16;$taskY++){for($taskX=0;$taskX -lt 16;$taskX++){$taskColor=$taskTarget.GetPixel($taskX,$taskY);$taskAlpha=if($taskColor.A -ge 128){255}else{0};$taskTarget.SetPixel($taskX,$taskY,[Drawing.Color]::FromArgb($taskAlpha,$taskColor.R,$taskColor.G,$taskColor.B))}}
  if($taskName -in @('mixed_wool','mixed_concrete','glow_wire_dot','glow_wire_line')){for($taskY=0;$taskY -lt 16;$taskY++){for($taskX=0;$taskX -lt 16;$taskX++){$taskColor=$taskTarget.GetPixel($taskX,$taskY);$taskLuma=[int][Math]::Round(($taskColor.R*3+$taskColor.G*6+$taskColor.B)/10);$taskTarget.SetPixel($taskX,$taskY,[Drawing.Color]::FromArgb($taskColor.A,$taskLuma,$taskLuma,$taskLuma))}}}
  $taskPixels=$taskTarget.LockBits([Drawing.Rectangle]::new(0,0,16,16),[Drawing.Imaging.ImageLockMode]::ReadWrite,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
  try{$taskRgba=[byte[]]::new($taskPixels.Stride*16);[Runtime.InteropServices.Marshal]::Copy($taskPixels.Scan0,$taskRgba,0,$taskRgba.Length);[TextureExport]::Quantize($taskRgba,16,16,$taskPixels.Stride,12);[Runtime.InteropServices.Marshal]::Copy($taskRgba,0,$taskPixels.Scan0,$taskRgba.Length)}finally{$taskTarget.UnlockBits($taskPixels)}
  $taskOutput=Join-Path $taskRoot "src/main/resources/assets/sum1rem0d/textures/$(if($taskName -in $taskTiles){'block'}else{'item'})/$taskName.png"
  New-Item -ItemType Directory -Force (Split-Path -Parent $taskOutput) | Out-Null
  $taskTarget.Save($taskOutput,[Drawing.Imaging.ImageFormat]::Png)
  $taskPreview=[Drawing.Bitmap]::new(512,512);$taskPreviewGraphics=[Drawing.Graphics]::FromImage($taskPreview)
  try {$taskPreviewGraphics.Clear([Drawing.Color]::FromArgb(255,42,45,50));$taskPreviewGraphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$taskPreviewGraphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half;$taskPreviewGraphics.DrawImage($taskTarget,[Drawing.Rectangle]::new(0,0,512,512));$taskPreview.Save((Join-Path $PSScriptRoot "preview/dedicated-2026/$taskName.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$taskPreviewGraphics.Dispose();$taskPreview.Dispose()}
  Write-Output "Exported $taskName 16x16"
 }finally{$taskGraphics.Dispose();$taskTarget.Dispose();$taskSource.Dispose()}
}
