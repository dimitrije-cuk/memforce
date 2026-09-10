<#
.SYNOPSIS
    Prepares a Windows machine to build MemForce.

.DESCRIPTION
    Installs the Android SDK packages the build needs and points the project at them
    through local.properties. Safe to re-run: every step is skipped when it is already
    satisfied, so nothing is downloaded twice.

    The script never edits PATH, never sets machine or user environment variables, and
    never touches JAVA_HOME. Everything it writes lives under -SdkRoot and in the
    repository's own gitignored local.properties. See docs/development-environment.md.

.PARAMETER SdkRoot
    Where the Android SDK lives. When omitted the script reuses an existing location in
    this order: sdk.dir in local.properties, then ANDROID_HOME, then ANDROID_SDK_ROOT,
    and finally installs into %LOCALAPPDATA%\Android\Sdk.

.PARAMETER SkipVerify
    Skips the closing build and unit-test run.

.EXAMPLE
    .\setup-env.ps1
    Installs into the default location and verifies the build.

.EXAMPLE
    .\setup-env.ps1 -SdkRoot D:\Android\Sdk
    Installs onto another drive, for when the system drive is short on space.
#>
[CmdletBinding()]
param(
    [string] $SdkRoot,
    [switch] $SkipVerify
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Pinned so every machine resolves the same tools. Bump deliberately, not by accident.
$CmdlineToolsUrl = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
# Dictated by the Android Gradle plugin version in gradle/libs.versions.toml, NOT by compileSdk.
# AGP downloads this itself on the first build if it is absent; installing it up front keeps
# setup self-contained. After an AGP bump, watch the first build for a line reading
# "Install Android SDK Build-Tools <version>" and update this to match.
$BuildToolsVersion = '36.0.0'
$MinimumJdk = 17
$RequiredFreeGb = 3

$RepoRoot = $PSScriptRoot
$LocalProperties = Join-Path $RepoRoot 'local.properties'

function Write-Step { param([string] $Message) Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Write-Ok { param([string] $Message) Write-Host "    $Message" -ForegroundColor Green }
function Write-Skip { param([string] $Message) Write-Host "    $Message" -ForegroundColor DarkGray }

function Get-JdkHome {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += @(
        [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
        [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    )

    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $javac = Join-Path $candidate 'bin\javac.exe'
        if (-not (Test-Path $javac)) { continue }

        # "javac 21.0.5" -> 21. Old builds report "javac 1.8.0_451", whose major is the second field.
        $reported = (& $javac -version 2>&1 | Out-String).Trim()
        if ($reported -notmatch 'javac\s+(\d+)(?:\.(\d+))?') { continue }
        $major = [int]$Matches[1]
        if ($major -eq 1 -and $Matches[2]) { $major = [int]$Matches[2] }

        if ($major -ge $MinimumJdk) {
            return [pscustomobject]@{ Home = $candidate; Version = $major; Reported = $reported }
        }
        Write-Skip "Ignoring JDK $major at $candidate (need $MinimumJdk or newer)."
    }
    return $null
}

function Get-ConfiguredSdkDir {
    if (-not (Test-Path $LocalProperties)) { return $null }
    foreach ($line in Get-Content $LocalProperties) {
        if ($line -match '^\s*sdk\.dir\s*=\s*(.+?)\s*$') {
            # Java properties escape the drive colon and the separators.
            return $Matches[1] -replace '\\:', ':' -replace '\\\\', '\'
        }
    }
    return $null
}

function Resolve-SdkRoot {
    if ($SdkRoot) { return [System.IO.Path]::GetFullPath($SdkRoot) }

    $configured = Get-ConfiguredSdkDir
    if ($configured) {
        Write-Skip "Reusing sdk.dir from local.properties."
        return [System.IO.Path]::GetFullPath($configured)
    }
    foreach ($name in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') {
        $value = [Environment]::GetEnvironmentVariable($name)
        if ($value) {
            Write-Skip "Reusing $name."
            return [System.IO.Path]::GetFullPath($value)
        }
    }
    return (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
}

function Assert-FreeSpace {
    param([string] $Path)

    $qualifier = Split-Path -Qualifier ([System.IO.Path]::GetFullPath($Path))
    $drive = Get-PSDrive -Name $qualifier.TrimEnd(':') -ErrorAction SilentlyContinue
    if (-not $drive) {
        throw "Drive $qualifier does not exist. Pass -SdkRoot a path on a drive this machine has."
    }

    $freeGb = [math]::Round($drive.Free / 1GB, 1)
    if ($freeGb -lt $RequiredFreeGb) {
        throw "Only $freeGb GB free on $qualifier but about $RequiredFreeGb GB is needed. Re-run with -SdkRoot on another drive, for example: .\setup-env.ps1 -SdkRoot D:\Android\Sdk"
    }
    Write-Skip "$freeGb GB free on $qualifier."
}

function Get-CompileSdk {
    $buildFile = Join-Path $RepoRoot 'app\build.gradle.kts'
    if (Test-Path $buildFile) {
        $match = Select-String -Path $buildFile -Pattern '^\s*compileSdk\s*=\s*(\d+)' | Select-Object -First 1
        if ($match) { return [int]$match.Matches[0].Groups[1].Value }
    }
    throw "Could not read compileSdk from app\build.gradle.kts."
}

function Install-CmdlineTools {
    param([string] $Root)

    $sdkmanager = Join-Path $Root 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (Test-Path $sdkmanager) {
        Write-Skip 'Command-line tools already present.'
        return $sdkmanager
    }

    $archive = Join-Path ([System.IO.Path]::GetTempPath()) "android-cmdline-tools-$PID.zip"
    $staging = Join-Path ([System.IO.Path]::GetTempPath()) "android-cmdline-tools-$PID"
    try {
        Write-Host "    Downloading command-line tools (about 150 MB)..."
        $progress = $ProgressPreference
        $ProgressPreference = 'SilentlyContinue'
        try {
            Invoke-WebRequest -Uri $CmdlineToolsUrl -OutFile $archive -UseBasicParsing
        } finally {
            $ProgressPreference = $progress
        }

        Write-Host '    Extracting...'
        Expand-Archive -Path $archive -DestinationPath $staging -Force

        # The archive unpacks to "cmdline-tools\", but sdkmanager only resolves its own
        # location correctly when it sits in "cmdline-tools\latest\".
        $target = Join-Path $Root 'cmdline-tools'
        New-Item -ItemType Directory -Force -Path $target | Out-Null
        Move-Item -Path (Join-Path $staging 'cmdline-tools') -Destination (Join-Path $target 'latest') -Force
    } finally {
        Remove-Item $archive -Force -ErrorAction SilentlyContinue
        Remove-Item $staging -Recurse -Force -ErrorAction SilentlyContinue
    }

    if (-not (Test-Path $sdkmanager)) { throw "Extraction finished but $sdkmanager is missing." }
    Write-Ok 'Command-line tools installed.'
    return $sdkmanager
}

function Install-SdkPackages {
    param(
        [string] $Sdkmanager,
        [string] $Root,
        [string[]] $Packages,
        [string] $JdkHome
    )

    $missing = @($Packages | Where-Object { -not (Test-SdkPackage -Root $Root -Package $_) })
    if ($missing.Count -eq 0) {
        Write-Skip 'All SDK packages already installed.'
        return
    }

    Write-Host "    Installing: $($missing -join ', ')"
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $JdkHome
    try {
        # sdkmanager prompts once to accept the SDK licence; "y" accepts only the
        # licences covering the packages requested here.
        $quoted = ($missing | ForEach-Object { '"' + $_ + '"' }) -join ' '
        $command = "echo y| `"$Sdkmanager`" --sdk_root=`"$Root`" $quoted"
        & $env:ComSpec /c $command | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "sdkmanager exited with code $LASTEXITCODE." }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }

    $stillMissing = @($Packages | Where-Object { -not (Test-SdkPackage -Root $Root -Package $_) })
    if ($stillMissing.Count -gt 0) { throw "sdkmanager reported success but these are missing: $($stillMissing -join ', ')" }
    Write-Ok 'SDK packages installed.'
}

function Test-SdkPackage {
    param([string] $Root, [string] $Package)

    # Package ids map onto directories: "platforms;android-34" -> "platforms\android-34".
    return Test-Path (Join-Path $Root ($Package -replace ';', '\'))
}

function Write-LocalProperties {
    param([string] $Root)

    # Java properties treat "\" and ":" as syntax, so both are escaped.
    $escaped = $Root -replace '\\', '\\' -replace ':', '\:'
    $line = "sdk.dir=$escaped"

    if ((Test-Path $LocalProperties) -and ((Get-ConfiguredSdkDir) -eq $Root)) {
        Write-Skip 'local.properties already points here.'
        return
    }

    $body = @()
    if (Test-Path $LocalProperties) {
        # Keep any other settings the developer put in this file.
        $body = @(Get-Content $LocalProperties | Where-Object { $_ -notmatch '^\s*sdk\.dir\s*=' })
    }
    $body += $line

    # No BOM: the Android Gradle plugin reads this file as plain ASCII.
    [System.IO.File]::WriteAllLines($LocalProperties, $body, (New-Object System.Text.UTF8Encoding($false)))
    Write-Ok "local.properties -> $Root"
}

# ---------------------------------------------------------------------------

Write-Host 'MemForce environment setup' -ForegroundColor White

Write-Step "Checking for a JDK $MinimumJdk or newer"
$jdk = Get-JdkHome
if (-not $jdk) {
    throw "No JDK $MinimumJdk or newer found. Install one (for example Microsoft Build of OpenJDK 21) and point JAVA_HOME at it, then re-run this script."
}
Write-Ok "$($jdk.Reported) at $($jdk.Home)"

Write-Step 'Resolving the SDK location'
$resolvedRoot = Resolve-SdkRoot
Write-Ok $resolvedRoot
if (-not (Test-Path $resolvedRoot)) {
    Assert-FreeSpace -Path $resolvedRoot
    New-Item -ItemType Directory -Force -Path $resolvedRoot | Out-Null
}

Write-Step 'Installing the Android command-line tools'
$sdkmanagerPath = Install-CmdlineTools -Root $resolvedRoot

Write-Step 'Installing the SDK packages'
$compileSdk = Get-CompileSdk
$packages = @('platform-tools', "platforms;android-$compileSdk", "build-tools;$BuildToolsVersion")
Write-Skip "compileSdk $compileSdk, read from app\build.gradle.kts. Build-tools $BuildToolsVersion, required by the Android Gradle plugin."
Install-SdkPackages -Sdkmanager $sdkmanagerPath -Root $resolvedRoot -Packages $packages -JdkHome $jdk.Home

Write-Step 'Pointing the project at the SDK'
Write-LocalProperties -Root $resolvedRoot

if ($SkipVerify) {
    Write-Step 'Skipping verification (-SkipVerify)'
} else {
    Write-Step 'Building and running the unit tests'
    $previousJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $jdk.Home
    try {
        Push-Location $RepoRoot
        try {
            & (Join-Path $RepoRoot 'gradlew.bat') assembleDebug testDebugUnitTest --console=plain
            if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE." }
        } finally {
            Pop-Location
        }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }
    Write-Ok 'Build and unit tests passed.'
}

Write-Host "`nEnvironment ready." -ForegroundColor Green
Write-Host "  SDK          $resolvedRoot"
Write-Host "  JDK          $($jdk.Home)"
Write-Host "  Build        .\gradlew.bat assembleDebug"
Write-Host "  Unit tests   .\gradlew.bat testDebugUnitTest"
