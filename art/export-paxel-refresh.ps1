param()
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskEntries=Get-Content (Join-Path $PSScriptRoot 'paxel-refresh-2026-prompts.json') -Raw | ConvertFrom-Json
$taskManifest=@()
foreach($taskEntry in $taskEntries){
 $taskSource=Join-Path $taskRoot $taskEntry.source
 New-Item -ItemType Directory -Force (Split-Path -Parent $taskSource) | Out-Null
 if(Test-Path -LiteralPath $taskEntry.generated_source){Copy-Item -LiteralPath $taskEntry.generated_source -Destination $taskSource -Force}
 $taskImage=[Drawing.Bitmap]::new($taskSource)
 $taskNative=[Drawing.Bitmap]::new(16,16,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
 try{
  $taskLeft=$taskImage.Width;$taskTop=$taskImage.Height;$taskRight=0;$taskBottom=0
  for($taskY=0;$taskY -lt $taskImage.Height;$taskY++){for($taskX=0;$taskX -lt $taskImage.Width;$taskX++){
   if($taskImage.GetPixel($taskX,$taskY).A -ge 128){$taskLeft=[Math]::Min($taskLeft,$taskX);$taskRight=[Math]::Max($taskRight,$taskX);$taskTop=[Math]::Min($taskTop,$taskY);$taskBottom=[Math]::Max($taskBottom,$taskY)}
  }}
  if($taskLeft -gt $taskRight){throw "Empty image: $($taskEntry.name)"}
  $taskWidth=$taskRight-$taskLeft+1;$taskHeight=$taskBottom-$taskTop+1
  $taskScale=14.0/[Math]::Max($taskWidth,$taskHeight)
  $taskOutW=[Math]::Max(1,[int][Math]::Round($taskWidth*$taskScale));$taskOutH=[Math]::Max(1,[int][Math]::Round($taskHeight*$taskScale))
  $taskOffsetX=[int][Math]::Floor((16-$taskOutW)/2);$taskOffsetY=[int][Math]::Floor((16-$taskOutH)/2)
  for($taskY=0;$taskY -lt $taskOutH;$taskY++){for($taskX=0;$taskX -lt $taskOutW;$taskX++){
   $taskPixel=$taskImage.GetPixel($taskLeft+[int][Math]::Floor(($taskX+.5)*$taskWidth/$taskOutW),$taskTop+[int][Math]::Floor(($taskY+.5)*$taskHeight/$taskOutH))
   if($taskPixel.A -ge 128){$taskNative.SetPixel($taskOffsetX+$taskX,$taskOffsetY+$taskY,[Drawing.Color]::FromArgb(255,$taskPixel.R,$taskPixel.G,$taskPixel.B))}
  }}
  $taskNative.Save((Join-Path $taskRoot $taskEntry.asset),[Drawing.Imaging.ImageFormat]::Png)
  $taskManifest+=@{name=$taskEntry.name;source=$taskEntry.source;asset=$taskEntry.asset;size=@(16,16);sha256=(Get-FileHash (Join-Path $taskRoot $taskEntry.asset) -Algorithm SHA256).Hash.ToLowerInvariant();tool=$taskEntry.tool;prompt=$taskEntry.prompt}
 }finally{$taskNative.Dispose();$taskImage.Dispose()}
}
$taskManifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $PSScriptRoot 'manifest-paxel-refresh-2026.json')
Write-Output "Exported $($taskManifest.Count) native 16x16 paxel textures."
