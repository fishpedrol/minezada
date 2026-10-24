<#
Teste visual automatico do Cultivo (so Windows, precisa de tela).

Copia o mundo de run\world (criado por .\gradlew.bat runServer) para run\saves\cultivo_probe, instala o
datapack de cena (tools\visual-probe\datapack), abre o runClient direto nesse mundo e tira prints (F2) em cada
etapa: plantas, estacoes, estufa, vitrine de itens, inventario, item na mao e fumar. Os prints vao para
-OutDir. O datapack so existe no mundo de teste; nada disso entra no .jar do mod.

Uso, na pasta do projeto:
  powershell -ExecutionPolicy Bypass -File tools\visual-probe\run-probe.ps1 -OutDir C:\caminho\prints [-Lang pt_br]
#>
param(
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string]$Lang = 'pt_br',
    [int]$Width = 1280,
    [int]$Height = 720
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$run = Join-Path $root 'run'
if (-not (Test-Path (Join-Path $run 'world\level.dat'))) {
    throw 'Falta run\world. Rode .\gradlew.bat runServer uma vez (aceite a EULA) e desligue com stop.'
}
if (-not $env:JAVA_HOME) { throw 'Configure o JAVA_HOME para o JDK 25 (GUIA_INICIANTE.md, secao 2.2).' }

Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class ProbeInput {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extra);
}
'@

# 1. Mundo de teste com o datapack
$save = Join-Path $run 'saves\cultivo_probe'
if (Test-Path $save) { Remove-Item -Recurse -Force $save }
Copy-Item -Recurse (Join-Path $run 'world') $save
Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $save 'session.lock')
Copy-Item -Recurse (Join-Path $PSScriptRoot 'datapack') (Join-Path $save 'datapacks\probe')

# 2. Opcoes do cliente (restauradas no fim)
$opt = Join-Path $run 'options.txt'
$optBackup = $null
if (Test-Path $opt) {
    $optBackup = [IO.File]::ReadAllLines($opt)
    $set = @{ 'pauseOnLostFocus' = 'false'; 'lang' = $Lang; 'tutorialStep' = 'none'; 'chatVisibility' = '2'; 'onboardAccessibility' = 'false' }
    $lines = foreach ($l in $optBackup) { $k = $l.Split(':')[0]; if ($set.ContainsKey($k)) { "${k}:$($set[$k])" } else { $l } }
    [IO.File]::WriteAllLines($opt, [string[]]$lines)
}

# 3. Abrir o jogo direto no mundo
$log = Join-Path $run 'logs\latest.log'
$started = Get-Date
$shotsDir = Join-Path $run 'screenshots'
New-Item -ItemType Directory -Force $shotsDir | Out-Null
$before = @(Get-ChildItem $shotsDir -File | ForEach-Object Name)
$gradleLog = Join-Path $OutDir 'gradle-client.log'
New-Item -ItemType Directory -Force $OutDir | Out-Null
$proc = Start-Process -FilePath (Join-Path $root 'gradlew.bat') -WorkingDirectory $root -PassThru -WindowStyle Minimized `
    -RedirectStandardOutput $gradleLog -RedirectStandardError "$gradleLog.err" -ArgumentList @(
    'runClient', '--console=plain', "`"--args=--quickPlaySingleplayer cultivo_probe --width $Width --height $Height`"")

function Wait-Log([string]$pattern, [int]$timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if ((Test-Path $log) -and (Get-Item $log).LastWriteTime -gt $started) {
            if (Select-String -Path $log -Pattern $pattern -Quiet) { return }
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Tempo esgotado esperando '$pattern' no log"
}
function Get-GameWindow {
    $w = Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
    if (-not $w) { throw 'Janela do Minecraft nao encontrada' }
    return $w.MainWindowHandle
}
function Focus { $h = Get-GameWindow; [void][ProbeInput]::ShowWindow($h, 9); [void][ProbeInput]::SetForegroundWindow($h); Start-Sleep -Milliseconds 300 }
function Key([byte]$vk) { Focus; [ProbeInput]::keybd_event($vk, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60; [ProbeInput]::keybd_event($vk, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 250 }
function Shot([string]$name) { Key 0x71; Start-Sleep -Milliseconds 800; $script:names += $name }
$script:names = @()
$F1 = 0x70; $F5 = 0x74; $E = 0x45; $ESC = 0x1B

try {
    # O mundo com o datapack de teste abre o aviso "ajustes experimentais"; Tab, Tab, Enter escolhe
    # "Sei o que estou fazendo!".
    Wait-Log 'Found new data pack' 600
    $deadline = (Get-Date).AddSeconds(90)
    while (-not (Select-String -Path $log -Pattern 'logged in with entity id' -Quiet)) {
        if ((Get-Date) -gt $deadline) { throw 'O jogo nao entrou no mundo de teste' }
        Start-Sleep 4
        if (-not (Select-String -Path $log -Pattern 'logged in with entity id' -Quiet)) { Key 0x09; Key 0x09; Key 0x0D }
    }
    Focus

    Wait-Log 'probe:cam_crops' 120;      Start-Sleep 4; Key $F1; Shot '01_plantas'; Key $F1
    Wait-Log 'probe:cam_stations' 60;   Start-Sleep 3; Key $F1; Shot '02_estacoes'; Key $F1
    Wait-Log 'probe:cam_greenhouse' 60; Start-Sleep 3; Key $F1; Shot '03_estufa'; Key $F1
    Wait-Log 'probe:cam_items' 60;      Start-Sleep 3; Key $F1; Shot '04_vitrine_itens'; Key $F1
    Wait-Log 'probe:inventory' 60;      Start-Sleep 2; Key $E; Start-Sleep 1; Shot '05_inventario'; Key $ESC
    Wait-Log 'probe:held' 60;           Start-Sleep 2; Shot '06_bong_primeira_pessoa'
    Key $F5; Key $F5; Start-Sleep 1; Shot '07_bong_terceira_pessoa'; Key $F5
    Wait-Log 'probe:smoke' 60;          Start-Sleep 2
    Focus
    [ProbeInput]::mouse_event(0x0008, 0, 0, 0, [UIntPtr]::Zero)   # botao direito pressionado
    Start-Sleep -Milliseconds 700; Shot '08_fumando'
    Start-Sleep -Milliseconds 1500
    [ProbeInput]::mouse_event(0x0010, 0, 0, 0, [UIntPtr]::Zero)   # solta
    Start-Sleep 1; Shot '09_efeito_chapado'
    Key $F5; Key $F5; Start-Sleep 1; Shot '10_fumaca_terceira_pessoa'; Key $F5
    Key $E; Start-Sleep 1; Shot '11_inventario_efeitos'; Key $ESC
}
finally {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match [regex]::Escape($root) } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    if ($optBackup) { [IO.File]::WriteAllLines($opt, [string[]]$optBackup) }
    $new = @(Get-ChildItem $shotsDir -File | Where-Object { $before -notcontains $_.Name } | Sort-Object LastWriteTime)
    for ($i = 0; $i -lt $new.Count; $i++) {
        $n = if ($i -lt $script:names.Count) { $script:names[$i] } else { "extra_$i" }
        Copy-Item $new[$i].FullName (Join-Path $OutDir "$n.png")
    }
    Copy-Item $log (Join-Path $OutDir 'client-latest.log') -ErrorAction SilentlyContinue
    "Prints: $($new.Count) em $OutDir"
}
