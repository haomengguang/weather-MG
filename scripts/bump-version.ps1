# MG Weather 版本号自增脚本
# 规则: versionName patch +1 (超过 99 进位 minor), versionCode +1
# 用法: powershell -File scripts\bump-version.ps1
$ErrorActionPreference = "Stop"
$gradlePath = Join-Path $PSScriptRoot "..\app\build.gradle.kts"

$content = [System.IO.File]::ReadAllText($gradlePath, [System.Text.Encoding]::UTF8)

$codeMatch = [regex]::Match($content, 'versionCode = (\d+)')
$nameMatch = [regex]::Match($content, 'versionName = "(\d+)\.(\d+)\.(\d+)"')
if (-not $codeMatch.Success -or -not $nameMatch.Success) {
    throw "build.gradle.kts 中未找到 versionCode/versionName"
}

$newCode = [int]$codeMatch.Groups[1].Value + 1
$major = [int]$nameMatch.Groups[1].Value
$minor = [int]$nameMatch.Groups[2].Value
$patch = [int]$nameMatch.Groups[3].Value + 1
if ($patch -gt 99) { $patch = 0; $minor += 1 }
$newName = "$major.$minor.$patch"

$content = $content -replace [regex]::Escape($codeMatch.Value), "versionCode = $newCode"
$content = $content -replace [regex]::Escape($nameMatch.Value), "versionName = `"$newName`""

[System.IO.File]::WriteAllText($gradlePath, $content, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "bumped: versionName=$newName, versionCode=$newCode"
