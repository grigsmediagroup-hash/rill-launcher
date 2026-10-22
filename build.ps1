param(
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$Jdk = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$tools = Join-Path $Sdk 'build-tools\36.0.0'
$androidJar = Join-Path $Sdk 'platforms\android-34\android.jar'
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
$output = Join-Path $project 'build'
$classes = Join-Path $output 'classes'
$dex = Join-Path $output 'dex'
$generated = Join-Path $output 'generated'
$dist = Join-Path $project 'dist'
$signing = Join-Path $project 'signing'
New-Item -ItemType Directory -Force $classes,$dex,$generated,$dist,$signing | Out-Null
function Check([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed ($LASTEXITCODE)" } }
& "$tools\aapt.exe" package -f -m -J $generated -M "$project\app\src\main\AndroidManifest.xml" -S "$project\app\src\main\res" -I $androidJar -F "$output\resources.apk"
Check 'Resources'
$sources = @(Get-ChildItem "$project\app\src\main\java",$generated -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $androidJar -d $classes @sources
Check 'Java compilation'
& "$Jdk\bin\jar.exe" cf "$output\classes.jar" -C $classes .
Check 'Class archive'
& "$tools\d8.bat" --min-api 21 --lib $androidJar --output $dex "$output\classes.jar"
Check 'Dex compilation'
Copy-Item -LiteralPath "$output\resources.apk" -Destination "$output\unsigned.apk" -Force
Push-Location $dex
try { & "$tools\aapt.exe" add "$output\unsigned.apk" classes.dex; Check 'APK packaging' } finally { Pop-Location }
& "$tools\zipalign.exe" -f -p 4 "$output\unsigned.apk" "$output\aligned.apk"
Check 'Alignment'
$key = Join-Path $signing 'personal.jks'
if (-not (Test-Path -LiteralPath $key)) {
    & "$Jdk\bin\keytool.exe" -genkeypair -keystore $key -storepass rill-personal -keypass rill-personal -alias rill -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Rill Personal,OU=Personal,O=Personal,C=IN'
    Check 'Personal signing key'
}
& "$tools\apksigner.bat" sign --ks $key --ks-key-alias rill --ks-pass pass:rill-personal --key-pass pass:rill-personal --v1-signing-enabled true --v2-signing-enabled true --out "$dist\Rill-0.1.0.apk" "$output\aligned.apk"
Check 'Signing'
& "$tools\apksigner.bat" verify --verbose "$dist\Rill-0.1.0.apk"
Check 'Signature verification'
& "$tools\aapt.exe" dump badging "$dist\Rill-0.1.0.apk"
Check 'Manifest verification'
Get-FileHash "$dist\Rill-0.1.0.apk" -Algorithm SHA256
