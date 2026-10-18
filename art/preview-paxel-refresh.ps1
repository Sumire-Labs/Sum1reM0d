$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRows=Get-Content art/paxel-refresh-2026-prompts.json -Raw | ConvertFrom-Json
$taskSheet=[Drawing.Bitmap]::new(660,440)
$taskGraphics=[Drawing.Graphics]::FromImage($taskSheet)
$taskFont=[Drawing.Font]::new('Segoe UI',9)
try{
 $taskGraphics.Clear([Drawing.Color]::FromArgb(255,36,39,46))
 $taskGraphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
 $taskGraphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 for($taskIndex=0;$taskIndex -lt $taskRows.Count;$taskIndex++){
  $taskImage=[Drawing.Bitmap]::new((Join-Path $PWD $taskRows[$taskIndex].asset))
  try{
   $taskX=($taskIndex%4)*165;$taskY=[int][Math]::Floor($taskIndex/4)*145
   $taskGraphics.DrawImage($taskImage,[Drawing.Rectangle]::new($taskX+20,$taskY+8,96,96))
   $taskGraphics.DrawImage($taskImage,[Drawing.Rectangle]::new($taskX+132,$taskY+88,16,16))
   $taskGraphics.DrawString($taskRows[$taskIndex].name,$taskFont,[Drawing.Brushes]::White,[single]($taskX+8),[single]($taskY+115))
  }finally{$taskImage.Dispose()}
 }
 $taskSheet.Save((Join-Path $PWD 'art/preview/paxel-refresh-2026.png'),[Drawing.Imaging.ImageFormat]::Png)
}finally{$taskFont.Dispose();$taskGraphics.Dispose();$taskSheet.Dispose()}
