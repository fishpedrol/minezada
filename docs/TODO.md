# Plano do mod

**Estado em 24/10/2026, com o código do commit `319cf0b` (branch `claude/polish-visual-uses`).**

Compilam e passam no PC do dono:
- o MVP;
- a mecânica de fumar;
- o polimento de 24/10: 40 genéticas, arte nova, usos de todos os artigos, português em tudo, animações e sons.

Resultados: `build` com 311 testes JUnit e `runGameTestServer` com `All 62 required tests passed :)` (61 do Cultivo + `minecraft:always_pass`).

O cliente gráfico abriu sem erros de recurso no Windows, mas com o código de `5bded22`, antes do polimento. A checagem visual e o teste multiplayer continuam pendentes (checklist em [GUIA_INICIANTE.md](GUIA_INICIANTE.md), seção 6).

## Evidências registradas

Em ordem cronológica. "PC local" é o Windows 11 do dono com Temurin 25.0.4.1+1.

| Data | Comando | Resultado real |
|---|---|---|
| 21/10/2026 (sessão local anterior) | `gradlew.bat build` no MDK de exemplo | `BUILD SUCCESSFUL in 5m 1s` (registrado na época em NEXT_SESSION.md) |
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
| 24/10/2026 | `./gradlew runServer` (código `1e7325a`, antes da 2ª revisão) | **`Done (0.669s)!`**, nenhum WARN/ERROR ligado ao Cultivo |
| 24/10/2026 | inspeção do jar (código `1e7325a`) | 305 entradas, 43 PNGs (inclui `textures/mob_effect/high.png`), 0 arquivos do Minecraft |
| 24/10/2026 | `sh tools/test-offline.sh` / `node tools/gen/generate.mjs` (código `1e7325a`) | 88 de 88 / 43 texturas e 149 JSONs |
| 24/10/2026 | 2ª revisão por workflow (45 agentes: correção dos fixes, validade dos testes, consistência dos docs + guia) | 18 achados confirmados, 1 refutado. Principal: a regra "Velocidade × Lentidão se anulam" apagava efeitos de **poções** (exploit com Mestre Tartaruga; perda de Velocidade longa); a regra foi retirada. Também: "chapado" infinito por comando agora é recusado; testes passam a usar os valores literais da especificação; docs corrigidos. |
| 24/10/2026 | teste negativo: recolocar a remoção do efeito oposto e o infinito como 0 | `cultivo:smoke_keeps_potion_effects failed ... a Lentidão IV da poção sumiu` e `cultivo:smoke_infinite_high_refused failed ... fumou com efeito infinito`: os testes novos pegam os dois bugs. Correção restaurada. |
| 24/10/2026 | `./gradlew build` / `runGameTestServer` (versão final da 2ª revisão, código `9b0ef80`) | **`BUILD SUCCESSFUL in 33s`**, 88 testes `PASSED`; **`All 30 required tests passed :)`** (`30 GAME TESTS COMPLETE IN 2.585 s`) |
| 24/10/2026 | PC local (`main` em `5bded22`): `.\gradlew.bat build` / `.\gradlew.bat runGameTestServer` | **`BUILD SUCCESSFUL in 29s`**, 88 testes, 0 falhas / **`All 30 required tests passed :)`** (`30 GAME TESTS COMPLETE IN 929.7 ms`) |
| 24/10/2026 | PC local: `node tools/gen/generate.mjs` (Node 24.19) | 43 texturas e 151 JSONs; `git status` sem diferença (o gerador reproduz os arquivos versionados) |
| 24/10/2026 | PC local: `.\gradlew.bat runServer` | **`Done (2.277s)!`**; erros só do ambiente (`server.properties` na 1ª execução, netty kqueue/epoll) |
| 24/10/2026 | PC local: `.\gradlew.bat runClient` (código `5bded22`) | Janela abre; `Reloading ResourceManager: vanilla, vanilla, mod:cultivo, mod:forge`, `Sound engine started`; nenhum modelo/textura faltando nem WARN/ERROR do Cultivo (só Realms sem conta e config do Forge na 1ª execução). Checagem visual (seção 6 do guia) ainda não feita. |
| 24/10/2026 | PC local, correções da auditoria (commit `910549e`): `.\gradlew.bat build --console=plain` / `.\gradlew.bat runGameTestServer --console=plain` / `node tools/gen/generate.mjs` | **`BUILD SUCCESSFUL in 5s`**, 94 testes, 0 falhas, 0 erros / **`All 35 required tests passed :)`** / 43 texturas e 176 JSONs. Teste negativo: com `getTicker` devolvendo null, `station_real_ticker` e `station_reload_resumes` falham; restaurado. |
| 24/10/2026 | PC local, 37 genéticas do catálogo (commit `e467d58`): os mesmos três comandos | **`BUILD SUCCESSFUL in 7s`**, 198 testes, 0 falhas / **`All 39 required tests passed :)`** / 302 texturas e 735 JSONs. Teste negativo: um nome real no `en_us.json` gerado faz `ParodyNamesTest.noRealStrainOrBreederNameInGameFiles` falhar; regerar restaura o arquivo, e uma 2ª rodada do gerador não muda nada. |
| 24/10/2026 | PC local, arte nova (commit `191d082`): os mesmos três comandos | **`BUILD SUCCESSFUL in 4s`**, 215 testes, 0 falhas / **`All 40 required tests passed :)`** / 272 texturas e 704 JSONs. Testes negativos: adulterar os assets gerados derruba os 10 testes visados; desligar `syncVisualState` derruba só `station_visual_state`. Tudo restaurado e regerado. |
| 24/10/2026 | PC local, usos dos artigos (commit `9cd902c`): os mesmos três comandos | **`BUILD SUCCESSFUL in 7s`**, 256 testes, 0 falhas / **`All 47 required tests passed :)`** / 281 texturas e 733 JSONs. Testes negativos: quebrar o bônus do dichavador, a metade da náusea, o gancho do cinzeiro, a marca de cura e o consumo da bandeja derruba exatamente `sieve_grinder`, `smoke_filtered_joint`, `ashtray_collects_ash`, `jar_cures_flowers` e `tray_rolls_joints`; quebrar a receita gerada `joint_filtered` e o loot do pote derruba `joint_filter_recipe`, `smoke_filtered_joint`, `jar_cures_flowers` e `placeable_articles_drop_self`. Restaurado e regerado. |
| 24/10/2026 | PC local, português em tudo, animações e sons (commit `16c7d3c`): os mesmos três comandos | **`BUILD SUCCESSFUL in 7s`**, 295 testes, 0 falhas / **`All 50 required tests passed :)`** / 283 texturas e 769 JSONs. Testes negativos: a flag desligada, 4 quadros-chave e frametime 0 derrubam 8 testes visados; um import de pacote de cliente no código comum, uma constante de braço trocada e um som vanilla no `AshtrayBlock` derrubam 6. Restaurado; o gerador dá o mesmo hash numa 2ª rodada. |
| 24/10/2026 | PC local, correções da revisão (commit `319cf0b`): os mesmos três comandos | **`BUILD SUCCESSFUL in 12s`** (`5 actionable tasks: 4 executed, 1 up-to-date`); XML do JUnit: 311 testes, 0 skipped, 0 falhas, 0 erros / **`All 62 required tests passed :)`** (`62 GAME TESTS COMPLETE IN 2.020 s`) / 285 texturas e 786 JSONs. Teste negativo: com os bugs originais reinjetados de uma vez, os 16 GameTests visados falham com as mensagens esperadas; fontes restauradas byte a byte. |

**Conferência dos formatos:**
- Os JSONs gerados foram conferidos contra os vanilla do `client-extra-26.3` jar: `loot_table/blocks/wheat.json`, `chests/village/village_plains_house.json`, `processor_list/farm_plains.json` (as regras vanilla do mod são idênticas às do jar), `feature/melon.json`, `placed_feature/patch_melon.json`, `recipe/bowl.json`/`book.json` e `test_instance/always_pass.json`.
- Os formatos novos de 24/10 foram conferidos contra o código e os JSONs do 26.3 e pelos testes JUnit de `data/`, mas os de cliente só carregam de verdade num `runClient`, ainda pendente. São eles:
  - avanços de receita;
  - definições de item com `condition`/`select`/`range_dispatch`/`composite`;
  - blockstates multipart;
  - `sounds.json`;
  - `.png.mcmeta`.
- O salto de 149 para 151 JSONs entre as linhas do código `1e7325a` e as do PC local vem dos 2 `test_instance` novos do commit `9b0ef80`.

## 1. Confirmar loader e criar base
- [x] Loader: Forge, mantido conforme NEXT_SESSION.md (sem dependência do grupo em NeoForge registrada).
- [x] Matriz fixada pelo MDK baixado: Minecraft 26.3, Forge 26.3-66.0.13, ForgeGradle `[7.0.17,8)`, Gradle 9.7.1, Java 25 (TECHNICAL_RESEARCH.md).
- [x] build.gradle, settings.gradle, gradle.properties e o exemplo do MDK inspecionados.
- [x] modId `cultivo`, pacote `io.github.peixeangola.cultivo`, versão 0.1.0; `examplemod`, `ExampleMod.java` e `Config.java` removidos; licença MIT (`LICENSE`), com a licença do MDK em `FORGE-MDK-LICENSE.txt`.
- [x] JUnit 5 no build.gradle (`testImplementation` + `useJUnitPlatform()`).
- [x] `./gradlew build` do mod renomeado: `BUILD SUCCESSFUL`.
- [x] Servidor dedicado (`runServer`): `Done (6.116s)!`; último no PC local com o código de `5bded22` (`Done (2.277s)!`).
- [x] Cliente de desenvolvimento (`runClient`) inicia no Windows com o mod carregado e sem erros de recurso (24/10/2026, código `5bded22`).
- [ ] `runServer` e `runClient` com o código atual: o último de cada um foi em `5bded22`, antes do polimento.
- [ ] Checagem visual no cliente (seção 6 do guia).

**Gate:** servidor OK; o cliente iniciou antes do polimento; checagem visual pendente.

## 2. Conteúdo e cultivo
- [x] **Regras puras** com testes passando:
  - qualidade: outdoor 2–3, estufa 4–5; bônus 0,20/0,35/0,50 pela potência;
  - crescimento: fórmula do trigo × crescimento da genética (×1,25/×1,0/×0,8) × 1,25 na estufa, luz ≥ 9;
  - estufa: teto + 4 paredes de vidro até 6 blocos;
  - fontes de sementes: 4/5 aldeias; pesos por raridade 4/2/1 com vazio 119; lavoura 0,05/0,025/0,01; silvestre 60/30/10; mascate com 2 ofertas a 2/4/8 esmeraldas.
- [x] **40 genéticas** (3 nativas + 37 paródias do catálogo) com a mesma tabela no Java, no manifesto e em `docs/strains-catalog.json`. Testes conferem as três; nenhum nome real nos arquivos do jogo.
- [x] **Dados conferidos por JUnit:**
  - injeção de baú com as 40 genéticas;
  - `farm_plains` com as regras do mod antes das vanilla;
  - loot da planta silvestre e biome modifiers de selva e planície;
  - tags `crops`/`maintains_farmland`/`bee_growables` e `mineable`;
  - loot de colheita com `cultivo:set_harvest_quality`;
  - nenhuma chave de loot anterior ao 26.3.
- [x] **Assets originais completos conferidos por JUnit:**
  - todo bloco tem blockstate (variantes ou multipart) → modelo → textura;
  - todo item tem `items/*.json` → modelo → textura;
  - 8 estágios de lavoura distintos por genética;
  - ícones distintos entre genéticas;
  - texturas animadas com `.mcmeta`;
  - traduções completas, com `en_us` = `pt_br`;
  - 16 eventos de som com legenda;
  - nenhuma textura órfã.
- [x] Código Java de conteúdo compila contra o Forge 26.3-66.0.13.
- [x] **GameTests:**
  - cultivo: `outdoor_growth`, `greenhouse_quality`, `harvest_drops`, `catalog_strain_grows_and_drops`;
  - fontes de sementes: `village_chest_seeds` (1600 baús com semente fixa: 45–55% com semente, as 40 genéticas aparecem, médias dentro de ±25%), `wandering_trader_catalog_seeds`, `villager_replants_mod_crop`;
  - organização e estado: `creative_tabs_split`, `block_states_match_ids`.
- [x] Dados carregam no servidor de GameTest: os 62 GameTests usam as receitas, o loot e as tags carregados.
- [ ] Checagem visual no cliente (precisa do cliente):
  - planta silvestre no mundo e lavouras de aldeia;
  - os 8 estágios;
  - os ícones das genéticas;
  - o mascate com sementes.

**Gate:** regras e drops OK no servidor; checagem visual pendente.

## 3. Processamento
- [x] **Regras puras com testes:**
  - secagem 600, peneira 200 (+1 resina com qualidade ≥ 4, +1 com dichavador), prensa 300 (2 resinas → 1 haxixe);
  - saída cheia ou de outra qualidade/genética congela o progresso; sem receita, zera;
  - estado visual vazia/carregada/trabalhando/pronta.
- [x] **Receitas JSON conferidas por JUnit:**
  - 55 receitas de crafting (11 artigos + 3 estações + 40 baseados `crafting_transmute` + baseado com piteira), cada uma com avanço do livro de receitas;
  - as receitas `cultivo:drying` (uma por genética), `sieving` e `pressing`;
  - as tags de entrada das estações iguais aos ingredientes.
- [x] Estações, `ProcessingRecipe` e serializers compilam.
- [x] **GameTests:**
  - estações e persistência: `drying_station`, `sieve_station`, `press_station`, `station_output_full`, `station_persistence`, `station_real_ticker` (só ticks do mundo), `station_reload_resumes` (recarregar a BlockEntity no meio);
  - cliques e entrega: `station_block_interaction` e `station_hold_click_keeps_input` (cliques reais), `interactions_swing_once`, `full_inventory_drops_handouts`, `creative_keeps_held_stacks`, `client_prediction_matches_server` (182 combinações);
  - visual e efeitos: `station_visual_state`, `station_stalled_visual`, `station_effects_run`;
  - dichavador: `sieve_grinder`, `sieve_grinder_shape`;
  - receitas: `article_recipes`.
- [ ] Teste manual com um cliente conectado ao servidor dedicado, com dois jogadores na mesma estação.

**Gate:** OK no servidor/GameTest; teste manual multiplayer pendente.

## 3b. Mecânica de fumar (pedida pelo usuário em 21/10/2026)
- [x] **Regras puras** (`SmokeRules`, `SmokeDevice`) com JUnit:
  - durações exatas por artigo/qualidade/haxixe;
  - soma até 6 min, recusa perto do limite, nível II a partir de 2 min;
  - perfis por genética (também as do catálogo);
  - ordem e mensagens das recusas, isqueiro de 64 usos;
  - piteira (×1,25 e metade da náusea depois do teto).
- [x] Efeito `cultivo:high` com ícone 18×18 original; componentes `cultivo:strain` e `cultivo:filtered`; baseado por `crafting_transmute`; peneira e prensa levam a genética para resina e haxixe.
- [x] **GameTests:**
  - receitas e genética: `joint_recipe_copies_quality`, `joint_strain_persists`, `hash_keeps_strain`, `catalog_joint_carries_strain`, `joint_filter_recipe`;
  - efeitos e durações: `smoke_joint_effects`, `smoke_bong_hash_stronger`, `smoke_stacking_and_cap`, `smoke_resmoke_after_heavy`, `smoke_hash_multiplier`, `smoke_joint_without_strain`, `smoke_keeps_potion_effects`, `smoke_filtered_joint`;
  - isqueiro e material: `smoke_requires_lighter_and_fuel`, `smoke_pipe_uses_fuel`, `smoke_offhand_fuel_first`, `smoke_lighter_breaks`, `smoke_creative_consumes_nothing`;
  - recusas: `smoke_at_cap_refused`, `smoke_refusal_messages`, `smoke_infinite_high_refused`, `smoke_refused_at_finish_explains`;
  - registro, uso real e dicas: `smoke_devices_registered`, `smoke_use_effects_run` (os 5 artigos fumados tick a tick), `tooltips_show_profile_and_marks`.
- [ ] **Ver no cliente** (precisa de `runClient`):
  - animação nova em 1ª e 3ª pessoa, nas duas mãos;
  - modelos na mão e acesos;
  - partículas e sons;
  - ícone e nome do efeito, náusea;
  - tooltips e mensagens.
- [ ] Testar em multiplayer: dois jogadores fumando no servidor dedicado, cada um vendo a animação e a fumaça do outro.

**Gate:** regras e servidor OK; checagem visual e multiplayer pendentes.

## 3c. Artigos com uso (24/10/2026)
- [x] **Regras puras com JUnit:**
  - bandeja (`RollingRules`: 2 flores + 3 folhas → 3 baseados, mão secundária);
  - pote (`CuringRules`: 2400 ticks, +1 até 5, uma vez, comparador);
  - cinzeiro (`AshtrayRules`: 0–4, alcance 4/2 com 405 posições, comparador ×3);
  - dichavador (`ProcessingRules`).
- [x] **GameTests:**
  - bandeja: `tray_rolls_joints`, `tray_rolls_from_off_hand`;
  - pote: `jar_cures_flowers`, `jar_reload_resumes`;
  - cinzeiro: `ashtray_collects_ash`, `ashtray_search_edges`;
  - blocos colocados: `placeable_articles_drop_self`.
- [ ] **Ver no cliente:**
  - bandeja, pote (vazio e cheio) e cinzeiro (0 a 4) de todos os lados;
  - peneira com dichavador;
  - baseado com piteira na mão.

## 3d. Português, animações e sons (pedido de 24/10/2026)
- [x] Flag `portuguese_everywhere` com `en_us` = `pt_br`, textos revisados e metadado em português (`PortugueseEverywhereTest`).
- [x] 16 eventos de som com legenda (`SoundsTest`, GameTest `sound_events_registered`).
- [x] Texturas animadas (`AnimatedTexturesTest`).
- [x] Animação de fumar e placa da prensa conferidas por regra pura e pelos modelos gerados (`SmokeAnimationTest`, `SmokeAnimationAssetsTest`, `StationAnimationTest`).
- [x] Isolamento do código de cliente (`ClientIsolationTest`).
- [ ] Ouvir os sons e ver as animações e partículas no cliente.

## 4. Conteúdo de release
- [x] Licença escolhida (MIT para código e assets do Cultivo) e registrada em `mods.toml` e `LICENSE`.
- [x] Jar inspecionado: assets incluídos, nenhum arquivo do Minecraft (última inspeção registrada: código `1e7325a`).
- [ ] Inspecionar e registrar o jar do código atual.
- [x] `build` (com `test`) e `runGameTestServer` com o código atual; `runServer` com o código de `5bded22`.
- [ ] `runClient` com o código atual e conexão de um cliente ao servidor dedicado.

**Gate:** pendente do cliente e de registrar o jar atual.

## Próximos passos
O passo a passo para leigos está em [GUIA_INICIANTE.md](GUIA_INICIANTE.md); a lista completa, em [NEXT_SESSION.md](NEXT_SESSION.md), seção 8.

1. **`runClient`** no PC do dono:
   - a checklist da seção 6.1 do guia;
   - o teste visual automático (`tools/visual-probe/run-probe.ps1`, seção 3.8 do guia).
2. **`runServer` + cliente conectado:** ciclo completo em multiplayer (plantar, colher, secar, peneirar, prensar, craftar, curar, enrolar, fumar).
3. **Mundo novo:** planta silvestre na selva e na planície, lavouras de aldeia, baús de aldeia e um mascate com sementes do catálogo.
4. **Ambiente do dono:** testar com o Serene Seasons e decidir a faixa do Forge no `mods.toml`.
5. **Balanceamento** depois de jogar (DECISIONS.md, "Em aberto").
