# Plano do mod

Estado em 21/10/2026: **código, dados e assets do MVP escritos; o build Forge ainda não rodou nesta versão do código.**
Na sessão na nuvem, a política de rede bloqueou `maven.minecraftforge.net` e os servidores da Mojang
(`piston-meta.mojang.com`, `libraries.minecraft.net`, `launchermeta.mojang.com`), que o ForgeGradle exige. Por isso,
nesta sessão **não foi possível compilar o código que depende do Minecraft nem rodar GameTests, cliente ou servidor**.
Os itens abaixo só estão marcados quando há evidência real; o restante está escrito, mas falta verificar.

## Evidências registradas

| Data | Comando | Resultado real |
|---|---|---|
| 21/10/2026 (sessão local anterior) | `gradlew.bat build` no MDK de exemplo | `BUILD SUCCESSFUL in 5m 1s`, jar `build/libs/examplemod-1.0.0.jar` (registrado em NEXT_SESSION.md) |
| 21/10/2026 (nuvem) | `./gradlew build` / `test` / `runGameTestServer` (JDK Temurin 25.0.1+8) | **Falha na configuração**: `Failed to download default tool: minecraft-mavenizer-0.5.21.jar`. Proxy: `maven.minecraftforge.net:443 — connect_rejected` (403). O plugin ForgeGradle foi resolvido pelo Gradle Plugin Portal; nenhum código foi compilado. |
| 21/10/2026 (nuvem) | `JAVA_HOME=<jdk25> sh tools/test-offline.sh` | **62 testes, 62 OK, 0 falhas** (JUnit 5.12.2 via console standalone; compila `core/` + `src/test` com `javac --release 25`) |
| 21/10/2026 (nuvem) | teste negativo: remover `textures/item/bong.png` e rodar `tools/test-offline.sh` | 2 falhas (`everyItemHasItemDefinitionModelAndTexture`, `flatItemsHaveOwnTextures`), como esperado; arquivo restaurado e suíte de volta a 62/62 |
| 21/10/2026 (nuvem) | `node tools/gen/generate.mjs` (Node 22) | `42 texturas e 130 arquivos JSON gerados em src/generated/resources` |
| 21/10/2026 (nuvem) | `javac` sem classpath em `src/main/java` (só parse) | Nenhum erro de sintaxe; todos os erros restantes são símbolos do Minecraft/Forge não resolvidos (esperado sem os jars) |

## 1. Confirmar loader e criar base
- [x] Loader: Forge, mantido conforme NEXT_SESSION.md (sem dependência do grupo em NeoForge registrada).
- [x] Matriz fixada pelo MDK baixado: Minecraft 26.3, Forge 26.3-66.0.13, ForgeGradle `[7.0.17,8)`, Gradle 9.7.1, Java 25 (TECHNICAL_RESEARCH.md).
- [x] build.gradle, settings.gradle, gradle.properties e o exemplo do MDK inspecionados.
- [x] modId `cultivo`, pacote `io.github.peixeangola.cultivo`, versão 0.1.0; `examplemod`, `ExampleMod.java` e `Config.java` removidos; licença MIT (`LICENSE`), com a licença do MDK em `FORGE-MDK-LICENSE.txt`.
- [x] JUnit 5 no build.gradle (`testImplementation` + `useJUnitPlatform()`).
- [ ] `./gradlew build` do mod renomeado — **bloqueado na nuvem** (rede); rodar localmente.
- [ ] Iniciar cliente de desenvolvimento e servidor dedicado (`runClient`, `runServer`).
**Gate:** pendente — build reproduzível e ambos iniciam.

## 2. Conteúdo e cultivo
- [x] Regras puras com testes passando: qualidade (outdoor 2–3, estufa 4–5; bônus 0,20/0,35/0,50), crescimento (fórmula do trigo × variedade × 1,25 na estufa, luz ≥ 9, estágios 0–1/2–3/4–5/6–7), estufa (teto + 4 paredes de vidro até 6 blocos), fontes de sementes (4/5 aldeias, pesos 50/30/14/6, lavoura 0,05/0,025/0,01, silvestre 60/30/10).
- [x] Dados conferidos por JUnit: injeção de baú, `farm_plains` com as regras do mod antes das vanilla, loot da planta silvestre, biome modifiers de selva e planície, tags `crops`/`maintains_farmland`/`bee_growables`, loot de colheita com `cultivo:set_harvest_quality`, sem chaves de loot anteriores ao 26.3.
- [x] Assets originais completos conferidos por JUnit: todo bloco tem blockstate → modelo → textura 16×16; todo item tem `items/*.json` → modelo → textura; traduções `en_us` e `pt_br` com as mesmas chaves e todas as chaves usadas no Java; nenhuma textura órfã. Sons/partículas: só os vanilla (herdados do trigo, da madeira e da grama).
- [ ] Código Java de conteúdo (`CannabisCropBlock`, `WildPlantBlock`, registros, componente `cultivo:quality`, função de loot, injeção de baú, creative tab) — **escrito, ainda não compilado**.
- [ ] GameTests `outdoor_growth`, `greenhouse_quality`, `harvest_drops` — **escritos, ainda não executados**.
- [ ] Validar no servidor dedicado.
**Gate:** pendente — falta compilar e rodar os GameTests.

## 3. Processamento
- [x] Regras puras com testes: secagem 600, peneira 200 (+1 resina com qualidade ≥ 4), prensa 300 (2 resinas → 1 haxixe); saída cheia ou de outra qualidade congela o progresso; sem receita, zera.
- [x] Receitas JSON conferidas por JUnit: 11 artigos + 3 estações + baseado (`rolling_paper` + `#cultivo:dried_flowers`), e as receitas `cultivo:drying`/`sieving`/`pressing`.
- [ ] `ProcessingStationBlock`/`BlockEntity`, `ProcessingRecipe` e serializers — **escritos, ainda não compilados**.
- [ ] GameTests `drying_station`, `sieve_station`, `press_station`, `station_output_full`, `station_persistence`, `village_chest_seeds`, `article_recipes` — **escritos, ainda não executados**.
**Gate:** pendente.

## 4. Conteúdo de release
- [x] Licença escolhida (MIT para código e assets do Cultivo) e registrada em `mods.toml` e `LICENSE`.
- [ ] Inspecionar o jar (assets incluídos, nenhum arquivo do Minecraft) — depende do build.
- [ ] Rodar `./gradlew test`, `runGameTestServer`, `build`, `runClient` e `runServer` e corrigir as falhas.
**Gate:** pendente.

## Próximos passos (máquina com acesso ao maven do Forge e da Mojang)
1. `./gradlew build`. Corrigir os erros de compilação contra a API 26.3, consultando o `forge-26.3-66.0.13-sources.jar`. Os pontos menos certos estão listados em NEXT_SESSION.md, seção 6.
2. `./gradlew test` (deve repetir os 62 testes de `tools/test-offline.sh`).
3. `./gradlew runGameTestServer`: os 10 GameTests devem passar.
4. Conferir os JSONs de worldgen, loot e processor list carregando sem erro no log, comparando com os vanilla do `client-extra` jar.
5. `runClient`: olhar texturas e tooltips. `runServer`: entrar com um cliente e testar o ciclo inteiro.
6. Inspecionar o jar e atualizar esta tabela com os resultados.
