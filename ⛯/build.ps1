# 2026-11-22 Dmitrii Fediuk https://github.com/dmitrii-fediuk
# "Initialize standalone df.idea IntelliJ IDEA plugin repository":
# https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/1
# "Remove nested ProjectRootEntity instances covered by module content roots":
# https://github.com/dmitrii-fediuk/IntelliJ_IDEA/issues/2
param(
	[string]$IdeaHome = '',
	[string]$IdeaPropertiesFile = '',
	[switch]$Install,
	[switch]$SkipTests,
	[switch]$VerifyOnly
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
function New-DeterministicJar {
	param(
		[string]$OutputJar,
		[string]$ClassesDir,
		[string]$ResourcesDir
	)
	$parent = Split-Path -Parent $OutputJar
	if (-not (Test-Path -LiteralPath $parent)) {
		New-Item -ItemType Directory -Path $parent -Force | Out-Null
	}
	if (Test-Path -LiteralPath $OutputJar) {
		Remove-Item -LiteralPath $OutputJar -Force
	}
	$fixedTime = [DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
	$zip = [System.IO.Compression.ZipFile]::Open($OutputJar, [System.IO.Compression.ZipArchiveMode]::Create)
	try {
		$manifestText = "Manifest-Version: 1.0`r`nCreated-By: build.ps1`r`n"
		$manifestEntry = $zip.CreateEntry('META-INF/MANIFEST.MF', [System.IO.Compression.CompressionLevel]::Optimal)
		$manifestEntry.LastWriteTime = $fixedTime
		$writer = New-Object System.IO.StreamWriter($manifestEntry.Open(), [System.Text.UTF8Encoding]::new($false))
		try {
			$writer.Write($manifestText)
		}
		finally {
			$writer.Dispose()
		}
		$addTree = {
			param([string]$RootDir)
			$rootPrefix = (Resolve-Path -LiteralPath $RootDir).Path.TrimEnd('\') + '\'
			foreach ($file in (Get-ChildItem -LiteralPath $RootDir -Recurse -File | Sort-Object FullName)) {
				$rel = $file.FullName.Substring($rootPrefix.Length).Replace('\', '/')
				$entry = $zip.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
				$entry.LastWriteTime = $fixedTime
				$inStream = [System.IO.File]::OpenRead($file.FullName)
				$outStream = $entry.Open()
				try {
					$inStream.CopyTo($outStream)
				}
				finally {
					$outStream.Dispose()
					$inStream.Dispose()
				}
			}
		}
		& $addTree $ResourcesDir
		& $addTree $ClassesDir
	}
	finally {
		$zip.Dispose()
	}
}
function New-LocalUpdatePluginsXml {
	param(
		[string]$PluginXmlPath,
		[string]$OutputXmlPath,
		[string]$JarFileName = 'df-idea.jar'
	)
	$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
	$raw = [System.IO.File]::ReadAllText($PluginXmlPath, $utf8NoBom)
	$doc = [xml]$raw
	$root = $doc.'idea-plugin'
	if ($null -eq $root) {
		throw "Invalid plugin.xml: missing <idea-plugin> root in '$PluginXmlPath'"
	}
	$id = [string]$root.id
	$name = [string]$root.name
	$version = [string]$root.version
	$ideaVersion = $root.'idea-version'
	$sinceBuild = if ($null -ne $ideaVersion) { [string]$ideaVersion.GetAttribute('since-build') } else { '' }
	$untilBuild = if ($null -ne $ideaVersion) { [string]$ideaVersion.GetAttribute('until-build') } else { '' }
	if (-not $id -or -not $name -or -not $version -or -not $sinceBuild) {
		throw 'plugin.xml is missing required id/name/version/since-build metadata'
	}
	$escAttr = {
		param([string]$v)
		return [System.Security.SecurityElement]::Escape($v)
	}
	$lines = New-Object System.Collections.Generic.List[string]
	$lines.Add('<?xml version="1.0" encoding="UTF-8"?>')
	$lines.Add('<plugins>')
	$lines.Add(("`t" + '<plugin id="{0}" url="{1}" version="{2}">' -f (& $escAttr $id), (& $escAttr $JarFileName), (& $escAttr $version)))
	if ($untilBuild) {
		$lines.Add(("`t`t" + '<idea-version since-build="{0}" until-build="{1}"/>' -f (& $escAttr $sinceBuild), (& $escAttr $untilBuild)))
	}
	else {
		$lines.Add(("`t`t" + '<idea-version since-build="{0}"/>' -f (& $escAttr $sinceBuild)))
	}
	$lines.Add(("`t`t" + '<name>{0}</name>' -f (& $escAttr $name)))
	$vendorNode = $root.SelectSingleNode('vendor')
	if ($null -ne $vendorNode) {
		$vendorAttrs = ''
		$vendorUrl = $vendorNode.GetAttribute('url')
		if ($vendorUrl) {
			$vendorAttrs += (' url="{0}"' -f (& $escAttr $vendorUrl))
		}
		$vendorEmail = $vendorNode.GetAttribute('email')
		if ($vendorEmail) {
			$vendorAttrs += (' email="{0}"' -f (& $escAttr $vendorEmail))
		}
		$lines.Add(("`t`t" + '<vendor{0}>{1}</vendor>' -f $vendorAttrs, (& $escAttr $vendorNode.InnerText.Trim())))
	}
	foreach ($depNode in $root.SelectNodes('depends')) {
		$depOptional = $depNode.GetAttribute('optional')
		if ($depOptional -and $depOptional.Equals('true', [System.StringComparison]::OrdinalIgnoreCase)) {
			continue
		}
		$depText = $depNode.InnerText.Trim()
		if ($depText) {
			$lines.Add(("`t`t" + '<depends>{0}</depends>' -f (& $escAttr $depText)))
		}
	}
	$descNode = $root.SelectSingleNode('description')
	if ($null -ne $descNode -and $descNode.InnerText) {
		$descText = $descNode.InnerText.Replace("`r`n", "`n").Trim()
		$safeCdata = $descText.Replace(']]>', ']]]]><![CDATA[>')
		$lines.Add("`t`t<description><![CDATA[$safeCdata]]></description>")
	}
	$lines.Add("`t</plugin>")
	$lines.Add('</plugins>')
	$parent = Split-Path -Parent $OutputXmlPath
	if (-not (Test-Path -LiteralPath $parent)) {
		New-Item -ItemType Directory -Path $parent -Force | Out-Null
	}
	$xmlContent = ($lines -join "`n") + "`n"
	[System.IO.File]::WriteAllText($OutputXmlPath, $xmlContent, $utf8NoBom)
}
function Register-PluginPathInIdeaProperties {
	param(
		[string]$PropertiesFile,
		[string]$JarPath,
		[string]$PluginRoot
	)
	$bookDir = [char]::ConvertFromUtf32(0x1F56E)
	$pluginDirSym = [char]::ConvertFromUtf32(0x1D40F)
	$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
	$normalizedJar = $JarPath.Replace('\', '/')
	$docRef = (Join-Path $PluginRoot ($bookDir + '\.md')).Replace('\', '/')
	$commentLine = "# 2026-11-22 $docRef"
	$parentDir = Split-Path -Parent $PropertiesFile
	if (-not (Test-Path -LiteralPath $parentDir)) {
		New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
	}
	$lines = New-Object System.Collections.Generic.List[string]
	if (Test-Path -LiteralPath $PropertiesFile) {
		$raw = [System.IO.File]::ReadAllText($PropertiesFile, $utf8NoBom)
		if (0 -lt $raw.Length) {
			foreach ($line in ($raw -split "`r?`n")) {
				$lines.Add($line)
			}
			while (0 -lt $lines.Count -and '' -eq $lines[$lines.Count - 1]) {
				$lines.RemoveAt($lines.Count - 1)
			}
		}
	}
	$propIndex = -1
	for ($i = 0; $i -lt $lines.Count; $i++) {
		if ($lines[$i] -match '^\s*plugin\.path\s*=') {
			$propIndex = $i
			break
		}
	}
	if (0 -gt $propIndex) {
		$lines.Add($commentLine)
		$lines.Add("plugin.path=$normalizedJar")
	}
	else {
		$commentPattern = '^\s*#\s*\d{4}-\d{2}-\d{2}\s+.*(Plugins/CopyReference|' + [regex]::Escape($pluginDirSym) + ')/' + [regex]::Escape($bookDir) + '/\.md\s*$'
		if (0 -lt $propIndex -and $lines[$propIndex - 1] -match $commentPattern) {
			$lines[$propIndex - 1] = $commentLine
		}
		$existingValue = ($lines[$propIndex] -replace '^\s*plugin\.path\s*=\s*', '').Trim()
		$entries = New-Object System.Collections.Generic.List[string]
		foreach ($part in ($existingValue -split '[;,]')) {
			$trimmed = $part.Trim()
			if (-not $trimmed) {
				continue
			}
			$normPart = $trimmed.Replace('\', '/')
			if ($normPart.EndsWith('/df-copy-reference.jar', [System.StringComparison]::OrdinalIgnoreCase)) {
				continue
			}
			if ($normPart.Equals($normalizedJar, [System.StringComparison]::OrdinalIgnoreCase)) {
				continue
			}
			$entries.Add($trimmed)
		}
		$entries.Add($normalizedJar)
		$lines[$propIndex] = 'plugin.path=' + ($entries -join ';')
	}
	$content = ($lines -join "`r`n") + "`r`n"
	[System.IO.File]::WriteAllText($PropertiesFile, $content, $utf8NoBom)
}
function Resolve-IdeaInstallation {
	param([string]$RequestedHome)
	if ($RequestedHome) {
		$infoPath = Join-Path $RequestedHome 'product-info.json'
		if (-not (Test-Path -LiteralPath $infoPath)) {
			throw "product-info.json not found under -IdeaHome '$RequestedHome'"
		}
		$info = Get-Content -LiteralPath $infoPath -Raw -Encoding UTF8 | ConvertFrom-Json
		$r = [PSCustomObject]@{
			Home = (Resolve-Path -LiteralPath $RequestedHome).Path
			Info = $info
		}
	}
	else {
		$candidates = @()
		$jbRoot = 'C:\Program Files\JetBrains'
		if (Test-Path -LiteralPath $jbRoot) {
			foreach ($dir in Get-ChildItem -LiteralPath $jbRoot -Directory -Filter 'IntelliJ IDEA*') {
				$infoPath = Join-Path $dir.FullName 'product-info.json'
				if (Test-Path -LiteralPath $infoPath) {
					$info = Get-Content -LiteralPath $infoPath -Raw -Encoding UTF8 | ConvertFrom-Json
					$candidates += [PSCustomObject]@{
						Home  = $dir.FullName
						Info  = $info
						Major = [int](($info.buildNumber -split '\.')[0])
						Minor = [int](($info.buildNumber -split '\.')[1])
					}
				}
			}
		}
		if (0 -eq $candidates.Count) {
			throw "No IntelliJ IDEA installation with product-info.json found under '$jbRoot'. Pass -IdeaHome explicitly."
		}
		$r = $candidates | Sort-Object Major, Minor -Descending | Select-Object -First 1
	}
	return $r
}
$sourceRoot = $PSScriptRoot
$pluginRoot = Split-Path -Parent $sourceRoot
$idea = Resolve-IdeaInstallation -RequestedHome $IdeaHome
$ideaLib = Join-Path $idea.Home 'lib'
$javac = Join-Path $idea.Home 'jbr\bin\javac.exe'
$java = Join-Path $idea.Home 'jbr\bin\java.exe'
if (-not (Test-Path -LiteralPath $javac)) {
	throw "Bundled javac not found at '$javac'"
}
$ideaLibWildcard = Join-Path $ideaLib '*'
$compileCp = $ideaLibWildcard
$srcMain = Join-Path $sourceRoot 'src\main\java'
$resMain = Join-Path $sourceRoot 'src\main\resources'
$pluginXmlPath = Join-Path $resMain 'META-INF\plugin.xml'
$srcTest = Join-Path $sourceRoot 'src\test\java'
$outDirSym = [string][char]0x227B
$outputJar = Join-Path $pluginRoot ($outDirSym + '\df-idea.jar')
$outputUpdateXml = Join-Path $pluginRoot ($outDirSym + '\updatePlugins.xml')
$tempWork = Join-Path ([System.IO.Path]::GetTempPath()) ('df-idea-' + [Guid]::NewGuid().ToString('N'))
$stagedMainSrc = Join-Path $tempWork 'src-main'
$stagedTestSrc = Join-Path $tempWork 'src-test'
$mainOut = Join-Path $tempWork 'main-classes'
$testOut = Join-Path $tempWork 'test-classes'
$stagedJar = Join-Path $tempWork 'df-idea.jar'
$stagedUpdateXml = Join-Path $tempWork 'updatePlugins.xml'
New-Item -ItemType Directory -Path $mainOut, $testOut -Force | Out-Null
try {
	Write-Output "IDEA_HOME=$($idea.Home)"
	Write-Output "IDEA_VERSION=$($idea.Info.version) ($($idea.Info.buildNumber))"
	Copy-Item -LiteralPath $srcMain -Destination $stagedMainSrc -Recurse -Force
	$mainSources = @(Get-ChildItem -LiteralPath $stagedMainSrc -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
	& $javac -encoding UTF-8 -Xlint:all -Werror -cp $compileCp -d $mainOut @mainSources
	if (0 -ne $LASTEXITCODE) {
		throw "Main compilation failed with exit code $LASTEXITCODE"
	}
	New-DeterministicJar -OutputJar $stagedJar -ClassesDir $mainOut -ResourcesDir $resMain
	New-LocalUpdatePluginsXml -PluginXmlPath $pluginXmlPath -OutputXmlPath $stagedUpdateXml -JarFileName 'df-idea.jar'
	$stagedJarHash = (Get-FileHash -LiteralPath $stagedJar -Algorithm SHA256).Hash.ToLowerInvariant()
	$stagedXmlHash = (Get-FileHash -LiteralPath $stagedUpdateXml -Algorithm SHA256).Hash.ToLowerInvariant()
	Write-Output "STAGED_JAR_SHA256=$stagedJarHash"
	Write-Output "STAGED_JAR_BYTES=$((Get-Item -LiteralPath $stagedJar).Length)"
	Write-Output "STAGED_UPDATE_XML_SHA256=$stagedXmlHash"
	Write-Output "STAGED_UPDATE_XML_BYTES=$((Get-Item -LiteralPath $stagedUpdateXml).Length)"
	if (-not $SkipTests) {
		Copy-Item -LiteralPath $srcTest -Destination $stagedTestSrc -Recurse -Force
		$testCp = "$mainOut;$ideaLibWildcard"
		$testSources = @(Get-ChildItem -LiteralPath $stagedTestSrc -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { $_.FullName })
		& $javac -encoding UTF-8 -Xlint:all -Werror -cp $testCp -d $testOut @testSources
		if (0 -ne $LASTEXITCODE) {
			throw "Test compilation failed with exit code $LASTEXITCODE"
		}
		$runCp = "$testOut;$mainOut;$ideaLibWildcard"
		$featureTests = @(
			'df.idea.feature.copyreference.ExclusionAwareVirtualFileQualifiedNameProviderTest',
			'df.idea.feature.pathmacros.MacroPropertiesLocatorTest',
			'df.idea.feature.pathmacros.MacroPropertiesParserTest',
			'df.idea.feature.pathmacros.MacroPathResolverTest',
			'df.idea.feature.pathmacros.IdePathMacroIntegrationTest',
			'df.idea.feature.projectroots.NestedProjectRootMatcherTest',
			'df.idea.feature.projectroots.NestedProjectRootCleanerTest',
			'df.idea.feature.projectroots.IdeProjectRootIntegrationTest',
			'df.idea.feature.updatesource.LocalPluginRepositoryLocatorTest',
			'df.idea.feature.updatesource.LocalUpdateSourceRegistrarTest'
		)
		foreach ($testClass in $featureTests) {
			& $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $runCp $testClass
			if (0 -ne $LASTEXITCODE) {
				throw "Test suite $testClass failed with exit code $LASTEXITCODE"
			}
		}
		& $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $runCp 'df.idea.feature.updatesource.IdeUpdateSourceIntegrationTest' $stagedJar
		if (0 -ne $LASTEXITCODE) {
			throw "Test suite IdeUpdateSourceIntegrationTest failed with exit code $LASTEXITCODE"
		}
		& $java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $runCp 'df.idea.plugin.PluginDescriptorVerificationTest' $stagedJar
		if (0 -ne $LASTEXITCODE) {
			throw "Plugin descriptor verification failed on JAR with exit code $LASTEXITCODE"
		}
	}
	if (-not $VerifyOnly) {
		$outParent = Split-Path -Parent $outputJar
		if (-not (Test-Path -LiteralPath $outParent)) {
			New-Item -ItemType Directory -Path $outParent -Force | Out-Null
		}
		try {
			Copy-Item -LiteralPath $stagedJar -Destination $outputJar -Force
		}
		catch [System.IO.IOException] {
			$runningIdea = @(Get-Process -Name 'idea64' -ErrorAction SilentlyContinue)
			if (0 -lt $runningIdea.Count) {
				$pids = ($runningIdea | ForEach-Object { "$($_.Id) ($($_.Path))" }) -join ', '
				throw "Cannot replace '$outputJar' because a running IntelliJ IDEA process holds a file mapping on it: $pids. Close IntelliJ IDEA and re-run build.ps1."
			}
			throw
		}
		Copy-Item -LiteralPath $stagedUpdateXml -Destination $outputUpdateXml -Force
		$jarHash = (Get-FileHash -LiteralPath $outputJar -Algorithm SHA256).Hash.ToLowerInvariant()
		$updateXmlHash = (Get-FileHash -LiteralPath $outputUpdateXml -Algorithm SHA256).Hash.ToLowerInvariant()
		Write-Output "BUILT_JAR=$outputJar"
		Write-Output "BUILT_SHA256=$jarHash"
		Write-Output "BUILT_UPDATE_XML=$outputUpdateXml"
		Write-Output "BUILT_UPDATE_XML_SHA256=$updateXmlHash"
	}
	if ($Install -and -not $VerifyOnly) {
		$dataDir = $idea.Info.dataDirectoryName
		$customPropertiesSpecified = [bool]$IdeaPropertiesFile
		if (-not $IdeaPropertiesFile) {
			if (-not $dataDir) {
				throw 'dataDirectoryName missing in product-info.json; pass -IdeaPropertiesFile explicitly.'
			}
			$IdeaPropertiesFile = Join-Path $env:APPDATA "JetBrains\$dataDir\idea.properties"
		}
		if ($dataDir) {
			foreach ($legacyName in @('df-copy-reference.jar', 'df-idea.jar')) {
				$legacyPluginsJar = Join-Path $env:APPDATA "JetBrains\$dataDir\plugins\$legacyName"
				if (Test-Path -LiteralPath $legacyPluginsJar) {
					Remove-Item -LiteralPath $legacyPluginsJar -Force
				}
			}
		}
		Register-PluginPathInIdeaProperties -PropertiesFile $IdeaPropertiesFile -JarPath $outputJar -PluginRoot $pluginRoot
		$runningIdea = @(Get-Process -Name 'idea64' -ErrorAction SilentlyContinue)
		Write-Output "REGISTERED_IN=$IdeaPropertiesFile"
		Write-Output "PLUGIN_PATH=$($outputJar.Replace('\', '/'))"
		if (0 -lt $runningIdea.Count) {
			Write-Output 'RESTART_REQUIRED=true'
		}
		else {
			Write-Output 'RESTART_REQUIRED=false'
		}
		if (-not $SkipTests) {
			$runCp = "$testOut;$mainOut;$ideaLibWildcard"
			$javaArgs = @(
				'-Dfile.encoding=UTF-8',
				'-Dstdout.encoding=UTF-8',
				'-Didea.vendor.name=JetBrains',
				"-Didea.home.path=$($idea.Home)"
			)
			if ($dataDir) {
				$javaArgs += "-Didea.paths.selector=$dataDir"
			}
			if ($customPropertiesSpecified) {
				$javaArgs += "-Didea.properties.file=$IdeaPropertiesFile"
			}
			$javaArgs += @('-cp', $runCp, 'df.idea.plugin.PluginDescriptorVerificationTest', '--from-idea-properties')
			& $java @javaArgs
			if (0 -ne $LASTEXITCODE) {
				throw "End-to-end idea.properties plugin.path verification failed with exit code $LASTEXITCODE"
			}
		}
	}
}
finally {
	if (Test-Path -LiteralPath $tempWork) {
		Remove-Item -LiteralPath $tempWork -Recurse -Force -ErrorAction SilentlyContinue
	}
}
