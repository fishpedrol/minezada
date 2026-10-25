#!/usr/bin/env bash
# Teste de compatibilidade em PRODUÇÃO (Linux/macOS, sem tela): instala o servidor oficial do Forge na versão pedida,
# coloca nele o jar do Cultivo já buildado (build/libs) e, se pedido, outros mods (ex.: Serene Seasons + GlitchCore),
# sobe o servidor dedicado e roda uma "sonda": comandos de console de tools/compat/probes/*.txt que plantam lavouras,
# aceleram os random ticks e dizem, com "say SONDA ...", o que cresceu. É o mais perto do launcher do dono que dá
# para chegar sem tela: o mesmo jar da pasta mods, no Forge do launcher, junto com os outros mods.
#
# Por que não os GameTests: em produção o Forge não avança o GameTestTicker
# (ForgeGameTestHooks.isGametestEnabled() exige !FMLLoader.isProduction()), então "/test run" monta as estruturas e
# nunca termina. Os GameTests rodam no ambiente de desenvolvimento (runGameTestServer, inclusive com
# -Pcultivo_forge_version); aqui só comandos de console.
#
# Uso:
#   ./gradlew build                                                   # gera build/libs/cultivo-0.1.0.jar
#   bash tools/compat/forge-server-compat.sh 26.3-66.0.2                          # só o Cultivo, sonda padrão
#   bash tools/compat/forge-server-compat.sh 26.3-66.0.2 pasta/dos/jars           # + os .jar da pasta
#   bash tools/compat/forge-server-compat.sh 26.3-66.0.2 pasta/dos/jars tools/compat/probes/seasons.txt
#
# Formato da sonda: uma linha por comando de console (sem "/"); "@wait N" espera N segundos; "#" é comentário.
# Variável opcional CULTIVO_COMPAT_CONFIG: pasta copiada para config/ do servidor antes de subir.
# Saída: a pasta run-compat-prod-<versão>[-extras]/ (ignorada pelo git), com console.out. O script termina com
# código 0 se o servidor subir, a sonda rodar até o fim e o servidor desligar; as linhas "SONDA" são o resultado.
# Precisa de rede para maven.minecraftforge.net, piston-meta/piston-data.mojang.com e libraries.minecraft.net.
set -euo pipefail

FORGE_VERSION=${1:?informe a versão do Forge, ex.: 26.3-66.0.2}
EXTRA_DIR=${2:-}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
PROBE=${3:-$ROOT/tools/compat/probes/growth.txt}
JAVA_BIN=${JAVA_HOME:+$JAVA_HOME/bin/}java
MOD_JAR=$(ls "$ROOT"/build/libs/cultivo-*.jar 2>/dev/null | grep -v -- '-sources' | head -1 || true)
[ -n "$MOD_JAR" ] || { echo "jar do Cultivo não encontrado em build/libs: rode ./gradlew build antes" >&2; exit 2; }
[ -f "$PROBE" ] || { echo "sonda não encontrada: $PROBE" >&2; exit 2; }

SUFFIX=""
[ -n "$EXTRA_DIR" ] && SUFFIX="-extras"
SERVER="$ROOT/run-compat-prod-$FORGE_VERSION$SUFFIX"
CACHE=${CULTIVO_COMPAT_CACHE:-$HOME/.cache/cultivo-compat}
INSTALLER="$CACHE/forge-$FORGE_VERSION-installer.jar"
mkdir -p "$CACHE"
if [ ! -f "$INSTALLER" ]; then
  curl -sSfL -o "$INSTALLER" \
    "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE_VERSION/forge-$FORGE_VERSION-installer.jar"
fi

# Instala o servidor uma vez; mods, mundo, config e logs são refeitos a cada execução.
if [ ! -f "$SERVER/.installed" ]; then
  rm -rf "$SERVER" && mkdir -p "$SERVER"
  (cd "$SERVER" && "$JAVA_BIN" -jar "$INSTALLER" --installServer > installer.log 2>&1)
  touch "$SERVER/.installed"
fi
rm -rf "$SERVER/mods" "$SERVER/world" "$SERVER/logs" "$SERVER/config"
mkdir -p "$SERVER/mods"
# Configs opcionais (ex.: config/sereneseasons/fertility.toml) copiadas antes de subir o servidor.
if [ -n "${CULTIVO_COMPAT_CONFIG:-}" ]; then
  mkdir -p "$SERVER/config" && cp -r "$CULTIVO_COMPAT_CONFIG"/. "$SERVER/config/"
fi
cp "$MOD_JAR" "$SERVER/mods/"
if [ -n "$EXTRA_DIR" ]; then
  cp "$EXTRA_DIR"/*.jar "$SERVER/mods/"
fi
echo "eula=true" > "$SERVER/eula.txt"
# Mundo normal sem estruturas. A sonda monta a própria plataforma no alto (y=120), com o bioma forçado por
# fillbiome, para não depender do relevo nem do bioma sorteado perto de 0,0.
# pause-when-empty-seconds=0: o 26.3 pausa o servidor vazio depois de 60 s, o que congelaria a sonda.
cat > "$SERVER/server.properties" <<'EOF'
generate-structures=false
spawn-protection=0
pause-when-empty-seconds=0
EOF

echo "Forge $FORGE_VERSION; mods em $SERVER/mods:"; ls -1 "$SERVER/mods"
echo "Sonda: $PROBE"

# O instalador do Forge cria run.sh (que lê user_jvm_args.txt). Comandos entram pelo stdin do servidor.
FIFO="$SERVER/console.in"
rm -f "$FIFO" && mkfifo "$FIFO"
LOG="$SERVER/console.out"
(cd "$SERVER" && exec bash ./run.sh nogui < "$FIFO" > "$LOG" 2>&1) &
SERVER_PID=$!
exec 3> "$FIFO"

wait_for() { # padrão, segundos
  local i=0
  while ! grep -qE "$1" "$LOG" 2>/dev/null; do
    sleep 1; i=$((i + 1))
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then echo "o servidor parou antes de: $1" >&2; return 1; fi
    if [ "$i" -ge "$2" ]; then echo "tempo esgotado esperando: $1" >&2; return 1; fi
  done
}

STATUS=1
if wait_for 'Done \([0-9.]+s\)!' 600; then
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|'#'*) ;;
      '@wait '*) sleep "${line#@wait }" ;;
      *) echo "$line" >&3 ;;
    esac
  done < "$PROBE"
  sleep 2
  STATUS=0
fi
echo "stop" >&3 || true
exec 3>&-
wait "$SERVER_PID" 2>/dev/null || STATUS=1
rm -f "$FIFO"

echo "---- resultado ($LOG)"
grep -E 'Forge mod loading, version|Done \([0-9.]+s\)!|SONDA|Unknown or incomplete command|Incorrect argument|Unknown block|Could not set the block' "$LOG" || true
exit "$STATUS"
