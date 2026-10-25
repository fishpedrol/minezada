#!/usr/bin/env bash
# Teste de compatibilidade em PRODUÇÃO (Linux/macOS, sem tela): instala o servidor oficial do Forge na versão pedida,
# coloca nele o jar do Cultivo já buildado (build/libs) e, se pedido, outros mods, sobe o servidor dedicado e roda
# todos os GameTests do Cultivo pelo console com "/test run cultivo:*". É o mais perto do launcher do dono que dá
# para chegar sem tela: o mesmo jar que vai para a pasta mods, no Forge do launcher, junto com os outros mods.
#
# Uso:
#   ./gradlew build                                   # gera build/libs/cultivo-0.1.0.jar (Forge da matriz)
#   bash tools/compat/forge-server-compat.sh 26.3-66.0.2                       # só o Cultivo
#   bash tools/compat/forge-server-compat.sh 26.3-66.0.2 pasta/com/jars/extras # Cultivo + os .jar da pasta
#
# Saída: a pasta run-compat-prod-<versão>[-extras]/ (ignorada pelo git) com logs/latest.log. O script termina com
# código 0 só se o servidor subir e o resumo do /test disser que todos os testes obrigatórios passaram.
# Precisa de rede para maven.minecraftforge.net, piston-meta/piston-data.mojang.com e libraries.minecraft.net.
set -euo pipefail

FORGE_VERSION=${1:?informe a versão do Forge, ex.: 26.3-66.0.2}
EXTRA_DIR=${2:-}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
JAVA_BIN=${JAVA_HOME:+$JAVA_HOME/bin/}java
MOD_JAR=$(ls "$ROOT"/build/libs/cultivo-*.jar 2>/dev/null | grep -v -- '-sources' | head -1 || true)
[ -n "$MOD_JAR" ] || { echo "jar do Cultivo não encontrado em build/libs: rode ./gradlew build antes" >&2; exit 2; }

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

# Instala o servidor uma vez; os mods e o mundo são refeitos a cada execução.
if [ ! -f "$SERVER/.installed" ]; then
  rm -rf "$SERVER" && mkdir -p "$SERVER"
  (cd "$SERVER" && "$JAVA_BIN" -jar "$INSTALLER" --installServer > installer.log 2>&1)
  touch "$SERVER/.installed"
fi
rm -rf "$SERVER/mods" "$SERVER/world" "$SERVER/logs"
mkdir -p "$SERVER/mods"
cp "$MOD_JAR" "$SERVER/mods/"
if [ -n "$EXTRA_DIR" ]; then
  cp "$EXTRA_DIR"/*.jar "$SERVER/mods/"
fi
echo "eula=true" > "$SERVER/eula.txt"
# Mundo plano e sem estruturas: o teste não depende de onde cai a área dos GameTests.
cat > "$SERVER/server.properties" <<'EOF'
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
max-tick-time=-1
EOF

echo "Mods em $SERVER/mods:"; ls -1 "$SERVER/mods"

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
  echo "test run cultivo:*" >&3
  # O resumo sai em inglês no console do servidor (commands.test.summary*).
  if wait_for 'All required tests passed|required test\(s\) failed|No tests found' 900; then
    sleep 2
    grep -qE 'All required tests passed' "$LOG" && STATUS=0
  fi
fi
echo "stop" >&3 || true
exec 3>&-
wait "$SERVER_PID" 2>/dev/null || true
rm -f "$FIFO"

echo "---- resumo ($SERVER/console.out)"
grep -E 'Forge mod loading, version|Done \(|Running [0-9]+ test|Game Test complete|All required tests passed|required test\(s\) failed|failed at|No tests found|Exception' "$LOG" | head -60 || true
exit "$STATUS"
