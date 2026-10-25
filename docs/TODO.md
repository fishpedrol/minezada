# Plano do mod

**Estufinha (25/10/2026, PC do dono, branch `claude/estufinha`):** `build` com 317 testes JUnit, `runGameTestServer` com `All 70 required tests passed :)` (também com o Serene Seasons, nos Forge 66.0.13 e 66.0.2) e o teste visual com 50 prints (seção 3f).

**Estado em 25/10/2026.** No PC do dono, com o código de `f515cb9` (branch `claude/polish-visual-uses`, já na `main` pelo PR #4). Na nuvem, com a branch `claude/forge-compat-seasons` (compatibilidade com o Forge 66.0.2 do launcher do dono e com o Serene Seasons): `build` com 316 testes JUnit e `runGameTestServer` com `All 63 required tests passed :)`, também no Forge 66.0.2 e com o Serene Seasons carregado (linhas de 25/10/2026, nuvem, na tabela abaixo).

Compilam e passam no PC do dono:
- o MVP;
- a mecânica de fumar;
- o polimento de 24/10: 40 genéticas, arte nova, usos de todos os artigos, português em tudo, animações e sons.

Resultados: `build` com 311 testes JUnit e `runGameTestServer` com `All 62 required tests passed :)` (61 do Cultivo + `minecraft:always_pass`).

O cliente gráfico com o código atual passou pelo teste visual automático no Windows (25/10/2026): 47 prints conferidos um a um e log sem erro de modelo, textura ou som. O teste achou e corrigiu 1 bug (fumaça cobrindo a tela em 1ª pessoa). O multiplayer local (servidor dedicado + cliente) só rodou em parte: o cliente entrou e as duas primeiras cenas sincronizaram, mas o teste foi parado por falta de memória no PC. Falta repetir o multiplayer e o teste com amigos (checklist em [GUIA_INICIANTE.md](GUIA_INICIANTE.md), seção 6).

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
| 25/10/2026 | PC local, conferência própria do código `dbe06a4` (fim do workflow): `node tools/gen/generate.mjs` / `.\gradlew.bat test --rerun --console=plain` / `.\gradlew.bat runGameTestServer --console=plain` | 285 texturas e 786 JSONs, `git status` sem diferença / **`BUILD SUCCESSFUL in 8s`**, XML do JUnit: 311 testes, 0 falhas / **`All 62 required tests passed :)`** (`62 GAME TESTS COMPLETE IN 2.062 s`) |
| 25/10/2026 | PC local: teste visual automático, 1ª rodada (`tools\visual-probe\run-probe.ps1`, 47 cenas, código `dbe06a4`) | 47 prints. `log-problems.txt`: nenhum WARN/ERROR do Cultivo e nenhum modelo, textura ou som faltando (só os avisos conhecidos de Realms, `end_of_frame` e remapeamento de IDs do mundo antigo). Conferido nos prints: planta em 8 estágios e 12 famílias de cor, estações 3D com a placa da prensa animada, bandeja/pote/cinzeiro, cura do pote com brilho, 40 genéticas, dicas em português com estrelas, as 2 abas do criativo, legendas de som em português, fumar cada aparelho em 1ª e 3ª pessoa e o efeito "Chapado". **Bug achado:** em 1ª pessoa os fios de fumaça nasciam a meio bloco do olho e cobriam a tela com uma mancha preta (prints 37 e 43). Também falharam 3 cenas do próprio teste: a mira da bandeja caía na pedra da frente e o suporte agora exige agachar para devolver a entrada. |
| 25/10/2026 | PC local: investigação da bandeja (log temporário no `useItemOn` e no clique do mouse, removido depois) | O raio do clique acertava a pedra ao lado da bandeja (2 px de altura); olhando de cima, o clique chega ao bloco e o mod funciona: `Baseado enrolado`, 3 baseados, e sem folhas "Você precisa de 3 folhas de seda no inventário". Não era bug do mod; o teste passou a mirar de cima. |
| 25/10/2026 | PC local, correção da fumaça em 1ª pessoa + teste ajustado (commit `f515cb9`): `.\gradlew.bat build` / teste visual completo de novo | **`BUILD SUCCESSFUL in 11s`**, 311 testes, 0 falhas (inclui a varredura que proíbe classes de cliente no código comum) / 47 prints, log limpo; a mancha preta sumiu (fica a brasa e a chama), bandeja enrola e recusa, suporte recebe e devolve (agachado). |
| 25/10/2026 | PC local: `run-probe.ps1 -Multiplayer` (servidor dedicado em 127.0.0.1 + cliente) | Parcial: `Done (0.434s)!`, `Dev[/127.0.0.1:55502] logged in with entity id 1`, cenas `cam_crops` e `cam_families` construídas e vistas pelo cliente (2 prints), nenhum erro nem desconexão nos dois logs. O Claude Code parou o teste por falta de memória (dois jogos + dois Gradle). `server.properties` e `options.txt` voltaram ao original. |
| 25/10/2026 | nuvem (Linux, Temurin 25.0.1+8, Node 22.22.0), ponto de partida `6d6ed4a` (main com o PR #4): `node tools/gen/generate.mjs` / `./gradlew build --console=plain` / `./gradlew runGameTestServer --console=plain` | `285 texturas e 786 arquivos JSON`, `git status` limpo / **`BUILD SUCCESSFUL in 1m 13s`**, XML do JUnit: 311 testes, 0 falhas / **`All 62 required tests passed :)`** |
| 25/10/2026 | nuvem: `./gradlew build -Pcultivo_forge_version=26.3-66.0.2 --console=plain` (propriedade nova; o padrão continua 66.0.13) | **`BUILD SUCCESSFUL in 2m 1s`**, log `Processing Minecraft dependency: net.minecraftforge:forge:26.3-66.0.2`; 311 testes, 0 falhas |
| 25/10/2026 | nuvem: `./gradlew runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-forge-66.0.2 --console=plain` | Log `Forge mod loading, version 66.0.2`; **`All 62 required tests passed :)`** |
| 25/10/2026 | nuvem: servidor de **produção** Forge 26.3-66.0.2 (instalador oficial `--installServer`) com o jar do Cultivo compilado no 66.0.13, `/test run cultivo:*` pelo console | Sobe (`Done (0.701s)!`) e responde `Running 61 test(s)...`, mas os testes nunca terminam (parado após 15 min): em produção o Forge não avança o `GameTestTicker` (`ForgeGameTestHooks.isGametestEnabled()` exige `!FMLLoader.isProduction()`). Não é falha do Cultivo; o teste de produção passou a usar sondas de console. |
| 25/10/2026 | nuvem: `bash tools/compat/forge-server-compat.sh 26.3-66.0.2` (produção, sonda `growth.txt`: 4 lavouras `brisa_crop` e 4 trigos a céu aberto, y=120, bioma plains, `random_tick_speed 300`, 30 s) | `SONDA diagnostico bioma=plains`; idades Cultivo [6, 7, 7, 7] e trigo [7, 7, 7, 7] |
| 25/10/2026 | nuvem: Serene Seasons 26.1.2.0.7 + GlitchCore 26.3.0.0.5 (Modrinth, sha512 conferido) em produção no Forge 66.0.2, com `out_of_season_crop_behavior = 1` (`CULTIVO_COMPAT_CONFIG=tools/compat/config-cant-grow`), sonda `seasons.txt` | `Set season to Early Spring`; os 4 **trigos** ao ar livre chegaram à idade 7 na primavera, o que seria impossível com o Serene Seasons ativo. Mixin confirmado com `-Dmixin.debug.export=true` (`Mixing MixinBlockStateBase from sereneseasons.mixins.json into ...BlockBehaviour$BlockStateBase`). Causa achada nas fontes do Forge 26.3 (66.0.2 e 66.0.13): `ForgeEventFactory.onTagsUpdated` só é chamado em `ClientPacketListener`; o servidor nunca dispara o `TagsUpdatedEvent`, e o Serene Seasons só monta a lista de lavouras nesse evento. **Num servidor dedicado o Serene Seasons não afeta nenhuma lavoura**; em single player/LAN o cliente dispara o evento e as estações funcionam. |
| 25/10/2026 | nuvem, commit `2f6ae47` (tags do Serene Seasons iguais às do trigo + `mods.toml` forge `[66.0.2,)`): `node tools/gen/generate.mjs` / `./gradlew build --console=plain` / `./gradlew runGameTestServer --console=plain` | `285 texturas e 791 arquivos JSON` (786 + 4 tags + 1 test_instance) / **`BUILD SUCCESSFUL in 8s`**, XML do JUnit: 316 testes, 0 falhas (5 do `CompatibilityTest`) / **`All 63 required tests passed :)`**, sem `Couldn't load tag` |
| 25/10/2026 | nuvem: `./gradlew runGameTestServer -Pcultivo_run_dir=run-compat-ss --console=plain` (`run-compat-ss/mods` com Serene Seasons + GlitchCore, `config/sereneseasons/fertility.toml` com `out_of_season_crop_behavior = 1`) | Log `Registered synced config with path sereneseasons/fertility.toml`; **`All 63 required tests passed :)`**. O `serene_seasons_compat`, com a sincronização de tags do single player simulada, viu: primavera, Cultivo e trigo parados em 300 random ticks; com um bloco de vidro 3 acima, os dois chegam a 7; no verão, os dois chegam a 7. |
| 25/10/2026 | nuvem: teste negativo, sem a linha `ForgeEventFactory.onTagsUpdated(...)` no `serene_seasons_compat`, mesmo comando | `cultivo:serene_seasons_compat failed ... Cultivo cresceu fora de estação (primavera)` e `1 required tests failed :(`: o ramo do Serene Seasons testa de verdade. Linha restaurada. |
| 25/10/2026 | nuvem: `./gradlew runGameTestServer -Pcultivo_forge_version=26.3-66.0.2 -Pcultivo_run_dir=run-compat-ss --console=plain` (a combinação do launcher do dono) | Log `Forge mod loading, version 66.0.2`; **`All 63 required tests passed :)`** |
| 25/10/2026 | nuvem: produção Forge 66.0.2 + Serene Seasons + GlitchCore com o jar novo (com as tags), config "não cresce", sonda `seasons.txt` | Todas as fileiras (Cultivo e trigo, ao ar livre e sob vidro, primavera e verão) com idades [7, 7, 7, 7]; 0 linhas `Couldn't load tag`. No servidor dedicado o Cultivo se comporta igual ao trigo. |
| 25/10/2026 | nuvem: inspeção do jar `build/libs/cultivo-0.1.0.jar` do código `2f6ae47` (`unzip -l`) | 1250 entradas, 285 PNGs, 105 classes (5 em `cultivo/client/`); 0 arquivos em `net/minecraft`, `com/mojang` ou `assets/minecraft`. Dados: `assets/cultivo`, `data/cultivo`, `data/minecraft` (5 tags + `farm_plains.json`, overrides do próprio mod) e `data/sereneseasons` (4 tags). `mods.toml`: `cultivo` 0.1.0, MIT, forge `[66.0.2,)`, minecraft `[26.3,26.4)`. |
| 25/10/2026 | nuvem, commit `ace0bf3` (revisão: `serene_seasons_compat` termina no meio do verão num `finally`; script de compatibilidade com `trap` de SIGPIPE/EXIT, checagem da pasta de extras e java do `JAVA_HOME`; `CompatibilityTest` tira `\r`): `node tools/gen/generate.mjs` / `./gradlew build --console=plain` / `./gradlew runGameTestServer -Pcultivo_run_dir=run-compat-ss --console=plain` / `./gradlew runGameTestServer --console=plain` | `285 texturas e 791 arquivos JSON`, sem diferença / XML do JUnit: 316 testes, 0 falhas / **`All 63 required tests passed :)`** (com Serene Seasons) / **`All 63 required tests passed :)`** (sem) |
| 25/10/2026 | nuvem: `bash tools/compat/forge-server-compat.sh 26.3-66.0.2` com o script revisado / mesmo script com pasta de extras inexistente / com uma pasta de extras contendo uma cópia do jar do Cultivo | Código 0, `SONDA diagnostico bioma=plains`, Cultivo [7, 7, 7, 7], trigo [7, 7, 4, 7] / `pasta de mods extras não encontrada`, código 2, antes de baixar ou apagar nada / o Forge ignorou o jar duplicado e a sonda rodou normal (código 0); o caminho "servidor morre no meio" segue sem teste real. |
| 25/10/2026 | PC local (Windows 11, Temurin 25.0.4.1+1, Node 24.19.0), branch `claude/estufinha` sobre a `main` em `bd1b745` (PR #6 mesclado): revisão do plano nas fontes `forge-26.3-66.0.13-sources.jar` e no jar `SereneSeasons-forge-26.3-26.1.2.0.7.jar` (`javap`) | 5 correções ao plano antes de codar (`PushReaction.DESTROY` não existe, `preventDropFromBottomPart` inacessível, chave do item, lista `CULTIVATION_BLOCKS`) e 5 achadas na implementação (sem `"shade"`, `isViewBlocking`, `BonemealSource`, `canSeeSky` pela luz); os 10 riscos conferidos. O SS só age por `ModFertility.isCrop` no `randomTick` e no `UseBlock`, sem `CropGrowEvent` (ESTUFINHA_PLANO.md, "Revisão") |
| 25/10/2026 | PC local, commit `8de8b1a` (estufinha): `node tools/gen/generate.mjs` / `.\gradlew.bat build --console=plain` / `.\gradlew.bat runGameTestServer --console=plain` | `292 texturas e 805 arquivos JSON` (285 + 7 texturas da estufinha; sai a tag do vidro e o test_instance antigo) / **`BUILD SUCCESSFUL in 8s`**, XML do JUnit: 317 testes, 0 skipped, 0 falhas, 0 erros / **`All 70 required tests passed :)`** (`70 GAME TESTS COMPLETE IN 1.405 s`; 69 do Cultivo + `minecraft:always_pass`) |
| 25/10/2026 | PC local: no caminho, 1ª rodada dos GameTests novos | `grow_tent_plants_and_grows` falhou com `o teste deveria rodar sem céu`: `Level#canSeeSky` é pela luz do céu e deu verdadeiro na área do teste. O teste passou a pôr um teto de pedra e conferir o bloco. Também o `PortugueseEverywhereTest` pegou "no" (de "(no soil or water)") no texto inglês da dica, que é palavra do português; a frase virou "without soil or water" |
| 25/10/2026 | PC local: teste negativo dos GameTests (função de loot com `tent = false &&` e o `preRemoveSideEffects` sem derrubar a planta), `.\gradlew.bat runGameTestServer --console=plain` | `cultivo:grow_tent_breaks_either_half failed ... madura, metade de baixo: 0 flores e 0 sementes`, `cultivo:grow_tent_creative_break failed ... a planta não caiu no criativo: []`, `cultivo:grow_tent_harvest failed ... estufinha deu qualidade 2` e `3 required tests failed :(`. Fontes restauradas com `git checkout -- src/main` |
| 25/10/2026 | PC local: teste negativo do JUnit (`GrowTentRules.MULTIPLIER` de 1,25 para 1,0), `.\gradlew.bat test --console=plain` | `GrowTentRulesTest > numbersOfTheSpec() FAILED`, `tentBeatsALoneHydratedPlot() FAILED`, `rollBoundsByGrowthClass() FAILED`, `317 tests completed, 3 failed`. Restaurado |
| 25/10/2026 | PC local: `node tools/visual-probe/gen-datapack.mjs` / `node tools/visual-probe/validate.mjs . tools/visual-probe/datapack tools/visual-probe/run-probe.ps1` (commit `6e1fcc8`) | `30 funcoes, 271 linhas no build` / `30 funcoes, 426 comandos, 28 passos, 26 marcadores`, `blocos conhecidos 48`, `OK: nenhum erro` |
| 25/10/2026 | PC local: teste visual automático, 1ª rodada com a estufinha (`tools\visual-probe\run-probe.ps1`, `runClient`, código `6e1fcc8`) | `Prints: 50`. `log-problems.txt`: nada ligado ao Cultivo além dos avisos conhecidos `Object did not get ID it asked for` do mundo antigo; nenhum modelo, textura ou som faltando. Nos prints: as 3 estufinhas com a planta dentro do vaso (vazia, Brisa pequena, roxa madura), ícone na vitrine e na aba Cultivo, a dica de 4 linhas, colher de mão vazia (flor fresca na mão, 3 sementes, legenda "Estufinha colhida") e replantar ("Semente plantada na estufinha"). **Achado:** a luz roxa quase não aparecia (o painel de LEDs fica virado para baixo). O número de sementes parecia 8 no print, mas com zoom é 3 com o ícone atrás |
| 25/10/2026 | PC local, commit `afa87eb` (mylar de dentro com `light_emission` 11 e tingido de lilás, câmera mais perto; `serene_seasons_compat` com a estufinha): teste visual de novo | `Prints: 50`, mesmo log limpo. O interior das 3 estufinhas aparece lilás, iluminado, na sala de pedra-negra; colher e replantar iguais à 1ª rodada |
| 25/10/2026 | PC local: `.\gradlew.bat runGameTestServer "-Pcultivo_run_dir=run-compat-ss" --console=plain` (jars `SereneSeasons-forge-26.3-26.1.2.0.7.jar` e `GlitchCore-forge-26.3-26.3.0.0.5.jar` copiados do `%APPDATA%\.minecraft\mods` do dono; sha512 começando com `5eddd576de1b540c` e `05f5d12f304c7924`; `fertility.toml` de `tools/compat/config-cant-grow`) | `Registered synced config with path sereneseasons/fertility.toml`; **`All 70 required tests passed :)`**. O `serene_seasons_compat` viu Cultivo e trigo parados no início da primavera e, na mesma primavera, a estufinha crescer até 7 |
| 25/10/2026 | PC local: o mesmo com `"-Pcultivo_forge_version=26.3-66.0.2"` (a combinação do launcher do dono) | `Forge mod loading, version 66.0.2`; **`All 70 required tests passed :)`** (`BUILD SUCCESSFUL in 3m 25s`, com o download desse Forge) |
| 25/10/2026 | PC local, rodada final no padrão (`afa87eb`): `node tools/gen/generate.mjs` / `.\gradlew.bat build --console=plain` / `.\gradlew.bat runGameTestServer --console=plain` / `unzip -l build/libs/cultivo-0.1.0.jar` | `292 texturas e 805 arquivos JSON`, `git status` sem mudança em `src/generated` / **`BUILD SUCCESSFUL in 8s`**, XML: 317 testes, 0 falhas / **`All 70 required tests passed :)`** / 1276 entradas, 22 com `grow_tent`, 0 em `net/minecraft`, `com/mojang` ou `assets/minecraft` |

**Conferência dos formatos:**
- Os JSONs gerados foram conferidos contra os vanilla do `client-extra-26.3` jar: `loot_table/blocks/wheat.json`, `chests/village/village_plains_house.json`, `processor_list/farm_plains.json` (as regras vanilla do mod são idênticas às do jar), `feature/melon.json`, `placed_feature/patch_melon.json`, `recipe/bowl.json`/`book.json` e `test_instance/always_pass.json`.
- Os formatos novos de 24/10 foram conferidos contra o código e os JSONs do 26.3 e pelos testes JUnit de `data/`; os de cliente carregaram sem erro no `runClient` do teste visual de 25/10. São eles:
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
- [x] `runClient` com o código atual (25/10/2026, teste visual automático, commit `f515cb9`).
- [x] Checagem visual no cliente: 47 prints do teste automático conferidos (25/10/2026). Ainda vale o dono olhar com calma (seção 6 do guia).
- [ ] `runServer` + cliente conectado com o código atual: só parcial (o cliente entrou; teste parado por falta de memória).

**Gate:** servidor OK; cliente e checagem visual OK com o código atual; multiplayer completo pendente.

## 2. Conteúdo e cultivo
- [x] **Regras puras** com testes passando:
  - qualidade: fora da estufinha 2–3, estufinha 4–5 (desde 25/10/2026; antes, estufa de vidro); bônus 0,20/0,35/0,50 pela potência;
  - crescimento: fórmula do trigo × crescimento da genética (×1,25/×1,0/×0,8), luz ≥ 9; o vidro não dá mais bônus;
  - estufinha: 4 × genética × 1,25, sem farmland, água nem luz de fora (seção 3f);
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
  - 18 eventos de som com legenda;
  - nenhuma textura órfã.
- [x] Código Java de conteúdo compila contra o Forge 26.3-66.0.13.
- [x] **GameTests:**
  - cultivo: `outdoor_growth`, `harvest_drops`, `catalog_strain_grows_and_drops` e os 8 da estufinha (seção 3f);
  - fontes de sementes: `village_chest_seeds` (1600 baús com semente fixa: 45–55% com semente, as 40 genéticas aparecem, médias dentro de ±25%), `wandering_trader_catalog_seeds`, `villager_replants_mod_crop`;
  - organização e estado: `creative_tabs_split`, `block_states_match_ids`.
- [x] Dados carregam no servidor de GameTest: os 69 GameTests usam as receitas, o loot e as tags carregados.
- [x] No cliente, pelo teste visual de 25/10: os 8 estágios, as 12 famílias de cor e os ícones das 40 genéticas.
- [ ] No cliente, num mundo novo: planta silvestre, lavouras de aldeia e o mascate com sementes.

**Gate:** regras e drops OK no servidor; visual OK no teste automático; mundo novo pendente.

## 3. Processamento
- [x] **Regras puras com testes:**
  - secagem 600, peneira 200 (+1 resina com qualidade ≥ 4, +1 com dichavador), prensa 300 (2 resinas → 1 haxixe);
  - saída cheia ou de outra qualidade/genética congela o progresso; sem receita, zera;
  - estado visual vazia/carregada/trabalhando/pronta.
- [x] **Receitas JSON conferidas por JUnit:**
  - 56 receitas de crafting (11 artigos + 3 estações + 40 baseados `crafting_transmute` + baseado com piteira + estufinha), cada uma com avanço do livro de receitas;
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
- [x] **Visto no cliente** (teste visual de 25/10): animação em 1ª e 3ª pessoa dos 5 aparelhos (bong e narguilé com as duas mãos), modelos na mão e acesos, partículas, legendas dos sons em português, ícone e nome do efeito, náusea, tooltips e mensagens. Corrigido: fumaça cobrindo a tela em 1ª pessoa (commit `f515cb9`).
- [ ] Animação com a mão esquerda (não coberta pelo teste) e ouvir os sons (o teste só vê as legendas).
- [ ] Testar em multiplayer: dois jogadores fumando no servidor dedicado, cada um vendo a animação e a fumaça do outro.

**Gate:** regras, servidor e visual OK; multiplayer pendente.

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
- [x] **Visto no cliente** (teste visual de 25/10): bandeja (2 direções), pote vazio e cheio com brilho de cura, cinzeiro de 0 a 4, peneira com dichavador, baseado com piteira na mão; cliques reais na bandeja (enrola e recusa), no suporte (põe e tira agachado) e na peneira (instala o dichavador).
- [ ] Ver os artigos de todos os lados (o teste olha de um ângulo só).

## 3d. Português, animações e sons (pedido de 24/10/2026)
- [x] Flag `portuguese_everywhere` com `en_us` = `pt_br`, textos revisados e metadado em português (`PortugueseEverywhereTest`).
- [x] 18 eventos de som com legenda (`SoundsTest`, GameTest `sound_events_registered`).
- [x] Texturas animadas (`AnimatedTexturesTest`).
- [x] Animação de fumar e placa da prensa conferidas por regra pura e pelos modelos gerados (`SmokeAnimationTest`, `SmokeAnimationAssetsTest`, `StationAnimationTest`).
- [x] Isolamento do código de cliente (`ClientIsolationTest`).
- [x] Animações, texturas animadas, partículas e legendas dos sons vistas no cliente (teste visual de 25/10).
- [ ] Ouvir os sons (o teste só confere as legendas).

## 3e. Compatibilidade com o ambiente do dono (25/10/2026)
- [x] Forge 26.3-66.0.2 (o do launcher do dono): build, 311 testes JUnit e 62 GameTests passam compilando contra ele; o jar compilado no 66.0.13 sobe num servidor de produção 66.0.2 e as lavouras crescem.
- [x] `mods.toml` passa a exigir forge `[66.0.2,)` (menor versão testada); o build continua no 66.0.13 da matriz.
- [x] Serene Seasons: as lavouras do Cultivo seguem as estações do trigo (tags `sereneseasons:summer_crops`/`autumn_crops`, bloco e semente), conferido por JUnit (`CompatibilityTest`) e pelo GameTest `serene_seasons_compat` com o Serene Seasons carregado, nos Forge 66.0.13 e 66.0.2.
- [x] Servidor dedicado: descoberto e documentado que o Serene Seasons não afeta lavoura nenhuma no Forge 26.3 (o servidor nunca dispara o `TagsUpdatedEvent`).
- [ ] Single player com o Serene Seasons e os outros mods do launcher (dica "Estações férteis" nas sementes, lavoura lenta na primavera, normal sob vidro): só o dono, com tela (GUIA_INICIANTE.md, seção 6).
- [ ] Decisões do dono sobre o Serene Seasons (DECISIONS.md, "Em aberto").
**Gate:** automático OK; falta a conferência do dono em single player.

## 3f. Estufinha (pedido de 25/10/2026, plano aprovado, implementado na branch `claude/estufinha`)

- [x] Implementado conforme [ESTUFINHA_PLANO.md](ESTUFINHA_PLANO.md), com a revisão das APIs nas fontes do 26.3 (commit `ff45eee`) e o código em `8de8b1a`:
  - bloco `cultivo:grow_tent` de 2 de altura, craftável sem vidro (lã roxa, ametista, tocha, vaso), luz de bloco 12 em cima, LEDs com `light_emission` 15 e partículas roxas;
  - um pé de planta dentro (BlockEntity na metade de baixo), sem farmland, sem água e sem céu;
  - clique direito na madura colhe pela tabela da lavoura com qualidade 4–5 e a estufinha fica vazia; farinha de osso +2 a +5.
- [x] Bônus da estufa de vidro removido: `GreenhouseRules`, `GreenhouseDetector`, `GreenhouseRulesTest`, a tag `cultivo:greenhouse_glass` e o GameTest `greenhouse_quality` saíram; a função de loot decide pela BlockEntity da estufinha. Fora dela, 2–3 (GameTest `glass_gives_no_bonus`).
- [x] **Testes:**
  - JUnit: `GrowTentRulesTest` (7 testes com literais: limites 5/6/7, velocidades 6,25/5/4, clique, farinha de osso, luz 12, qualidade 4–5), `DataConsistencyTest` (`greenhouseGlassTagIsGone`, `growTentLootDropsOnlyFromLowerHalf`, 56 receitas), `AssetCompletenessTest.growTentBlockstateCoversEveryState`, `TooltipHintsTest`, `PortugueseEverywhereTest`, `SoundsTest` (18), `StrainCatalogTest`;
  - GameTests: `grow_tent_places_two_halves`, `grow_tent_plants_and_grows` (random ticks, médias 35/42/49 com sorteio fixo, sem farmland, água ou céu), `grow_tent_bone_meal`, `grow_tent_harvest` (200 colheitas, 4 e 5 vistos, vazia depois, replanta), `glass_gives_no_bonus`, `grow_tent_breaks_either_half` (7 casos, sem duplicar nem perder), `grow_tent_creative_break`, `grow_tent_persists` (salvar/carregar e visão do cliente); `client_prediction_matches_server` (+30 casos) e `interactions_swing_once` estendidos;
  - testes negativos (tabela de evidências);
  - cena nova no visual-probe (`cam_grow_tent`, `use_tent`, dica no inventário), rodada no cliente real com 50 prints;
  - `serene_seasons_compat`: a estufinha fora das tags do SS e crescendo na primavera com o SS carregado (Forge 66.0.13 e 66.0.2).
- [ ] Ver no jogo de verdade (GUIA_INICIANTE.md, seção 6.1, "Plantas e estufinha"): a luz roxa no escuro, a planta dentro do vaso sem atravessar a lona, colher, replantar e quebrar.

## 4. Conteúdo de release
- [x] Licença escolhida (MIT para código e assets do Cultivo) e registrada em `mods.toml` e `LICENSE`.
- [x] Jar inspecionado: assets incluídos, nenhum arquivo do Minecraft (última inspeção registrada: código `afa87eb`, 25/10/2026, 1276 entradas).
- [x] `build` (com `test`), `runGameTestServer` e `runClient` (teste visual) com o código atual.
- [ ] Cliente conectado ao servidor dedicado do começo ao fim (25/10 só parcial: entrou e sincronizou 2 cenas; parado por falta de memória).

**Gate:** pendente do multiplayer completo.

## Próximos passos
O passo a passo para leigos está em [GUIA_INICIANTE.md](GUIA_INICIANTE.md); a lista completa, em [NEXT_SESSION.md](NEXT_SESSION.md), seção 8.

1. **Multiplayer:** repetir `run-probe.ps1 -Multiplayer` com o PC folgado (feche navegador e jogos; são dois Minecraft e dois Gradle). Depois, o ciclo completo com um amigo (plantar, colher, secar, peneirar, prensar, craftar, curar, enrolar, fumar).
2. **Olhar com calma no `runClient`** (seção 6.1 do guia): mão esquerda, sons e o que o teste automático não cobre.
3. **Mundo novo:** planta silvestre na selva e na planície, lavouras de aldeia, baús de aldeia e um mascate com sementes do catálogo.
4. **Ambiente do dono:** jogar em single player com o Serene Seasons e os outros mods do launcher (Forge 66.0.2 agora aceito) e conferir as estações nas lavouras do Cultivo (GUIA_INICIANTE.md, seção 6); decidir as perguntas em aberto do Serene Seasons (DECISIONS.md).
5. **Balanceamento** depois de jogar (DECISIONS.md, "Em aberto").
