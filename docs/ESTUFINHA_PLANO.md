# Plano da estufinha (grow tent)

Pedido do dono em 25/10/2026: trocar a estufa de vidro por uma **estufinha** craftável com **luz roxa**, que caiba **um pé** da planta, **sem farmland e sem água**, com **qualidade maior**. Respostas dele às perguntas:

- **Estufa de vidro:** trocar pela estufinha. O vidro deixa de dar bônus: ao ar livre a qualidade fica em 2–3, e a qualidade alta (4–5) só sai da estufinha.
- **Tamanho:** 2 blocos de altura, como uma grow tent de verdade. A planta aparece no tamanho normal dentro dela.
- **Colheita:** colhe e esvazia. Clique direito na planta madura entrega flores e sementes como a lavoura, e a estufinha fica vazia para replantar.
- **Qualidade:** 4–5, como a estufa de hoje.

O dono aprovou este plano em 25/10/2026 ("aprovo o plano"). **Nada dele foi implementado ainda.**

**Situação deste documento:** é o projeto gerado na nuvem a partir de 5 leituras do código e das fontes do Minecraft/Forge 26.3. Ele passou por crítica adversarial em 3 frentes (APIs do 26.3, exploits/conservação de itens, encaixe no código) e foi revisado; o apêndice do fim lista as críticas aceitas e as rejeitadas. Antes de codar, mesmo assim:

1. Confira cada API citada em `/tmp/forge-src` (extraia o jar de fontes do Forge 66.0.13 como em NEXT_SESSION.md).
2. Revise os riscos da seção final.

As leituras que embasaram o plano estão em [estufinha-mapas.md](estufinha-mapas.md), com citações `arquivo:linha` do commit `09846e1`. O texto abaixo está em inglês, como saiu do workflow.

---

# SPEC: Estufinha (grow tent) replaces the glass greenhouse (revised)

Status: design only. No files changed, and Gradle and Minecraft were not run. Paths are under `/home/user/minezada/`. Abbreviations:
- `C/` = `src/main/java/io/github/peixeangola/cultivo/`
- `T/` = `src/test/java/io/github/peixeangola/cultivo/`
- `F/` = `/tmp/forge-src/`
- `CE:` = a file inside the vanilla `client-extra-26.3-20260918.230105.jar`

Another workflow is editing `docs/*.md` right now. Where a doc and the code disagree, this spec follows the code, and doc line numbers are approximate.

What the owner asked for (25/10/2026):
- A crafted block, not made from glass, 2 blocks tall and placed like a door.
- One plant fits inside at normal size.
- Purple light. No farmland and no water needed.
- Harvest quality 4–5. Outdoor stays 2–3.
- Right-clicking a ripe plant gives flowers and seeds and leaves the tent empty to replant.
- Glass gives no bonus any more.
- Everything is decided on the server. Player text is in Portuguese through the manifest. Sounds go only through `ModSounds`. Client code goes only in `client/`.

---

## 1. Gameplay rules (exact numbers)

### 1.1 Growth

The new pure class is `C/core/GrowTentRules.java`. It does not use Minecraft classes and depends only on `Variety`.

| Constant | Value | Reason |
|---|---|---|
| `SOIL_SPEED` | `4.0F` | The base of a lone hydrated farmland plot, 1 + 3 (`C/block/CannabisCropBlock.java:57-93`). |
| `MULTIPLIER` | `1.25F` | Replaces `GrowthRules.GREENHOUSE_MULTIPLIER` (`C/core/GrowthRules.java:10`), which is deleted. |
| `LAMP_LIGHT` | `12` | Block light emitted by the upper half. |
| `BONE_MEAL_MIN` / `BONE_MEAL_MAX` | `2` / `5` | Copied from `CropBlock.getBonemealAgeIncrease`, which is protected (`F/net/minecraft/world/level/block/CropBlock.java:90-92`). |
| `MAX_AGE` | `7` | |

**Formulas**
- `speed(Variety v) = SOIL_SPEED * v.growthMultiplier() * MULTIPLIER`. The multiplication order is the same as `GrowthRules.effectiveSpeed` today (`GrowthRules.java:14-17`).
- `rollBound(v) = GrowthRules.growthRollBound(speed(v))`, which computes `(int)(25/v)+1` (`GrowthRules.java:20-25`).

**Literal results** (multipliers from `C/core/Variety.java:85-99,202-205`)

| Class | Example | Speed | Bound |
|---|---|---|---|
| FAST | BRISA, `"growth":"rapida"` (`tools/gen/manifest.json:20`) | 6.25 | 5 |
| MEDIUM | AURORA, `"media"` (`manifest.json:21`) | 5.0 | 6 |
| SLOW | MORANGO_AZEDO | 4.0 | 7 |

- For comparison, a lone hydrated plot outdoors gives bounds 6 / 7 / 8 (`T/core/VarietyAndGrowthTest.java:42,63-65`).
- A dense wheat-style field reaches about 5 × variety, so the tent grows about as fast as a good field, with no soil or water.

**Expected time to mature** at `randomTickSpeed=3`:
- Formula: 7 × bound × about 1365 ticks.
- FAST ≈ 39.8 min, MEDIUM ≈ 47.8 min, SLOW ≈ 55.8 min.

**Light, soil and water**
- No light is required. `GrowthRules.hasEnoughLight` is never called, because the tent has its own lamp.
- No soil and no water. The tent never reads the block below except in `canSurvive` (§2.5).

**Tick**
- Random ticks run on the lower half only, the same pattern as `CannabisCropBlock.java:39-55` and `F/.../PitcherCropBlock.java:135-147`.

```java
@Override protected boolean isRandomlyTicking(BlockState s) { return s.getValue(HALF) == DoubleBlockHalf.LOWER; }
@Override protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
    if (!(level.getBlockEntity(pos) instanceof GrowTentBlockEntity tent) || !GrowTentRules.canGrow(tent.hasPlant(), tent.age())) return;
    boolean grows = r.nextInt(GrowTentRules.rollBound(tent.variety())) == 0;
    if (ForgeHooks.onCropsGrowPre(level, pos, s, grows)) { tent.setAge(tent.age() + 1); ForgeHooks.onCropsGrowPost(level, pos, s); }
}
```

- `isRandomlyTicking` is cached per state (`F/.../state/BlockBehaviour.java:897`). Empty or ripe tents are still ticked and return after one block-entity lookup. That cost is accepted to keep the blockstate small.
- `canGrow` returns false for an inert tent (§2.6) because its `variety()` is null.
- `ForgeHooks.onCropsGrowPre` and `onCropsGrowPost` are at `F/net/minecraftforge/common/ForgeHooks.java:643-650`.

### 1.2 Bone meal

`GrowTentBlock implements BonemealableBlock`. In 26.3 the three methods take a `BonemealSource` (`F/.../BonemealableBlock.java:14-18`).

**Methods**
- `isValidBonemealTarget`:
  - Resolve the lower half with the validated `lowerTent(...)` (§2.5).
  - Valid if the tent has a resolved plant and age < 7.
  - This also runs on the client (`F/net/minecraft/world/item/BoneMealItem.java:73-90`), where it reads the synced block entity.
  - Returns false if there is no valid lower tent, for example an orphan half during a piston move.
- `isBonemealSuccess`: always `true`.
- `performBonemeal`:
  - `age = GrowTentRules.afterBoneMeal(age, Mth.nextInt(random, 2, 5))`, which is `min(7, age+bonus)`.
  - `Mth.nextInt` is at `F/net/minecraft/util/Mth.java:146`.
  - Does nothing if there is no valid lower tent.

**Behaviour**
- Bone meal works on either half.
- Player use goes through `BoneMealItem.useOn` (`BoneMealItem.java:35-56`).
- Dispensers go through `growCrop` → `applyBonemeal(..., null)` (`BoneMealItem.java:64-69`, called from `F/net/minecraft/core/dispenser/DispenseItemBehavior.java:191`).
- Particles: levelEvent 1505 plays at the clicked position (`BoneMealItem.java:43`), so clicking the upper half shows them there. The mod plays no sound of its own.

### 1.3 Quality

- Quality comes only from `QualityRules.harvestQuality` (`C/core/QualityRules.java:22-27`).
- Rename `GREENHOUSE_BASE` to `TENT_BASE` (still `4`) and the parameter `greenhouse` to `tent`.
- Result: tent 4–5; everything else 2–3, including crops under glass.
- Curing still adds +1 once (`C/core/CuringRules.java:30-32`).

### 1.4 Drops: the crop's own loot table

**`GrowTentBlockEntity.rollPlantDrops(ServerLevel lvl, @Nullable Entity who, ItemInstance tool, @Nullable Float explosionRadius)`**
- With no resolved plant, it returns `List.of()`.
- Otherwise it builds `cropState = ModBlocks.CROPS.get(variety).get().getStateForAge(age)` and rolls the crop's loot table through `LootParams.Builder`, with these parameters:
  - `ORIGIN` = `Vec3.atCenterOf(worldPosition)`
  - `TOOL` = `tool`
  - `BLOCK_STATE` = `cropState`
  - `BLOCK_ENTITY` = `this`
  - `THIS_ENTITY` = `who`, optional
  - `EXPLOSION_RADIUS` = `explosionRadius`, optional
- When `explosionRadius == null`, this is equivalent to `Block.getDrops(cropState, lvl, pos, this, who, tool)` (`F/.../Block.java:372-385`).
- `ItemStack` implements `ItemInstance` (`F/net/minecraft/world/item/ItemStack.java:103`).
- The param set `LootContextParamSets.BLOCK` already allows an optional `BLOCK_ENTITY` and `EXPLOSION_RADIUS` (`F/.../loot/parameters/LootContextParamSets.java:109-117`).

**What drops** (`cultivo:blocks/<v>_crop`, `tools/gen/generate.mjs:266-305`)
- Ripe: 1 fresh flower plus 1 + Bin(3+Fortune, 4/7) seeds.
- Immature: 1 seed.
- Global loot modifiers still apply.

**Change to `C/loot/SetHarvestQualityFunction.java:38-46`**
- Remove the `GreenhouseDetector` import and call.
- Decide the route like this:

  ```java
  boolean tent = context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GrowTentBlockEntity;
  ```

- Outdoor crops have no block entity, so they get 2–3.
- Read the block entity rather than the block at ORIGIN, because by the time side effects run the chunk already holds the new block (`F/.../chunk/LevelChunk.java:279,304-315`).
- The ORIGIN null check is no longer needed. Keep the `BLOCK_STATE instanceof CannabisCropBlock` check.

### 1.5 Harvest (right-click) and emptying

The server always re-evaluates `GrowTentRules.use(...)` on its own block entity. It never trusts the client's prediction. When two players click at once, the clicks are handled one after another, so there is no duplication.

On a ripe tent (either half), the server does this:
1. `drops = rollPlantDrops(server, player, player.getMainHandItem(), null)`. Fortune in the main hand applies.
2. `tent.clear()`: the variety becomes null and age 0, followed by a sync.
3. If `level.getGameRules().get(GameRules.BLOCK_DROPS)` is on, give each stack with `Handouts.give(player, stack)` (`C/block/Handouts.java:20-22`); anything that does not fit drops at the player's feet. If the rule is off, give nothing. This matches the vanilla berry bush, whose `popResource` respects the rule (`F/.../Block.java:447,455`).
4. Play `ModSounds.GROW_TENT_HARVEST.get()` with `level.playSound(null, …)`.
5. Send `gameEvent(GameEvent.BLOCK_CHANGE, lowerPos, GameEvent.Context.of(player, state))`, as in `F/.../SweetBerryBushBlock.java:118-133`.

Other cases:
- Right-click on an immature plant returns PASS. Break the tent to get the seed back.
- Fortune applies only to the right-click harvest. Breaking the tent rolls with `ItemStack.EMPTY` (§1.6). Record this in DECISIONS.

### 1.6 Breaking and other removal

**Tent item**
- It comes only from the tent's own loot table: one pool with `survives_explosion`, item `cultivo:grow_tent`, and the condition `minecraft:match_block` with `state {half: "lower"}`. This is the vanilla door pattern (`CE:data/minecraft/loot_table/blocks/oak_door.json`).
- So a survival break of either half drops exactly one tent.

**Plant**
- It drops from `GrowTentBlockEntity.preRemoveSideEffects(BlockPos pos, BlockState state)`, which is at `F/.../block/entity/BlockEntity.java:243`:

  ```java
  if (level instanceof ServerLevel sl) rollPlantDrops(sl, null, ItemStack.EMPTY, null).forEach(s -> Block.popResource(sl, pos, s));
  variety = null; rawVariety = null; age = 0;   // direct assignment, no changed()/sync: the block is already gone
  ```

- This mirrors `C/block/JarBlockEntity.java:206-213`, which also clears fields directly instead of calling `contentsChanged`.
- Results: ripe gives a flower at 4–5 plus seeds; immature gives 1 seed; empty or inert gives nothing.

**Removal paths, all verified to drop at most one tent and one plant**

| Path | What happens |
|---|---|
| Survival break of the upper half | The lower half's `updateShape` returns AIR, then `Block.updateOrDestroy` → `destroyBlock(pos, true)` (`F/.../Block.java:222-225`). |
| Survival break of the lower half | The mirror case. The upper half's loot is empty. |
| Side effects in general | `preRemoveSideEffects` runs only on a server-side block-type change without flag 256, and the block entity is removed right after (`LevelChunk.java:304-315`). |
| Piston (`PushReaction.POPPED`) | `dropResources(state, level, pos, be)` then `setBlock(…, 18)` (`F/.../piston/PistonBaseBlock.java:291-297`). Tent once, plant once. The other half can float briefly; see the orphan rule in §2.5. |
| `/setblock … replace` | Uses flags `2|256` (`F/net/minecraft/server/commands/SetBlockCommand.java:125`), so the plant is deleted, not dropped. `/setblock … destroy` calls `destroyBlock(pos, true)` (`:118`) and drops it. Document both. |
| `/clone` | Clears the source with `|816` (`F/.../server/commands/CloneCommands.java:247,264`), so there is no duplication. |

**Creative break**
- The plant does not drop, the same as a creative crop break (`F/.../server/level/ServerPlayerGameMode.java:315-316`).
- In `playerWillDestroy`, on the server, when `player.preventsBlockDrops()` (`F/.../player/Player.java:1363`):
  1. Clear the lower block entity's fields directly.
  2. If the broken half is the upper one, run the copied bottom-part removal (§2.5).
  3. Call super.
- Result: 0 tent items and 0 plant items.

**Explosions**: override `GrowTentBlock.onExplosionHit(state, level, pos, explosion, onHit)`.
1. Resolve the lower position. If it is not a valid lower tent, call super.
2. If `explosion.getBlockInteraction() != TRIGGER_BLOCK`:
   - roll the plant drops with `explosionRadius = explosion.radius()` when the interaction is `DESTROY_WITH_DECAY`, so `explosion_decay` in the crop table applies (`generate.mjs:271`);
   - pass each stack to `onHit.accept(stack, lowerPos)`;
   - clear the block entity's fields directly.
3. Call `super.onExplosionHit(lowerState, level, lowerPos, explosion, onHit)`. This rolls the tent loot with the explosion parameters, including `survives_explosion`, and removes the lower half (`F/.../state/BlockBehaviour.java:166-188`, `F/net/minecraftforge/common/extensions/IForgeBlock.java:663-666`). The upper half then turns to air with empty loot.

Because of this, it does not matter which half the explosion hits first. The second position is re-read and is already air (`F/.../level/ServerExplosion.java:221`, `BlockBehaviour.java:169`).

### 1.7 Compatibility

**Serene Seasons**
- The tent block is in no SS tag. SS gates growth with `ModFertility.isCrop` in its randomTick mixin and its `UseBlock` bone meal handler.
- No class in `run-compat-ss/mods/SereneSeasons-forge-26.3-26.1.2.0.7.jar` or `GlitchCore-forge-26.3-26.3.0.0.5.jar` references `CropGrowEvent` (byte search of every class done for this revision).
- So tent plants grow all year. `CompatibilityTest:55-73` stays valid (the SS tags hold exactly the 40 crops). Record this in DECISIONS.

**Villagers**
- `HarvestFarmland` breaks only `CropBlock`s, so villagers never touch tents. `C/village/FarmerReplant.java` is unchanged.

**Bees**
- The tent stays out of `bee_growables`, `crops` and `maintains_farmland` (`DataConsistencyTest:359-368`).

---

## 2. Block design

### 2.1 IDs and classes

**Ids and names**
- Id `cultivo:grow_tent`, constant `ContentIds.GROW_TENT = "grow_tent"`.
- Names: pt "Estufinha", en "Grow Tent". `en_us` gets the pt text through `portuguese_everywhere`.

**New classes**

| Class | Role |
|---|---|
| `C/block/GrowTentBlock.java` | `extends Block implements EntityBlock, BonemealableBlock`, the same base as `C/block/JarBlock.java:44`. It must live in `block/` because `Handouts` is package-private. It must not extend `DoublePlantBlock`, which has its own vegetation drop path (`F/.../DoublePlantBlock.java:93-103`). |
| `C/block/GrowTentBlockEntity.java` | Holds the plant. |
| `C/core/GrowTentRules.java` | Pure rules. |
| `C/item/GrowTentItem.java` | `extends DoubleHighBlockItem` (`F/net/minecraft/world/item/DoubleHighBlockItem.java:10-22`). Its `appendHoverText` (5-argument form, `F/.../Item.java:350`) calls `TooltipItem.addHint(..., "tooltip.cultivo.grow_tent")` (`C/item/TooltipItem.java:29-40`). |
| `C/client/GrowTentRenderer.java` | Draws the plant. |

**Item properties**
- `new Item.Properties().setId(ITEMS.key(GROW_TENT)).useBlockDescriptionPrefix()` (`Item.java:716`). Stack size 64.

### 2.2 Blockstate (8 states)

| Property | Values | Notes |
|---|---|---|
| `half` | `BlockStateProperties.DOUBLE_BLOCK_HALF` | Default `LOWER`. |
| `facing` | `HorizontalDirectionalBlock.FACING` | Default `NORTH`. Placement uses `getHorizontalDirection().getOpposite()`, as in `C/block/ProcessingStationBlock.java:106-108`, so the open front of the model (north before rotation) faces the player. This differs from the door (`F/.../DoorBlock.java:135`) on purpose. `rotate` and `mirror` as at `ProcessingStationBlock.java:116-123`. |

- No age, planted or strain property; that data lives in the block entity.
- The properties must be static, because `createBlockStateDefinition` runs inside the `Block` constructor (`F/.../Block.java:233-236`).

### 2.3 Block properties (`C/registry/ModBlocks.java`, next to JAR at `:64-67`)

```java
BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.8F).sound(SoundType.WOOL)
    .noOcclusion().pushReaction(PushReaction.POPPED)
    .lightLevel(s -> s.getValue(GrowTentBlock.HALF) == DoubleBlockHalf.UPPER ? GrowTentRules.LAMP_LIGHT : 0)
    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
    .setId(BLOCKS.key(ContentIds.GROW_TENT))
```

- `PushReaction` in 26.3 has no `DESTROY` (`F/net/minecraft/world/level/material/PushReaction.java:3-9`). `POPPED` is the mod's existing choice (`ModBlocks.java:60,66,73`).
- The lamp is always on. The lower half gets about 11 block light, because the tent is not a full block and dampens no light (`BlockBehaviour.java:403-405`).
- Side effect: hostile mobs won't spawn next to a tent. Record this in DECISIONS.
- No `requiresCorrectToolForDrops` and no mineable tag (`DataConsistencyTest:602-618` is unchanged). Not flammable.
- `isPathfindable` returns false (`BlockBehaviour.java:129`).

### 2.4 Shapes

- Lower half `Block.box(1,0,1,15,16,15)`, upper half `Block.box(1,0,1,15,14,15)`.
- Both are symmetric, so no rotation is needed. Collision shape = outline shape.

### 2.5 Two-tall mechanics (door pattern, `F/.../DoorBlock.java`)

| Hook | Behaviour |
|---|---|
| `getStateForPlacement` | Return the state only if `pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(ctx)` (`DoorBlock.java:129-143`). Otherwise return null, and `BlockItem` returns FAIL (`F/.../BlockItem.java:71-74`). |
| `setPlacedBy` | `level.setBlockAndUpdate(pos.above(), state.setValue(HALF, UPPER))` (`DoorBlock.java:146-148`). Placement order is safe: `DoubleHighBlockItem` clears the space above first (`DoubleHighBlockItem.java:16-21`), then the lower half and its block entity are placed, and only then the upper half. |
| `updateShape` | If the neighbour in `half.getDirectionToOther()` is not this block with the other half, return AIR (`F/.../DoublePlantBlock.java:35-56`). Lower half: if the block below changed and `!canSurvive`, return AIR. |
| `canSurvive` | Lower half: `below.isFaceSturdy(level, pos.below(), Direction.UP)`. Upper half: the block below is this block with `half=lower` (`DoorBlock.java:230-235`). |
| `playerWillDestroy` | On the server, if `player.preventsBlockDrops()`: clear the lower block entity's fields (§1.6), then, if this is the upper half, run the private helper `removeBottomWithoutDrops(level, pos, state, player)`. Then call super. |
| `removeBottomWithoutDrops` | A copy of the body of `DoublePlantBlock.preventDropFromBottomPart` (`DoublePlantBlock.java:117-128`). That method is `protected static` in another package, so it cannot be called from here. If the block below is this block with `half=lower`, `level.setBlock(below, AIR, 35)` and `level.levelEvent(player, 2001, below, Block.getId(belowState))`. |
| `newBlockEntity` | `half == LOWER ? new GrowTentBlockEntity(pos, state) : null` (`LevelChunk.java:337-341`). No ticker. |
| `lowerTent(level, state, pos)` | `lowerPos = half == LOWER ? pos : pos.below()`, the pattern in `PitcherCropBlock.getLowerHalf` (`:188-196`). Returns the block entity only if `level.getBlockState(lowerPos)` is this block with `HALF == LOWER` and the block entity is a `GrowTentBlockEntity`. Otherwise it returns empty. |

**Orphan rule.** Every hook returns early or PASS when `lowerTent` is empty: `useItemOn`, `useWithoutItem`, `isValidBonemealTarget`, `performBonemeal`, `animateTick` and `onExplosionHit`. This covers the half left floating while a piston moves the other one.

### 2.6 `GrowTentBlockEntity` data

**Fields**
- `@Nullable Variety variety`
- `@Nullable String rawVariety`: the stored id when it does not resolve.
- `int age` (0..7)

**Accessors**
- `hasPlant()` = `variety != null`
- `occupied()` = `variety != null || rawVariety != null`
- `variety()`, `age()`

**Mutators**
- `plant(Variety)`: only if `!occupied()`. Sets age 0.
- `setAge(int)`: does nothing when `!hasPlant()`; otherwise clamps to 0..7. It is also the GameTest hook, like `JarBlockEntity.setItem` (`:61-74`).
- `clear()`
- Every mutation calls `changed()`: `setChanged()`, then, on the server only, `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)` (`JarBlockEntity.java:135-142`).
- `preRemoveSideEffects` and `onExplosionHit` assign the fields directly without `changed()` (§1.6).

**`saveAdditional(ValueOutput o)`**
- If `variety != null`: `o.putString("variety", variety.id())` and `o.putInt("age", age)`.
- Otherwise, if `rawVariety != null`: write `rawVariety` and `age` back unchanged (`F/.../storage/ValueOutput.java:21,29`).

**`loadAdditional(ValueInput i)`** always assigns all three fields:
- `id = i.getString("variety")`
- `variety = id.flatMap(Variety::byId).orElse(null)` (`ValueInput.java:11`, `C/core/Variety.java:128-131`)
- `rawVariety = (variety == null) ? id.orElse(null) : null`
- `age = occupied() ? Mth.clamp(i.getIntOr("age", 0), 0, 7) : 0`
- An unknown id logs one warning and loads the tent as **inert**: occupied, not growing, not harvestable, not replantable, not rendered, and it drops nothing when broken. The id is kept in the save, so a mod downgrade or a renamed strain does not erase it.
- Because all fields are always assigned, a client update with an empty tag also clears the client view (`F/.../ClientPacketListener.java:1494-1498`, `F/net/minecraftforge/common/extensions/IForgeBlockEntity.java:40-42`), so harvesting never leaves a ghost plant.

**Sync**
- `getUpdatePacket` returns `ClientboundBlockEntityDataPacket.create(this)`.
- `getUpdateTag` writes the same fields through `TagValueOutput.createWithContext` (`JarBlockEntity.java:191-204`).

**Render box**
- `getRenderBoundingBox()` = `new AABB(x, y, z, x+1, y+2, z+1)`.
- The default box is the collision box (`IForgeBlockEntity.java:78-95`), and it is used for culling (`F/net/minecraft/client/renderer/extract/LevelExtractor.java:326,356`). The ripe plant pokes into the upper half (§3.2).

**Registration**
- `ModBlockEntities.GROW_TENT`, following the pattern at `C/registry/ModBlockEntities.java:28-29`, with `Set.of(ModBlocks.GROW_TENT.get())`.

**Pick-block and give**
- Creative pick-block with Ctrl and `/give … [block_entity_data=…]` can produce an item that places an already planted tent. This is accepted for creative and ops only, is bounded by the load clamp, and is recorded in DECISIONS. There is no survival path to such an item.

### 2.7 Interactions (either half resolves through `lowerTent`)

Client and server both call the pure function `GrowTentRules.use(boolean occupied, boolean hasPlant, int age, boolean holdingSeed, boolean mayPlace)`, which returns `PLANT`, `HARVEST` or `NONE`. The client runs it on the synced block entity and the server then acts. This is the `JarBlock.predictInsert/predictTake` approach (`C/block/JarBlock.java:86-118`).

`mayPlace = player.mayBuild() || stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, lowerPos, false))` (`Player.java:1663`, `ItemStack.java:1086`). This copies the gate the item path uses (`ItemStack.java:373`) but which block `useItemOn` skips (`ServerPlayerGameMode.java:444-446`). Both sides compute it, so the prediction matches.

| Hand / state | `useItemOn` | `useWithoutItem` (main hand) | Server effect | Result |
|---|---|---|---|---|
| `SeedItem`, tent not occupied, `mayPlace` | PLANT | – | Plant `seed.variety()` at age 0. Shrink 1 unless `player.hasInfiniteMaterials()` (`Player.java:1359`). Play `ModSounds.GROW_TENT_PLANT.get()`. Send gameEvent BLOCK_PLACE. | `SUCCESS` |
| Seed, adventure mode without `can_place_on` | `TRY_WITH_EMPTY_HAND` | as for an empty hand | – | `PASS` (or harvest if ripe) |
| Seed, tent occupied | `TRY_WITH_EMPTY_HAND` | Ripe: HARVEST. Otherwise PASS. | §1.5 | `SUCCESS` / `PASS` |
| Empty hand, ripe | – | HARVEST | §1.5 | `SUCCESS` |
| Empty hand, empty, growing or inert | – | NONE | – | `PASS` |
| Bone meal, growing | `TRY_WITH_EMPTY_HAND` | NONE → PASS | `BoneMealItem.useOn` runs next (`ServerPlayerGameMode.java:440-475`) | vanilla |
| Bone meal, ripe | `TRY_WITH_EMPTY_HAND` | HARVEST | §1.5 | `SUCCESS` |
| Any other item | `TRY_WITH_EMPTY_HAND` | as for an empty hand | ripe: harvest, with main-hand Fortune | `SUCCESS` / `PASS` |
| Sneaking with an item | the block is not called (`ServerPlayerGameMode.java:440-442`); the seed's placement fails without farmland | – | – | `PASS` |
| No valid lower tent | `PASS` | `PASS` | – | – |

- Always return `SUCCESS`, never `SUCCESS_SERVER` (`F/net/minecraft/world/InteractionResult.java:11-16`, `C/block/ProcessingStationBlock.java:49-52`, GameTest `interactions_swing_once`).
- The seed check is `stack.getItem() instanceof SeedItem seed`. `SeedItem` is common code, so the check works on the client.

---

## 3. Rendering

### 3.1 Tent models (`tools/gen/models.mjs`: new `growTentModels()` and `growTentBlockstate()`)

**Generator change.** Extend `box()` (`models.mjs:54-70`) with a `light` option that writes `out.light_emission`. The value must be an integer 0..15; anything else throws a `JsonParseException` (`F/net/minecraft/client/resources/model/cuboid/CuboidModelElement.java:43,64-73`). The existing `shade` option keeps writing `shade_direction_override` (`:67`). There is no `"shade"` field in 26.3; the deserializer reads only `from`, `to`, `rotation`, `faces`, `shade_direction_override` and `light_emission` (`CuboidModelElement.java:41-83`).

**General rules**
- Parent `minecraft:block/block`.
- Coordinates stay in [0,16], inside the test's allowed range [-16,32] (`T/data/AssetCompletenessTest.java:268-272`).
- The front is north before rotation.

| Model | Elements |
|---|---|
| `cultivo:block/grow_tent_lower` | 4 corner poles at 1..2 and 14..15, y 0..16 (`grow_tent_frame`). Floor tray 1..15 × 0..1 (`grow_tent_canvas`). Back and side walls 0.5 px thick (outside `grow_tent_canvas`, inside `grow_tent_mylar`). Front sill 1..15 × 0..3 at z 1..1.5. Fabric pot 5..11 × 1..4 × 5..11 (`grow_tent_pot`, top face shows substrate; **soil top at y=4**). The front is open. |
| `cultivo:block/grow_tent_upper` | Poles 0..13. Side and back walls 0..13. Roof 1..15 × 13..14. Rolled flap 1..15 × 10..13 × 1..3 (`grow_tent_flap`). Lamp housing 4..12 × 11..12.5 × 4..12 (`grow_tent_lamp`). LED panel: the down face of an element 4.5..11.5 × 10.9..11 × 4.5..11.5, with `light: 15` and `shade: 'up'`, following `CE:assets/minecraft/models/block/cross_emissive.json` and the repo precedent at `models.mjs:100`. A 1×1 px purple LED on the outer front pole, `light: 15`. |

**Blockstate** `grow_tent.json`
- 8 variants `facing=<f>,half=<h>`, each pointing to `cultivo:block/grow_tent_<h>` with `y = FACING_Y[f]` (`models.mjs:80`).
- `particle` texture = `cultivo:block/grow_tent_canvas`.
- Textures use alpha 0/255 only (`TextureArtTest:91-106`).

### 3.2 Plant: `C/client/GrowTentRenderer.java`

It copies `C/client/PressPlatenRenderer.java:43-112`.

**Structure**
- `implements BlockEntityRenderer<GrowTentBlockEntity, State>`.
- `State extends BlockEntityRenderState` and holds a `BlockModelRenderState plant`.
- The constructor takes `context.blockModelResolver()`.

**`extractRenderState`**
- Call super first.
- If `!be.hasPlant()`, call `plant.clear()`. Inert tents are not drawn.
- Otherwise call `models.update(plant, cropState(variety, age), DISPLAY_CONTEXT)` (`F/.../client/renderer/block/BlockModelResolver.java:21-25`).

**`submit`**
1. `pushPose`
2. `translate(0.5, 5/16f, 0.5)`. The crop model spans y −1..15 (`CE:assets/minecraft/models/block/crop.json`), so the base lands on the soil at y=4.
3. `scale(0.875, 1, 0.875)`
4. `translate(-0.5, 0, -0.5)`
5. `plant.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0)`
6. `popPose`

**Other details**
- The ripe plant reaches y 20/16, so the render box override in §2.6 is needed.
- The plant renders on the cutout block sheet (`BlockModelRenderState.java:59-62`).
- `getViewDistance()` as at `PressPlatenRenderer.java:81-83`.
- No new plant art; it reuses the 40×8 crop states.
- Register in `C/client/CultivoClientEvents.java:28-30`. Common code never references `client/` (`ClientIsolationTest:64-103`).

### 3.3 Particles

`animateTick` (`F/.../Block.java:358`) runs on the client only (`ClientLevel.java:607`) and follows the orphan rule.
- Upper half, `nextInt(3)==0`: `new DustParticleOptions(0xB04DFF, 0.6F)` at `x+0.25+r*0.5, y+0.6, z+0.25+r*0.5`. The colour is plain RGB24 (`F/net/minecraft/core/particles/DustParticleOptions.java:26-37`).
- Lower half, ripe plant, `nextInt(5)==0`: one more particle at y+0.9.

### 3.4 Textures (`tools/art/textures.mjs`)

All are 16×16, alpha 0/255 only, original pixel art, unanimated (`AnimatedTexturesTest` is unchanged).

| Texture | Look |
|---|---|
| `block/grow_tent_canvas` | Charcoal fabric #2b2b30 / #34343a, a seam, purple stitching #6a3fa0. |
| `block/grow_tent_mylar` | Silver diamond pattern #c8c8d0 / #a9a9b6, lavender highlights #d9c2ff. |
| `block/grow_tent_frame` | Metal pole #55555e / #6e6e78. |
| `block/grow_tent_flap` | Rolled canvas, gold zipper pull #c9b037. |
| `block/grow_tent_lamp` | Housing #1e1e22, purple LED grid #7a1fd1 / #b04dff / #e0a8ff. |
| `block/grow_tent_pot` | Black fabric sides, substrate #4a3222 / #3a2618. |
| `item/grow_tent` | Flat icon: a tall tent, open front, a green plant, a purple glow line. |

- Item model: `cultivo:item/grow_tent`, parent `minecraft:item/generated`.
- Item definition: `{"model":{"type":"minecraft:model","model":"cultivo:item/grow_tent"}}`, the same as vanilla doors (`CE:assets/minecraft/items/oak_door.json`).

---

## 4. Recipe (vanilla items, no glass)

```
"WAW",
"WLW",
"WPW"
```

| Key | Item |
|---|---|
| W | `minecraft:purple_wool` ×6 |
| A | `minecraft:amethyst_shard` |
| L | `minecraft:torch` |
| P | `minecraft:flower_pot` |

- Result: 1 `cultivo:grow_tent`. All ingredients come from the overworld.
- No vanilla shaped recipe combines these keys in this pattern.
- No cultivo recipe uses this combination (`C/core/ArticleRecipes.java:48-66`).
- The proof of no collision is the `article_recipes` GameTest, which uses the real `RecipeManager` (`C/gametest/CultivoGameTests.java:518-532`).

Wiring:
- `ArticleRecipes`: a new list `CULTIVATION = List.of(shaped("grow_tent", 1, List.of("WAW","WLW","WPW"), Map.of(...)))`, added to `ALL` (`:77-83`).
- The same entry in `manifest.crafting` (`manifest.json:131-145`).
- The generator writes `data/cultivo/recipe/grow_tent.json` and the recipe advancement (`generate.mjs:380-435`).

---

## 5. Removing the glass greenhouse and migrating data

### 5.1 Delete

- `C/core/GreenhouseRules.java`
- `C/block/GreenhouseDetector.java`
- `T/core/GreenhouseRulesTest.java`
- `ModTags.GREENHOUSE_GLASS` (`C/registry/ModTags.java:13-14`), plus the now-unused `Block` import (`:10`).
- `greenhouse_glass` in `manifest.json:183-197`, and its write line at `generate.mjs:463`. The regenerated output loses `data/cultivo/tags/block/greenhouse_glass.json`.
- `GrowthRules.GREENHOUSE_MULTIPLIER` and the 3-argument `effectiveSpeed` (`GrowthRules.java:5,10,14-17`). Replace with `effectiveSpeed(float vanillaSpeed, Variety v)`.

### 5.2 Edit

**Main code**
- `CannabisCropBlock.java:19` (javadoc) and `:48-49`: use the 2-argument `effectiveSpeed`. The light check at `:41` stays.
- `SetHarvestQualityFunction.java:6,21-22,38-46`: as in §1.4.
- `QualityRules.java:3-5` (javadoc), `:11`, `:15-17`, `:22-23`: rename.

**Wording**
- `CuringRules.java:6-7` javadoc: say "estufinha".
- `src/main/resources/META-INF/mods.toml:11`: say "estufinha", and keep the phrase "tudo é mecânica de jogo" (`PortugueseEverywhereTest:188`).

**GameTest rename**
- `greenhouse_quality` becomes `grow_tent_harvest`. Change all three places together: `manifest.json:200`, `ContentIds.java:112`, `CultivoGameTests.java:108`.
- The regenerated output deletes `test_instance/greenhouse_quality.json`.

**`CultivoGameTests`**
- Remove the import at `:5` and `:261`.
- Keep `buildGreenhouse` (`:209-216`) only as the fixture for `glass_gives_no_bonus`.
- Replace `:277-305`.
- Update the "16 eventos" javadoc and the count at `:2064,2070` to 18.
- Leave `serene_seasons_compat` (`:2933-3013`) unchanged.

**Tests whose names or comments mention the greenhouse**
- `T/core/SmokeRulesTest.java:73-74` and `T/core/CuringRulesTest.java:31-38`: rename to the estufinha.
- `T/data/CompatibilityTest.java:84`: update the comment.

### 5.3 Migrating existing worlds

- Crops stay `cultivo:<v>_crop[age]`. No registry entry is removed, so there are no missing mappings.
- Crops already under glass become outdoor crops: quality 2–3.
- Flowers already harvested keep their `cultivo:quality` component.
- A datapack that references `#cultivo:greenhouse_glass` will log a tag error. Note this in the changelog.
- There is no automatic conversion from glass boxes to tents.

---

## 6. Tests

### 6.1 New pure JUnit: `T/core/GrowTentRulesTest.java`

**`rollBound`**
- BRISA (FAST) = 5, AURORA (MEDIUM) = 6, MORANGO_AZEDO (SLOW) = 7.

**`speed`**
- BRISA = `6.25F`, AURORA = `5.0F`, within 1e-6.

**Comparison**
- For every variety, the tent bound ≤ `growthRollBound(effectiveSpeed(4.0F, v))`.

**`canGrow`**
- `(true, 0..6)` is true; `(true, 7)` and `(false, 0)` are false.

**`use(occupied, hasPlant, age, seed, mayPlace)`**

| Arguments | Result | Note |
|---|---|---|
| `(false,false,0,true,true)` | PLANT | |
| `(false,false,0,true,false)` | NONE | adventure mode |
| `(false,false,0,false,true)` | NONE | |
| `(true,true,3,true,true)` | NONE | |
| `(true,true,7,false,true)` | HARVEST | |
| `(true,true,7,true,true)` | HARVEST | |
| `(true,true,6,false,true)` | NONE | |
| `(true,false,7,false,true)` | NONE | inert |

**Others**
- `afterBoneMeal(6,5)==7`; `afterBoneMeal(0,2)==2`.
- `LAMP_LIGHT == 12`.

### 6.2 Changes to existing JUnit tests

| Test | Change |
|---|---|
| `VarietyAndGrowthTest` | Move every 3-argument call (`:42,47,48,49,50,51,63,64,65,66`) to the 2-argument form. Remove the greenhouse cases (`:45-55,66`). Assert that the tent is faster than a lone hydrated plot. |
| `QualityRulesTest:13-16,36-37,47-48,51-62` | Follow the rename. Keep 4–5 vs 2–3 and `routesNeverOverlap`. |
| `SmokeRulesTest:73-74`, `CuringRulesTest:31-38` | Rename only. |
| `DataConsistencyTest:71-82` | Add `grow_tent`; count `11 + 3 + 40 + 1 + 1`. |
| `DataConsistencyTest:370-378` | Replace with `greenhouseGlassTagIsGone`: no file and no manifest key. |
| `DataConsistencyTest`, new `growTentLootDropsOnlyFromLowerHalf` | One pool, `survives_explosion`, `match_block` with `half: lower`. Use the 26.3 field names (`:331-355`). |
| `AssetCompletenessTest`, new `growTentBlockstateCoversEveryState` | 8 variants with the right models and `y`. At least one element in `grow_tent_upper` has `light_emission` 15. No element has a `shade` key. |
| `SoundsTest:46` and GameTest `sound_events_registered` | 16 → 18. Play both sounds as `ModSounds.X.get()` so `soundsUsedInJavaAreRegistered` (`SoundsTest:104`) sees them. |
| `StrainCatalogTest:166-168` | Add `grow_tent` to `mainTabItems`. |
| `PortugueseEverywhereTest` | Add "Estufinha" to `reviewedPortugueseNames` (`:158-180`). Add the new `cultivation_blocks` manifest section to the source lists at `:77` and `:105` (required). |
| `TooltipHintsTest` | `tooltip.cultivo.grow_tent` with 4 lines. `hintsNameTheButtonAndTheGesture` checks for "botão direito", "semente", "mão vazia", "colhe". Measured widths are ≤ 220 px (worst line 203 px). |
| `ClientIsolationTest:46` | Unchanged; the common class count stays well above 50. |

### 6.3 GameTests (`C/gametest/CultivoGameTests.java`)

**Setup**
- Each name goes in `ContentIds.GAME_TESTS`, `manifest.game_tests` and `TESTS.register`, in the same order.
- Placement: `placeWithItem` (`:2004-2016`) on stone at PLANT (4,2,4) of `empty9x9x9`.
- New helper `tryPlaceWithItem`, which returns the result without asserting. `placeWithItem` asserts `consumesAction` (`:2013`).
- No farmland or water anywhere.
- The ceiling in these tests is made of barriers, which let skylight through (`F/.../TestInstanceBlockEntity.java:370-397`, `F/.../BarrierBlock.java:33-35`). So no test asserts sky light 0. The proof that no light is needed is the tick code path plus `GrowTentRulesTest`.

| Test | Steps | Assertions |
|---|---|---|
| `grow_tent_places_two_halves` | Place by item. Then `tryPlaceWithItem` under a 1-block stone ceiling. | Lower half is `half=lower` and upper is `half=upper`. The block entity exists only on the lower half. Under the ceiling, placement fails and the stack count is unchanged. |
| `grow_tent_plants_and_grows` | Surround the tent with stone (open-front column). Survival player with a seed; `rightClick` the upper half. Then up to 2000 × `randomTick`. | 1 seed consumed; the variety matches; age 0; age reaches 7. |
| `grow_tent_adventure_plant` | Adventure-mode player with a seed. | `PASS`; no seed consumed; tent stays empty. |
| `grow_tent_bone_meal` | Bone meal on the upper half. | +2..5 per use; ripe in ≤ 4 uses; on a ripe tent bone meal harvests. |
| `grow_tent_harvest` | 200 × (`setAge(7)` on a variety with chance 0.5, then an empty-hand click). | 1 flower with quality in {4,5} and ≥ 1 seed each time; tent empty after; both 4 and 5 seen; replanting works. Then a full inventory: the overflow lands as item entities and the total count is conserved. Then `blockDrops=false`: tent emptied, 0 items. |
| `glass_gives_no_bonus` | `buildGreenhouse` fixture; 200 × `harvest(...)` (`:218-222`). | Every quality in {2,3}. |
| `grow_tent_breaks` | Survival `gameMode.destroyBlock` on each half; `level.destroyBlock(pos,true)`; a piston pushing into each half; a piston pulling the floor block; `/setblock destroy` and `/setblock replace`. Ripe, immature and empty variants. | Tent item 0 or 1 (1 everywhere except `replace`: 0). Ripe: 1 flower at 4–5 plus seeds. Immature: 1 seed. Empty: none. `replace`: no plant items. The other half is air. |
| `grow_tent_explosion` | `level.explode` centred on each half. | ≤ 1 tent item; plant items ≤ crop maximum; both halves air. |
| `grow_tent_creative_break` | Creative `gameMode.destroyBlock` on the upper half, then on the lower half. | 0 tent items and 0 plant items. |
| `grow_tent_persists` | Plant and `setAge(5)`; `saveWithFullMetadata` → `loadWithComponents`; client view from `getUpdateTag`, as in `jarView` (`:2564-2580`). Unknown id `"nao_existe"` with age 7. `setAge(9)` on an empty tent. | Same variety and age. The unknown id loads as inert, writes the same id and age back, cannot be harvested or replanted, and drops nothing. `setAge` on an empty tent does nothing. |

**Extensions to existing GameTests**
- `client_prediction_matches_server` (`:2623`): add empty+seed, adventure+seed, growing+empty hand, ripe+empty hand, growing+bone meal. The prediction from the `getUpdateTag` view must equal `serverBlockResult`. The client-side `useWithoutItem` must not change the view's age.
- `interactions_swing_once` (`:2332`): add plant and harvest.
- No edit needed: `block_states_match_ids` (`:2821-2836`), once `ContentIds.stateProperties("grow_tent")` returns `half` and `facing`; and `creative_tabs_split`.

### 6.4 Visual probe (`tools/visual-probe/`)

**Scene E** (`gen-datapack.mjs:138-144`, same footprint x 39–45, z 10–16, y 150–154)
- `fill … minecraft:blackstone hollow`, then open the south wall and the south half of the roof.
- Three tents at z 13, `facing=south`, placed lower first, then upper, with block-entity NBT as on the jar line (`:186`):
  - x 40: empty;
  - x 42: `{variety:"brisa",age:3}`;
  - x 44: `{variety:"<a roxo-family strain>",age:7}`.

**Camera**
- Lower `cam_grow_tent` to about `tp @a 42.5 152.5 21.5 180 10` so the line of sight clears the roof (the old value at `:190` was y 155.8, pitch 28).
- Rename it in the timeline (`:53`), the function (`:190`) and `run-probe.ps1:304` (shot `11_estufinha`).
- Update `run-probe.ps1:7` and `PRINTS.md:16`.

**Other probe changes**
- Add `grow_tent` to the icon row (`:167-168`), to `inventory.18` (`:199-233`) and to `$tips` (`run-probe.ps1:312-316`).
- `validate.mjs`:
  - Already accepted: two-property variant keys (`:22`) and the marker name (`:119`).
  - Add: check that a tent's `variety` NBT is a known strain (`:89-98` does not check it today).
- Rerun `gen-datapack.mjs` (it rewrites `datapack/`, `:298`) and `validate.mjs`. Commit the regenerated `datapack/`: the old `cam_greenhouse.mcfunction` is removed, and `build.mcfunction:93` and `tick.mcfunction:11` change.

---

## 7. Generator, manifest and wiring

### 7.1 Manifest (`tools/gen/manifest.json`)

**New section** `"cultivation_blocks": [{"id":"grow_tent","pt":"Estufinha","en":"Grow Tent"}]`
- Not in `articles`: that would put the tent in `flatItems` (`generate.mjs:42`) and break `articlesArePartitionedByUse` (`DataConsistencyTest:534-550`).

**Tooltip** (4 lines)
- pt: `["Clique com o botão direito:", "com semente, planta sem terra nem água", "de mão vazia, colhe a planta madura", "Luz roxa: qualidade 4 a 5"]`
- en: `["Right-click:", "with a seed, plants it without soil or water", "with an empty hand, harvests when ripe", "Purple light: quality 4 to 5"]`
- The English text must not contain "no", because "no" appears in pt texts such as "no inventário" (`manifest.json:328,332,333,349`) and `PortugueseEverywhereTest.noEnglishWordsLeftInGameTexts` (`:73-87,128-145`) would flag those keys. Re-check every new English word against the pt texts.

**Sounds** (both vanilla events exist, `F/net/minecraft/sounds/SoundEvents.java:478,1599`)
- `{"id":"grow_tent.plant","event":"minecraft:item.crop.plant","volume":0.8,"pitch":1.1,"en":"Seed planted in tent","pt":"Semente plantada na estufinha"}`
- `{"id":"grow_tent.harvest","event":"minecraft:block.sweet_berry_bush.pick_berries","volume":1.0,"pitch":1.1,"en":"Tent harvested","pt":"Estufinha colhida"}`

**Also**
- The recipe (§4) and the game tests (§6.3).

### 7.2 `tools/gen/generate.mjs`

Today the generator emits output only for its known lists, so without changes the tent would get no name, item definition, model, blockstate or loot table:
- names: `:236`
- item definitions: `:141`
- flat item models: `:99`
- loot tables: `:267-340`
- block models: `:73-93`

Add:
- the `cultivation_blocks` section in `langTable` (`:218`);
- `flatModel('grow_tent')`;
- `itemDefinitions.grow_tent`;
- the loot table: drops itself, with entry condition `{condition:"minecraft:match_block", block:"cultivo:grow_tent", properties:{half:"lower"}}` in the 26.3 field names the existing tables use (check `DataConsistencyTest:331-355` and `oak_door.json`), plus `survives_explosion`;
- the `growTentBlockstate()` and `growTentModels()` output, imported from `models.mjs` (`:9-10`);
- the tooltip and the sounds;
- removal of the `greenhouse_glass` line (`:463`).

These cover `everyBlockHasLootTable` (`DataConsistencyTest:324-329`), `everyBlockHasBlockstateModelsAndTextures` (`AssetCompletenessTest:54-60`), `everyItemHasItemDefinitionModelAndTexture` (`:298-304`) and `everyIdIsTranslatedInBothLanguages` (`:559-572`). That last test checks `block.*` for `allBlocks` and `item.*` only for `flatItems` (`:563-567`), so `useBlockDescriptionPrefix` is correct.

Run `node tools/gen/generate.mjs` and commit `src/generated/resources`.

### 7.3 Code wiring

**`ContentIds`**
- `GROW_TENT`.
- `CULTIVATION_BLOCKS = List.of(GROW_TENT)`, added to `allBlocks()` (`:188-190`) and `allItems()` (`:248-250`).
- `mainTabItems()` after the stations (`:256-262`).
- A `stateProperties` case (`:214-220`).
- `SOUNDS` (`:65-81`) gets both new ids.
- Keep the tent out of `ARTICLES` and `PLACEABLE_ARTICLES`.

**Other registries**
- `ModSounds.GROW_TENT_PLANT` / `GROW_TENT_HARVEST` (`C/registry/ModSounds.java:53-55`).
- `ModItems` registers `GrowTentItem`.
- `TooltipHints.LINES` (`:15-25`).

---

## 8. Docs to update (together with the implementation; TODO only after real evidence)

| File | Change |
|---|---|
| `CLAUDE.md` | A dated line like `:14-15`: "Em 25/10/2026 o dono pediu a estufinha…", pointing to DECISIONS. |
| `docs/DECISIONS.md` | Quote the request. Replace the "Estufa (`GreenhouseRules`)" section (about `:78-82`) with an "Estufinha" section: the §1 numbers, recipe, lamp light 12, no soil, water or light needed, harvest and `blockDrops` behaviour, glass gives nothing, Fortune only on right-click, creative gives no drops, `/setblock replace` deletes the plant, inert tent for an unknown id, pick-block note, mob-spawn note, SS all year. Rewrite the formula (about `:66`) and the SS part (about `:390-394`). |
| `docs/PRODUCT_SPEC.md` | `:19` and wording at about `:27,59,71-72`. |
| `docs/GUIA_INICIANTE.md` | Rewrite §5.5 (about `:749-788`) and the other mentions. Add the recipe row and the tests section. |
| `docs/NEXT_SESSION.md` | `:9,14` (18 sounds), `:134,141` (ModBlockEntities), `:144,146`. Client list `:157-160` gets `GrowTentRenderer`. Code map gets the 5 new classes. Test counts `:161,165` only after evidence. |
| `docs/TODO.md` | `:97,118,121`, only with a date, the command and the real output. |
| `docs/README.md:3`, `docs/CLOUD_PROMPT.md`, `docs/TECHNICAL_RESEARCH.md:140` | Rename the section reference to "Estufinha". |
| `docs/ESTUFINHA_PLANO.md`, `docs/estufinha-mapas.md` (from the other workflow) | Align their numbers with this spec. |

---

## 9. Risks still to check in the build

1. **`light_emission` rendering.** It parses (`CuboidModelElement.java:64-73`). Check visually that the down face of the LED looks fullbright on the cutout layer.
2. **Plant scale.** Check that the 0.875 scale and the 5/16 offset look right, with no z-fighting at the soil.
3. **Explosion override.** Check that the `onExplosionHit` override drops exactly once across TNT, creeper and wind charge (`TRIGGER_BLOCK`).
4. **Loot rolled outside `Block.getDrops`.** The manual `LootParams` build with `EXPLOSION_RADIUS` must pass validation for the 40 crop tables.
5. **Balance.** `SOIL_SPEED = 4.0F`, `MULTIPLIER = 1.25F` and `LAMP_LIGHT = 12` are balance choices. The literal tests lock them; the owner confirms them in DECISIONS.

Closed by evidence in this revision:
- placement order (§2.5);
- item translation key (§7.2);
- Serene Seasons (§1.7);
- the cutout layer for the plant (§3.2);
- piston and side-effect timing (§1.6);
- tooltip widths (§6.2);
- the probe parser (§6.4).

---

## Appendix: critique dispositions

**Accepted after checking the sources**
- **C0.1 / `preventDropFromBottomPart`.** It is `protected static` (`F/.../DoublePlantBlock.java:117`), so it is copied as a private helper (§2.5).
- **C0.2 / C2.6 `"shade"`.** The field does not exist; the deserializer reads only `shade_direction_override` and `light_emission` (`CuboidModelElement.java:42-43,64-83`).
- **C0.3 `box()` option.** `box()` has no light option (`models.mjs:54`); the spec adds `light`.
- **C0.4 `ServerLevel`.** `preRemoveSideEffects` gets a `Level`, so the code narrows it to `ServerLevel`.
- **C0.5 dispenser citation.** Corrected to `BoneMealItem.java:64-69` and `DispenseItemBehavior.java:191`, plus the note about where the 1505 particles play.
- **C0.6 coordinate range.** The test allows [-16,32] (`AssetCompletenessTest:268-272`).
- **C0.7 facing.** The choice is now stated explicitly.
- **C0.8 / C2.9 plant height.** The offset is now 5/16, so the top is at 20/16.
- **C1.A orphan half.** The orphan rule is applied to every hook, with validation of the lower half.
- **C1.B1 adventure mode.** The `can_place_on` gate is added (`ItemStack.java:373`, `ServerPlayerGameMode.java:444-446`).
- **C1.B2 explosions.** `onExplosionHit` is overridden. The base method uses the explosion parameters only for the block's own loot (`BlockBehaviour.java:166-188`).
- **C1.B3 creative drops.** Changed to no drops, the same as a creative crop break (`ServerPlayerGameMode.java:315-316`).
- **C1.B4 `changed()` on removal.** Fields are now assigned directly, as the jar does (`JarBlockEntity.java:210-211`).
- **C1.B5 `blockDrops`.** Harvest now respects the rule, like the berry bush's `popResource` (`Block.java:447,455`).
- **C1.B6 unknown variety.** The tent loads as inert and keeps the raw id.
- **C1.B7 `setAge`.** Guarded and clamped.
- **C1.B8 pick-block.** Documented.
- **C1.C extra tests.** Added.
- **C2.1 `PushReaction`.** `DESTROY` does not exist (`PushReaction.java:3-9`); the spec uses `POPPED`.
- **C2.2 English "no".** The English line is reworded.
- **C2.3 call sites.** All 3-argument call sites are listed (`VarietyAndGrowthTest.java:42-66`).
- **C2.4 sky light.** Barriers let skylight through (`BarrierBlock.java:33-35`), so the assertion is removed.
- **C2.5 BRISA.** BRISA is FAST (`manifest.json:20`); AURORA is the MEDIUM case.
- **C2.7 `/setblock`.** `replace` uses `2|256` (`SetBlockCommand.java:125`), so it deletes the plant.
- **C2.8 recipe collisions.** `PURPLE_WOOL_SLAB` and `PURPLE_WOOL_STAIRS` do not exist in `Blocks.java`; removed.
- **C2.10 generator.** The generator wiring is added (§7.2). The lists cited at `generate.mjs:44,73,86,99,141,218` match.
- **C2.11 class count.** The exact number is dropped; it stays above 50.
- **C2.12–14 missing references.** Added. `docs/README.md` exists; there is no root README.
- **C2.15 `placeWithItem`.** It asserts `consumesAction` (`:2013`), so `tryPlaceWithItem` is added.
- **C2.16 camera and NBT check.** The camera is lowered and the strain check is added.
- **Smaller notes.** The `ModSounds.X.get()` form is required. Fortune on break is documented.

**Rejected or narrowed**
- **C0, risk 5 left open.** Closed here instead. A byte search of every class in the Serene Seasons and GlitchCore jars in `run-compat-ss/mods/` finds no `CropGrowEvent` reference. C2's argument rested on `docs/DECISIONS.md:376`, and docs are not cited over code, so this rests on the jar search instead.
- **C1.B2, "remove both halves with flags 35 and skip super".** Narrowed. The spec delegates to `super.onExplosionHit` on the lower state instead. That keeps the vanilla `survives_explosion` roll, `spawnAfterBreak` and Forge's `onBlockExploded` (`IForgeBlock.java:663-666`), and still drops each item at most once.
- **C1.B3, "keep the jar-like creative drops".** Not kept. The owner's request treats the tent as a crop route, so vanilla crop behaviour wins. DECISIONS keeps a note that this can be reverted.
- **C2.14 exact `docs/` line numbers.** Treated as approximate only, because another workflow is editing `docs/*.md` right now.
