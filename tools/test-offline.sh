#!/usr/bin/env sh
# Roda os testes JUnit das regras puras (pacote core) e de completude de assets/dados SEM o Minecraft.
# Útil quando maven.minecraftforge.net / servidores da Mojang não estão acessíveis (o Gradle do Forge
# precisa deles). Baixa só o JUnit console standalone e o Gson do Maven Central.
# Uso: JAVA_HOME=<jdk25> sh tools/test-offline.sh
set -eu
ROOT=$(cd "$(dirname "$0")/.." && pwd)
JAVA_BIN=${JAVA_HOME:+$JAVA_HOME/bin/}
CACHE=${CULTIVO_TEST_CACHE:-$HOME/.cache/cultivo-offline-test}
OUT=$ROOT/build/offline-test
JUNIT=junit-platform-console-standalone-1.12.2.jar
GSON=gson-2.13.1.jar
mkdir -p "$CACHE"
[ -f "$CACHE/$JUNIT" ] || curl -sSfL -o "$CACHE/$JUNIT" "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.12.2/$JUNIT"
[ -f "$CACHE/$GSON" ] || curl -sSfL -o "$CACHE/$GSON" "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.1/$GSON"
rm -rf "$OUT" && mkdir -p "$OUT/main" "$OUT/test"
"${JAVA_BIN}javac" --release 25 -encoding UTF-8 -d "$OUT/main" $(find "$ROOT/src/main/java" -path '*/core/*.java')
"${JAVA_BIN}javac" --release 25 -encoding UTF-8 -d "$OUT/test" -cp "$OUT/main:$CACHE/$JUNIT:$CACHE/$GSON" $(find "$ROOT/src/test/java" -name '*.java')
cd "$ROOT"
"${JAVA_BIN}java" -jar "$CACHE/$JUNIT" execute --disable-banner --disable-ansi-colors --details=tree \
  -cp "$OUT/main:$OUT/test:$CACHE/$GSON" --scan-classpath "$OUT/test" --reports-dir "$OUT/reports"
