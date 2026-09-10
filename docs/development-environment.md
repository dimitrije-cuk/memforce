# Development environment

MemForce is a plain Android Gradle project, so a machine can build it once three things are
true: a JDK 17 or newer is installed, the Android SDK holds the packages the build asks for,
and `local.properties` tells Gradle where that SDK is.

[setup-env.ps1](../setup-env.ps1) does the second and third for you and checks the first.

```powershell
.\setup-env.ps1
```

The script is idempotent — it skips whatever is already in place, so re-running it after a
toolchain change is cheap and safe.

## What the script needs from you

| Requirement | Why | If it is missing |
| --- | --- | --- |
| JDK 17 or newer, found through `JAVA_HOME` | The Android Gradle plugin refuses to run on anything older | The script stops and tells you to install one. It will not install a JDK for you |
| Internet access | The SDK packages and Gradle dependencies are downloaded | The download step fails |
| ~3 GB free disk | SDK ~0.4 GB, Gradle caches and build output take the rest | The script stops before downloading and suggests `-SdkRoot` on another drive |

A JRE is not enough. The check looks for `bin\javac.exe` and reads its version, so a
JRE-only `JAVA_HOME` is rejected with a clear message rather than failing later inside Gradle.

## Options

| Option | Effect |
| --- | --- |
| `-SdkRoot <path>` | Installs the SDK somewhere specific. Use this when the system drive is short on space |
| `-SkipVerify` | Skips the closing build and unit-test run |

When `-SdkRoot` is omitted the script reuses an SDK you already have, checking in this order:

1. `sdk.dir` in `local.properties`
2. `ANDROID_HOME`
3. `ANDROID_SDK_ROOT`
4. `%LOCALAPPDATA%\Android\Sdk` — only used when none of the above are set

That order means running the script on a machine that already has Android Studio adopts the
existing SDK instead of downloading a second copy.

## What it changes

Everything the script writes is confined to the SDK directory and the repository's own
gitignored `local.properties`.

| Change | Location | Size | Reversible |
| --- | --- | --- | --- |
| Command-line tools | `<SdkRoot>\cmdline-tools\latest` | ~150 MB | Delete `<SdkRoot>` |
| `platform-tools` | `<SdkRoot>\platform-tools` | ~15 MB | `sdkmanager --uninstall` |
| `platforms;android-34` | `<SdkRoot>\platforms` | ~60 MB | `sdkmanager --uninstall` |
| `build-tools;36.0.0` | `<SdkRoot>\build-tools` | ~160 MB | `sdkmanager --uninstall` |
| Accepted SDK licence | `<SdkRoot>\licenses\android-sdk-license` | 42 B | Delete the file |
| `sdk.dir=…` | `local.properties` | 27 B | Delete the file |

It explicitly does **not**:

- edit `PATH`,
- set `ANDROID_HOME`, `ANDROID_SDK_ROOT` or any other persistent environment variable,
- modify `JAVA_HOME` or install a JDK,
- write outside `<SdkRoot>` and the repository,
- require administrator rights.

Gradle separately populates its own shared cache in `%USERPROFILE%\.gradle` and writes build
output to `app\build`. Both are normal Gradle behaviour rather than something the script does,
and both are safe to delete.

### Temporary files

The command-line tools archive is downloaded to `%TEMP%`, unpacked, and deleted again in a
`finally` block, so an interrupted run does not leave a 150 MB file behind.

## Which package versions, and why

The script does not hardcode a package list. Two of the three entries are derived, because
they come from different places and drift apart:

| Package | Comes from | Kept in sync by |
| --- | --- | --- |
| `platforms;android-<N>` | `compileSdk` in [app/build.gradle.kts](../app/build.gradle.kts) | Read out of the build file at run time |
| `build-tools;<version>` | The Android Gradle plugin version | Pinned in the script as `$BuildToolsVersion` |
| `platform-tools` | Unversioned | — |

The build-tools version is the trap. It tracks **the AGP version, not `compileSdk`**: AGP 9.2.1
uses build-tools 36.0.0 even though `compileSdk` is 34. Deriving it from `compileSdk` produces
34.0.0, which the build never opens; AGP then quietly downloads 36.0.0 mid-build instead.

If AGP is upgraded in [gradle/libs.versions.toml](../gradle/libs.versions.toml), watch the next
build for a line like:

```
"Install Android SDK Build-Tools 36 v.36.0.0" complete.
```

That line means `$BuildToolsVersion` in the script is stale — set it to the version named there.
Nothing breaks in the meantime, because AGP installs what it needs on its own; you just lose the
guarantee that a fresh setup is complete before the first build starts.

## Doing it by hand

The script automates exactly these steps.

```powershell
# 1. A JDK 17+ must be on the machine and JAVA_HOME must point at it.
$env:JAVA_HOME = 'C:\path\to\jdk-21'

# 2. Unpack the command-line tools so that sdkmanager sits in cmdline-tools\latest\bin.
#    sdkmanager only resolves its own location correctly from that exact layout.
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' -OutFile "$env:TEMP\cmdline-tools.zip"
Expand-Archive "$env:TEMP\cmdline-tools.zip" "$env:TEMP\cmdline-tools"
New-Item -ItemType Directory -Force "$sdk\cmdline-tools"
Move-Item "$env:TEMP\cmdline-tools\cmdline-tools" "$sdk\cmdline-tools\latest"

# 3. Install the packages, accepting the licence prompt.
cmd /c "echo y| `"$sdk\cmdline-tools\latest\bin\sdkmanager.bat`" --sdk_root=`"$sdk`" platform-tools `"platforms;android-34`" `"build-tools;36.0.0`""

# 4. Point the project at the SDK. Java properties escape "\" and ":".
"sdk.dir=$($sdk -replace '\\','\\' -replace ':','\:')" | Set-Content local.properties -Encoding ascii

# 5. Verify.
.\gradlew.bat assembleDebug testDebugUnitTest
```

## Undoing it

```powershell
Remove-Item -Recurse -Force <SdkRoot>                    # the whole SDK, licence included
Remove-Item .\local.properties                           # from the repository root
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle"   # optional: shared Gradle cache
```

Removing the SDK affects any other project pointing at the same directory, so check before
deleting a shared location such as `%LOCALAPPDATA%\Android\Sdk`.

## Troubleshooting

**`SDK location not found`** — `local.properties` is missing or points at a directory that no
longer exists. Re-run `.\setup-env.ps1`.

**`cannot be loaded because running scripts is disabled`** — PowerShell's execution policy is
blocking the script. Run it without changing the machine's policy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-env.ps1
```

**`No JDK 17 or newer found`** — `JAVA_HOME` is unset, points at a JRE, or points at Java 8.
A `java` on `PATH` is not consulted, only `JAVA_HOME`, because that is what Gradle itself uses.

**Diagram regeneration is not covered here.** [database-erd.svg](database/database-erd.svg) is
rendered from the `.puml` beside it with PlantUML and Graphviz. That is only needed when the
schema changes, so it is deliberately left out of the build environment.
