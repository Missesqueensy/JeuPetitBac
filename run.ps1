#!/usr/bin/env pwsh
Write-Host "Preparing Java environment for PetitBac project..."

# Check for javac
$javac = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javac) {
    Write-Error "javac not found. Install a JDK (11+) and add javac to PATH. Set JAVA_HOME if needed."; exit 1
}

# Ensure lib folder
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$libDir = Join-Path $scriptDir 'lib'
if (-not (Test-Path $libDir)) { New-Item -ItemType Directory -Path $libDir | Out-Null }

# Try to auto-detect Maven installs under Program Files and add to PATH for this session
try {
    $mavenCandidates = Get-ChildItem 'C:\Program Files' -Directory -Filter 'apache-maven*' -ErrorAction SilentlyContinue | ForEach-Object { Join-Path $_.FullName 'bin' }
    foreach ($md in $mavenCandidates) {
        if (Test-Path (Join-Path $md 'mvn.cmd')) { $env:Path = "$md;$env:Path"; break }
    }
} catch {
    # ignore
}

# Download Gson if missing
$gsonJar = Join-Path $libDir 'gson-2.10.1.jar'
if (-not (Test-Path $gsonJar)) {
    Write-Host "gson not found in lib/, downloading gson-2.10.1.jar..."
    $gsonUrl = 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar'
    try {
        Invoke-WebRequest -Uri $gsonUrl -OutFile $gsonJar -UseBasicParsing -ErrorAction Stop
        Write-Host "Downloaded gson to $gsonJar"
    } catch {
        Write-Warning "Failed to download Gson automatically. Please download gson jar and place it in lib/. Suggested URL: $gsonUrl"
    }
} else { Write-Host "Gson already present." }

# Check for jade.jar
$jadeJar = Join-Path $libDir 'jade.jar'
if (-not (Test-Path $jadeJar)) {
    Write-Warning "jade.jar not found in lib/. Please download JADE (binary distribution) and copy jade.jar into the lib folder."
    Write-Host "Suggested: download jade-bin from http://jade.tilab.com/download/ and extract jade.jar into lib/."
    while (-not (Test-Path $jadeJar)) {
        Write-Host "Waiting for jade.jar to appear in $libDir."
        Write-Host "If you want to download it automatically, visit: http://jade.tilab.com/download/ or place the file manually."
        Read-Host "Once jade.jar is in lib, press Enter to continue (Ctrl+C to abort)"
    }
    Write-Host "Detected jade.jar in lib/. Continuing..."
}

# Prefer Maven if available to create a reproducible environment
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Maven detected. Building with Maven..."

    # Ensure dictionary.json is copied into src/main/resources for Maven builds
    $resDir = Join-Path $scriptDir 'src\main\resources'
    if (-not (Test-Path $resDir)) { New-Item -ItemType Directory -Path $resDir -Force | Out-Null }
    $dictSrc = Join-Path $scriptDir 'dictionary.json'
    if (Test-Path $dictSrc) {
        Copy-Item -Path $dictSrc -Destination $resDir -Force
    }

    # Run mvn package
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { Write-Error "Maven build failed."; exit $LASTEXITCODE }

    # Run JADE via exec:java (exec plugin configured in pom.xml)
    & mvn -q exec:java
} else {
    Write-Host "Maven not found. Falling back to direct javac/java compilation."
    $outDir = Join-Path $scriptDir 'out'
    if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
    New-Item -ItemType Directory -Path $outDir | Out-Null

    Write-Host "Compiling Java sources..."
    $sources = Get-ChildItem -Path $scriptDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    if (-not $sources) { Write-Error "No .java files found in the project."; exit 1 }

    $cp = "$scriptDir;" + (Join-Path $libDir '*')
    & javac -cp $cp -d $outDir $sources
    if ($LASTEXITCODE -ne 0) { Write-Error "Compilation failed."; exit $LASTEXITCODE }

    Write-Host "Compilation succeeded. Running agents with JADE..."
    Write-Host "Ensure the JADE gui opens (it may require a display)."

    # Launch JADE with 2 players and the referee
    & java -cp ("$outDir;" + (Join-Path $libDir '*')) jade.Boot -gui Joueur1:PetitBac.AgentJoueur Joueur2:PetitBac.AgentJoueur ArbitreAgent:PetitBac.AgentArbitre
}
