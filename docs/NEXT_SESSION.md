# Orientação para a próxima sessão

**Atualizado em 25/10/2026, na branch `claude/forge-compat-seasons` (commits `0590b40`, `c737633` e `2f6ae47`, sobre a `main` em `6d6ed4a`).** Este é o documento para retomar o projeto sem a conversa anterior. Regras e números de jogo: [DECISIONS.md](DECISIONS.md). Evidências: [TODO.md](TODO.md). Passo a passo do dono (leigo em Java): [GUIA_INICIANTE.md](GUIA_INICIANTE.md). Prompt pronto para a nuvem: [CLOUD_PROMPT.md](CLOUD_PROMPT.md).

## 1. Estado

O mod "Cultivo" (modid `cultivo`, versão 0.1.0) está implementado para Minecraft 26.3 + Forge 26.3-66.0.13 (o build) e roda a partir do Forge 66.0.2 (o do launcher do dono):

- **40 genéticas:** 3 nativas (Brisa, Aurora, Eclipse) e 37 do catálogo High THC da Santa Semente BR, com nomes de paródia. Lavoura com 8 estágios visuais, estufa com qualidade superior, planta silvestre, duas abas criativas.
- **Fontes de sementes:** baús de casa de 4 dos 5 tipos de aldeia (todas as genéticas, peso pela raridade), lavouras de aldeia de planície e planta silvestre (só nativas) e o mascate (2 ofertas do catálogo). O fazendeiro replanta a lavoura do mod que colhe.
- **Processamento:** suporte de secagem, peneira de bancada (com o dichavador como melhoria) e prensa manual, por ticks, com estados visuais, animações, partículas e sons.
- **Artigos:** todos têm uso. Baseado com piteira, bandeja de enrolar, pote de vidro que cura flores, cinzeiro que vira farinha de osso, além dos artigos de fumar e do isqueiro.
- **Mecânica fictícia de fumar** (pedida em 21/10/2026), com animação própria em 1ª e 3ª pessoa.
- **Tudo em português** em qualquer idioma do cliente (pedido de 24/10/2026), com 16 eventos de som do mod.
- **Compatível com o Serene Seasons** (25/10/2026): as 40 lavouras e as 40 sementes estão nas tags de verão e outono do Serene Seasons, como o trigo. Sem o Serene Seasons, essas tags ficam sem leitor.

**Verificado** no PC do dono (Windows 11, Temurin 25.0.4.1+1), com o código de `f515cb9` (25/10/2026):

| Verificação | Resultado |
|---|---|
| `.\gradlew.bat build` | `BUILD SUCCESSFUL`, 311 testes JUnit, 0 falhas |
| `.\gradlew.bat runGameTestServer` | `All 62 required tests passed :)` (61 do Cultivo + `minecraft:always_pass`) |
| `node tools/gen/generate.mjs` | `285 texturas e 786 arquivos JSON` |
| Teste visual automático (`run-probe.ps1`, `runClient`) | 47 prints conferidos; log sem modelo, textura ou som faltando; achou e corrigiu a fumaça cobrindo a tela em 1ª pessoa |
| `run-probe.ps1 -Multiplayer` | Parcial: o cliente entrou no servidor dedicado em 127.0.0.1 e 2 cenas sincronizaram sem erro; parado por falta de memória no PC |

**Verificado na nuvem** (Linux, Temurin 25.0.1+8, Node 22.22.0), com o código de `2f6ae47` (25/10/2026). Serene Seasons 26.1.2.0.7 + GlitchCore 26.3.0.0.5, baixados do Modrinth com sha512 conferido:

| Verificação | Resultado |
|---|---|
| `./gradlew build` | `BUILD SUCCESSFUL`, 316 testes JUnit (5 novos do `CompatibilityTest`), 0 falhas |
| `./gradlew runGameTestServer` | `All 63 required tests passed :)` (62 do Cultivo + `minecraft:always_pass`), sem `Couldn't load tag` |
| `node tools/gen/generate.mjs` | `285 texturas e 791 arquivos JSON` (786 + 4 tags do Serene Seasons + 1 test_instance) |
| `build` e `runGameTestServer` com `-Pcultivo_forge_version=26.3-66.0.2`, antes das tags do Serene Seasons (`runGameTestServer` com `-Pcultivo_run_dir=run-compat-forge-66.0.2`) | `BUILD SUCCESSFUL`, 311 testes, 0 falhas; log `Forge mod loading, version 66.0.2` e `All 62 required tests passed :)` |
| `runGameTestServer -Pcultivo_run_dir=run-compat-ss` (Serene Seasons + GlitchCore em `run-compat-ss/mods`, `out_of_season_crop_behavior = 1`) | `All 63 required tests passed :)`; o `serene_seasons_compat` viu Cultivo e trigo parados na primavera, crescendo sob vidro e no verão |
| O mesmo com `-Pcultivo_forge_version=26.3-66.0.2` (a combinação do launcher do dono) | Log `Forge mod loading, version 66.0.2`; `All 63 required tests passed :)` |
| Teste negativo: `serene_seasons_compat` sem a linha `ForgeEventFactory.onTagsUpdated(...)` | Falha com `Cultivo cresceu fora de estação (primavera)`; linha restaurada |
| Servidor de produção Forge 66.0.2 (`tools/compat/forge-server-compat.sh`), sonda `growth.txt`, jar compilado no 66.0.13 | Sobe (`Done (0.701s)!`); idades Cultivo [6, 7, 7, 7] e trigo [7, 7, 7, 7] após 30 s com `random_tick_speed 300` |
| Produção 66.0.2 + Serene Seasons + GlitchCore, jar com as tags, config "não cresce", sonda `seasons.txt` | Todas as fileiras (Cultivo e trigo, ao ar livre e sob vidro, primavera e verão) com [7, 7, 7, 7]; 0 `Couldn't load tag`. No servidor dedicado o Serene Seasons não age em lavoura nenhuma (seção 6) |

**Visto num cliente de verdade (25/10/2026):** o teste visual automático abriu o `runClient` com o código de `f515cb9` (a branch do Serene Seasons só acrescentou dados, sem asset de cliente) e passou por todas as cenas: árvores de modelo de item, blockstates multipart, texturas animadas, placa da prensa (BlockEntityRenderer), poses de braço, partículas e legendas dos sons. **Ainda não visto:** mão esquerda, os artigos de todos os lados, ouvir os sons e o multiplayer do começo ao fim. Erros de carregamento de modelo do cliente não aparecem no `runGameTestServer`; rode o teste visual depois de mexer em asset.

**Branches:**
- `main` está em `6d6ed4a` (merge do PR #4, o polimento de `claude/polish-visual-uses`).
- A branch `claude/forge-compat-seasons`, criada de `6d6ed4a`, tem a compatibilidade com o Forge 66.0.2 e com o Serene Seasons: `0590b40` (propriedades do build e script de produção), `c737633` (sondas de console), `2f6ae47` (tags do Serene Seasons e `mods.toml` `[66.0.2,)`) e o commit destes docs. Ela entra na `main` por PR.

## 2. Como retomar

1. **Escolher a base.**
   - Rode `git fetch origin`. Se o PR de `claude/forge-compat-seasons` já foi mesclado, parta da `main`; senão, faça `git checkout claude/forge-compat-seasons`.
   - Para saber se já foi mesclado: `git cat-file -e origin/main:tools/compat/forge-server-compat.sh && echo mesclado`. O arquivo só existe a partir desta branch, e o teste funciona mesmo se o PR tiver sido mesclado com squash.
   - Crie uma branch nova `claude/<tema>`. Commits em inglês; entrega por PR para a `main`.
   - Pronto quando `git log --oneline -1` mostrar o commit destes docs (depois de `2f6ae47`) ou um mais novo.
2. **Preparar o ambiente** (seção 3).
   - Pronto quando `java -version` mostrar 25 e o primeiro `./gradlew build` terminar.
3. **Verificar o ponto de partida.** Rode nesta ordem e nunca dois comandos do Gradle ao mesmo tempo:
   1. `node tools/gen/generate.mjs`: termina com `285 texturas e 791 arquivos JSON gerados em ...`, e `git status` não mostra mudança.
   2. `./gradlew build --console=plain`: `BUILD SUCCESSFUL`. Os totais ficam nos XML de `build/test-results/test` (316 testes, 0 falhas em `2f6ae47`).
   3. `./gradlew runGameTestServer --console=plain`: `All 63 required tests passed :)`.

   Os números crescem com testes novos; o que vale é zero falha. No Windows (PC do dono), defina antes, na mesma janela: `$env:JAVA_HOME = "C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"`.
4. **Escolher a tarefa:** o pedido do dono, ou o primeiro item da seção 8 que der para fazer no ambiente que você tem.
5. **Fechar.** Pronto quando:
   - os três comandos do passo 3 passam;
   - toda regra nova tem teste;
   - os docs afetados estão atualizados (o guia, se mudou algo que o dono vê ou testa);
   - `TODO.md` só ganhou linhas com evidência (data, comando exato, saída real);
   - o commit foi feito.

## 3. Ambiente e versões

| Item | Valor |
|---|---|
| Minecraft | 26.3 (mappings `26.3-20260918.230105`) |
| Forge | `26.3-66.0.13` (`net.minecraftforge:forge:26.3-66.0.13`); `mods.toml` aceita forge `[66.0.2,)` (menor versão testada) e minecraft `[26.3,26.4)` |
| Propriedades opcionais do Gradle | `-Pcultivo_forge_version` (padrão `26.3-66.0.13`) e `-Pcultivo_run_dir` (padrão `run`), só para checar compatibilidade com outro Forge ou com outros mods numa pasta própria (`tools/compat/`); os padrões são a matriz fixada |
| ForgeGradle | `net.minecraftforge.gradle` `[7.0.17,8)` |
| Gradle | wrapper 9.7.1 |
| Java | toolchain 25; o plugin foojay em `settings.gradle` pode baixar um JDK 25 se faltar |
| Testes | JUnit 5.12.2 (BOM) + Gson 2.13.1 |
| Gerador | Node.js (testado com 24.19 no PC do dono); sem dependências npm |

- **PC do dono:**
  - Windows 11; projeto em `C:\Users\Cliente\Documents\minezada` (essa pasta Documentos não está no OneDrive, então está ok).
  - JDK 25 em `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`, com JAVA_HOME configurado em 24/10/2026. O `java` do PATH é um Java 8 de 32 bits, então use o JAVA_HOME.
  - Git e Node 24 instalados.
  - Shell das sessões locais: PowerShell 5.1. Ali, `Get-Content -Raw` lê UTF-8 sem BOM como ANSI e estraga os acentos de um `.java` que passar por ele. Para editar ou restaurar fontes, use node ou `git show`.
- **Launcher do dono:**
  - O perfil "Forge 26.3" usa o Forge **26.3-66.0.2**, não o 66.0.13 do build. Desde 25/10/2026 o 66.0.2 é testado e suportado: build, JUnit e GameTests passam compilando contra ele (`-Pcultivo_forge_version=26.3-66.0.2`), o jar sobe num servidor de produção 66.0.2, e o `mods.toml` exige `[66.0.2,)` (a 66.0.2 corrige "players dropping items", e o Cultivo devolve sobras com `Player#drop`).
  - `%APPDATA%\.minecraft\mods` já tem outros mods: Serene Seasons (com GlitchCore), FallingTree, Macaw's Furniture, Simply Eggs e Xaero's Minimap.
  - O Serene Seasons age nas lavouras por um mixin no começo de `BlockBehaviour$BlockStateBase.randomTick`, e não pelo `ForgeHooks.onCropsGrowPre/Post` nem pelo `BonemealEvent`. Só afeta blocos das tags `sereneseasons:spring_crops`, `summer_crops`, `autumn_crops` e `winter_crops`; desde `2f6ae47` as lavouras do Cultivo estão nas de verão e outono, como o trigo. Fora de estação crescem cerca de 1/6 no padrão; sob um bloco de vidro (painel não conta) até 16 acima, crescem normalmente. A qualidade da colheita não muda. Detalhes em DECISIONS.md, "Compatibilidade: Serene Seasons e versões do Forge".
  - **Servidor dedicado:** no Forge 26.3 o `TagsUpdatedEvent` só dispara no cliente, e o Serene Seasons só monta a lista de lavouras nesse evento. Num servidor dedicado ele não afeta lavoura nenhuma (nem o trigo); em single player ou LAN o cliente do anfitrião dispara o evento e as estações valem.
- **Nuvem (Linux):**
  - Instale o Temurin 25 e exporte `JAVA_HOME` (comandos em [CLOUD_PROMPT.md](CLOUD_PROMPT.md)).
  - O primeiro build precisa de rede para `maven.minecraftforge.net`, `piston-meta.mojang.com` e `libraries.minecraft.net`, além de `services.gradle.org`, `plugins.gradle.org` e `repo1.maven.org`. Em 21/10/2026 a sessão na nuvem só buildou depois de liberar esses domínios (TECHNICAL_RESEARCH.md).
- **Sem tela** (nuvem ou qualquer sessão sem display) não dá para fazer:
  - `runClient`;
  - o teste visual automático `tools/visual-probe/run-probe.ps1`, que só roda no Windows, com tela, a partir de `run\world` criado por um `runServer`;
  - um teste multiplayer com cliente de verdade.

  Essas caixas do TODO ficam abertas até o dono testar. Uma sessão sem tela prepara a checklist para ele (GUIA_INICIANTE.md, seção 6).

### Consultar a API sem pesquisar na web
Depois do primeiro build, o ForgeGradle deixa no projeto:
- **Fontes do Minecraft e do Forge** (decompiladas, nomes oficiais): `.gradle/mavenizer/repo/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-sources.jar`. Extraia num diretório temporário e use grep:

  ```
  mkdir -p /tmp/forge-src && unzip -q -o .gradle/mavenizer/repo/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-sources.jar -d /tmp/forge-src
  ```
- **JSONs vanilla** (loot, receitas, worldgen, modelos, blockstates, `items/*.json`): `.gradle/mavenizer/repo/net/minecraft/client-extra/26.3-20260918.230105/client-extra-26.3-20260918.230105.jar`. Leia com `unzip -p <jar> data/minecraft/loot_table/blocks/wheat.json`. Copie os formatos do vanilla: o 26.3 mudou muitos (seção 6).

## 4. Regras de trabalho

- **Recursos gerados:** `src/generated/resources` sai inteiro de `node tools/gen/generate.mjs`. Conteúdo, nomes, receitas, pesos e testes ficam em `tools/gen/manifest.json`; os modelos 3D em `tools/gen/models.mjs`; a pixel art (grades ASCII; encoder em `tools/art/png.mjs`) em `tools/art/textures.mjs`. Edite as fontes, rode o gerador e versione a saída. O gerador lê o manifesto antes de apagar a pasta; um erro depois disso deixa a pasta pela metade (`git restore src/generated/resources`).
- **Duas fontes para a mesma regra:** muitas regras existem no Java puro de `core/` (o jogo usa) e no manifesto (gera o JSON), e os testes JUnit conferem que as duas concordam. Mude as duas.
- **GameTest novo:** nome em `ContentIds.GAME_TESTS` e entrada em `game_tests` do manifesto (`max_ticks`, padrão 100; `"sky_access": true` se a planta precisa ver o céu), depois o gerador.
- **Tags do Serene Seasons:** as estações em que as lavouras do Cultivo são férteis saem de `compat.serene_seasons.seasons` do manifesto (o gerador escreve `data/sereneseasons/tags/{block,item}/<estação>_crops.json`, `replace: false`, entradas `required: false`, sem a planta silvestre) e estão também em `ContentIds.SERENE_SEASONS_CROP_SEASONS`. São duas fontes: mude as duas; o `CompatibilityTest` confere que concordam com os literais `summer`/`autumn`, e o GameTest `serene_seasons_compat` tem a própria lista.
- **Testes honestos:** valores esperados como literais da especificação, não recalculados com o código testado. Toda regra nova ganha teste, e um teste negativo (quebrar a regra e ver o teste falhar) vale como evidência.
- **Servidor decide tudo:** crescimento, qualidade, drops, estações, crafting, fumo. O cliente só prevê cliques com o que sabe (tags `cultivo:<estação>_inputs`, slots sincronizados) e mostra.
- **Ficção de jogo:** nada de instruções ou números do mundo real (nem % de THC, nem semanas de cultivo); não mexa em autenticação ou licença do jogo.
- **Nomes reais** de genéticas e de bancos de sementes ficam só em `docs/strains-catalog.json`. `ParodyNamesTest` falha se aparecerem em `src/main`, `src/generated`, `tools/gen`, `tools/art` ou `tools/visual-probe`, assim como a palavra "THC".
- **Textos:** só chaves de tradução, sem `Component.literal` (só o diagnóstico de falha dos GameTests usa). Todo texto novo vai no manifesto em pt e en: com `portuguese_everywhere: true`, o `en_us.json` sai igual ao `pt_br.json`. `PortugueseEverywhereTest` procura inglês esquecido. Dicas de tooltip em linhas curtas: `core/TooltipHints`, no máximo 220 px de largura.
- **Código de cliente** só no pacote `client/`, alcançado apenas por `SmokingItem.initializeClient` e por `CultivoClientEvents` (`Dist.CLIENT`). `ClientIsolationTest` confere as fontes e o bytecode.
- **Sons** só pelos eventos de `ModSounds` (`ContentIds.SOUNDS`, seção `sounds` do manifesto), tocados no servidor com `playSound(null, ...)`. `SoundsTest` proíbe `SoundEvents` vanilla no código de jogo.
- **Estilo:** comentários e javadoc em português, como o código ao redor; identificadores em inglês.
- **Evidência:** atualize `TODO.md` somente com data, comando exato e saída real. Caixas de checagem visual ficam abertas sem evidência do dono.

## 5. Mapa do código

Pacote `src/main/java/io/github/peixeangola/cultivo/`:
- `Cultivo` (@Mod): registra os DeferredRegister e os listeners de jogo (`LootTableLoadEvent` → `SeedLootInjector`, `VanillaGameEvent` → `FarmerReplant`, `MobSpawnEvent.FinalizeSpawn` → `WanderingTraderSeeds`).
- `core/`, regras **puras** sem Minecraft, todas com JUnit:
  - genéticas e cultivo: `Variety` (as 40 genéticas), `GrowthRules`, `QualityRules`, `GreenhouseRules`, `SeedSources`;
  - estações: `StationKind`, `ProcessingRules` (dichavador incluso), `StationVisualRules`, `StationAnimation`;
  - fumo: `SmokeRules` (piteira inclusa), `SmokeDevice`, `SmokeAnimation`;
  - artigos: `RollingRules`, `CuringRules`, `AshtrayRules`;
  - listas e dados: `ArticleRecipes`, `ContentIds` (lista única de IDs, GameTests, sons, propriedades de blockstate, estações do Serene Seasons), `TooltipHints`.
  - `GrowthRules.visualStage` (4 estágios) é sobra sem uso no jogo: a lavoura tem 8.
- `registry/`:
  - `ModBlocks`, `ModItems`, `ModBlockEntities` (`processing_station`, `jar`), `ModRecipes` (drying/sieving/pressing), `ModLootFunctions` (`set_harvest_quality`), `ModEffects` (`cultivo:high`), `ModSounds`;
  - `ModComponents`: `cultivo:quality`, `cultivo:strain`, `cultivo:filtered`, `cultivo:cured`;
  - `ModCreativeTabs` ("Cultivo" e "Cultivo: Genéticas");
  - `ModTags`: entradas das estações, `jar_storable` e vidro de estufa.
- `block/`:
  - lavoura e mundo: `CannabisCropBlock`, `WildPlantBlock`, `GreenhouseDetector`;
  - estações: `ProcessingStationBlock` + `ProcessingStationBlockEntity` (as três estações), `SieveBlock` (dichavador), `StationContents`;
  - artigos colocáveis: `RollingTrayBlock`, `JarBlock` + `JarBlockEntity`, `AshtrayBlock`;
  - `Handouts`: o que vai para a mão do jogador, com sobra caindo aos pés.
- `item/`:
  - `SeedItem`, `FlowerItem` e `QualityItem`, com `StrainProfile` (linha de raridade · ciclo · genética · potência);
  - `SmokingItem` e `LighterItem`;
  - `StationBlockItem`, `ArticleBlockItem` (bloco com ícone plano) e `TooltipItem` (dicas em linhas).
- `smoke/`: `SmokeSession` (servidor: isqueiro, material, consumo, efeitos, cinzeiro) e `SmokeEffects` (partículas e sons do fumo).
- `effect/HighEffect`, `recipe/ProcessingRecipe`, `loot/SetHarvestQualityFunction`, `loot/SeedLootInjector`.
- `village/`: `FarmerReplant` e `WanderingTraderSeeds`.
- `client/`, só no cliente:
  - `SmokingItemClient`: poses de braço e transformação em 1ª pessoa;
  - `PressPlatenRenderer`: placa da prensa;
  - `CultivoClientEvents`.
- `gametest/CultivoGameTests`: 62 GameTests (61 + `serene_seasons_compat`), com os nomes em `ContentIds.GAME_TESTS`.
  - `serene_seasons_compat` sempre confere as tags em jogo; com o Serene Seasons na pasta de mods (só em `-Pcultivo_run_dir=run-compat-ss`), confere também a paridade com o trigo na primavera, sob vidro e no verão. Exige `out_of_season_crop_behavior = 1` e dispara o `TagsUpdatedEvent` como o cliente do single player.

Outras pastas:
- `src/test/java/.../core` e `.../data`: 316 testes JUnit.
  - `core`: regras puras.
  - `data`: completude e consistência dos recursos, nomes de paródia, português, sons, texturas animadas, isolamento do cliente, animação e dicas.
  - `data/CompatibilityTest` (5 testes): tags do Serene Seasons (as 40 lavouras e sementes, só verão e outono, sem planta silvestre), `sky_access` do test_instance, `mods.toml` forge `[66.0.2,)` e o padrão 66.0.13 do `build.gradle`.
  - Os testes leem os arquivos a partir da raiz do projeto.
- `src/generated/resources/data/sereneseasons/tags/{block,item}/{summer,autumn}_crops.json`: gerados; sem o Serene Seasons, ninguém os lê.
- `tools/gen/`: `manifest.json`, `generate.mjs` e `models.mjs`.
- `tools/art/`: `textures.mjs` e `png.mjs`.
- `tools/visual-probe/`:
  - `run-probe.ps1` + datapack de cena: abre o `runClient` num mundo de teste e tira 47 prints (só Windows com tela; ferramenta de desenvolvimento, fora do jar). Cobre tudo do polimento; `-Multiplayer` sobe também o servidor dedicado em 127.0.0.1 (precisa de bastante memória livre).
  - `gen-datapack.mjs` gera o datapack a partir do `manifest.json` (rode depois de mudar genéticas) e `validate.mjs` confere a sintaxe sem abrir o jogo. Os `.mcfunction` têm de ficar em LF (`.gitattributes`).
  - Prints e o `log-problems.txt` vão para a pasta de `-OutDir`; olhe os prints um por um, porque o script não julga a imagem.
- `tools/compat/` (teste de compatibilidade; instruções em `tools/compat/README.md`):
  - `forge-server-compat.sh <versão do Forge> [pasta com jars extras] [sonda]`: instala o servidor oficial daquele Forge (cache em `~/.cache/cultivo-compat`), copia o jar de `build/libs` e os extras para `run-compat-prod-<versão>[-extras]/mods`, sobe o servidor dedicado e manda os comandos da sonda pelo console; o resultado são as linhas "SONDA" de `console.out`. Variável opcional `CULTIVO_COMPAT_CONFIG` (pasta copiada para `config/`). Só Linux/macOS (bash, mkfifo).
  - `gen-probes.mjs` gera `probes/growth.txt` e `probes/seasons.txt` (4 plantas por condição, plataforma em y=120, bioma plains). Não edite as `.txt` à mão.
  - `config-cant-grow/sereneseasons/fertility.toml`: `out_of_season_crop_behavior = 1`, para resultados determinísticos.
  - As pastas `run-compat*` ficam fora do git. No Windows, os GameTests com o Serene Seasons rodam com `.\gradlew.bat runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-ss`, depois de pôr os 2 jars em `run-compat-ss\mods` e o `fertility.toml` em `run-compat-ss\config\sereneseasons`.
- `tools/test-offline.sh`: roda o JUnit sem Gradle nem Minecraft (Linux/macOS).
  - Rodado pela última vez em 24/10/2026 com 88 testes.
  - Pelo código, os dois testes de bytecode do `ClientIsolationTest` precisam das classes que o Gradle compila em `build/sourceSets/main`; sem um build anterior, eles dão erro "classes compiladas não encontradas".
- `docs/strains-catalog.json`: catálogo do dono, a fonte das genéticas.

## 6. Diferenças e armadilhas da API 26.3 (já confirmadas no build)

**Registro e blocos**
- `Identifier` substitui `ResourceLocation`. Blocos e itens precisam de `.setId(BLOCKS.key(...))` / `.setId(ITEMS.key(...))` nas Properties. Blocos não precisam mais de `codec()`.
- Item-semente que nomeia o crop: `new BlockItem(block, props.useItemDescriptionPrefix())`. Compostável: `Item.Properties#compostable(ContextIntProviders.COMPOSTABLE_LOW)`.
- `FarmBlock` virou `FarmlandBlock`. Os vidros tingidos estão em `Blocks.STAINED_GLASS.<cor>()`. `PushReaction.DESTROY` virou `POPPED`.
- `createBlockStateDefinition` roda dentro do construtor de `Block`, antes dos campos da subclasse; para uma propriedade a mais, use uma subclasse (como a `SieveBlock`). `BooleanProperty` tem `true` como primeiro valor: defina o padrão explicitamente.
- `Shapes.rotateHorizontal(shape)` devolve o mapa por direção. `Level.markAndNotifyBlock` só atualiza comparadores com o flag de vizinhos: para mudar o sinal, use `UPDATE_ALL`.

**Interação**
- `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` e `useWithoutItem(...)`. `TRY_WITH_EMPTY_HAND` só leva ao `useWithoutItem` na mão principal.
- Devolva `SUCCESS` no servidor quando o cliente já previu o clique. `SUCCESS_SERVER` faz o servidor repetir o balanço do braço para quem clicou.
- `PASS` deixa o vanilla seguir (usar o item, colocar o bloco, tentar a outra mão).
- O cliente não recebe receitas: preveja por tag de item e pelos slots sincronizados.
- Agachado, `ServerPlayerGameMode#useItemOn` só chama o bloco com as duas mãos vazias. Segurar o botão repete o clique a cada 4 ticks.
- `Inventory#add` com o jogador criativo e o inventário cheio zera a pilha e devolve true. Para entregar itens, use `placeItemBackInInventory(stack, false, Prediction.PREDICTED)` (`block/Handouts`). `Player#drop` recebe um `Prediction`.

**BlockEntity**
- Salva e carrega com `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` e `ContainerHelper`. O drop ao quebrar vai em `preRemoveSideEffects`. O tipo é `new BlockEntityType<>(factory, Set.of(block))`, e o ticker usa `createTickerHelper`. Modelo completo: `CampfireBlock`.
- Sincronização com o cliente: `getUpdatePacket()` + `getUpdateTag(...)` com só os itens, e `level.sendBlockUpdated(...)` quando o conteúdo muda.
- Recarregar como no load de chunk: `BlockEntity.loadStatic` + `Level#setBlockEntity`, que registra o ticker de novo.

**Componentes e receitas**
- Componentes: `DataComponentType.<T>builder().persistent(codec).networkSynchronized(streamCodec)`. Marca sem valor: `Unit.CODEC`, que é `{}` no JSON.
- `Recipe<T>`: `matches`, `assemble(T)`, `placementInfo` (use `PlacementInfo.create(ingredient)`; `NOT_PLACEABLE` some dos índices), `recipeBookCategory`. `RecipeSerializer` é um record `(MapCodec, StreamCodec)`; o resultado é `ItemStackTemplate`. No servidor, a busca é `serverLevel.recipeAccess().getRecipeFor(type, input, level)`.
- `crafting_transmute` copia só o *patch* de componentes da entrada: um valor igual ao padrão do protótipo não fica no patch. Por isso há um transmute por genética para o baseado.
- Livro de receitas: um avanço `data/<ns>/advancement/recipes/misc/<id>.json` por receita, com critérios `inventory_changed` por ingrediente + `recipe_unlocked`, uma só lista de requisitos (OU) e `rewards.recipes`.

**Loot, worldgen e eventos**
- Loot: o registro de funções é de `MapCodec`; `LootItemFunctionType` não existe. `LootItemConditionalFunction` recebe `Optional<Holder<LootItemCondition>>`. Os providers são holders (`ContextIntProviders.exactly/between`).
- JSON de loot: `condition`/`modifier` no lugar de `conditions`/`functions`, e o campo de tipo é `type`.
- `NestedLootTable` pede `Holder<LootTable>`, e o `LootTableLoadEvent` não dá o registro. Por isso `SeedLootInjector` monta o pool em Java; `loot_table/inject/village_seeds.json` é um espelho conferido por teste.
- Worldgen:
  - não existe mais `random_patch`;
  - feature: `{"type":"minecraft:simple_block","to_place":{...}}`;
  - o patch vai no placed feature (`count` + `minecraft:offset` + `block_predicate_filter`).
  - O biome modifier do Forge fica em `data/<ns>/forge/biome_modifier/`.
- Não há evento de trocas do mascate no Forge 26.3: o `WanderingTrader` usa três conjuntos fixos. Ofertas novas entram em `MobSpawnEvent.FinalizeSpawn`.
- O `HarvestFarmland` do aldeão quebra qualquer `CropBlock` maduro (`CropBlock.isMaxAge` é final) e só replanta sementes da tag vanilla. O Cultivo replanta reagindo ao `VanillaGameEvent` BLOCK_DESTROY.

**Recursos de cliente**
- Cada item precisa de `assets/<ns>/items/<id>.json`. Árvores usadas:
  - `minecraft:condition` com `minecraft:using_item` ou `minecraft:has_component`;
  - `minecraft:select` em `minecraft:display_context` (inclui `on_shelf`) ou em `minecraft:component`;
  - `minecraft:range_dispatch` em `minecraft:use_duration`;
  - `minecraft:composite`.
- Sem transformação de mão esquerda, o 26.3 usa a da direita.
- A camada de render vem da transparência da textura: alpha só 0 ou 255 mantém sólido ou cutout.
- Textura animada: tira vertical 16×(16·n) + `.png.mcmeta`.
- `sounds.json` com `"type": "event"` reaproveita um evento vanilla.
- Poses de braço próprias: `HumanoidModel.ArmPose.create(...)` do Forge, criadas no carregamento do item (`IClientItemExtensions`). `Item#initializeClient` só roda no cliente físico, dentro do construtor do item.
- Uma ArmPose estendida não troca `animateUseItem`, e não há gancho por frame para o item na 3ª pessoa: por isso a subida em 3ª pessoa usa 5 modelos-chave.
- Modelo de bloco sem bloco (a placa da prensa): estado sintético registrado em `ModelEvent.RegisterModelStateDefinitions`, como o vanilla faz com a moldura.

**GameTests no Forge 26.3**
- Funções em `Registries.TEST_FUNCTION` (`Consumer<GameTestHelper>`). Um JSON por teste em `data/cultivo/test_instance/` com a estrutura `forge:empty9x9x9`.
- `forge.enabledGameTestNamespaces=cultivo` está no build.gradle.
- Helpers usados:
  - `makeMockPlayer`, `makeMockServerPlayer`; e `player.gameMode.useItemOn(...)` para o clique real;
  - `runAfterDelay`, `succeedWhen`;
  - `spawn(..., EntitySpawnReason.EVENT)`.
- Sem `"sky_access": true` no test_instance, a área do teste ganha teto (`TestInstanceBlockEntity`, `hasCeiling`) e a planta não vê o céu.
- `ModList.isLoaded(modid)` é estático.
- Os GameTests nunca avançam em produção: `ForgeGameTestHooks.isGametestEnabled()` exige `!FMLLoader.isProduction()`, e só com isso o `MinecraftServer` chama `GameTestTicker.SINGLETON.tick()`. Num servidor instalado pelo instalador, `/test run cultivo:*` monta as estruturas e nunca termina; ali, use as sondas de console de `tools/compat/`.

**Servidor dedicado e compatibilidade** (confirmadas em 25/10/2026 na nuvem, Forge 66.0.2 e 66.0.13)
- `TagsUpdatedEvent` só no cliente: `ForgeEventFactory.onTagsUpdated` só é chamado pelo `ClientPacketListener` (`fromClientPacket=true`); o servidor nunca dispara o evento ao carregar os dados. Mod que monta listas nesse evento (o Serene Seasons, via GlitchCore) não funciona num servidor dedicado. Para simular o single player num GameTest: `ForgeEventFactory.onTagsUpdated(registryAccess, true, true)`.
- `pause-when-empty-seconds` (padrão 60) pausa o servidor sem jogadores ("Server empty for 60 seconds, pausing"): use 0 em teste sem jogador.
- `fillbiome` aceita no máximo 32768 blocos por comando.
- `level-type=minecraft:flat` sem `generator-settings` dá "No key layers in MapLike[{}]".

## 7. Histórico curto

- **21/10/2026, nuvem:** o MDK virou o mod, nasceram o MVP (3 variedades, estufa, estações, fontes de sementes, artigos) e a mecânica de fumar (pedida pelo dono). Os primeiros builds mostraram as diferenças da API listadas na seção 6.
- **24/10/2026, até as 16h35:**
  - o PR #1 levou o MVP e a mecânica de fumar para a `main`;
  - duas revisões adversariais (commits `1e7325a` e `9b0ef80`) entraram pelos PRs #2 e #3 (o merge do #3 é o `5bded22`);
  - no PC do dono, `build`, GameTests, `runServer` e um `runClient` que abriu sem erros, com o código de `5bded22`.
- **24/10/2026, depois disso**, branch `claude/polish-visual-uses`:
  - teste visual automático (`121cc4b`, `5533a37`);
  - correções da auditoria (`910549e`);
  - 37 genéticas do catálogo (`e467d58`);
  - arte nova (`191d082`);
  - usos dos artigos (`9cd902c`);
  - português, animações e sons (`16c7d3c`);
  - correções da revisão (`319cf0b`) e docs (`dbe06a4`).
- **25/10/2026, madrugada, PC do dono:** conferência própria de build e GameTests; teste visual estendido (47 cenas) no cliente real, que achou a fumaça cobrindo a tela em 1ª pessoa (corrigido em `f515cb9`); multiplayer local parcial; docs; branch enviada ao GitHub e mesclada pelo PR #4 (`6d6ed4a`).
- **25/10/2026, nuvem**, branch `claude/forge-compat-seasons`:
  - propriedades `-Pcultivo_forge_version`/`-Pcultivo_run_dir` e o script de servidor de produção (`0590b40`), com sondas de console no lugar do `/test` (`c737633`);
  - build e GameTests no Forge 66.0.2 do launcher do dono; achado que o Serene Seasons não age em servidor dedicado no Forge 26.3;
  - lavouras e sementes do Cultivo nas tags de verão e outono do Serene Seasons, `mods.toml` `[66.0.2,)`, `CompatibilityTest` e GameTest `serene_seasons_compat` (`2f6ae47`); estes docs.
- Detalhes e resultados de cada etapa estão em TODO.md (evidências) e em `git log`. O prompt da sessão de 21/10 está em `git show 319cf0b:docs/CLOUD_PROMPT.md`.

## 8. Pendências, em ordem

1. **Multiplayer completo** (PC do dono): `run-probe.ps1 -Multiplayer` com o PC folgado (o de 25/10 foi parado por falta de memória: são dois Minecraft e dois Gradle). Depois, dois jogadores de verdade numa estação e fumando perto um do outro (GUIA, seção 6.3).
2. **O que o teste visual não cobre** (PC do dono, seção 6.1 do guia): mão esquerda, artigos de todos os lados e ouvir os sons.
3. **Serene Seasons no PC do dono** (a compatibilidade e o Forge 66.0.2 já foram feitos e testados na nuvem em 25/10):
   - jogar em single player com o Serene Seasons e os outros mods do launcher e ver: dica "Estações férteis: Verão, Outono" nas sementes do Cultivo; lavoura lenta na primavera; normal sob vidro inteiro (GUIA_INICIANTE.md, seção 6);
   - decisões do dono (DECISIONS.md, "Em aberto"): manter a paridade com o trigo (verão e outono) ou outra regra; se usar `out_of_season_crop_behavior = 2`, pôr as lavouras do Cultivo em `unbreakable_infertile_crops` para não perder sementes raras; painéis contarem como estufa do Serene Seasons (mudaria o vanilla); e, se o grupo jogar em servidor dedicado, saber que lá as estações não afetam plantação nenhuma (bug do Forge 26.3, seção 6).
   - Numa atualização futura do Forge ou do Serene Seasons, repita a checagem com `tools/compat/`: `-Pcultivo_forge_version=<nova>` no `build` e no `runGameTestServer` (também com `-Pcultivo_run_dir=run-compat-ss`) e `forge-server-compat.sh <nova>` com as sondas.
4. **Teste visual automático:** o enquadramento de algumas cenas é largo (artigos, genéticas e "na mão" ficam pequenos); aproximar as câmeras ajudaria a revisar.
5. **Catálogo:** o filtro High THC da loja tinha cerca de 58 produtos num snapshot antigo; entraram 37. Se o dono colar a página, acrescente as que faltam (DECISIONS.md, "Como adicionar uma genética").
6. **Balanceamento** depois de jogar (pesos, raridade da planta silvestre, velocidades, preços do mascate).
7. **Opcionais** registrados nas etapas:
   - pote com funil (hopper);
   - brilho no pote curado;
   - ícone do suporte de secagem com maços;
   - silhueta por genética na lavoura;
   - áudio próprio no lugar dos eventos vanilla;
   - chama no isqueiro enquanto fuma.
8. **Limpeza:** apagar a sobra `GrowthRules.visualStage` e o teste dela. A última inspeção do jar registrada no TODO é do código `2f6ae47`; repita depois de mudar recursos ou dependências.
