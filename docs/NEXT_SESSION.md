# Orientação para a próxima sessão

**Atualizado em 24/10/2026, com o código do commit `319cf0b`.** Este é o documento para retomar o projeto sem a conversa anterior. Regras e números de jogo: [DECISIONS.md](DECISIONS.md). Evidências: [TODO.md](TODO.md). Passo a passo do dono (leigo em Java): [GUIA_INICIANTE.md](GUIA_INICIANTE.md). Prompt pronto para a nuvem: [CLOUD_PROMPT.md](CLOUD_PROMPT.md).

## 1. Estado

O mod "Cultivo" (modid `cultivo`, versão 0.1.0) está implementado para Minecraft 26.3 + Forge 26.3-66.0.13:

- **40 genéticas:** 3 nativas (Brisa, Aurora, Eclipse) e 37 do catálogo High THC da Santa Semente BR, com nomes de paródia. Lavoura com 8 estágios visuais, estufa com qualidade superior, planta silvestre, duas abas criativas.
- **Fontes de sementes:** baús de casa de 4 dos 5 tipos de aldeia (todas as genéticas, peso pela raridade), lavouras de aldeia de planície e planta silvestre (só nativas) e o mascate (2 ofertas do catálogo). O fazendeiro replanta a lavoura do mod que colhe.
- **Processamento:** suporte de secagem, peneira de bancada (com o dichavador como melhoria) e prensa manual, por ticks, com estados visuais, animações, partículas e sons.
- **Artigos:** todos têm uso. Baseado com piteira, bandeja de enrolar, pote de vidro que cura flores, cinzeiro que vira farinha de osso, além dos artigos de fumar e do isqueiro.
- **Mecânica fictícia de fumar** (pedida em 21/10/2026), com animação própria em 1ª e 3ª pessoa.
- **Tudo em português** em qualquer idioma do cliente (pedido de 24/10/2026), com 16 eventos de som do mod.

**Verificado** no PC do dono (Windows 11, Temurin 25.0.4.1+1), com o código de `319cf0b`:

| Verificação | Resultado |
|---|---|
| `.\gradlew.bat build` | `BUILD SUCCESSFUL`, 311 testes JUnit, 0 falhas |
| `.\gradlew.bat runGameTestServer` | `All 62 required tests passed :)` (61 do Cultivo + `minecraft:always_pass`) |
| `node tools/gen/generate.mjs` | `285 texturas e 786 arquivos JSON` |

**Ainda não visto num cliente de verdade:** o `runClient` abriu sem erros de recurso em 24/10/2026, mas com o código de `5bded22`, antes do polimento. Tudo o que é só do cliente foi conferido apenas por JUnit, revisão contra as fontes do 26.3 e um renderizador de software fora do repositório. Isso inclui árvores de modelo de item, blockstates multipart, texturas animadas, a placa da prensa (BlockEntityRenderer), poses de braço e sons. Erros de carregamento de modelo do cliente não aparecem no `runGameTestServer`.

**Branches:**
- `main` está em `5bded22` (PR #3).
- A branch `claude/polish-visual-uses` tem o polimento de 24/10, de `121cc4b` até o commit destes docs. Quando estes docs foram escritos, ela ainda não tinha sido enviada nem mesclada; ela entra na `main` por PR.

## 2. Como retomar

1. **Escolher a base.**
   - Rode `git fetch origin`. Se o PR de `claude/polish-visual-uses` já foi mesclado, parta da `main`; senão, faça `git checkout claude/polish-visual-uses`.
   - Para saber se já foi mesclado: `git cat-file -e origin/main:docs/strains-catalog.json && echo mesclado`. O arquivo só existe a partir do polimento, e o teste funciona mesmo se o PR tiver sido mesclado com squash.
   - Crie uma branch nova `claude/<tema>`. Commits em inglês; entrega por PR para a `main`.
   - Pronto quando `git log --oneline -1` mostrar `319cf0b` ou um commit mais novo.
2. **Preparar o ambiente** (seção 3).
   - Pronto quando `java -version` mostrar 25 e o primeiro `./gradlew build` terminar.
3. **Verificar o ponto de partida.** Rode nesta ordem e nunca dois comandos do Gradle ao mesmo tempo:
   1. `node tools/gen/generate.mjs`: termina com `285 texturas e 786 arquivos JSON gerados em ...`, e `git status` não mostra mudança.
   2. `./gradlew build --console=plain`: `BUILD SUCCESSFUL`. Os totais ficam nos XML de `build/test-results/test` (311 testes, 0 falhas em `319cf0b`).
   3. `./gradlew runGameTestServer --console=plain`: `All 62 required tests passed :)`.

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
| Forge | `26.3-66.0.13` (`net.minecraftforge:forge:26.3-66.0.13`); `mods.toml` aceita forge `[66,)` e minecraft `[26.3,26.4)` |
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
  - O perfil "Forge 26.3" usa o Forge **26.3-66.0.2**, não o 66.0.13 do projeto.
  - `%APPDATA%\.minecraft\mods` já tem outros mods: Serene Seasons (com GlitchCore), FallingTree, Macaw's Furniture, Simply Eggs e Xaero's Minimap.
  - O Serene Seasons mexe no crescimento das plantações (a lavoura do Cultivo chama `ForgeHooks.onCropsGrowPre/Post`, onde mods de estação costumam agir). Teste o Cultivo no `runClient` ou num perfil limpo com o Forge 66.0.13.
  - O `mods.toml` aceita `[66,)`, então o jar também tenta carregar no 66.0.2, uma versão não testada.
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
- **GameTest novo:** nome em `ContentIds.GAME_TESTS` e entrada em `game_tests` do manifesto (`max_ticks`, padrão 100), depois o gerador.
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
  - listas e dados: `ArticleRecipes`, `ContentIds` (lista única de IDs, GameTests, sons, propriedades de blockstate), `TooltipHints`.
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
- `gametest/CultivoGameTests`: 61 GameTests, com os nomes em `ContentIds.GAME_TESTS`.

Outras pastas:
- `src/test/java/.../core` e `.../data`: 311 testes JUnit.
  - `core`: regras puras.
  - `data`: completude e consistência dos recursos, nomes de paródia, português, sons, texturas animadas, isolamento do cliente, animação e dicas.
  - Os testes leem os arquivos a partir da raiz do projeto.
- `tools/gen/`: `manifest.json`, `generate.mjs` e `models.mjs`.
- `tools/art/`: `textures.mjs` e `png.mjs`.
- `tools/visual-probe/`:
  - `run-probe.ps1` + datapack de cena: abre o `runClient` num mundo de teste e tira 11 prints (só Windows com tela; ferramenta de desenvolvimento, fora do jar).
  - A cena ainda mostra só as 3 nativas e as estações vazias.
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
  - correções da revisão (`319cf0b`);
  - estes docs.
- Detalhes e resultados de cada etapa estão em TODO.md (evidências) e em `git log`. O prompt da sessão de 21/10 está em `git show 319cf0b:docs/CLOUD_PROMPT.md`.

## 8. Pendências, em ordem

1. **Checagem visual no `runClient`** (Windows com tela, PC do dono): a checklist da seção 6.1 do guia e o teste visual automático. Veja o que só o cliente mostra (seção 1); se um modelo não carregar, aparece um cubo roxo e preto ou um aviso no log.
2. **Multiplayer:** `runServer` com o código atual (o último foi em `5bded22`) e um cliente de verdade conectado, com dois jogadores numa estação e fumando perto um do outro (GUIA, seção 6.3).
3. **Ambiente do dono:** testar com o Serene Seasons, ou documentar que as lavouras do Cultivo sofrem com as estações; decidir entre exigir `[66.0.13,)` no `mods.toml` ou testar no Forge 66.0.2.
4. **Teste visual automático:** estender a cena para as genéticas do catálogo, a bandeja, o pote e o cinzeiro colocados, o dichavador na peneira e o baseado com piteira.
5. **Catálogo:** o filtro High THC da loja tinha cerca de 58 produtos num snapshot antigo; entraram 37. Se o dono colar a página, acrescente as que faltam (DECISIONS.md, "Como adicionar uma genética").
6. **Balanceamento** depois de jogar (pesos, raridade da planta silvestre, velocidades, preços do mascate).
7. **Opcionais** registrados nas etapas:
   - pote com funil (hopper);
   - brilho no pote curado;
   - ícone do suporte de secagem com maços;
   - silhueta por genética na lavoura;
   - áudio próprio no lugar dos eventos vanilla;
   - chama no isqueiro enquanto fuma.
8. **Limpeza:** apagar a sobra `GrowthRules.visualStage` e o teste dela; registrar uma inspeção do jar do código atual (a última registrada é do código `1e7325a`).
