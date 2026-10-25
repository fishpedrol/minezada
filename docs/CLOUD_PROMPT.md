# Prompt para a próxima sessão na nuvem

**Atualizado em 25/10/2026, com o código da branch `claude/forge-compat-seasons` (commit `2f6ae47`).** Copie tudo o que está abaixo da linha e cole como primeira mensagem de uma sessão nova do Claude na nuvem aberta neste repositório (`claude --cloud` ou Claude Code na web). Antes de colar, troque o texto da linha "Tarefa desta sessão". O prompt antigo, de 21/10/2026, já foi executado e fica no histórico do Git: `git show 319cf0b:docs/CLOUD_PROMPT.md`.

---

Você vai continuar o "Cultivo": um mod privado de Minecraft Java 26.3 para Forge 26.3-66.0.13 (modid `cultivo`, Java 25), já implementado e testado. O build usa o 66.0.13; o `mods.toml` aceita a partir do 66.0.2, que é o do launcher do dono. Antes de mudar qualquer coisa, leia CLAUDE.md e docs/NEXT_SESSION.md. As regras e os números de jogo estão em docs/DECISIONS.md, as evidências em docs/TODO.md e o guia do dono em docs/GUIA_INICIANTE.md. A matriz de versões está fixada em docs/TECHNICAL_RESEARCH.md: use-a como está.

**Tarefa desta sessão:** <escreva aqui o que você quer>. Se esta linha ficou sem tarefa, faça o primeiro item de "Pendências" que der para fazer sem tela.

## Estado em 25/10/2026
- Implementado:
  - 40 genéticas: 3 nativas e 37 do catálogo High THC da Santa Semente BR, com nomes de paródia;
  - lavoura de 8 estágios, estufa com qualidade maior, planta silvestre;
  - sementes em baús de aldeia, lavouras de planície e no mascate;
  - suporte de secagem, peneira (com dichavador) e prensa por ticks, com estados visuais e animação;
  - baseado com piteira, bandeja de enrolar, pote que cura flores e cinzeiro;
  - a mecânica fictícia de fumar, com animação própria;
  - 16 eventos de som;
  - todo o texto em português em qualquer idioma;
  - compatibilidade com o Serene Seasons: as 40 lavouras e as 40 sementes estão nas tags de verão e outono dele, como o trigo.
- No PC do dono (Windows 11), com o código de `f515cb9`:
  - `build` passa com 311 testes JUnit;
  - `runGameTestServer` passa com `All 62 required tests passed :)` (61 do Cultivo + `minecraft:always_pass`);
  - `node tools/gen/generate.mjs` gera `285 texturas e 786 arquivos JSON`;
  - o teste visual automático (`tools/visual-probe/run-probe.ps1`) abriu o cliente real e passou pelas 47 cenas sem erro de modelo, textura ou som no log; achou e corrigiu a fumaça cobrindo a tela em 1ª pessoa;
  - multiplayer local só parcial: o cliente entrou no servidor dedicado e sincronizou 2 cenas, e o teste foi parado por falta de memória.
- Na nuvem (Linux), com o código de `2f6ae47`:
  - `build` passa com 316 testes JUnit;
  - `runGameTestServer` passa com `All 63 required tests passed :)` (62 do Cultivo + `minecraft:always_pass`), também com o Serene Seasons + GlitchCore na pasta `run-compat-ss`, no 66.0.13 e com `-Pcultivo_forge_version=26.3-66.0.2` (tools/compat/README.md);
  - `node tools/gen/generate.mjs` gera `285 texturas e 791 arquivos JSON`;
  - o jar sobe num servidor de produção Forge 66.0.2 (`tools/compat/forge-server-compat.sh`), e as lavouras crescem como o trigo;
  - num servidor dedicado com Forge 26.3 o Serene Seasons não age em lavoura nenhuma, nem no trigo: o `TagsUpdatedEvent` só dispara no cliente (docs/TECHNICAL_RESEARCH.md, "Versões testadas e compatibilidade").
- Ainda não visto no cliente: mão esquerda, os artigos de todos os lados, os sons tocando (só as legendas) e o multiplayer do começo ao fim.

## Branch e entrega
1. `git fetch origin`. Se o PR da branch `claude/forge-compat-seasons` já foi mesclado, parta da `main`; senão, parta de `claude/forge-compat-seasons`. Para saber se já foi mesclado: `git cat-file -e origin/main:tools/compat/forge-server-compat.sh && echo mesclado`.
2. Crie uma branch `claude/<tema-curto>`, faça commits pequenos com mensagens em inglês e abra um pull request para a `main` no fim. A `main` só muda por PR.

## Preparar o Linux
```
java -version 2>&1 | head -1          # se já mostrar 25, pule o bloco do JDK
curl -fsSL -o /tmp/jdk25.tar.gz "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse"
mkdir -p "$HOME/jdk" && tar -xzf /tmp/jdk25.tar.gz -C "$HOME/jdk"
export JAVA_HOME="$(ls -d "$HOME"/jdk/jdk-25* | head -1)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version                          # precisa dizer 25 e 64-Bit
node --version                         # o gerador precisa do Node (testado com 24.19)
./gradlew build --console=plain        # o primeiro baixa o Forge e o Minecraft (mais de 1 GB)
```
- **JDK:** se a API do Adoptium estiver bloqueada, baixe o Temurin 25 de Linux x64 em <https://github.com/adoptium/temurin25-binaries/releases>. Em 21/10/2026 o JDK veio por downloads de releases do GitHub.
- **Rede:** o primeiro build precisa de `maven.minecraftforge.net`, `piston-meta.mojang.com` e `libraries.minecraft.net`, além de `services.gradle.org`, `plugins.gradle.org` e `repo1.maven.org`.
  - Se falhar com `Failed to download default tool: minecraft-mavenizer-...` ou `403` no CONNECT, peça ao dono para liberar esses domínios nas configurações de rede do ambiente.
  - Se aparecer outro host recusado no erro (por exemplo `piston-data.mojang.com`, de onde vêm os jars do jogo), libere-o também.
- **Sem o Forge:** enquanto o build não baixar, `sh tools/test-offline.sh` roda o JUnit sem Gradle. Os dois testes de bytecode do `ClientIsolationTest` precisam das classes de um build anterior.

## Consultar a API sem web
Depois do primeiro build:
```
mkdir -p /tmp/forge-src && unzip -q -o .gradle/mavenizer/repo/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-sources.jar -d /tmp/forge-src
grep -rn "class TransmuteRecipe" /tmp/forge-src | head
unzip -p .gradle/mavenizer/repo/net/minecraft/client-extra/26.3-20260918.230105/client-extra-26.3-20260918.230105.jar data/minecraft/loot_table/blocks/wheat.json
```
O primeiro jar tem as fontes completas do Minecraft e do Forge (nomes oficiais); o segundo, os JSONs vanilla. Copie os formatos do vanilla: o 26.3 mudou muitos (docs/NEXT_SESSION.md, seção 6).

## Verificar (nesta ordem, um comando do Gradle por vez)
1. `node tools/gen/generate.mjs`: termina com `... texturas e ... arquivos JSON gerados em ...`. Sem mudança no gerador, `git status` fica limpo.
2. `./gradlew build --console=plain`: `BUILD SUCCESSFUL`. Os totais ficam nos XML de `build/test-results/test` (316 testes, 0 falhas em `2f6ae47`).
3. `./gradlew runGameTestServer --console=plain`: `All N required tests passed :)` (63 em `2f6ae47`).
4. Se mexer em dados ou worldgen: `./gradlew runServer --console=plain`. Aceite a EULA em `run/eula.txt`; o servidor está de pé quando aparece `Done (...)!`; desligue com `stop`.
5. Se mexer em lavouras, em sementes ou nas tags do Serene Seasons: `./gradlew runGameTestServer -Pcultivo_run_dir=run-compat-ss --console=plain`, com o Serene Seasons, o GlitchCore e a config de `tools/compat/config-cant-grow` em `run-compat-ss` (preparo em tools/compat/README.md).

## O que esta sessão não consegue fazer
- Na nuvem não há tela. Por isso não rodam:
  - o `runClient`;
  - o teste visual automático (`tools/visual-probe/run-probe.ps1`, que só roda no Windows, com tela, a partir de `run\world` de um `runServer`);
  - o teste com um cliente de verdade conectado.
- Esses itens ficam para o dono, no PC dele. Deixe as caixas deles abertas em docs/TODO.md e, se mudar algo visível, atualize a checklist do guia (seção 6).
- Erros de modelo ou de textura só do cliente não aparecem no `runGameTestServer`: confira formatos de recurso contra o client-extra e os testes JUnit de `data/`.

## Regras
- Para mudar um recurso:
  - edite as fontes do gerador: `tools/gen/manifest.json`, `tools/gen/generate.mjs`, `tools/gen/models.mjs` e `tools/art/textures.mjs`;
  - rode `node tools/gen/generate.mjs`;
  - versione a saída regerada em `src/generated/resources`.
- Toda regra nova ganha teste, com os valores esperados escritos como literais da especificação. GameTest novo: nome em `ContentIds.GAME_TESTS` e entrada em `game_tests` do manifesto.
- Resultados de cultivo, crafting, estações e fumo são decididos no servidor. Tudo é mecânica fictícia de jogo, sem instruções nem números do mundo real; autenticação e licença do jogo ficam como estão.
- Textos novos vão no manifesto em pt e en. O jogo mostra o pt em qualquer idioma (`portuguese_everywhere`).
- Nomes reais de genéticas e de bancos de sementes ficam só em `docs/strains-catalog.json`.
- Código de cliente fica só no pacote `client/`. Sons, só pelos eventos de `ModSounds`.
- Comentários e javadoc em português; identificadores em inglês.
- Atualize docs/TODO.md somente com evidência: data, comando exato e saída real.
- Ao terminar, informe:
  - os arquivos alterados;
  - a versão exata (Minecraft, Forge, Java);
  - os comandos de build e teste com os resultados;
  - as pendências.

## Pendências (detalhes em docs/NEXT_SESSION.md, seção 8)
1. Multiplayer completo: `run-probe.ps1 -Multiplayer` com memória livre e depois com amigos (só o dono, no Windows).
2. O que o teste visual não cobre: mão esquerda, artigos de todos os lados, ouvir os sons (só o dono).
3. Serene Seasons (docs/DECISIONS.md, "Em aberto"). O Forge 66.0.2 do launcher já é aceito (`mods.toml` `[66.0.2,)`), e a compatibilidade foi testada sem tela. Falta:
   - só o dono, com tela: jogar em single player com o Serene Seasons e os outros mods do launcher e ver a dica "Estações férteis: Verão, Outono" nas sementes, a lavoura lenta na primavera e normal sob um bloco de vidro (docs/GUIA_INICIANTE.md, seção 6);
   - decisão do dono: manter a paridade com o trigo (verão e outono) ou adotar outra regra de estações;
   - decisão do dono: se usar `out_of_season_crop_behavior = 2` (quebra fora de estação), pôr as lavouras do Cultivo em `unbreakable_infertile_crops`, para não perder sementes raras;
   - decisão do dono: se vidraças (painéis) devem contar como estufa do Serene Seasons, o que mudaria também o vanilla;
   - decisão do dono: onde jogar, sabendo que num servidor dedicado com Forge 26.3 o Serene Seasons não afeta plantação nenhuma, e em single player e LAN afeta.
4. Se mexer em genéticas, regere o datapack do teste visual (`node tools/visual-probe/gen-datapack.mjs . tools/visual-probe/datapack`) e confira com `validate.mjs`.
5. Completar o catálogo se o dono colar a página da loja (cerca de 58 produtos High THC num snapshot antigo; entraram 37).
6. Balanceamento depois de jogar.
7. Opcionais: o pote com funil; áudio próprio; limpar a sobra `GrowthRules.visualStage`.
