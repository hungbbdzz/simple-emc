$source = "C:\Users\hunga\Downloads\item"
$dest = "$PSScriptRoot\..\shared-resources\assets\velorise_emc\textures\item"

if (Test-Path $source) {
    Get-ChildItem -Path $source -Filter "*.png" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $dest -Force
        Write-Host "Copied $($_.Name) to $($dest)"
    }
} else {
    Write-Host "Source directory not found: $source"
}
