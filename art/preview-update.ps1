param([string]$Output=(Join-Path $PSScriptRoot 'preview/update-2.0.0-assets.png'))
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskNames=@('life_berry','cigarette','bottled_milk','milk_bundle','cat_meat','cooked_cat_meat','iron_dust','copper_dust','gold_dust','washed_iron_dust','washed_copper_dust','washed_gold_dust','sulfur','wooden_paxel','stone_paxel','copper_paxel','iron_paxel','golden_paxel','diamond_paxel','netherite_paxel','emerald_paxel','redstone_paxel','lapis_paxel','manyullyn_paxel','dust','auto_clicker_front','delivery_box_front')
$taskColumns=6;$taskCellWidth=150;$taskCellHeight=108
$taskRows=[int][Math]::Ceiling($taskNames.Count/[double]$taskColumns)
$taskSheet=[Drawing.Bitmap]::new($taskColumns*$taskCellWidth,$taskRows*$taskCellHeight)
$taskGraphics=[Drawing.Graphics]::FromImage($taskSheet)
$taskFont=[Drawing.Font]::new('Segoe UI',8)
$taskManifest=[Collections.Generic.List[object]]::new()
try{
 $taskGraphics.Clear([Drawing.Color]::FromArgb(255,32,35,41))
 $taskGraphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
 $taskGraphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
 for($taskIndex=0;$taskIndex -lt $taskNames.Count;$taskIndex++){
  $taskName=$taskNames[$taskIndex];$taskFolder=if($taskName -in @('dust','auto_clicker_front','delivery_box_front')){'block'}else{'item'}
  $taskAsset="src/main/resources/assets/sum1rem0d/textures/$taskFolder/$taskName.png"
  $taskImage=[Drawing.Bitmap]::new((Join-Path $taskRoot $taskAsset))
  try{
   if($taskImage.Width -ne 16 -or $taskImage.Height -ne 16){throw "Unexpected texture dimensions: $taskName"}
   $taskX=($taskIndex%$taskColumns)*$taskCellWidth;$taskY=[int][Math]::Floor($taskIndex/$taskColumns)*$taskCellHeight
   $taskGraphics.DrawImage($taskImage,[Drawing.Rectangle]::new($taskX+43,$taskY+4,64,64))
   $taskGraphics.DrawString($taskName,$taskFont,[Drawing.Brushes]::White,[single]($taskX+4),[single]($taskY+77))
   $taskManifest.Add(@{name=$taskName;tool='built-in image_gen';source="art/source/dedicated-2026/$taskName-ai.png";asset=$taskAsset;size=@(16,16);sha256=(Get-FileHash -LiteralPath (Join-Path $taskRoot $taskAsset) -Algorithm SHA256).Hash.ToLowerInvariant();prompt_summary='Modern vanilla Minecraft 16x16 pixel-art material; crisp discrete colors. Inventory sprites have transparent padding; block faces are seamless opaque tiles.'})
  }finally{$taskImage.Dispose()}
 }
 New-Item -ItemType Directory -Force (Split-Path -Parent $Output) | Out-Null
 $taskSheet.Save($Output,[Drawing.Imaging.ImageFormat]::Png)
 $taskManifest | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $PSScriptRoot 'manifest-update-2.0.0.json')
 Write-Output "Previewed $($taskNames.Count) native-size assets: $Output"
}finally{$taskFont.Dispose();$taskGraphics.Dispose();$taskSheet.Dispose()}
