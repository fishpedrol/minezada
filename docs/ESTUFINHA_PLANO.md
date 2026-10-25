# Plano da estufinha (grow tent)

Pedido do dono em 25/10/2026: trocar a estufa de vidro por uma **estufinha** craftável com **luz roxa**, que caiba **um pé** da planta, **sem farmland e sem água**, com **qualidade maior**. Respostas dele às perguntas:

- **Estufa de vidro:** trocar pela estufinha. O vidro deixa de dar bônus: ao ar livre a qualidade fica em 2–3, e a qualidade alta (4–5) só sai da estufinha.
- **Tamanho:** 2 blocos de altura, como uma grow tent de verdade. A planta aparece no tamanho normal dentro dela.
- **Colheita:** colhe e esvazia. Clique direito na planta madura entrega flores e sementes como a lavoura, e a estufinha fica vazia para replantar.
- **Qualidade:** 4–5, como a estufa de hoje.

O dono aprovou este plano em 25/10/2026 ("aprovo o plano"). **Nada dele foi implementado ainda.**

**Situação deste documento:** é o projeto gerado na nuvem a partir de 5 leituras do código e das fontes do Minecraft/Forge 26.3. A etapa de crítica adversarial não terminou na nuvem; a revisão abaixo foi feita no PC do dono em 25/10/2026, antes de codar.

## Revisão de 25/10/2026 (fontes do Forge 26.3-66.0.13 e jar do Serene Seasons 26.1.2.0.7)

Conferido nas fontes decompiladas (`forge-26.3-66.0.13-sources.jar`) e no jar `SereneSeasons-forge-26.3-26.1.2.0.7.jar` do launcher do dono (com `javap`). **Correções ao plano:**

1. **`PushReaction.DESTROY` não existe** no 26.3: o enum é `PUSH_PULL, PUSH, POPPED, IMMOVEABLE, IGNORE_ENTITY`. Use **`IMMOVEABLE`**. Motivo: a metade de baixo tem BlockEntity e o pistão já não a empurra, mas a de cima não tem, e um pistão a separaria; com `POPPED`, o pistão quebra o bloco com a flag 18 (sem atualização de forma), e a outra metade ficaria órfã. Imóvel, como a mesa de encantamento, evita os dois casos.
2. **`DoublePlantBlock.preventDropFromBottomPart` é `protected static`** no pacote `net.minecraft.world.level.block`. A porta o chama porque está no mesmo pacote; a `GrowTentBlock` (outro pacote, sem herdar de `DoublePlantBlock`) não pode. A estufinha tem uma cópia própria do método (mesmas flags 35 e o `levelEvent 2001`).
3. **`Block.getDrops(..., Entity, ItemInstance tool)`**: o último parâmetro é `ItemInstance` (o `ItemStack` serve). Confirmado.
4. **Item:** sem `useBlockDescriptionPrefix`. O `AssetCompletenessTest.everyIdIsTranslatedInBothLanguages` exige `item.cultivo.<id>` para todo item plano e `block.cultivo.<id>` para todo bloco, como nos artigos colocáveis; o gerador escreve as duas chaves.
5. **`ContentIds`:** a estufinha entra numa lista própria `CULTIVATION_BLOCKS` (em `allBlocks()`) e em `flatItems()` (o ícone é `textures/item/grow_tent`), como os artigos colocáveis, e não em `blocksWithItems()` (que é para itens com modelo de bloco).

**Riscos da seção 8, conferidos:**

1. `DoubleHighBlockItem` limpa o espaço de cima (flags 27), põe a metade de baixo e só então o `BlockItem` chama `setPlacedBy`, que põe a de cima. A BlockEntity nasce no `setBlock` da de baixo (`LevelChunk.setBlockState` chama `newBlockEntity`), antes de existir a de cima; colocar a de cima atualiza a forma da de baixo com o vizinho certo. Sem problema.
2. Ver correção 4. O `flatItemsHaveOwnTextures` só percorre `flatItems()`, e a estufinha tem textura `item/grow_tent`.
3. `light_emission` é lido por `CuboidModelElement.Deserializer` (inteiro de 0 a 15; fora disso, erro). O `modelElementsStayWithinLimits` não recusa a chave. A face emissiva só se confirma no cliente (teste visual).
4. O `BlockModelRenderState.setupModel` usa `Sheets.cutoutBlockItemSheet()` quando o modelo não tem translucidez: a planta (alfa só 0 ou 255) sai em cutout. A escala e o encaixe no vaso só se confirmam no cliente.
5. O Serene Seasons 26.1.2.0.7 **não** escuta `CropGrowEvent`: ele age pelo mixin no `randomTick` (`SeasonalCropGrowthHandler.onCropGrowth`) e pela farinha de osso no `PlayerInteractEvent$UseBlock` do GlitchCore (`applyBonemeal`), os dois só quando `ModFertility.isCrop(state)` vale para o bloco tocado. A estufinha não está em tag nenhuma do Serene Seasons, então cresce o ano todo.
6. O conjunto `LootContextParamSets.BLOCK` tem `BLOCK_ENTITY` como opcional: rolar a tabela da lavoura com a BlockEntity da estufinha passa na validação. Não é preciso `getReferencedContextParams`.
7. `LevelChunk.setBlockState` chama `preRemoveSideEffects` com a BlockEntity ainda registrada (antes de `removeBlockEntity`) e só pula com a flag 256. `Level.destroyBlock` rola o loot do bloco antes do `setBlock`; o loot da estufinha é só o item (metade de baixo), e a planta sai uma vez, no `preRemoveSideEffects`. Explosões rolam o loot de cada metade e o `setBlock` dispara o mesmo `preRemoveSideEffects`. Pistões: ver correção 1.
8. Larguras e palavras: conferidas pelo `TooltipHintsTest` e pelo `PortugueseEverywhereTest` no build.
9. Visual probe: conferido pelo `validate.mjs` depois de gerar o datapack.
10. Números de balanço: travados pelo `GrowTentRulesTest` e registrados em DECISIONS.md para o dono confirmar.

As leituras que embasaram o plano estão em [estufinha-mapas.md](estufinha-mapas.md), com citações `arquivo:linha` do commit `09846e1`. O texto abaixo está em inglês, como saiu do workflow.

---

# SPEC: Estufinha (grow tent) replaces the glass greenhouse

Status: design only. I changed no files and did not run Gradle or Minecraft. Paths are under `/home/user/minezada/`. `C/` = `src/main/java/io/github/peixeangola/cultivo/`, `T/` = `src/test/java/io/github/peixeangola/cultivo/`, `F/` = `/tmp/forge-src/`.

What the owner asked for (25/10/2026): a crafted block (no glass) that is 2 blocks tall. One plant fits inside at normal size. It has a purple light and needs no farmland and no water. Harvest quality is 4–5 (outdoor stays 2–3). Right-clicking a ripe plant harvests it and leaves the tent empty. Glass no longer gives any bonus.

---

## 1. Gameplay rules (exact numbers)

### 1.1 Growth

The new pure class is `C/core/GrowTentRules.java`. It is Minecraft-free and depends only on `Variety`.

| Constant | Value | Reason |
|---|---|---|
| `SOIL_SPEED` | `4.0F` | The same base as a lone hydrated farmland plot: 1 + 3 (`CannabisCropBlock.vanillaGrowthSpeed`, `C/block/CannabisCropBlock.java:57-93`). |
| `MULTIPLIER` | `1.25F` | Takes over the role of `GrowthRules.GREENHOUSE_MULTIPLIER` (`C/core/GrowthRules.java:10`), which is deleted. |
| `LAMP_LIGHT` | `12` | Block light the upper half emits. |
| `BONE_MEAL_MIN` / `BONE_MEAL_MAX` | `2` / `5` | Copied from `CropBlock.getBonemealAgeIncrease` (`F/net/minecraft/world/level/block/CropBlock.java:85-92`), because that method is protected. |

- `speed(Variety v) = SOIL_SPEED * v.growthMultiplier() * MULTIPLIER`.
  - Multiply in this order, as `GrowthRules.effectiveSpeed` does today (`GrowthRules.java:14-17`), so the float results are identical.
- `rollBound(Variety v) = GrowthRules.growthRollBound(speed(v))`.
  - That function is `(int)(25/v)+1` (`GrowthRules.java:20-25`).
- Literal results:

  | Class | Speed | Bound |
  |---|---|---|
  | FAST | 4 × 1.25 × 1.25 = 6.25 | 5 |
  | MEDIUM | 5.0 | 6 |
  | SLOW | 4 × 0.8 × 1.25 ≈ 4.0 | 7 |

  - Multipliers come from `Variety.java:85-99,202-205`.
  - For comparison, a lone hydrated plot outdoors gives 6 / 7 / 8 (`T/core/VarietyAndGrowthTest.java:39-43`).
  - A dense wheat-style field gives (1+3+8×0.75)/2 = 5 × variety, which is the same as the tent's MEDIUM value. So the tent grows as fast as a good field, without soil or water.
- Expected time to mature at the default `randomTickSpeed=3`, averaging 4096/3 ≈ 1365 ticks between random ticks per block:
  - formula: 7 × bound × 1365 ticks;
  - FAST ≈ 39.8 min, MEDIUM ≈ 47.8 min, SLOW ≈ 55.8 min.
- Light: **none required.** The tent has its own lamp, so `GrowthRules.hasEnoughLight` (`GrowthRules.java:27-29`) is not called. The tent grows in a sealed dark room.
- Soil and water: none. There is no farmland or moisture check, and the tent never touches the block below it except through `canSurvive` (§2.6).
- Tick hook: random ticks on the lower half only, the same pattern as the crop (`CannabisCropBlock.java:39-55`) and `PitcherCropBlock` (`F/.../PitcherCropBlock.java:135-147`).

  ```java
  // GrowTentBlock
  @Override protected boolean isRandomlyTicking(BlockState s) { return s.getValue(HALF) == DoubleBlockHalf.LOWER; }
  @Override protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
      if (!(level.getBlockEntity(pos) instanceof GrowTentBlockEntity tent) || !GrowTentRules.canGrow(tent.hasPlant(), tent.age())) return;
      boolean grows = r.nextInt(GrowTentRules.rollBound(tent.variety())) == 0;
      if (ForgeHooks.onCropsGrowPre(level, pos, s, grows)) { tent.setAge(tent.age() + 1); ForgeHooks.onCropsGrowPost(level, pos, s); }
  }
  ```

  - `isRandomlyTicking` is cached per state (`F/.../state/BlockBehaviour.java:520-522,897`). Empty or ripe tents therefore still get ticked, but they return after one block-entity lookup. That is accepted to keep the blockstate small (§2.2).

### 1.2 Bone meal

`GrowTentBlock implements BonemealableBlock`. The 26.3 signatures take a `BonemealSource` (`F/.../BonemealableBlock.java:14-18`).

- `isValidBonemealTarget(level, pos, state, src)`: resolve the lower half (§2.5). Valid if the tent has a plant and age < 7. This runs on the client too, through `BoneMealItem.applyBonemeal` (`F/net/minecraft/world/item/BoneMealItem.java:73-90`), so it reads the synced block entity.
- `isBonemealSuccess`: `true`.
- `performBonemeal`: `age = GrowTentRules.afterBoneMeal(age, Mth.nextInt(random, 2, 5))`, which is `min(7, age + bonus)`.
- It works on either half, from dispensers too (`BoneMealItem.java:35-56`). The vanilla item plays its own particles (levelEvent 1505), so the mod plays no sound of its own here.

### 1.3 Quality

- Quality still comes only from `QualityRules.harvestQuality` (`C/core/QualityRules.java:22-27`).
- Rename `GREENHOUSE_BASE` to `TENT_BASE`; it stays `4`. Rename the boolean parameter `greenhouse` to `tent`.
- Results: tent 4–5, everything else (outdoor, under glass, anywhere) 2–3. Curing in the jar still adds +1 once (`C/core/CuringRules.java:30-32`).

### 1.4 Drops: the crop's own loot table

- `GrowTentBlockEntity.rollPlantDrops(ServerLevel lvl, @Nullable Entity who, ItemInstance tool)` returns `Block.getDrops(cropState, lvl, worldPosition, this, who, tool)`:
  - `cropState = ModBlocks.CROPS.get(variety).get().getStateForAge(age)`;
  - `Block.getDrops` is at `F/.../Block.java:371-385`.
- The roll goes through `cultivo:blocks/<v>_crop`, so the drops are exactly the crop's:
  - ripe: 1 fresh flower plus 1 + Bin(3+Fortune, 4/7) seeds;
  - immature: 1 seed (`tools/gen/generate.mjs:266-305`).
  - Global loot modifiers and datapack overrides still apply.
- Change to `C/loot/SetHarvestQualityFunction.java:38-46`. Remove the `GreenhouseDetector` import and call, and decide the route from the BLOCK_ENTITY context parameter:

  ```java
  boolean tent = context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GrowTentBlockEntity;
  ```

  - Outdoor crops have no block entity, so they get 2–3.
  - Read the block entity rather than the block at ORIGIN: when the tent is broken, the chunk already holds the new block by the time side effects run (`F/.../chunk/LevelChunk.java:279,304-315`).
  - The ORIGIN null check is no longer needed. Keep the BLOCK_STATE `instanceof CannabisCropBlock` check.

### 1.5 Harvest (right-click) and emptying

- Right-click on a ripe tent (either half):
  1. roll `rollPlantDrops(server, player, player.getMainHandItem())`, so a Fortune tool in the main hand adds seeds as it would when breaking a crop;
  2. `tent.clear()` (variety = null, age = 0);
  3. hand every stack out with `Handouts.give(player, stack)` (`C/block/Handouts.java:20-22`); anything that doesn't fit drops at the player's feet;
  4. play `ModSounds.GROW_TENT_HARVEST`;
  5. send `gameEvent(GameEvent.BLOCK_CHANGE, lowerPos, GameEvent.Context.of(player, state))`, as in `F/.../SweetBerryBushBlock.java:118-133`.
- The tent is then empty and can be replanted at once.
- Right-click on an immature plant does nothing (PASS). To get the seed back, break the tent.

### 1.6 Breaking

- Breaking either half in survival drops 1 `cultivo:grow_tent` and the plant's drops:
  - a ripe plant gives a flower at quality 4–5 plus seeds;
  - an immature plant gives 1 seed;
  - an empty tent gives no plant drops.
- The plant drops from `GrowTentBlockEntity.preRemoveSideEffects(pos, state)`: `rollPlantDrops(level, null, ItemStack.EMPTY).forEach(s -> Block.popResource(level, pos, s))`, then `clear()`. This is the pattern at `C/block/JarBlockEntity.java:206-213`.
- Creative: the door path (`F/.../DoorBlock.java:111-118` calling `DoublePlantBlock.preventDropFromBottomPart`, `DoublePlantBlock.java:117-128`, flags 35) suppresses the tent item. `preRemoveSideEffects` still runs (flag 256 is not set), so the plant still drops, the same way the jar drops its contents. This is deliberate; write it down.
- Explosions: the tent item is behind `survives_explosion`. The plant drops through `preRemoveSideEffects`, and `explosion_decay` in the crop table does nothing without EXPLOSION_RADIUS.
- `/setblock` replacing the tent also drops the plant, as the jar does.

### 1.7 Compatibility

- Serene Seasons: the tent block is in no SS tag. SS gates growth through `ModFertility.isCrop` (season tags) in its randomTick mixin, so tent plants grow all year.
  - `CompatibilityTest:55-73` requires the SS tags to contain exactly the 40 crops, so the tent stays out of them.
  - Write this in DECISIONS.
- Villagers: `HarvestFarmland` breaks only `CropBlock`s on farmland, so villagers never touch tents. `C/village/FarmerReplant.java` is unchanged.
- Bees: the tent stays out of `bee_growables`, `crops` and `maintains_farmland` (`DataConsistencyTest:359-368`).

---

## 2. Block design

### 2.1 IDs and classes

- Id: `cultivo:grow_tent`. Constant `ContentIds.GROW_TENT = "grow_tent"`.
- Names: pt "Estufinha", en "Grow Tent". With `portuguese_everywhere`, `en_us.json` gets the pt text.
- New classes:

  | Class | Role |
  |---|---|
  | `C/block/GrowTentBlock.java` | `extends Block implements EntityBlock, BonemealableBlock`, the same base as `JarBlock` (`C/block/JarBlock.java:44`). It must be in `block/` because `Handouts` is package-private. |
  | `C/block/GrowTentBlockEntity.java` | Holds the plant. |
  | `C/core/GrowTentRules.java` | Pure rules. |
  | `C/client/GrowTentRenderer.java` | Draws the plant. |

- Item: `new DoubleHighBlockItem(ModBlocks.GROW_TENT.get(), new Item.Properties().setId(ITEMS.key(GROW_TENT)))`.
  - `DoubleHighBlockItem` is at `F/net/minecraft/world/item/DoubleHighBlockItem.java:10-22`.
  - Stack size 64, the vanilla default.
  - The tooltip goes through a small subclass or an `appendHoverText` that calls `TooltipItem.addHint(..., "tooltip.cultivo.grow_tent")` (`C/item/TooltipItem.java:29-33`). Suggested name: `item/GrowTentItem extends DoubleHighBlockItem`.

### 2.2 Blockstate (8 states)

| Property | Values | Notes |
|---|---|---|
| `half` | `BlockStateProperties.DOUBLE_BLOCK_HALF` (`upper`, `lower`) | Default `LOWER` (`F/.../DoublePlantBlock.java:28,32`). |
| `facing` | `HorizontalDirectionalBlock.FACING` | Default `NORTH`. Placement uses `getHorizontalDirection().getOpposite()`, as in `ProcessingStationBlock.java:106-108`. `rotate`/`mirror` as at `:116-123`. |

- No `age`, `planted` or strain property. The variety and age live only in the block entity, so the blockstate stays at 8 states.
- `createBlockStateDefinition` runs inside the `Block` constructor (`F/.../Block.java:233-236`), so use static properties only.

### 2.3 Block properties (`C/registry/ModBlocks.java`, next to JAR at `:64-67`)

```java
BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.8F).sound(SoundType.WOOL)
    .noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
    .lightLevel(s -> s.getValue(GrowTentBlock.HALF) == DoubleBlockHalf.UPPER ? GrowTentRules.LAMP_LIGHT : 0)
    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
    .setId(BLOCKS.key(ContentIds.GROW_TENT))
```

- `lightLevel` is at `F/.../BlockBehaviour.java:1171`.
- The lamp is always on, which gives about 11 block light at the lower half (`propagatesSkylightDown` is true because the shape is not a full block).
- Side effect: hostile mobs won't spawn right next to a tent. Write this in DECISIONS.
- No `requiresCorrectToolForDrops` and no mineable tag, so `DataConsistencyTest:602-618` is unchanged.
- Not flammable.
- `isPathfindable` returns false.

### 2.4 Shapes

- Lower half: `Block.box(1,0,1,15,16,15)`. Upper half: `Block.box(1,0,1,15,14,15)`. Both are symmetric, so no rotation is needed.
- Collision shape = outline shape.

### 2.5 Two-tall mechanics (door pattern, `F/.../DoorBlock.java`)

| Hook | Behaviour |
|---|---|
| `getStateForPlacement` | Return the state only if `pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(ctx)`; set `facing` and `HALF=LOWER` (`DoorBlock.java:129-143`). Otherwise return null and `BlockItem` fails (`F/.../BlockItem.java:71-74`). |
| `setPlacedBy` | `level.setBlockAndUpdate(pos.above(), state.setValue(HALF, UPPER))` (`DoorBlock.java:146-148`). |
| `updateShape` | If the neighbour in `half.getDirectionToOther()` is not this block with the other half, return AIR (`DoublePlantBlock.java:35-56`). Lower half: if the neighbour below changed and `!canSurvive`, return AIR. |
| `canSurvive` | Lower half: `below.isFaceSturdy(level, pos.below(), Direction.UP)`. Upper half: the block below is this block with `half=lower` (`DoorBlock.java:230-235`). |
| `playerWillDestroy` | Copy the door: on the server, if `player.preventsBlockDrops()`, call the tent's own copy of `preventDropFromBottomPart` (the vanilla one is not accessible, see the review); then call super. |
| `newBlockEntity` | `half == LOWER ? new GrowTentBlockEntity(pos, state) : null` (`F/.../chunk/LevelChunk.java:330-341`). No ticker (`getTicker` returns null). |
| `lowerPos(state, pos)` | `half == LOWER ? pos : pos.below()`, the pattern in `PitcherCropBlock.getLowerHalf` (`:188-196`). Every read and write goes through `level.getBlockEntity(lowerPos)`. |

### 2.6 `GrowTentBlockEntity` data

- Fields: `@Nullable Variety variety`, `int age` (0..7).
- Public accessors: `hasPlant()`, `variety()`, `age()`.
- Public mutators: `plant(Variety)` and `setAge(int)`; `setAge` also serves as the GameTest hook, like `JarBlockEntity.setItem` (`:61-74`). Plus `clear()`.
- Every mutation calls `changed()`: `setChanged()`, then, only on the server, `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)` (`JarBlockEntity.java:135-142`).
- `saveAdditional(ValueOutput o)`: if a variety is present, `o.putString("variety", variety.id())` and `o.putInt("age", age)` (`F/.../storage/ValueOutput.java:29`).
- `loadAdditional(ValueInput i)`:
  - `variety = i.getString("variety").flatMap(Variety::byId).orElse(null)` (`ValueInput.java:45`, `C/core/Variety.java:128-131`);
  - `age = variety == null ? 0 : Mth.clamp(i.getIntOr("age", 0), 0, 7)` (`ValueInput.java:35`).
  - An unknown id loads as an empty tent; no crash.
- `getUpdatePacket` returns `ClientboundBlockEntityDataPacket.create(this)`. `getUpdateTag` writes the same two fields through `TagValueOutput.createWithContext` (`JarBlockEntity.java:191-204`).
- `getRenderBoundingBox()` is overridden to `new AABB(worldPosition.getX(), y, z, x+1, y+2, z+1)`. The default box is the lower collision shape (`F/net/minecraftforge/common/extensions/IForgeBlockEntity.java:78-95`), and the ripe plant pokes 3 px into the upper half (§3.2).
- Registration: `ModBlockEntities.GROW_TENT = BLOCK_ENTITIES.register("grow_tent", () -> new BlockEntityType<>(GrowTentBlockEntity::new, Set.of(ModBlocks.GROW_TENT.get())))`, following the pattern at `C/registry/ModBlockEntities.java:28-29`.

### 2.7 Interactions (either half resolves to `lowerPos`)

Both sides call the same pure function `GrowTentRules.use(boolean hasPlant, int age, boolean holdingSeed)`, which returns one of `PLANT`, `HARVEST` or `NONE`. The client evaluates it on the synced block entity; the server then acts. This is the `JarBlock.predictInsert/predictTake` approach (`C/block/JarBlock.java:86-118`).

| Hand / state | `useItemOn` | `useWithoutItem` (main hand only) | Server effect | Result on both sides |
|---|---|---|---|---|
| `SeedItem`, tent empty | PLANT | – | Plant `seed.variety()`, age 0. Shrink 1 unless `player.hasInfiniteMaterials()`. Play `ModSounds.GROW_TENT_PLANT`. Send gameEvent BLOCK_PLACE. | `SUCCESS` |
| Seed, tent occupied | `TRY_WITH_EMPTY_HAND` | Ripe: HARVEST. Otherwise PASS. | Harvest (§1.5) | `SUCCESS` / `PASS` |
| Empty hand, ripe | – | HARVEST | Harvest (§1.5) | `SUCCESS` |
| Empty hand, empty or growing | – | NONE | – | `PASS` |
| Bone meal, growing | `TRY_WITH_EMPTY_HAND` | NONE, so PASS | `BoneMealItem.useOn` runs next (`ServerPlayerGameMode.java:440-458` then the item path) and calls §1.2 | vanilla bone meal result |
| Bone meal, ripe | `TRY_WITH_EMPTY_HAND` | HARVEST | Harvest | `SUCCESS` |
| Any other item (shears, hoe…) | `TRY_WITH_EMPTY_HAND` | as for an empty hand | ripe: harvest, Fortune from the main hand applies | `SUCCESS` / `PASS` |
| Sneaking with any item | the block is not called (`ServerPlayerGameMode.java:440-442`) | – | The seed's `BlockItem` placement fails without farmland | `PASS` |
| Sneaking, both hands empty | as for an empty hand | | | |
| No block entity (corrupt) | `PASS` | `PASS` | – | – |

- Bone meal needs no special case. Because `useWithoutItem` returns PASS while the plant grows, the bone meal item's own `useOn` runs.
- **Always return `SUCCESS`, never `SUCCESS_SERVER`.** The client prediction already swung the arm; see the javadoc at `C/block/ProcessingStationBlock.java:49-52` and the GameTest `interactions_swing_once` (`C/gametest/CultivoGameTests.java:2322-2332`).
- The seed check is `stack.getItem() instanceof SeedItem seed`. `SeedItem` is common code, so the check works on the client.

---

## 3. Rendering

### 3.1 Tent models (`tools/gen/models.mjs`, new `growTentModels()` and `growTentBlockstate()`, built with `box()` at `:54-74`)

Parent `minecraft:block/block` (`blockParent`, `:82`), all coordinates in [0,16] (`AssetCompletenessTest:258-293`), front facing north before rotation. `FACING_Y` is at `:80`.

| Model | Elements |
|---|---|
| `cultivo:block/grow_tent_lower` | 4 corner poles 1..2 / 14..15, y 0..16 (`grow_tent_frame`). Floor tray 1..15 × 0..1 (`grow_tent_canvas`). Back and side walls 0.5 px thick, outside `grow_tent_canvas`, inside `grow_tent_mylar`. A front sill 1..15 × 0..3 at z 1..1.5. Fabric pot 5..11 × 1..4 × 5..11 (`grow_tent_pot`; its top face shows substrate). The front is open. |
| `cultivo:block/grow_tent_upper` | Poles 0..13. Side and back walls 0..13. Roof 1..15 × 13..14. A rolled-up front flap 1..15 × 10..13 × 1..3 (`grow_tent_flap`, with a zipper pull). Lamp housing 4..12 × 11..12.5 × 4..12 (`grow_tent_lamp`). The LED panel is the down face of an element 4.5..11.5 × 10.9..11 × 4.5..11.5 with `"light_emission": 15` and `"shade": false`; this per-element field is new in 26.3 (`F/net/minecraft/client/resources/model/cuboid/CuboidModelElement.java:43,64-70`, pattern `CE:assets/minecraft/models/block/cross_emissive.json`). There is also a small purple LED dot on the outside front pole (1×1 px element, `light_emission` 15) so the light reads from outside. |

- Blockstate `assets/cultivo/blockstates/grow_tent.json` uses `variants` with all 8 keys `facing=<f>,half=<h>`, each pointing to `cultivo:block/grow_tent_<h>` with `y` = `FACING_Y[f]`. This matches the vanilla `oak_door.json` style without the hinge.
- The `particle` texture is `cultivo:block/grow_tent_canvas`.
- No translucency: the textures use alpha 0/255 only (`TextureArtTest:91-106`). The plant shows through the open front.
- The glow comes from `light_emission` plus the world `lightLevel`. No `emissiveRendering` on the whole block.

### 3.2 Plant: `C/client/GrowTentRenderer.java`

- `implements BlockEntityRenderer<GrowTentBlockEntity, GrowTentRenderer.State>` and copies `PressPlatenRenderer` (`C/client/PressPlatenRenderer.java:43-112`):
  - `State extends BlockEntityRenderState` holds a `BlockModelRenderState plant`.
  - The constructor takes `context.blockModelResolver()`.
  - `extractRenderState`:
    1. call super first (`:86-88`);
    2. if `!be.hasPlant()`, `state.plant.clear()`;
    3. otherwise `models.update(state.plant, ModBlocks.CROPS.get(be.variety()).get().getStateForAge(be.age()), DISPLAY_CONTEXT)`. `BlockModelResolver.update` takes any registered state (`F/.../client/renderer/block/BlockModelResolver.java:21-25`).
  - `submit`: `pushPose`; `translate(0.5, 4/16f, 0.5)` (the crop model spans y −1..15, `CE:assets/minecraft/models/block/crop.json`, so its base sits on the pot soil at y=3); `scale(0.875, 1, 0.875)` so the leaves clear the walls; `translate(-0.5, 0, -0.5)`; `state.plant.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0)`; `popPose`.
  - The ripe plant reaches y 19/16, which is why the render box is overridden (§2.6).
  - `getViewDistance()` returns the render distance × 16, as at `:81-83`.
- This reuses the existing 40×8 crop states and models. No new plant art is needed.
- Register it in `C/client/CultivoClientEvents.java:28-30` with `event.registerBlockEntityRenderer(ModBlockEntities.GROW_TENT.get(), GrowTentRenderer::new)`. Common code never references `client/` (`ClientIsolationTest:64-103`).

### 3.3 Particles

`GrowTentBlock.animateTick(state, level, pos, random)` (`F/.../Block.java:358`). It is client-only by the caller (`ClientLevel.java:569,607`) and uses only `Level#addParticle`.

- Upper half, `random.nextInt(3)==0`: `level.addParticle(new DustParticleOptions(0xB04DFF, 0.6F), x+0.25+r*0.5, y+0.6, z+0.25+r*0.5, 0, 0, 0)` (`F/net/minecraft/core/particles/DustParticleOptions.java:26-29`).
- Lower half, only if the synced block entity has a ripe plant, `random.nextInt(5)==0`: one more purple dust particle at y+0.9 as a "ready" cue.

### 3.4 Textures (`tools/art/textures.mjs`)

All are 16×16 with alpha 0/255 only, original pixel art, and unanimated (so `AnimatedTexturesTest` `EXPECTED_FRAMES` doesn't change).

| Texture | Look |
|---|---|
| `block/grow_tent_canvas` | Charcoal fabric (#2b2b30 / #34343a) with a vertical seam and purple stitching (#6a3fa0). |
| `block/grow_tent_mylar` | Reflective silver diamond pattern (#c8c8d0, #a9a9b6) with lavender highlights (#d9c2ff). |
| `block/grow_tent_frame` | Dark metal pole (#55555e, #6e6e78). |
| `block/grow_tent_flap` | Rolled canvas with a band and a gold zipper pull (#c9b037). |
| `block/grow_tent_lamp` | Black housing (#1e1e22) with a purple LED grid (#7a1fd1 / #b04dff / #e0a8ff). Also used for the emissive faces. |
| `block/grow_tent_pot` | Black fabric pot sides, dark substrate on top (#4a3222 / #3a2618). |
| `item/grow_tent` | Flat icon: a tall dark tent silhouette, open front, a small green plant, a purple lamp glow line at the top. |

- Item model: `cultivo:item/grow_tent` with parent `minecraft:item/generated`, like vanilla doors.
- Item definition: `assets/cultivo/items/grow_tent.json` = `{"model":{"type":"minecraft:model","model":"cultivo:item/grow_tent"}}`.
- Every texture is used by a model (`AssetCompletenessTest:451-474`).

---

## 4. Recipe (vanilla items, no glass)

```
"WAW",
"WLW",
"WPW"
```

| Key | Item | Meaning |
|---|---|---|
| W | `minecraft:purple_wool` ×6 | the fabric |
| A | `minecraft:amethyst_shard` | the purple lens |
| L | `minecraft:torch` | the lamp |
| P | `minecraft:flower_pot` | the pot |

- Result: `cultivo:grow_tent` ×1. Everything is obtainable in the overworld.
- Collisions:
  - A shaped recipe must match its exact pattern. No vanilla recipe uses amethyst + torch + pot + wool together.
  - The vanilla recipes that use purple wool have different patterns: `purple_carpet` is `"##"`; `purple_bed`, `purple_banner`, `purple_wool_slab` and `purple_wool_stairs` use wool or planks only.
  - The flower pot recipe is brick `"# #"," # "`.
  - No `cultivo` recipe uses these keys (`C/core/ArticleRecipes.java:48-66`).
  - Re-check with the GameTest `article_recipes` (it uses the real `RecipeManager`).
- Where it lives:
  - `ArticleRecipes`: a new list `CULTIVATION = List.of(shaped("grow_tent", 1, List.of("WAW","WLW","WPW"), Map.of('W', Key.item("minecraft:purple_wool"), 'A', Key.item("minecraft:amethyst_shard"), 'L', Key.item("minecraft:torch"), 'P', Key.item("minecraft:flower_pot"))))`, added to `ALL` (`:77-83`);
  - the same entry in `manifest.crafting` (`tools/gen/manifest.json:131-145`);
  - the generator writes `data/cultivo/recipe/grow_tent.json` and `advancement/recipes/misc/grow_tent.json` (`generate.mjs:380-435`).

---

## 5. Removing the glass greenhouse and migrating data

### 5.1 Delete

| What | Where |
|---|---|
| `C/core/GreenhouseRules.java` | whole file |
| `C/block/GreenhouseDetector.java` | whole file |
| `T/core/GreenhouseRulesTest.java` | whole file |
| `ModTags.GREENHOUSE_GLASS` | `C/registry/ModTags.java:13-14` |
| `greenhouse_glass` array | `tools/gen/manifest.json:183-197` |
| the line writing `tags/block/greenhouse_glass.json` | `tools/gen/generate.mjs:463` (the regenerated output then loses `src/generated/resources/data/cultivo/tags/block/greenhouse_glass.json`) |
| `GrowthRules.GREENHOUSE_MULTIPLIER` and the boolean overload | `GrowthRules.java:5,10,14-17`. Replace with `effectiveSpeed(float vanillaSpeed, Variety v)`. |

### 5.2 Edit

- `CannabisCropBlock.java:19` (javadoc) and `:48-49`: outdoor speed = `effectiveSpeed(vanillaGrowthSpeed(...), variety)`. The light check (`:41`) and `vanillaGrowthSpeed` stay.
- `SetHarvestQualityFunction.java:6,21-22,41-43`: as in §1.4.
- `QualityRules.java:4-5,11,15-17,22-23`: rename to `TENT_BASE` / `tent`; the values stay.
- `CuringRules.java:6-7` javadoc and `mods.toml:11`: change the wording to "estufinha".
- GameTest `greenhouse_quality` becomes `grow_tent_harvest`. Change all three places together: `manifest.json:200`, `ContentIds.java:112`, `CultivoGameTests.java:108`. The regenerated output deletes `test_instance/greenhouse_quality.json` and writes the new one (`DataConsistencyTest:380-393`, `GameTestRegistrationTest:25`).
- In `CultivoGameTests`:
  - remove the import at `:5`, `buildGreenhouse` at `:209-216` (or keep it only as a negative fixture), and `:261`;
  - replace `:277-305`;
  - leave `serene_seasons_compat` (`:2933-3013`) as it is; its glass is Serene Seasons' own column rule.

### 5.3 Migrating existing worlds

- Block data is untouched: crops are still `cultivo:<v>_crop[age]`, and no registry entry is removed. Only the tag and the rule go away, so there are no missing mappings.
- Crops already under glass become outdoor: growth = vanilla × variety, harvest quality 2–3.
- Flowers already harvested keep their `cultivo:quality` component, including 4–5 and 5 after curing.
- A third-party datapack that references `#cultivo:greenhouse_glass` will fail to resolve that tag (a log error). Note this in the changelog in DECISIONS.
- There is no conversion from glass boxes to tents. The player crafts tents.

---

## 6. Tests

### 6.1 Pure JUnit: new `T/core/GrowTentRulesTest.java` (literal values)

- `rollBound`: BRISA (MEDIUM) = 6. A FAST variety = 5. MORANGO_AZEDO (SLOW) = 7.
- `speed`: MEDIUM = `5.0F`, FAST = `6.25F`, within 1e-6.
- Negative: no variety's tent bound is larger than its outdoor bound on lone hydrated farmland (`GrowthRules.growthRollBound(GrowthRules.effectiveSpeed(4.0F, v))`).
- `canGrow(true, 0..6)` is true; `canGrow(true, 7)` and `canGrow(false, 0)` are false.
- `use`:
  - `(false, 0, true)` → PLANT;
  - `(false, 0, false)` → NONE;
  - `(true, 3, true)` → NONE (no replanting over a plant);
  - `(true, 7, false)` → HARVEST;
  - `(true, 7, true)` → HARVEST;
  - `(true, 6, false)` → NONE.
- `afterBoneMeal(6, 5) == 7`; `afterBoneMeal(0, 2) == 2`.
- `LAMP_LIGHT == 12`.

### 6.2 Changes to existing JUnit tests

| Test | Change |
|---|---|
| `VarietyAndGrowthTest:45-55,66` | Replace the greenhouse cases with `effectiveSpeed(4, v)` literal cases. Assert that the tent is faster than a lone hydrated plot. |
| `QualityRulesTest:13-16,36-37,47-48,51-62` | Follow the rename. Keep 4–5 vs 2–3 and `routesNeverOverlap`. |
| `DataConsistencyTest:71-82` | Expected set adds `grow_tent`; count becomes `11 + 3 + 40 + 1 + 1`. |
| `DataConsistencyTest:370-378` | Replace with `greenhouseGlassTagIsGone`: no file at `data/cultivo/tags/block/greenhouse_glass.json` and no manifest key. |
| `DataConsistencyTest`, new `growTentLootDropsOnlyFromLowerHalf` | One pool, `survives_explosion`, item `cultivo:grow_tent` with `condition` `minecraft:match_block` and `state {half: lower}`. Must use 26.3 field names (`:331-355`). |
| `AssetCompletenessTest`, new `growTentBlockstateCoversEveryState` | 8 variants, models `grow_tent_lower`/`_upper`, `y` matches `FACING_Y`. At least one element in `grow_tent_upper` has `light_emission` 15. |
| `SoundsTest:46` and GameTest `sound_events_registered` (`CultivoGameTests.java:2070`) | 16 → 18. |
| `StrainCatalogTest:166-168` | The `mainTabItems` literal adds `grow_tent`. |
| `PortugueseEverywhereTest:158-180` | Add "Estufinha" to `reviewedPortugueseNames`. If the tent gets its own manifest section, add it to `:77` and `:105`. |
| `TooltipHintsTest` | `tooltip.cultivo.grow_tent` is in `LINES` (4 lines). Add `hintsNameTheButtonAndTheGesture` assertions: contains "botão direito", "semente", "mão vazia", "colhe". Each line ≤ 220 px. |
| `ClientIsolationTest:46` | Unchanged (59 − 2 + 3 common classes = 60 > 50). |

### 6.3 GameTests (`C/gametest/CultivoGameTests.java`)

- Names go in `ContentIds.GAME_TESTS`, in `manifest.game_tests` (no `sky_access`, so there is a ceiling), and in `TESTS.register`, all in the same order.
- Each test places the tent with a real `placeWithItem` (`:2004-2016`) on stone at PLANT (4,2,4) of `empty9x9x9`, so the upper half sits at y+1. There is no farmland and no water anywhere in the structure.

| Test | Steps | Assertions |
|---|---|---|
| `grow_tent_places_two_halves` | Place by item. | Lower is `half=lower`, upper is `half=upper`, block entity only on lower. Placement under a 1-block ceiling fails and the item is not consumed. |
| `grow_tent_plants_and_grows` | Survival mock player; seed in main hand; `rightClick` the **upper** half (`:1069-1081`). Loop `level.getBlockState(lower).randomTick(level, lower, level.getRandom())` up to 2000 times. | One seed consumed; `hasPlant` with the right variety; age 0. Reaches 7 (the chance of failing is negligible at bound 7). Sky light is 0 at the position, which proves no light is needed. Below is stone, which proves no farmland or water is needed. |
| `grow_tent_bone_meal` | Plant; use `minecraft:bone_meal` on the upper half until mature. | Age goes up by 2–5 per use; ripe after at most 4 uses; bone meal on a ripe tent harvests instead. |
| `grow_tent_harvest` (replaces `greenhouse_quality`) | 200 times: `setAge(7)` on a variety with chance 0.5, right-click with an empty hand. | Each harvest gives exactly 1 `<v>_fresh_flower` with quality in {4,5} and ≥1 `<v>_seeds`; tent empty afterwards; both 4 and 5 seen; replanting with a seed works. |
| `glass_gives_no_bonus` | Crop at age 7 on farmland inside a hollow glass box (the old `buildGreenhouse` fixture); 200 × `harvest(...)` (`:218-222`). | Every quality in {2,3}. |
| `grow_tent_breaks_either_half` | Four tents: ripe/break lower, ripe/break upper, immature/break upper, empty/break lower. Use `level.destroyBlock(pos, true)`, then count item entities. | Exactly 1 `grow_tent` each. Ripe tents: 1 flower at 4–5 plus seeds. Immature: exactly 1 seed. Empty: no plant items. The other half is air. |
| `grow_tent_creative_break` | Creative player `gameMode.destroyBlock(upper)`. | 0 `grow_tent` items; plant drops present (documented behaviour). |
| `grow_tent_persists` | Plant + `setAge(5)`. Run `saveWithFullMetadata` then `loadWithComponents` into a fresh block entity. Also build the client view from `getUpdateTag`, as in `jarView` (`:2564-2580`). | Same variety and age in both. An unknown variety id loads as empty. |

- Extend `client_prediction_matches_server` (`:2623`) with tent cases: empty+seed, growing+empty hand, ripe+empty hand, growing+bone meal. The prediction from the `getUpdateTag` view must equal `serverBlockResult`.
- Extend `interactions_swing_once` (`:2332`) with planting and harvesting.
- Server-side only:
  - growth happens only in `randomTick(ServerLevel…)`;
  - quality is set only inside the loot function;
  - add an assertion that the client view's age doesn't change when the client-side `useWithoutItem` is called.
- Automatic, no edit needed: `block_states_match_ids` (`:2821-2836`) once `ContentIds.stateProperties("grow_tent")` returns `half: [upper, lower]` and `facing: HORIZONTAL_FACINGS`; `creative_tabs_split` (`:1474-1497`).

### 6.4 Visual probe (`tools/visual-probe/`)

Replace scene E at `gen-datapack.mjs:138-144`, keeping the same footprint (x 39–45, z 10–16, y 150–154):

- `fill 39 150 10 45 154 16 minecraft:blackstone hollow`, then open the south wall (z=16) toward the camera, so the room is dark and the purple light shows.
- Three tents at z 13 on the stone floor (y 151 lower, y 152 upper), all `facing=south`:
  - x 40: empty;
  - x 42: `{variety:"brisa",age:3}`;
  - x 44: `{variety:"<roxo family rep>",age:7}`.
  - Use `setblock` with block-entity NBT, as the jar line at `:186` already does. Place lower first, then upper.
- Rename `cam_greenhouse` to `cam_grow_tent` in the timeline (`:53`), the function (`:190`, same camera position) and `run-probe.ps1:304` (shot `11_estufinha`). Update the description at `run-probe.ps1:7` and `PRINTS.md:16`.
- Add `grow_tent` to the icon row (`:167-168`), to `inventory.18` (`:199-233`), and to the tooltip hover list `$tips` (`run-probe.ps1:312-316`).
- `validate.mjs:13-29` reads `half` and `facing` from the variants JSON. Confirm it parses variant keys with two properties.

---

## 7. Docs to update

Another workflow is editing these docs right now, so the line numbers below may shift. Update them only together with the implementation, and TODO only after real evidence.

| File | What changes |
|---|---|
| `CLAUDE.md` | Add a dated line like `:14-15`: "Em 25/10/2026 o dono pediu a estufinha…", pointing to DECISIONS. `:12` stays. |
| `docs/DECISIONS.md` | Quote the request (`:5-17`). Replace the "Estufa (`GreenhouseRules`)" section (`:78-82`) with an "Estufinha" section covering: the numbers in §1, the recipe, the lamp light 12, no soil, water or light needed, right-click harvest, glass gives nothing, creative drop behaviour, mob-spawn note. Rewrite the formula at `:66`. Rewrite the Serene Seasons "Estufa" part (`:390-394`) per §1.7. |
| `docs/PRODUCT_SPEC.md` | `:19` "estufinha craftada"; wording at `:27,59,71-72`; an extension bullet near `:55-64`. |
| `docs/GUIA_INICIANTE.md` | Rewrite §5.5 (`:749-788`): rule, diagram, numbers, harvest. Also `:8`, `:461`, `:1044`, `:1095`, `:1328`, `:1630`, `:1702`. Tests at `:1449-1467`. Add the recipe row near `:848-886`. |
| `docs/NEXT_SESSION.md` | `:9`, `:133`, `:143`, `:145`; code map adds the 4 new classes; test count at `:160` only after evidence. |
| `docs/TODO.md` | `:97`, `:118`, `:121`, only with date, command and real output. |
| `README.md:3`, `CLOUD_PROMPT.md:14`, `TECHNICAL_RESEARCH.md:140` | Section reference changes to "Estufinha". |

### 7.1 Manifest additions (`tools/gen/manifest.json`)

- Block name.
- Tooltip, 4 lines each:
  - pt `["Clique com o botão direito:", "com semente, planta sem terra nem água", "de mão vazia, colhe a planta madura", "Luz roxa: qualidade 4 a 5"]`;
  - en `["Right-click:", "with a seed, plants it (no soil or water)", "with an empty hand, harvests when ripe", "Purple light: quality 4 to 5"]`.
- Sounds:
  - `{"id":"grow_tent.plant","event":"minecraft:item.crop.plant","volume":0.8,"pitch":1.1,"en":"Seed planted in tent","pt":"Semente plantada na estufinha"}`;
  - `{"id":"grow_tent.harvest","event":"minecraft:block.sweet_berry_bush.pick_berries","volume":1.0,"pitch":1.1,"en":"Tent harvested","pt":"Estufinha colhida"}`.
  - Both vanilla events exist (`F/net/minecraft/sounds/SoundEvents.java:478,1599`).
  - Append the ids to `ContentIds.SOUNDS` (`:65-81`) and add `ModSounds.GROW_TENT_PLANT` / `GROW_TENT_HARVEST` (`C/registry/ModSounds.java:53-55`).
- Recipe (§4) and the game tests (§6.3).

### 7.2 Code wiring

- `ContentIds`: `GROW_TENT`; a new `CULTIVATION_BLOCKS = List.of(GROW_TENT)` added to `allBlocks()` (`:188-190`) and `allItems()` (`:248-250`); `mainTabItems()` (`:256-262`, after the stations); a `stateProperties` case (`:214-220`). Keep it out of `ARTICLES` and `PLACEABLE_ARTICLES`, which are the MVP smoking articles.
- `TooltipHints.LINES` (`:15-25`): add `tooltip.cultivo.grow_tent` with 4 lines.

---

## 8. Risks to check in the build

1. **`DoubleHighBlockItem` and `setPlacedBy` order.** `DoubleHighBlockItem` clears the space above with flags 27 before placing (`DoubleHighBlockItem.java:15-22`). Confirm that the upper half doesn't trigger the lower half's `updateShape` before the block entity exists.
2. **Item translation key.** Check whether `AssetCompletenessTest.everyIdIsTranslatedInBothLanguages` (`:559-572`) expects `item.cultivo.grow_tent` for non-flat ids or accepts the block key through `useBlockDescriptionPrefix`. Also check whether a block in `allItems()` with an `item/generated` model trips `flatItemsHaveOwnTextures` (`:307`, which iterates only `flatItems()`).
3. **`light_emission` support.** Is it accepted by the generator's model checks and by `modelElementsStayWithinLimits`? Does the down-facing LED face actually render fullbright under the `noOcclusion` cutout layer?
4. **Renderer details.** Is the plant's render layer cutout in a block-entity renderer (`BlockModelRenderState.java:59-62`)? Does the 0.875 scale on a `crop`-parent model look right, with no z-fighting against the pot top at y=3?
5. **Serene Seasons and the Forge event.** Confirm SS 26.1.2.0.7 does not also listen to `BlockEvent.CropGrowEvent.Pre`, which `ForgeHooks.onCropsGrowPre` fires (`F/net/minecraftforge/common/ForgeHooks.java:643-650`). If it does, the tent would get seasonal growth.
6. **Loot validation.** Confirm the loot validator accepts the crop table rolled with the BLOCK_ENTITY parameter, and that adding `getReferencedContextParams` (if done) doesn't break validation of the 40 crop tables.
7. **`preRemoveSideEffects` timing.** It runs after the new block is set (`LevelChunk.java:279,311`). Confirm `level.getRandom()` and the loot roll are safe there, and that pistons (`PushReaction.DESTROY`) and explosions drop the plant exactly once.
8. **Width and English-word checks.** `TooltipHintsTest` measures each line with its `guiWidth` meter. "com semente, planta sem terra nem água" may be close to 220 px. `PortugueseEverywhereTest.noEnglishWordsLeftInGameTexts` could flag a pt word that also appears in English text.
9. **Visual probe.** Confirm `validate.mjs` accepts NBT on tent `setblock` lines (it already does for the jar), and that the `cam_grow_tent` marker is not a prefix of another marker (`validate.mjs:119`).
10. **Balance numbers for the owner.** `SOIL_SPEED = 4.0F` and light 12 are balance choices. Lock them with the literal tests and record them in DECISIONS for the owner to confirm.
