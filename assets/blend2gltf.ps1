<#
    .SYNOPSIS
        Converts all .blend files in the project assets folder to .blend.glb files
    .NOTES
        Author: murph9y@gmail.com
#>

$ErrorActionPreference = "Stop"

$blenderPath = 'C:\Program Files\Blender Foundation\Blender 2.81\'
$blenderPythonFile = 'blend2gltf.py'

function Convert-gltf {
    param(
        [Parameter(Mandatory = "true")] [string] $FilePath,
        [Parameter(Mandatory = "true")] [string] $BlenderPython
    )
    $generatedFile = @(Get-ChildItem -ErrorAction SilentlyContinue "$FilePath.glb")
    $blendFile = Get-ChildItem $FilePath
    if (($generatedFile.Length -gt 0) -and ($blendFile.LastWriteTime -lt $generatedFile[0].LastWriteTime)) {
        Write-Host "Ignored: $blendFile"
        return $false # already converted, so ignore
    }
    #run blender command and ignore output
    & "$blenderPath\blender" $FilePath --background --python $BlenderPython > $null
    Write-Host "Converted: $FilePath"
    return $true
}

$files = Get-ChildItem -Recurse -Include '*.blend'
Write-Host "$(($files).Length) .blend files found"

$results = $files | ForEach-Object { Convert-gltf $_ $blenderPythonFile }
Write-Host "$(($results | Where-Object { $_ }).Length) updated .blend.glb files"
