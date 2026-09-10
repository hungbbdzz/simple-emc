# PowerShell script to package main source code and README to Downloads directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

$UserDownloads = [System.IO.Path]::Combine([System.Environment]::GetFolderPath('UserProfile'), 'Downloads')
if (Test-Path $UserDownloads) {
    $OutputDir = $UserDownloads
} else {
    $OutputDir = "C:\Downloads"
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$ZipPath = Join-Path $OutputDir "simple_emc_source_$Timestamp.zip"
$LatestPath = Join-Path $OutputDir "simple_emc_source.zip"

Write-Host "Packaging source code from: $RootDir"
Write-Host "Target output: $ZipPath"

$ExcludeDirs = @('build', '.gradle', '.git', '.vscode', '.agents', 'bin', 'out', 'run', '.idea', '__pycache__', 'dist', '.gemini')
$ExcludeExts = @('.jar', '.class', '.log', '.pyc', '.pyo', '.swp', '.tmp')

$AllFiles = Get-ChildItem -Path $RootDir -Recurse -File | Where-Object {
    $rel = $_.FullName.Substring($RootDir.Length + 1)
    $parts = $rel.Split([System.IO.Path]::DirectorySeparatorChar)
    
    # Check if any parent folder is excluded or hidden
    $hasExcludedDir = $false
    foreach ($p in $parts[0..($parts.Length - 2)]) {
        if ($ExcludeDirs -contains $p.ToLower() -or $p.StartsWith('.')) {
            $hasExcludedDir = $true
            break
        }
    }
    if ($hasExcludedDir) { return $false }
    if ($_.Name.StartsWith('.')) { return $false }
    if ($ExcludeExts -contains $_.Extension.ToLower()) { return $false }

    return $true
}

if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path $AllFiles.FullName -DestinationPath $ZipPath -Force
Copy-Item $ZipPath -Destination $LatestPath -Force

Write-Host "Successfully zipped $($AllFiles.Count) files into:"
Write-Host "  1. $ZipPath"
Write-Host "  2. $LatestPath"
