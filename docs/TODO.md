# Plano do mod

Estado em 24/10/2026: **MVP + mecânica de fumar compilados, testados e rodando em Forge 26.3-66.0.13.** `./gradlew build` passa (88 testes JUnit), os GameTests passam (30 de 30 obrigatórios, incluindo os 29 do Cultivo) e o servidor dedicado sobe com o mod. Falta testar com o cliente gráfico e com um jogador conectado (checklist em [GUIA_INICIANTE.md](GUIA_INICIANTE.md)).

## Evidências registradas

| Data | Comando | Resultado real |
|---|---|---|
| 21/10/2026 (sessão local anterior) | `gradlew.bat build` no MDK de exemplo | `BUILD SUCCESSFUL in 5m 1s` (registrado em NEXT_SESSION.md) |
| 21/10/2026 (nuvem, antes de liberar a rede) | `./gradlew build` | Falha na configuração: `Failed to download default tool: minecraft-mavenizer-0.5.21.jar` (`maven.minecraftforge.net` recusado pelo proxy) |
| 21/10/2026 (nuvem, rede liberada) | `./gradlew compileJava` (1ª vez) | 8 erros de API do 26.3 (`LootItemFunctionType` removido, `ConstantValue` movido, `FarmBlock` → `FarmlandBlock`); depois de corrigidos, mais 3 (`NestedLootTable` pede `Holder`, `Blocks.STAINED_GLASS.lime()`, `Player#drop(…, Prediction)`). Todos corrigidos. |
| 21/10/2026 | `./gradlew build` (Temurin 25.0.1+8) | **`BUILD SUCCESSFUL in 12s`**, 62 testes `PASSED`, 0 `FAILED`; jar `build/libs/cultivo-0.1.0.jar` (267 entradas) |
| 21/10/2026 | `./gradlew runGameTestServer` | **`All 11 required tests passed :)`** (`11 GAME TESTS COMPLETE IN 1.781 s`), sem avisos de dados do Cultivo no log |
| 21/10/2026 | teste negativo: forçar falha em `article_recipes` | `1 required tests failed :( - cultivo:article_recipes: FALHA PROPOSITAL bong`; a task falha. Teste restaurado; nova rodada com 11 de 11. |
| 21/10/2026 | `./gradlew runServer` (EULA aceita em `run/`) | **`Done (6.116s)!`**. Nenhum WARN/ERROR do Cultivo; os erros no log são do ambiente (sem `server.properties` na 1ª execução, serviços de autenticação inacessíveis, aviso do netty kqueue). Se loot, worldgen, biome modifiers e `farm_plains` não carregassem, o servidor não subiria. |
| 21/10/2026 | inspeção do jar | Só `io/github/...`, `assets/cultivo`, `data/cultivo`, `META-INF/mods.toml`, `pack.mcmeta` e os overrides de dados do próprio mod em `data/minecraft` (3 tags + `farm_plains.json`). 42 PNGs. Nenhum arquivo do jogo. `mods.toml`: `cultivo` 0.1.0, MIT, forge `[66,)`, minecraft `[26.3,26.4)`. |
| 21/10/2026 | `sh tools/test-offline.sh` | 62 de 62 (mesma suíte, sem Gradle) |
| 21/10/2026 | `node tools/gen/generate.mjs` | 42 texturas e 130 JSONs |
| 21/10/2026 | mecânica de fumar (commit `740bf86`): `./gradlew build` / `runGameTestServer` | `BUILD SUCCESSFUL`, 80 testes `PASSED`; 1ª rodada de GameTests com 3 falhas, todas do próprio teste (referência a pilha antiga após `Inventory#add`); corrigido o teste, `All 17 required tests passed :)` |
| 24/10/2026 | revisão adversarial por workflow (54 agentes: 4 revisores + 2 céticos por achado + guia) | 22 achados confirmados, 1 refutado. Bug real: refumar enquanto um "chapado" nível II acabava caía no "efeito escondido" do vanilla e não somava tempo (gastando itens). Todos os 22 tratados no commit `1e7325a`. |
| 24/10/2026 | teste negativo: voltar `forceAddEffect` para `addEffect` | `cultivo:smoke_resmoke_after_heavy failed ... 500 + 900 = 1400 no nível I, veio 500`: o novo teste pega o bug. Correção restaurada. |
| 24/10/2026 | `./gradlew build` (após as correções) | **`BUILD SUCCESSFUL in 1m 11s`**, 88 testes `PASSED`, 0 `FAILED` |
| 24/10/2026 | `./gradlew runGameTestServer` (após as correções) | 1ª rodada: 2 falhas de teste (contagem de ticks com erro de 1; saída pré-carregada sem variedade em `station_persistence`, que agora não empilha por causa da variedade na resina, comportamento esperado). Testes ajustados: **`All 28 required tests passed :)`** |
| 24/10/2026 | 2ª revisão por workflow (45 agentes: correção dos fixes, validade dos testes, consistência dos docs + guia) | 18 achados confirmados, 1 refutado. Principal: a regra "Velocidade × Lentidão se anulam" apagava efeitos de **poções** (exploit com Mestre Tartaruga; perda de Velocidade longa); a regra foi retirada. Também: "chapado" infinito por comando agora é recusado; testes passam a usar os valores literais da especificação; docs corrigidos. |
| 24/10/2026 | teste negativo: recolocar a remoção do efeito oposto e o infinito como 0 | `cultivo:smoke_keeps_potion_effects failed ... a Lentidão IV da poção sumiu` e `cultivo:smoke_infinite_high_refused failed ... fumou com efeito infinito`: os testes novos pegam os dois bugs. Correção restaurada. |
| 24/10/2026 | `./gradlew build` / `runGameTestServer` (versão final) | **`BUILD SUCCESSFUL in 33s`**, 88 testes `PASSED`; **`All 30 required tests passed :)`** (`30 GAME TESTS COMPLETE IN 2.585 s`) |
| 24/10/2026 | `./gradlew runServer` | **`Done (0.669s)!`**, nenhum WARN/ERROR ligado ao Cultivo |
| 24/10/2026 | inspeção do jar | 305 entradas, 43 PNGs (inclui `textures/mob_effect/high.png`), 0 arquivos do Minecraft |
| 24/10/2026 | `sh tools/test-offline.sh` / `node tools/gen/generate.mjs` | 88 de 88 / 43 texturas e 149 JSONs |
| 24/10/2026 | PC local (Windows 11, Temurin 25.0.4.1+1, `main` em `5bded22`): `.\gradlew.bat build` / `.\gradlew.bat runGameTestServer` | **`BUILD SUCCESSFUL in 29s`**, 88 testes, 0 falhas / **`All 30 required tests passed :)`** (`30 GAME TESTS COMPLETE IN 929.7 ms`) |
| 24/10/2026 | PC local: `node tools/gen/generate.mjs` (Node 24.19) | 43 texturas e 151 JSONs; `git status` sem diferença (o gerador reproduz os arquivos versionados) |
| 24/10/2026 | PC local: `.\gradlew.bat runServer` | **`Done (2.277s)!`**; erros só do ambiente (`server.properties` na 1ª execução, netty kqueue/epoll) |
| 24/10/2026 | PC local: `.\gradlew.bat runClient` | Janela abre; `Reloading ResourceManager: vanilla, vanilla, mod:cultivo, mod:forge`, `Sound engine started`; nenhum modelo/textura faltando nem WARN/ERROR do Cultivo (só Realms sem conta e config do Forge na 1ª execução). Checagem visual (seção 6 do guia) ainda não feita. |

Os JSONs gerados foram conferidos contra os vanilla do `client-extra-26.3` jar: `loot_table/blocks/wheat.json`, `chests/village/village_plains_house.json`, `processor_list/farm_plains.json` (as regras vanilla do mod são idênticas às do jar), `feature/melon.json`, `placed_feature/patch_melon.json`, `recipe/bowl.json`/`book.json` e `test_instance/always_pass.json`.

## 1. Confirmar loader e criar base
- [x] Loader: Forge, mantido conforme NEXT_SESSION.md (sem dependência do grupo em NeoForge registrada).
- [x] Matriz fixada pelo MDK baixado: Minecraft 26.3, Forge 26.3-66.0.13, ForgeGradle `[7.0.17,8)`, Gradle 9.7.1, Java 25 (TECHNICAL_RESEARCH.md).
- [x] build.gradle, settings.gradle, gradle.properties e o exemplo do MDK inspecionados.
- [x] modId `cultivo`, pacote `io.github.peixeangola.cultivo`, versão 0.1.0; `examplemod`, `ExampleMod.java` e `Config.java` removidos; licença MIT (`LICENSE`), com a licença do MDK em `FORGE-MDK-LICENSE.txt`.
- [x] JUnit 5 no build.gradle (`testImplementation` + `useJUnitPlatform()`).
- [x] `./gradlew build` do mod renomeado: `BUILD SUCCESSFUL`.
- [x] Servidor dedicado (`runServer`): `Done (6.116s)!`.
- [x] Cliente de desenvolvimento (`runClient`) inicia no Windows com o mod carregado e sem erros de recurso (24/10/2026).
- [ ] Checagem visual no cliente (seção 6 do guia).
**Gate:** servidor OK; cliente inicia; checagem visual pendente.

## 2. Conteúdo e cultivo
- [x] Regras puras com testes passando: qualidade (outdoor 2–3, estufa 4–5; bônus 0,20/0,35/0,50), crescimento (fórmula do trigo × variedade × 1,25 na estufa, luz ≥ 9, estágios 0–1/2–3/4–5/6–7), estufa (teto + 4 paredes de vidro até 6 blocos), fontes de sementes (4/5 aldeias, pesos 50/30/14/6, lavoura 0,05/0,025/0,01, silvestre 60/30/10).
- [x] Dados conferidos por JUnit: injeção de baú, `farm_plains` com as regras do mod antes das vanilla, loot da planta silvestre, biome modifiers de selva e planície, tags `crops`/`maintains_farmland`/`bee_growables`, loot de colheita com `cultivo:set_harvest_quality`, sem chaves de loot anteriores ao 26.3.
- [x] Assets originais completos conferidos por JUnit: todo bloco tem blockstate → modelo → textura 16×16; todo item tem `items/*.json` → modelo → textura; traduções `en_us` e `pt_br` com as mesmas chaves e todas as chaves usadas no Java; nenhuma textura órfã. Sons/partículas: só os vanilla (herdados do trigo, da madeira e da grama).
- [x] Código Java de conteúdo compila contra o Forge 26.3-66.0.13.
- [x] GameTests `outdoor_growth` (planta em farmland, cresce até a idade 7, não sobrevive em pedra), `greenhouse_quality` (estufa 4–5, outdoor 2–3) e `harvest_drops` (madura: flor + sementes; imatura: 1 semente; silvestre: sementes) passam.
- [x] Dados carregam no servidor dedicado sem erro.
- [ ] Checagem visual da planta silvestre no mundo e das lavouras de aldeia (precisa do cliente).
**Gate:** regras e drops OK no servidor; checagem visual pendente.

## 3. Processamento
- [x] Regras puras com testes: secagem 600, peneira 200 (+1 resina com qualidade ≥ 4), prensa 300 (2 resinas → 1 haxixe); saída cheia ou de outra qualidade/variedade congela o progresso; sem receita, zera.
- [x] Receitas JSON conferidas por JUnit: 11 artigos + 3 estações + 3 baseados (`crafting_transmute` `joint_{brisa,aurora,eclipse}`: flor seca da variedade + `rolling_paper`, copia a qualidade e grava `cultivo:strain`), e as receitas `cultivo:drying`/`sieving`/`pressing`.
- [x] Estações, `ProcessingRecipe` e serializers compilam.
- [x] GameTests `drying_station`, `sieve_station`, `press_station`, `station_output_full` (saída cheia/outra qualidade congela, sem perda nem duplicação), `station_persistence` (save/load da BE; ao quebrar, os itens caem), `village_chest_seeds` (4 tabelas geram sementes; Brisa > 3× Eclipse) e `article_recipes` (17 grades: 11 artigos, 3 baseados transmute e 3 estações, resolvem para o item do mod) passam.
- [ ] Teste manual com um cliente conectado ao servidor dedicado.
**Gate:** OK no servidor/GameTest; teste manual multiplayer pendente.

## 3b. Mecânica de fumar (pedida pelo usuário em 21/10/2026)
- [x] Regras puras (`SmokeRules`, `SmokeDevice`) com JUnit: durações exatas por artigo/qualidade/haxixe, soma até 6 min, recusa perto do limite, nível II a partir de 2 min, perfis por variedade, ordem e mensagens das recusas, isqueiro de 64 usos, constantes da especificação (6 min, 2 min, 30 s) com valores literais.
- [x] Efeito `cultivo:high` com ícone 18×18 original; componente `cultivo:strain`; baseado por `crafting_transmute` (qualidade copiada, variedade gravada); peneira e prensa levam a variedade para resina e haxixe.
- [x] GameTests: `joint_recipe_copies_quality`, `smoke_joint_effects`, `smoke_requires_lighter_and_fuel`, `smoke_pipe_uses_fuel`, `smoke_bong_hash_stronger`, `smoke_stacking_and_cap`, `smoke_resmoke_after_heavy`, `smoke_at_cap_refused`, `smoke_hash_multiplier`, `smoke_offhand_fuel_first`, `smoke_lighter_breaks`, `smoke_creative_consumes_nothing`, `smoke_joint_without_strain`, `smoke_refusal_messages`, `joint_strain_persists`, `hash_keeps_strain`, `smoke_devices_registered`, `smoke_keeps_potion_effects`, `smoke_infinite_high_refused`, todos passando.
- [ ] Ver no cliente: animação de levar à boca, fumaça, sons, ícone e nome do efeito, náusea, tooltips e mensagens (precisa de `runClient`).
- [ ] Testar em multiplayer: dois jogadores fumando no servidor dedicado.
**Gate:** regras e servidor OK; checagem visual e multiplayer pendentes.

## 4. Conteúdo de release
- [x] Licença escolhida (MIT para código e assets do Cultivo) e registrada em `mods.toml` e `LICENSE`.
- [x] Jar inspecionado: assets incluídos, nenhum arquivo do Minecraft.
- [x] `build` (com `test`), `runGameTestServer` e `runServer` rodados e passando.
- [ ] `runClient` e conexão de um cliente ao servidor dedicado.
**Gate:** pendente só do cliente.

## Próximos passos
O passo a passo para leigos está em [GUIA_INICIANTE.md](GUIA_INICIANTE.md).
1. `./gradlew runClient` numa máquina com display: olhar texturas, tooltips, a aba criativa, a estufa e a mecânica de fumar (animação, fumaça, ícone do efeito).
2. `./gradlew runServer` + cliente conectado: ciclo completo em multiplayer (plantar, colher, secar, peneirar, prensar, craftar).
3. Num mundo novo, procurar a planta silvestre em selva/planície e o crop nas lavouras de aldeia de planície.
4. Balanceamento depois de jogar (DECISIONS.md, "Em aberto").
