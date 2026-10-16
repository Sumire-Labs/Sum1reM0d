$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskParts=@('sword','pickaxe','axe','shovel','hoe','helmet','chestplate','leggings','boots')
$taskFamilies=@('iron','emerald','redstone','lapis','manyullyn')
$taskSheet=[Drawing.Bitmap]::new(910,580);$taskG=[Drawing.Graphics]::FromImage($taskSheet);$taskFont=[Drawing.Font]::new('Segoe UI',12);$taskSmall=[Drawing.Font]::new('Segoe UI',9)
try{
 $taskG.Clear([Drawing.Color]::FromArgb(255,38,42,48));$taskG.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$taskG.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 $taskG.DrawString('Vanilla geometry / dedicated material colors',[Drawing.Font]::new('Segoe UI',17),[Drawing.Brushes]::White,16,10)
 for($taskRow=0;$taskRow -lt $taskFamilies.Count;$taskRow++){
  $taskFamily=$taskFamilies[$taskRow];$taskY=54+$taskRow*102;$taskG.DrawString($taskFamily,$taskFont,[Drawing.Brushes]::White,12,$taskY+25)
  for($taskCol=0;$taskCol -lt $taskParts.Count;$taskCol++){
   $taskPart=$taskParts[$taskCol];$taskPath=if($taskFamily -eq 'iron'){Join-Path $taskRoot ".research/dedicated-vanilla-reference/iron_$taskPart.png"}else{Join-Path $taskRoot "src/main/resources/assets/sum1rem0d/textures/item/${taskFamily}_$taskPart.png"}
   if(!(Test-Path -LiteralPath $taskPath)){continue};$taskImage=[Drawing.Bitmap]::new($taskPath)
   try{$taskX=112+$taskCol*88;$taskG.DrawImage($taskImage,[Drawing.Rectangle]::new($taskX,$taskY,64,64),0,0,$taskImage.Width,$taskImage.Height,[Drawing.GraphicsUnit]::Pixel);$taskG.DrawString($taskPart,$taskSmall,[Drawing.Brushes]::LightGray,$taskX,$taskY+68)}finally{$taskImage.Dispose()}
  }
 }
 $taskSheet.Save((Join-Path $PSScriptRoot 'preview/vanilla-colors.png'),[Drawing.Imaging.ImageFormat]::Png)
}finally{$taskG.Dispose();$taskSheet.Dispose();$taskFont.Dispose();$taskSmall.Dispose()}
