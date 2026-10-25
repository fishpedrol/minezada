<#
Teste visual automatico do Cultivo (so Windows, precisa de tela).

Copia o mundo de run\world (criado por .\gradlew.bat runServer) para um mundo de teste, instala o datapack de cena
(tools\visual-probe\datapack), abre o runClient direto nesse mundo e tira prints (F2) em cada etapa: plantas nos 8
estagios e uma de cada familia de cor, estacoes vazias/trabalhando/prontas (placa da prensa animada), bandeja, pote e
cinzeiro, estufinha, as geneticas, produtos e artigos, modelos na mao, dicas (tooltips) no inventario, abas do criativo,
cliques de verdade na bandeja, no suporte, na peneira e na estufinha, e fumar cada aparelho em 1a e 3a pessoa. As legendas de som
ficam ligadas durante o teste. No fim procura no log problemas de modelo, textura e som (log-problems.txt). O datapack
so existe no mundo de teste; nada disso entra no .jar do mod. Nao mexa no mouse nem no teclado enquanto roda.

-Multiplayer: sobe tambem o servidor dedicado (runServer) com uma copia do mundo e o cliente entra nele por
127.0.0.1. Pega erros que o modo um jogador esconde (no single os dados nem passam pela rede). So durante o teste o
run\server.properties fica em modo offline (o cliente de desenvolvimento nao tem conta), ligado so em 127.0.0.1, sem
whitelist e sem protecao de spawn; o arquivo original volta no fim. Nada disso muda o mod.

Uso, na pasta do projeto:
  powershell -ExecutionPolicy Bypass -File tools\visual-probe\run-probe.ps1 -OutDir C:\caminho\prints [-Lang pt_br] [-Multiplayer]
#>
param(
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string]$Lang = 'pt_br',
    [int]$Width = 1280,
    [int]$Height = 720,
    [switch]$Multiplayer
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$run = Join-Path $root 'run'
$OutDir = [IO.Path]::GetFullPath($OutDir)
if (-not (Test-Path (Join-Path $run 'world\level.dat'))) {
    throw 'Falta run\world. Rode .\gradlew.bat runServer uma vez (aceite a EULA) e desligue com stop.'
}
if (-not $env:JAVA_HOME) { throw 'Configure o JAVA_HOME para o JDK 25 (GUIA_INICIANTE.md, secao 2.2).' }
New-Item -ItemType Directory -Force $OutDir | Out-Null

Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class CultivoProbeInput {
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X; public int Y; }
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extra);
}
'@
# Coordenadas em pixels fisicos mesmo com a escala do Windows acima de 100%
[void][CultivoProbeInput]::SetProcessDPIAware()

# ---------- Arquivos mexidos so durante o teste ----------
# Copia de seguranca ao lado (.probe-bak; .probe-new quando o arquivo nao existia). Se um teste anterior foi
# interrompido, a copia ainda esta la e volta antes de tudo.
function Restore-File([string]$path) {
    $bak = "$path.probe-bak"; $new = "$path.probe-new"
    if (Test-Path $bak) { Copy-Item -Force $bak $path; Remove-Item -Force $bak }
    if (Test-Path $new) { Remove-Item -Force -ErrorAction SilentlyContinue $path; Remove-Item -Force $new }
}
function Backup-File([string]$path) {
    Restore-File $path
    if (Test-Path $path) { Copy-Item -Force $path "$path.probe-bak" } else { [IO.File]::WriteAllText("$path.probe-new", '') }
}
# Troca valores de chave (key:valor ou key=valor), acrescentando as chaves que faltam
function Set-Keys([string]$path, [string]$sep, $values) {
    $lines = New-Object System.Collections.Generic.List[string]
    if (Test-Path $path) { $lines.AddRange([string[]][IO.File]::ReadAllLines($path)) }
    $done = @{}
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $k = $lines[$i].Split($sep)[0]
        if ($values.Contains($k)) { $lines[$i] = "$k$sep$($values[$k])"; $done[$k] = $true }
    }
    foreach ($k in $values.Keys) { if (-not $done.ContainsKey($k)) { $lines.Add("$k$sep$($values[$k])") } }
    [IO.File]::WriteAllLines($path, $lines.ToArray())
}

# ---------- Logs ----------
function Read-Shared([string]$path) {
    if (-not (Test-Path $path)) { return '' }
    try {
        $fs = [IO.File]::Open($path, 'Open', 'Read', 'ReadWrite, Delete')
        try { return (New-Object IO.StreamReader($fs, [Text.Encoding]::UTF8)).ReadToEnd() } finally { $fs.Dispose() }
    } catch { return '' }
}
$failRx = '#@!@# Game crashed|Crash report saved to|BUILD FAILED|compileJava FAILED|FAILED TO BIND|Failed to start the minecraft server'
$script:watchFiles = @()
function Wait-Text([string]$path, [string]$pattern, [int]$timeoutSec, [datetime]$after = [datetime]::MinValue) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        if ((Test-Path $path) -and (Get-Item $path).LastWriteTime -gt $after -and [regex]::IsMatch((Read-Shared $path), $pattern)) { return }
        foreach ($f in $script:watchFiles) {
            if ([regex]::IsMatch((Read-Shared $f), $failRx)) { throw "O jogo caiu ou o build falhou (veja $f)" }
        }
        Start-Sleep -Milliseconds 250
    }
    throw "Tempo esgotado esperando '$pattern' em $path"
}
# Marcador "say probe:<nome>" do datapack (no log do jogo no single, na saida do servidor no multiplayer)
function Wait-Step([string]$name, [int]$timeoutSec = 90) { Wait-Text $script:stepLog ('probe:' + $name + '\b') $timeoutSec $script:stepAfter }

# ---------- Janela, teclado e mouse ----------
function Get-GameWindow {
    if ($script:hwnd) { return $script:hwnd }
    $w = Get-Process java, javaw -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like 'Minecraft*' } | Select-Object -First 1
    if (-not $w) { throw 'Janela do Minecraft nao encontrada' }
    $script:hwnd = $w.MainWindowHandle
    return $script:hwnd
}
function Focus {
    $h = Get-GameWindow
    if ([CultivoProbeInput]::GetForegroundWindow() -ne $h) {
        [void][CultivoProbeInput]::ShowWindow($h, 9); [void][CultivoProbeInput]::SetForegroundWindow($h); Start-Sleep -Milliseconds 300
    }
}
function Key([byte]$vk) { Focus; [CultivoProbeInput]::keybd_event($vk, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60; [CultivoProbeInput]::keybd_event($vk, 0, 2, [UIntPtr]::Zero); Start-Sleep -Milliseconds 250 }
function Click { Focus; [CultivoProbeInput]::mouse_event(0x0002, 0, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 60; [CultivoProbeInput]::mouse_event(0x0004, 0, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 400 }
function Right-Down { Focus; [CultivoProbeInput]::mouse_event(0x0008, 0, 0, 0, [UIntPtr]::Zero) }
function Right-Up { [CultivoProbeInput]::mouse_event(0x0010, 0, 0, 0, [UIntPtr]::Zero) }
function Right-Tap { Right-Down; Start-Sleep -Milliseconds 90; Right-Up; Start-Sleep -Milliseconds 150 }
function Wait-Until($sw, [int]$ms) { $left = $ms - $sw.ElapsedMilliseconds; if ($left -gt 0) { Start-Sleep -Milliseconds $left } }
# Segura o botao direito (fumar): print no meio do uso e solta depois do fim do uso
function Smoke([int]$shotMs, [string]$name, [int]$releaseMs) {
    Right-Down; $sw = [Diagnostics.Stopwatch]::StartNew()
    try { Wait-Until $sw $shotMs; Shot $name; Wait-Until $sw $releaseMs } finally { Right-Up }
}

# ---------- Prints ----------
$shotsDir = Join-Path $run 'screenshots'
New-Item -ItemType Directory -Force $shotsDir | Out-Null
$script:taken = New-Object System.Collections.Generic.List[string]
$script:missed = New-Object System.Collections.Generic.List[string]
function Get-PngSize([string]$path) {
    $b = [IO.File]::ReadAllBytes($path)
    $w = ([int]$b[16] -shl 24) -bor ([int]$b[17] -shl 16) -bor ([int]$b[18] -shl 8) -bor [int]$b[19]
    $h = ([int]$b[20] -shl 24) -bor ([int]$b[21] -shl 16) -bor ([int]$b[22] -shl 8) -bor [int]$b[23]
    return @($w, $h)
}
# F2 e copia o PNG novo com o nome da etapa (assim um print que falhar nao desloca os nomes dos outros)
function Shot([string]$name) {
    $known = @{}; Get-ChildItem $shotsDir -File | ForEach-Object { $known[$_.Name] = $true }
    Key 0x71
    $deadline = (Get-Date).AddSeconds(4)
    while ((Get-Date) -lt $deadline) {
        $new = Get-ChildItem $shotsDir -File -Filter '*.png' | Where-Object { -not $known.ContainsKey($_.Name) } | Select-Object -First 1
        if ($new) {
            # O jogo grava o PNG em outra thread: espera o tamanho parar de mudar
            $size = -1
            while ($size -ne (Get-Item $new.FullName).Length) { $size = (Get-Item $new.FullName).Length; Start-Sleep -Milliseconds 120 }
            $dest = Join-Path $OutDir "$name.png"
            for ($try = 0; $try -lt 5; $try++) {
                try { Copy-Item -Force $new.FullName $dest; break } catch { Start-Sleep -Milliseconds 200 }
            }
            $script:taken.Add($name)
            if (-not $script:fbW) { $s = Get-PngSize $dest; $script:fbW = $s[0]; $script:fbH = $s[1] }
            return
        }
        Start-Sleep -Milliseconds 100
    }
    $script:missed.Add($name)
}

# ---------- Posicoes da interface (em unidades da GUI, como o jogo calcula) ----------
# Window#calculateScale com guiScale 0 (automatico): maior escala em que cabe 320x240; tela da GUI = ceil(fb / escala).
# O tamanho do framebuffer sai do primeiro print. O mouse vai na mesma fracao da area cliente da janela.
function Get-Gui {
    $w = $script:fbW; $h = $script:fbH; $s = 1
    while ($s -ne $script:guiMax -and $s -lt $w -and $s -lt $h -and [math]::Floor($w / ($s + 1)) -ge 320 -and [math]::Floor($h / ($s + 1)) -ge 240) { $s++ }
    if ($script:unicodeFont -and ($s % 2) -ne 0) { $s++ }
    return @{ W = [int][math]::Ceiling($w / $s); H = [int][math]::Ceiling($h / $s) }
}
function Move-Gui([double]$gx, [double]$gy) {
    $g = Get-Gui; $h = Get-GameWindow
    $r = New-Object CultivoProbeInput+RECT; [void][CultivoProbeInput]::GetClientRect($h, [ref]$r)
    $p = New-Object CultivoProbeInput+POINT; [void][CultivoProbeInput]::ClientToScreen($h, [ref]$p)
    $x = [int]($p.X + ($gx + 0.5) * ($r.Right - $r.Left) / $g.W)
    $y = [int]($p.Y + ($gy + 0.5) * ($r.Bottom - $r.Top) / $g.H)
    [void][CultivoProbeInput]::SetCursorPos($x, $y); Start-Sleep -Milliseconds 150
}
# Inventario do sobrevivencia (176x166): hotbar em y 142, inventario em y 84 + 18 por linha; slot 16x16
function Hover-Slot([string]$slot) {
    $g = Get-Gui; $l = [math]::Floor(($g.W - 176) / 2); $t = [math]::Floor(($g.H - 166) / 2)
    $kind, $n = $slot.Split('.'); $n = [int]$n
    if ($kind -eq 'hotbar') { Move-Gui ($l + 16 + 18 * $n) ($t + 150) }
    else { Move-Gui ($l + 16 + 18 * ($n % 9)) ($t + 92 + 18 * [math]::Floor($n / 9)) }
}

# ---------- Problemas no log ----------
function Write-LogProblems([string[]]$files, [string]$dest) {
    $known = '(?i)VanillaPackResourcesBuilder|end_of_frame|Realms|Advanced terminal features|sun\.misc\.Unsafe|restricted method|native-access|Restricted methods'
    $problem = '(?i)missing|unable to|failed to|exception|couldn''t|could not|unknown|invalid|not found|lost connection|disconnect|kicked'
    $level = '/(WARN|ERROR|FATAL)\]'
    $mod = New-Object System.Collections.Generic.List[string]
    $other = New-Object System.Collections.Generic.List[string]
    $benign = New-Object System.Collections.Generic.List[string]
    $seen = @{}
    foreach ($f in $files) {
        if (-not (Test-Path $f)) { continue }
        $tag = Split-Path $f -Leaf
        foreach ($line in ((Read-Shared $f) -split "`r?`n")) {
            $isMod = $line -match '(?i)cultivo'
            $isProblem = $line -match $problem
            if (-not (($line -match $level) -or ($isProblem -and ($isMod -or $line -match 'STDERR|Exception|Caused by')))) { continue }
            $key = $line -replace '^\[[^\]]*\]\s*', ''
            if ($seen.ContainsKey($key)) { continue }
            $seen[$key] = $true
            if ($line -match $known) { $benign.Add("[$tag] $line") }
            elseif ($isMod) { $mod.Add("[$tag] $line") }
            else { $other.Add("[$tag] $line") }
        }
    }
    $out = New-Object System.Collections.Generic.List[string]
    if ($mod.Count -eq 0 -and $other.Count -eq 0) { $out.Add('nenhum') }
    $out.Add('== Ligados ao Cultivo =='); $out.AddRange($mod)
    $out.Add('== Outros avisos e erros =='); $out.AddRange($other)
    $out.Add('== Conhecidos (nao sao do mod) =='); $out.AddRange($benign)
    [IO.File]::WriteAllLines($dest, $out.ToArray(), (New-Object Text.UTF8Encoding($false)))
}

# ---------- Preparo e execucao ----------
$opt = Join-Path $run 'options.txt'
$props = Join-Path $run 'server.properties'
$log = Join-Path $run 'logs\latest.log'
$gradleLog = Join-Path $OutDir 'gradle-client.log'
$serverLog = Join-Path $OutDir 'gradle-server.log'
$F1 = 0x70; $F5 = 0x74; $E = 0x45; $ESC = 0x1B; $K1 = 0x31; $K2 = 0x32; $SHIFT = 0xA0
$script:guiMax = 0; $script:unicodeFont = $false
$started = Get-Date

try {
    Restore-File $props
    # 1. Mundo de teste com o datapack
    if ($Multiplayer) {
        $world = Join-Path $run 'probe_mp_world'
    } else {
        $world = Join-Path $run 'saves\cultivo_probe'
    }
    if (Test-Path $world) { Remove-Item -Recurse -Force $world }
    Copy-Item -Recurse (Join-Path $run 'world') $world
    Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $world 'session.lock')
    Copy-Item -Recurse (Join-Path $PSScriptRoot 'datapack') (Join-Path $world 'datapacks\probe')

    # 2. Opcoes do cliente (voltam no fim): legendas ligadas, chat escondido, GUI/FOV/particulas no padrao
    if ((Test-Path $opt) -or (Test-Path "$opt.probe-bak")) {
        Backup-File $opt
        $script:unicodeFont = [bool](Select-String -Path $opt -Pattern '^forceUnicodeFont:true' -Quiet)
        Set-Keys $opt ':' ([ordered]@{
            'pauseOnLostFocus' = 'false'; 'lang' = $Lang; 'tutorialStep' = 'none'; 'chatVisibility' = '2'
            'onboardAccessibility' = 'false'; 'showSubtitles' = 'true'; 'guiScale' = '0'; 'fov' = '0.0'; 'particles' = '0'
            'fullscreen' = 'false'; 'skipMultiplayerWarning' = 'true'; 'joinedFirstServer' = 'true' })
    }

    # 3. Multiplayer: servidor dedicado so em 127.0.0.1. Cliente e servidor usam a mesma pasta run (e o mesmo
    # run\logs\latest.log), entao os marcadores vem da saida do servidor, que vai para um arquivo proprio.
    if ($Multiplayer) {
        Backup-File $props
        Set-Keys $props '=' ([ordered]@{
            'online-mode' = 'false'; 'server-ip' = '127.0.0.1'; 'server-port' = '25565'; 'enforce-secure-profile' = 'false'
            'level-name' = 'probe_mp_world'; 'white-list' = 'false'; 'enforce-whitelist' = 'false'; 'spawn-protection' = '0' })
        $script:watchFiles = @($serverLog, "$serverLog.err")
        Start-Process -FilePath (Join-Path $root 'gradlew.bat') -WorkingDirectory $root -WindowStyle Minimized `
            -RedirectStandardOutput $serverLog -RedirectStandardError "$serverLog.err" -ArgumentList @('runServer', '--console=plain') | Out-Null
        Wait-Text $serverLog 'Done \(' 900
        $script:stepLog = $serverLog; $script:stepAfter = [datetime]::MinValue
        $quick = '--quickPlayMultiplayer 127.0.0.1:25565'
    } else {
        $script:stepLog = $log; $script:stepAfter = $started
        $quick = '--quickPlaySingleplayer cultivo_probe'
    }

    # 4. Cliente direto no mundo de teste
    $script:watchFiles = @($script:watchFiles) + @($gradleLog, "$gradleLog.err")
    Start-Process -FilePath (Join-Path $root 'gradlew.bat') -WorkingDirectory $root -WindowStyle Minimized `
        -RedirectStandardOutput $gradleLog -RedirectStandardError "$gradleLog.err" -ArgumentList @(
        'runClient', '--console=plain', "`"--args=$quick --width $Width --height $Height`"") | Out-Null
    if ($Multiplayer) {
        Wait-Text $serverLog 'logged in with entity id' 900
    } else {
        # O mundo com o datapack de teste abre o aviso "ajustes experimentais"; Tab, Tab, Enter escolhe
        # "Sei o que estou fazendo!".
        Wait-Text $log 'Found new data pack' 900 $started
        $deadline = (Get-Date).AddSeconds(90)
        while (-not [regex]::IsMatch((Read-Shared $log), 'logged in with entity id')) {
            if ((Get-Date) -gt $deadline) { throw 'O jogo nao entrou no mundo de teste' }
            Start-Sleep 4
            if (-not [regex]::IsMatch((Read-Shared $log), 'logged in with entity id')) { Key 0x09; Key 0x09; Key 0x0D }
        }
    }
    Focus

    # 5. Cenas (F1 esconde a interface; sem F1 aparecem as legendas de som)
    Wait-Step 'cam_crops' 180;   Start-Sleep 4; Key $F1; Shot '01_plantas'; Key $F1
    Wait-Step 'cam_families';    Start-Sleep 3; Key $F1; Shot '02_familias'; Key $F1
    Wait-Step 'cam_drying';      Start-Sleep 3; Shot '03_suporte_secagem'
    Wait-Step 'cam_sieve';       Start-Sleep 3; Shot '04_peneira'
    Wait-Step 'cam_press';       Start-Sleep -Milliseconds 2500; Shot '05_prensa_a'; Shot '06_prensa_b'; Shot '07_prensa_c'
    Wait-Step 'cam_articles';    $sw = [Diagnostics.Stopwatch]::StartNew()
    Wait-Until $sw 1000; Key $F1; Shot '08_artigos'; Key $F1
    Wait-Until $sw 2300; Shot '09_pote_cura_a'; Wait-Until $sw 2900; Shot '10_pote_cura_b'
    Wait-Step 'cam_grow_tent';   Start-Sleep 3; Key $F1; Shot '11_estufinha'; Key $F1
    Wait-Step 'cam_strains_a';   Start-Sleep 3; Key $F1; Shot '12_geneticas_1'; Key $F1
    Wait-Step 'cam_strains_b';   Start-Sleep 3; Key $F1; Shot '13_geneticas_2'; Key $F1
    Wait-Step 'cam_products';    Start-Sleep 3; Key $F1; Shot '14_produtos'; Key $F1
    Wait-Step 'cam_in_hand';     Start-Sleep 3; Key $F1; Shot '15_na_mao_3p'; Key $F1

    # 6. Inventario e dicas (o mouse passa por slots conhecidos do datapack)
    Wait-Step 'inventory';       Start-Sleep 2; Key $E; Start-Sleep 1; Shot '16_inventario'
    $tips = @(
        @('hotbar.0', '17_dica_semente_rara'), @('hotbar.2', '18_dica_semente_incomum'),
        @('hotbar.4', '19_dica_flor_curada'), @('hotbar.5', '20_dica_haxixe'), @('hotbar.6', '21_dica_baseado_piteira'),
        @('hotbar.8', '22_dica_isqueiro'), @('inventory.3', '23_dica_dichavador'), @('inventory.7', '24_dica_estacao'),
        @('inventory.4', '25_dica_bandeja'), @('inventory.17', '26_dica_baseado_sem_variedade'),
        @('inventory.18', '48_dica_estufinha'))
    foreach ($t in $tips) { Hover-Slot $t[0]; Start-Sleep -Milliseconds 350; Shot $t[1] }
    Key $ESC

    # 7. Criativo (195x136): as abas do mod ficam na 2a pagina do Forge (10 abas por pagina), colunas 0 e 1 em cima
    Wait-Step 'creative';        Start-Sleep 2; Key $E; Start-Sleep 1
    $g = Get-Gui; $cl = [math]::Floor(($g.W - 195) / 2); $ct = [math]::Floor(($g.H - 136) / 2)
    # Botao ">" (20x20 em leftPos+175, topPos-50), abas de 26x32 acima do painel e grade de itens em (9, 18)
    Move-Gui ($cl + 185) ($ct - 40); Click
    Move-Gui ($cl + 13) ($ct - 16); Click; Start-Sleep -Milliseconds 300; Shot '27_criativo_aba_cultivo'
    Move-Gui ($cl + 40) ($ct - 16); Click; Start-Sleep -Milliseconds 300; Shot '28_criativo_aba_geneticas'
    Move-Gui ($cl + 17) ($ct + 26); Start-Sleep -Milliseconds 350; Shot '29_criativo_dica'
    Key $ESC

    # 8. Cliques de verdade (o datapack ja deixou o jogador mirando o bloco)
    Wait-Step 'use_tray_ok';      Start-Sleep -Milliseconds 1500; Key $K1; Right-Tap; Start-Sleep -Milliseconds 900; Shot '30_bandeja_enrolou'
    Wait-Step 'use_tray_refused'; Start-Sleep -Milliseconds 1500; Key $K1; Right-Tap; Start-Sleep -Milliseconds 300; Shot '31_bandeja_recusa'
    Wait-Step 'use_rack';         Start-Sleep -Milliseconds 1500; Key $K1; Right-Tap; Start-Sleep -Milliseconds 900; Shot '32_suporte_inserir'
    # Tirar a entrada que ainda esta secando: agachado (Shift) e com as duas maos vazias
    Key $K2; Focus; [CultivoProbeInput]::keybd_event($SHIFT, 0, 0, [UIntPtr]::Zero); Start-Sleep -Milliseconds 300
    try { Right-Tap } finally { [CultivoProbeInput]::keybd_event($SHIFT, 0, 2, [UIntPtr]::Zero) }
    Start-Sleep -Milliseconds 900; Shot '33_suporte_retirar'
    Wait-Step 'use_grinder';      Start-Sleep -Milliseconds 1500; Key $K1; Right-Tap; Start-Sleep -Milliseconds 900; Shot '34_peneira_dichavador'

    # 9. Na mao e fumando (F5 duas vezes = camera de frente; mais uma volta para a 1a pessoa)
    Wait-Step 'held';            Start-Sleep 2; Shot '35_bong_primeira_pessoa'
    Key $F5; Key $F5; Start-Sleep 1; Shot '36_bong_terceira_pessoa'; Key $F5
    Wait-Step 'smoke_joint';     Start-Sleep 2; Smoke 600 '37_fumando_baseado_1p' 2000
    Start-Sleep -Milliseconds 400; Shot '38_efeito_chapado'; Key $E; Start-Sleep 1; Shot '39_inventario_efeitos'; Key $ESC
    Wait-Step 'smoke_filtered';  Start-Sleep 2; Key $F5; Key $F5; Start-Sleep 1; Smoke 600 '40_baseado_piteira_3p' 2000
    Start-Sleep -Milliseconds 300; Shot '41_baforada_3p'; Key $F5
    Wait-Step 'smoke_bong';      Start-Sleep 2; Key $F5; Key $F5; Start-Sleep 1; Smoke 900 '42_bong_3p_duas_maos' 2400; Key $F5
    Start-Sleep 1; Smoke 900 '43_bong_1p' 2400; Start-Sleep -Milliseconds 800; Right-Tap; Start-Sleep -Milliseconds 300; Shot '44_recusa_sem_material'
    Wait-Step 'smoke_hookah';    Start-Sleep 2; Key $F5; Key $F5; Start-Sleep 1; Smoke 1200 '45_narguile_3p' 2800; Key $F5
    Wait-Step 'smoke_pipe';      Start-Sleep 2; Key $F5; Key $F5; Start-Sleep 1; Smoke 600 '46_cachimbo_3p' 2000; Key $F5
    Wait-Step 'smoke_chillum';   Start-Sleep 2; Key $F5; Key $F5; Start-Sleep 1; Smoke 600 '47_chillum_3p' 2000; Key $F5
    # Estufinha: colher a madura de mao vazia (slot 2) e plantar de novo com a semente (slot 1)
    Wait-Step 'use_tent';        Start-Sleep -Milliseconds 1500; Key $K2; Right-Tap; Start-Sleep -Milliseconds 900; Shot '49_estufinha_colheu'
    Key $K1; Right-Tap; Start-Sleep -Milliseconds 900; Shot '50_estufinha_plantou'
    Wait-Step 'end'
}
finally {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match [regex]::Escape($root) } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Start-Sleep 2
    Restore-File $opt
    Restore-File $props
    $scan = @($gradleLog, "$gradleLog.err")
    if ($Multiplayer) {
        $scan += @($serverLog, "$serverLog.err")
    } else {
        Copy-Item $log (Join-Path $OutDir 'client-latest.log') -ErrorAction SilentlyContinue
        $scan += (Join-Path $OutDir 'client-latest.log')
    }
    Write-LogProblems $scan (Join-Path $OutDir 'log-problems.txt')
    "Prints: $($script:taken.Count) em $OutDir"
    if ($script:missed.Count) { "Prints que faltaram: $($script:missed -join ', ')" }
    "Problemas no log: $(Join-Path $OutDir 'log-problems.txt')"
}
