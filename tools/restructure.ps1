param(
  [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot  # tools/.. = project root
Write-Host "Project root: $root"

# Thư mục bỏ qua khi duyệt
$excludeDirs = @(
  '.git','node_modules','bin','obj','build','dist','.vs','tools','docs'
)

# Ánh xạ phần mở rộng -> thư mục đích tương đối
$map = @{
  # code
  '.cs'   = 'src'
  '.js'   = 'src'
  '.jsx'  = 'src'
  '.ts'   = 'src'
  '.tsx'  = 'src'
  '.py'   = 'src'
  # styles
  '.css'  = 'src/styles'
  '.scss' = 'src/styles'
  # images
  '.png'  = 'assets/images'
  '.jpg'  = 'assets/images'
  '.jpeg' = 'assets/images'
  '.gif'  = 'assets/images'
  '.svg'  = 'assets/images'
  # audio
  '.mp3'  = 'assets/audio'
  '.wav'  = 'assets/audio'
  '.ogg'  = 'assets/audio'
  # fonts
  '.ttf'  = 'assets/fonts'
  '.otf'  = 'assets/fonts'
  '.woff' = 'assets/fonts'
  '.woff2'= 'assets/fonts'
  # config
  '.json' = 'config'
  '.yaml' = 'config'
  '.yml'  = 'config'
  # data
  '.csv'  = 'data'
  '.txt'  = 'data'
  # docs (trừ docs hiện tại)
  '.md'   = 'docs'
}

function Should-ExcludePath($path) {
  foreach ($ex in $excludeDirs) {
    if ($path -like "*\${ex}\*") { return $true }
    if ((Split-Path -Leaf $path) -eq $ex) { return $true }
  }
  return $false
}

# Tạo thư mục đích cần thiết
$destDirs = $map.Values | Sort-Object -Unique
foreach ($rel in $destDirs) {
  $full = Join-Path $root $rel
  if (-not (Test-Path $full)) {
    New-Item -ItemType Directory -Path $full | Out-Null
    Write-Host "Created: $rel"
  }
}

# Duyệt toàn bộ file
$files = Get-ChildItem -Path $root -Recurse -File | Where-Object {
  -not (Should-ExcludePath $_.DirectoryName)
}

$dryRun = -not $Execute
if ($dryRun) { Write-Host "Dry run mode. Add -Execute to apply changes." -ForegroundColor Yellow }

foreach ($f in $files) {
  $ext = [System.IO.Path]::GetExtension($f.Name).ToLowerInvariant()
  if (-not $map.ContainsKey($ext)) { continue }

  # Không di chuyển file .md trong docs/
  if ($ext -eq '.md' -and ($f.FullName -like "*\docs\*")) { continue }

  $targetRel = $map[$ext]
  $targetDir = Join-Path $root $targetRel

  # Nếu đã ở đúng thư mục đích thì bỏ qua
  if ($f.DirectoryName -like (Join-Path $targetDir '*')) { continue }
  if ($f.DirectoryName -eq $targetDir) { continue }

  $destPath = Join-Path $targetDir $f.Name

  # Tránh ghi đè: nếu tồn tại, thêm hậu tố -moved
  if (Test-Path $destPath) {
    $base = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $newName = "$base-moved$ext"
    $destPath = Join-Path $targetDir $newName
  }

  if ($dryRun) {
    Write-Host "[WhatIf] Move '$($f.FullName)' -> '$destPath'"
  } else {
    Move-Item -Path $f.FullName -Destination $destPath
    Write-Host "Moved: $($f.Name) -> $targetRel"
  }
}

Write-Host "Done."
