# Orientação para a próxima sessão

**Atualizado em 21/10/2026, ao fim da sessão na nuvem.** O MVP inteiro está escrito: código Java, dados, assets originais, testes JUnit e GameTests. Nesta sessão, porém, **o código que depende do Minecraft nunca foi compilado**: a rede da nuvem bloqueou `maven.minecraftforge.net` e os servidores da Mojang, e o ForgeGradle falhou ao configurar (`Failed to download default tool: minecraft-mavenizer-0.5.21.jar`). Só rodaram os 62 testes JUnit que não dependem do Minecraft (`tools/test-offline.sh`, todos passando). Os resultados reais estão em docs/TODO.md.

**Tarefa da próxima sessão:** numa máquina, ou num ambiente de nuvem, com acesso a `maven.minecraftforge.net`, `piston-meta.mojang.com`, `libraries.minecraft.net` e `launchermeta.mojang.com`:
1. rodar `./gradlew build` e corrigir a compilação (seção 6 lista as suposições de API mais arriscadas);
2. rodar `./gradlew test` e `./gradlew runGameTestServer`;
3. conferir os JSONs contra os vanilla;
4. testar `runClient`/`runServer`, inspecionar o jar e atualizar TODO.md.

As seções 1–4 abaixo continuam valendo. As seções 5 e 6 descrevem o estado atual.

## 1. Ambiente e versões (verificadas no MDK baixado)

| Item | Valor |
|---|---|
| Minecraft | 26.3 (MCP/mappings `26.3-20260918.230105`) |
| Loader | Forge `26.3-66.0.13` (`net.minecraftforge:forge:26.3-66.0.13`) |
| Plugin | `net.minecraftforge.gradle` `[7.0.17,8)` (ForgeGradle 7) |
| Gradle | wrapper 9.7.1 |
| Java | toolchain 25 |

- **Decisão: manter 26.3 + Forge.** O build do MDK de exemplo já passou (`BUILD SUCCESSFUL in 5m 1s`, jar `build/libs/examplemod-1.0.0.jar`). Não vale a pena trocar de versão.
- Máquina local: JDK 25 em `C:\Users\Cliente\Documents\jdk\jdk-25.0.4.1+1`. O `java` no PATH é Java 8 de 32 bits, então use `JAVA_HOME` apontando para o JDK 25. Na nuvem, instale um JDK 25 (Temurin). O plugin foojay em `settings.gradle` pode baixar o toolchain.
- Ainda falta atualizar `docs/TECHNICAL_RESEARCH.md` com essa matriz exata, porque hoje ele diz que a versão não está fixada.

### Como consultar a API sem pesquisar na web
Depois do primeiro build, o ForgeGradle deixa estes arquivos no projeto:
- **Fontes completas do Minecraft e do Forge (decompiladas, nomes oficiais):** `.gradle/mavenizer/repo/net/minecraftforge/forge/26.3-66.0.13/forge-26.3-66.0.13-sources.jar`. Extraia num diretório temporário e use grep.
- **Dados e assets vanilla em JSON (loot, receitas, worldgen, modelos):** `.gradle/mavenizer/repo/net/minecraft/client-extra/26.3-20260918.230105/client-extra-26.3-20260918.230105.jar`. Leia com `unzip -p`.

## 2. Diferenças da API 26.3 já confirmadas
- `Identifier` substitui `ResourceLocation`. Blocos e itens precisam de `.setId(BLOCKS.key("nome"))` / `.setId(ITEMS.key("nome"))` nas Properties (veja o ExampleMod).
- Para um item-semente que nomeia o crop: `new BlockItem(block, props.useItemDescriptionPrefix())`. Itens compostáveis usam `Item.Properties#compostable(ContextIntProviders.COMPOSTABLE_LOW)`.
- Blocos **não** precisam mais de `codec()`.
- Interação com blocos:
  - `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` retorna `InteractionResult.SUCCESS_SERVER / CONSUME / TRY_WITH_EMPTY_HAND`.
  - `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)`.
- BlockEntity:
  - salva e carrega com `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)`, usando `ContainerHelper.saveAllItems/loadAllItems`;
  - o drop ao quebrar vai em `preRemoveSideEffects(BlockPos, BlockState)`;
  - a sincronização usa `getUpdatePacket()` + `getUpdateTag(HolderLookup.Provider)`;
  - o construtor do tipo é `new BlockEntityType<>(factory, Set.of(block))`.
  - Modelo completo: `CampfireBlock` + `CampfireBlockEntity`. O ticker usa `createTickerHelper`.
- Tooltip: `Item#appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` (marcado @Deprecated, mas ainda funciona).
- Componentes de dados: `DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()`, registrados via DeferredRegister em `Registries.DATA_COMPONENT_TYPE`.
- Receitas:
  - a interface `Recipe<T>` tem `matches`, `assemble(T)`, `showNotification`, `group`, `getSerializer`, `getType`, `placementInfo`, `recipeBookCategory`;
  - `RecipeSerializer` é um record `(MapCodec, StreamCodec)`;
  - resultado como `ItemStackTemplate`;
  - modelo a seguir: `SingleItemRecipe` / `StonecutterRecipe` com `SingleRecipeInput`;
  - no servidor, a busca é `serverLevel.recipeAccess().getRecipeFor(type, input, level)`.
- Função de loot: estender `LootItemConditionalFunction`, com `MapCodec` montado via `commonFields(i).apply(...)`, e registrar em `Registries.LOOT_FUNCTION_TYPE`. Parâmetros de contexto: `LootContextParams.ORIGIN` e `BLOCK_STATE`.
- **Formato JSON novo de loot:** usa `"modifier"` e `"condition"`, no lugar de `functions`/`conditions`. Copie os arquivos vanilla `loot_table/blocks/wheat.json` e `short_grass.json`.
- **Worldgen:**
  - as features configuradas ficam em `data/<ns>/worldgen/feature/`; o tipo `random_patch` não existe mais;
  - exemplo de feature: `{"type":"minecraft:simple_block","to_place":{"id":"..."}}`;
  - o "patch" é montado no placed_feature com `count` + `offset` + `block_predicate_filter`; copie `placed_feature/patch_melon.json`.
- Biome modifier do Forge: `data/cultivo/forge/biome_modifier/x.json` com `{"type":"forge:add_features","biomes":"#minecraft:is_jungle","features":"cultivo:...","step":"vegetal_decoration"}`. Confira o caminho exato da pasta no registro `forge:biome_modifier`.
- **Render layer automático:** a camada cutout é inferida da transparência da textura, então não é preciso `render_type`.
- Modelos de item: além de `models/item/x.json`, cada item precisa de `assets/<ns>/items/x.json` com `{"model":{"type":"minecraft:model","model":"cultivo:item/x"}}`.
- Injeção de loot: `LootTableLoadEvent.BUS.addListener(...)` e depois `event.getTable().addPool(...)`, com uma entrada `NestedLootTable.lootTableReference(ResourceKey)`. O evento roda só no servidor.
- **GameTests no Forge 26.3:**
  - registre as funções via DeferredRegister em `Registries.TEST_FUNCTION`, como `Consumer<GameTestHelper>`;
  - crie um JSON por teste em `data/cultivo/test_instance/<nome>.json` no formato `{"type":"minecraft:function","function":"cultivo:<nome>","environment":"minecraft:default","structure":"forge:empty9x9x9","max_ticks":200}`;
  - o Forge gera sozinho a estrutura vazia `forge:emptyXxYxZ`;
  - para rodar: `gradlew runGameTestServer`, com `forge.enabledGameTestNamespaces=cultivo` no build.gradle.
  - Helpers úteis: `setBlock`, `getBlockState`, `randomTick`, `useBlock(pos, player)`, `makeMockPlayer(GameType)`, `getLevel`, `succeed`, `fail`.

## 3. Inventário vanilla que direciona o design
- **Aldeias em 26.3:** plains, desert, savanna, snowy e taiga (5 tipos). **Não existe aldeia de selva.**
- Baús das casas: `minecraft:chests/village/village_{plains,desert,savanna,snowy,taiga}_house`.
- Lavouras das aldeias: os processor lists `minecraft:worldgen/processor_list/farm_<tipo>.json` usam regras `random_block_match` que trocam trigo por outras culturas.
- Tags úteis: `#minecraft:is_jungle` (jungle, sparse_jungle, bamboo_jungle), `#minecraft:has_structure/village_plains` (plains, meadow). Adicione os crops em `#minecraft:maintains_farmland`, `#minecraft:crops` e `#minecraft:bee_growables`.

## 4. Arquitetura decidida (não rediscutir; registrar em DECISIONS.md)
- **Identificação:** modId `cultivo`, nome "Cultivo", group/pacote `io.github.peixeangola.cultivo`, versão 0.1.0. Substitua todo `examplemod` no build.gradle (incluindo os args de `runs` e `enabledGameTestNamespaces`), em settings.gradle, no mods.toml e nas classes; apague `Config.java` e `ExampleMod.java`.
- **Variedades** (enum puro):
  - Brisa: comum, automática/sativa, cresce mais rápido;
  - Aurora: incomum, fotoperíodo/híbrida;
  - Eclipse: rara, fotoperíodo/índica, cresce mais devagar.
- **Itens por variedade:** `<v>_seeds` (BlockItem do crop), `<v>_crop` (bloco), `<v>_fresh_flower` e `<v>_dried_flower`.
- **Itens genéricos:** `resin` (resina fictícia), `hash`, `joint` (baseado), mais os 11 artigos: `bong`, `pipe`, `hookah`, `chillum`, `rolling_paper`, `filter_tip`, `grinder`, `lighter`, `rolling_tray`, `jar`, `ashtray`.
- **Qualidade:**
  - componente `cultivo:quality`, valor inteiro de 1 a 5, mostrado no tooltip;
  - outdoor: base 2; estufa: base 4;
  - +1 com chance por variedade: 0,20 / 0,35 / 0,50;
  - resultado: outdoor fica em 2–3 e estufa em 4–5;
  - a função de loot `cultivo:set_harvest_quality` calcula a rota no servidor, no momento da colheita.
- **Crop:**
  - `CannabisCropBlock extends CropBlock`, sem filtro de bioma; precisa de farmland e luz ≥ 9, igual ao trigo;
  - `randomTick` replica o vanilla, incluindo os hooks `ForgeHooks.onCropsGrowPre/Post`, com velocidade × multiplicador da variedade × 1,25 na estufa;
  - 4 texturas visuais por variedade, mapeando idades 0–1, 2–3, 4–5 e 6–7;
  - drops maduros: flor fresca com qualidade + sementes (bônus binomial); drops imaturos: 1 semente.
- **Estufa** (`GreenhouseRules`, puro, testável):
  - o primeiro bloco não-ar acima da planta, até 6 blocos, precisa ser da tag `cultivo:greenhouse_glass` (vidros, vidros tingidos, painéis);
  - nas 4 direções horizontais, precisa haver vidro dessa tag a até 6 blocos, na altura da planta ou um bloco acima.
- **Estações** (a mesma BlockEntity serve as três):
  - `drying_rack` (suporte de secagem), `sieve` (peneira de bancada) e `press` (prensa manual);
  - cada uma tem 1 slot de entrada (pilha) + 1 de saída e progresso em ticks;
  - interação: clique direito com o item insere; mão vazia retira primeiro a saída;
  - se a saída estiver cheia ou não for compatível (outra qualidade), o progresso para, sem perder nem duplicar itens;
  - a qualidade da entrada é copiada para a saída.
  - Receitas data-driven com os tipos `cultivo:drying`, `cultivo:sieving` e `cultivo:pressing`; campos `ingredient`, `input_count`, `result` e `processing_time`:
    - secagem: 600 ticks;
    - peneira: 200 ticks, flor seca → resina, +1 resina com qualidade ≥ 4;
    - prensa: 300 ticks, 2 resinas → 1 haxixe.
- **Fontes de sementes** (constantes em `SeedSources`, com testes que conferem os JSONs):
  - Baús: injeção nas casas de **plains, savanna, taiga e desert** (4/5 tipos; snowy fica de fora). O pool injetado tem 1 rolagem com pesos vazio 50, brisa 30 (1–3), aurora 14 (1–2), eclipse 6 (1).
  - Lavouras de aldeia de planície: sobrescrever `data/minecraft/worldgen/processor_list/farm_plains.json`, mantendo as regras vanilla e acrescentando **antes** delas brisa 0,05, aurora 0,025 e eclipse 0,01 no crop com age 0.
  - Campo natural: bloco `wild_plant`, colocado por worldgen em `#minecraft:is_jungle` (equivalente da selva, que não tem aldeia) e em plains/meadow. Ao quebrar, dá sementes com pesos 60/30/10.
- **Receita do baseado:** shapeless `rolling_paper` + `#cultivo:dried_flowers` → `joint`.
- **Artigos:** cada um com receita shaped/shapeless de itens vanilla. Use um GameTest para garantir que cada padrão resolve para a receita do mod e não conflita com receitas vanilla.
- **Assets:**
  - texturas originais 16×16 geradas por script Node em `tools/art/` (usando `png.mjs`);
  - blockstates, modelos e lang (en_us/pt_br) podem vir de um gerador `tools/gen/` alimentado por um manifesto único;
  - os sons e partículas usados são os vanilla (por exemplo `SoundEvents.CROP_BREAK`), então não há sons próprios para criar.
- **Testes:**
  - JUnit (adicionar `testImplementation` do JUnit 5 + `useJUnitPlatform()`) para as regras puras (qualidade, estufa, pesos de fonte, rendimento de processamento) e para a completude dos assets e receitas (parse dos JSON);
  - GameTests para crescimento, colheita, estufa, estações (incluindo persistência via save/load da BE), injeção de loot e receitas.

## 5. O que existe agora (mapa do código)
- `src/main/java/io/github/peixeangola/cultivo/`:
  - `Cultivo` (@Mod): registra todos os DeferredRegister e o listener de `LootTableLoadEvent`;
  - `core/`: regras **puras**, sem Minecraft: `Variety`, `QualityRules`, `GrowthRules`, `GreenhouseRules`, `ProcessingRules`, `StationKind`, `SeedSources`, `ArticleRecipes`, `ContentIds`;
  - `registry/`: `ModBlocks`, `ModItems`, `ModComponents` (`cultivo:quality`), `ModBlockEntities` (`processing_station`), `ModRecipes` (tipos e serializers drying/sieving/pressing), `ModLootFunctions` (`set_harvest_quality`), `ModCreativeTabs`, `ModTags`;
  - `block/`: `CannabisCropBlock`, `WildPlantBlock`, `ProcessingStationBlock` e `ProcessingStationBlockEntity`, `GreenhouseDetector`;
  - `item/`: `QualityItem`, `SeedItem`, `DecorativeItem` e `StationBlockItem` (tooltips);
  - `recipe/ProcessingRecipe`, `loot/SetHarvestQualityFunction`, `loot/SeedLootInjector`;
  - `gametest/CultivoGameTests`: 10 GameTests registrados em `Registries.TEST_FUNCTION`.
- `src/generated/resources/`: **gerado** por `node tools/gen/generate.mjs`, que também chama `tools/art/textures.mjs`. Não editar à mão. Para mudar algo, edite `tools/gen/manifest.json` e os scripts e rode o gerador de novo.
- `src/test/java/`: testes das regras puras (`core/`) e da completude e consistência dos dados (`data/`). Os testes leem os arquivos a partir da raiz do projeto.
- `tools/test-offline.sh`: roda o JUnit sem Gradle nem Minecraft. Útil quando o maven do Forge não está acessível.

## 6. Suposições não verificadas (conferir primeiro quando o build rodar)
Foram escritas sem o jar de fontes do 26.3. Se algo não compilar, confira primeiro estes pontos.

**API Java**
1. `SetHarvestQualityFunction`: o construtor de `LootItemConditionalFunction` recebe `Optional<LootItemCondition>` (no 26.3 a condição é única); `new LootItemFunctionType<>(MapCodec)`; `LootContext#getOptionalParameter`.
2. `ProcessingRecipe`:
   - `Recipe#assemble(SingleRecipeInput)` com 1 argumento;
   - `PlacementInfo.NOT_PLACEABLE`, `RecipeBookCategories.CRAFTING_MISC`;
   - `Ingredient.CODEC`/`CONTENTS_STREAM_CODEC`;
   - `new RecipeSerializer<>(MapCodec, StreamCodec)`;
   - `BuiltInRegistries.ITEM.byNameCodec()`.
   O resultado foi modelado como `{id, count}` próprio, sem `ItemStackTemplate`.
3. `WildPlantBlock extends VegetationBlock`: construtor e métodos abstratos.
4. `BlockBehaviour.Properties.ofFullCopy(...)`, `noOcclusion()`, `Item.Properties#useBlockDescriptionPrefix()` e `useItemDescriptionPrefix()`.
5. `ProcessingStationBlockEntity`:
   - `ContainerHelper.saveAllItems(ValueOutput, …)`/`loadAllItems(ValueInput, …)`, `ValueInput#getIntOr`;
   - `preRemoveSideEffects`;
   - `level.recipeAccess().getRecipeFor(...)`.
6. `ProcessingStationBlock`: `useItemOn`/`useWithoutItem`, `Player#hasInfiniteMaterials()` e `level.isClientSide()`.
7. `CannabisCropBlock`: `ForgeHooks.onCropsGrowPre(Level, BlockPos, BlockState, boolean)`/`onCropsGrowPost` e `getBaseSeedId()`.
8. `SeedLootInjector`: `LootTableLoadEvent#getName()` retornando `Identifier`, `LootTable#addPool`, `LootPool.Builder#name`, `NestedLootTable.lootTableReference`.
9. `CultivoGameTests`:
   - `helper.fail(Component)`;
   - `BlockEntity#saveWithFullMetadata(HolderLookup.Provider)` + `BlockEntity.loadStatic(...)`;
   - `Block.getDrops(state, ServerLevel, pos, null)`;
   - `LootTable#getRandomItems(LootParams)`;
   - `CraftingInput.of(w, h, list)`;
   - `BuiltInRegistries.ITEM.getValue(Identifier)`.
10. `QualityItem`, `SeedItem` e afins: assinatura de `appendHoverText` (vem de NEXT_SESSION, seção 2).

**Formatos JSON** (copie dos vanilla em `client-extra-*.jar` se diferirem):
1. `worldgen/feature/wild_plant.json`: usei `simple_block` + `to_place` como provider (`simple_state_provider`), sem o wrapper `config`. A nota da seção 2 dizia `"to_place":{"id":...}`; confira em `worldgen/feature/` vanilla.
2. `worldgen/placed_feature/wild_plant_*.json`: `rarity_filter`, `count`, `random_offset` (int providers `uniform` com `min_inclusive`/`max_inclusive`) e `block_predicate_filter`. Compare com `patch_melon.json`.
3. `data/minecraft/worldgen/processor_list/farm_plains.json`: as regras vanilla (cenoura 0,3, batata 0,2, beterraba 0,1) vieram de memória do 1.21. **Substitua-as pelas do jar 26.3**, mantendo as 3 do mod antes. O teste `farmPlainsAddsModRulesBeforeVanillaRules` aceita qualquer quantidade de regras vanilla, desde que fiquem depois das do mod.
4. Loot: entradas com `condition` e `modifier` (objeto ou lista), predicados e funções com `type`; `set_count` com `{"type":"minecraft:uniform","min","max"}`. Compare com `loot_table/blocks/wheat.json`.
5. `forge/biome_modifier/`: confira o caminho exato da pasta no registro `forge:biome_modifier`.
6. `test_instance` e `forge:empty9x9x9`: confira se o GameTest `outdoor_growth` tem luz ≥ 9 (o teste falha com a mensagem "luz insuficiente" se não tiver).

## 7. Próximos passos, em ordem
1. Liberar a rede (ou usar a máquina local com JDK 25) e rodar `./gradlew build`; corrigir os itens da seção 6.
2. `./gradlew test`: deve repetir os 62 testes de `tools/test-offline.sh`.
3. `./gradlew runGameTestServer`: os 10 GameTests devem passar.
4. `runClient` (ver texturas, tooltips, estufa) e `runServer` (aceitar a EULA em `run/`; entrar com cliente).
5. Inspecionar `build/libs/cultivo-0.1.0.jar` e atualizar TODO.md **somente com evidências**.

## Histórico: plano da sessão de investigação (passos 1–5 e 7 feitos na nuvem; o 6 foi bloqueado pela rede)
1. Commitar `tools/art/png.mjs` e este arquivo.
2. Renomear o MDK para `cultivo`, criar o esqueleto de registros e rodar `gradlew build`.
3. Implementar as regras puras com JUnit (TDD).
4. Escrever o Java de conteúdo: crop, estações, receitas, função de loot, injeção, creative tab.
5. Gerar os dados JSON e os assets (script de arte + gerador).
6. Rodar GameTests (`runGameTestServer`), `build` e, se possível, `runServer` para testar o servidor dedicado.
7. Atualizar TODO.md, DECISIONS.md e TECHNICAL_RESEARCH.md **somente com evidências** (comandos e resultados reais).
