# Mapas do código para a estufinha (25/10/2026, commit `09846e1`)

Leituras automáticas usadas pelo [plano](ESTUFINHA_PLANO.md). Em inglês.

# Glass greenhouse to "estufinha": everything that has to change

This is a read-only inventory. I modified no files and ran neither Gradle nor Minecraft. To get the SS facts I disassembled the Serene Seasons jar in `/tmp/claude-0/-home-user-minezada/04ca155e-fbba-50b8-9463-d8cd725a40bc/scratchpad/ss`. To get the Forge facts I read the decompiled sources in `/tmp/forge-src`, which was already extracted.

All paths are under `/home/user/minezada/`. Line numbers come from the working tree as it is now. `git status` shows another workflow has edited docs/{DECISIONS,GUIA_INICIANTE,NEXT_SESSION,README,TECHNICAL_RESEARCH,CLOUD_PROMPT}.md without committing, so doc line numbers may shift. Where docs and code disagree, the code is right.

## 1. Java main code
| file:line | What it does now | What must happen |
|---|---|---|
| `core/GreenhouseRules.java:1-59` | The pure glass rule. `RANGE=6` (:12). `Cell{AIR,GLASS,OTHER}` (:14). `Probe` (:17-20). The roof is the first non-air block above the plant (:30-38). Walls must exist in 4 directions within 6 blocks, at plant height or one above (:40-58). | **Delete.** Optionally replace it with a pure tent rule (for example `core/GrowTentRules`). It would cover: planting only when empty, harvesting only at age 7 and then emptying, no farmland or water needed, and a fixed growth speed. |
| `block/GreenhouseDetector.java:1-23` | Classifies world blocks using the `ModTags.GREENHOUSE_GLASS` tag (:18). | **Delete.** Its callers are `CannabisCropBlock:48`, `SetHarvestQualityFunction:6,42` and `CultivoGameTests:5,261,284,294`. |
| `registry/ModTags.java:13-14` | `GREENHOUSE_GLASS` tag key. | **Delete.** `GreenhouseDetector` is its only user. |
| `block/CannabisCropBlock.java:19` (javadoc), `:48-49` | Random tick speed = vanilla × variety × 1.25 when the glass check passes. | Remove the greenhouse lookup, so outdoor growth = vanilla × variety (`effectiveSpeed(...,false)` or a new overload). Keep the light check (:41) and `vanillaGrowthSpeed` (:61-93) so outdoor still behaves like wheat. |
| `core/GrowthRules.java:5` (javadoc), `:10` `GREENHOUSE_MULTIPLIER=1.25F`, `:14-17` `effectiveSpeed(..., boolean greenhouse)` | Applies the 1.25 growth multiplier for the greenhouse. | Either the flag now means "in the tent" (keeping 1.25), or remove it. This is a decision, see §7. |
| `core/QualityRules.java:4-5`, `:11` `GREENHOUSE_BASE=4`, `:15-17`, `:22-23` | Base quality 2 outdoor, 4 in the greenhouse. | The values stay (outdoor 2–3, tent 4–5). Only the meaning of the flag changes from glass to tent. Renaming it (for example a route enum) is optional; if you rename, `QualityRulesTest` must follow. |
| `loot/SetHarvestQualityFunction.java:6` (import), `:21-22` (javadoc), `:41-43` | Runs the glass check at the loot origin, then calls `harvestQuality`. | Remove the glass check. **Option (a), recommended:** the tent's right-click harvest calls `Block.getDrops(cropState(variety,7), serverLevel, tentLowerPos, null, player, tool)` (`/tmp/forge-src/net/minecraft/world/level/block/Block.java:372`, which sets ORIGIN and TOOL). The function then picks the route with `level.getBlockState(BlockPos.containing(origin)).getBlock() instanceof <TentBlock>`. This keeps the same crop loot table, so the tent drops flowers and seeds (with the Fortune bonus) exactly like the crop. **Option (b):** the function is always outdoor and the tent sets quality in its own Java. Either way it is decided on the server. |
| `registry/ModLootFunctions.java:15-16` | Registers `set_harvest_quality`. | Stays under both options. |
| `core/CuringRules.java:6-7` (javadoc) | Says the greenhouse goes from 4–5 to 5 after curing. | Still true if the tent gives 4–5. Wording only. |
| `src/main/resources/META-INF/mods.toml:11` | Description says "com estufa". | Still accurate. Optional rewording to "estufinha". |

**Tooltips that mention estufa/vidro: none.** `core/TooltipHints.java:15-25` has no greenhouse key, and the manifest translations (en :266-303, pt :310+) contain no "estufa" or "vidro" apart from "Pote de Vidro". There is nothing to remove. The tent's new hint goes into `TooltipHints.LINES` plus pt/en entries in the manifest; `TooltipHintsTest:88` checks the line counts.

## 2. Manifest, generator and generated data
- **`tools/gen/manifest.json:183-197`**: the `greenhouse_glass` array (35 ids). **Delete.**
- **`tools/gen/generate.mjs:463`**: writes `data/cultivo/tags/block/greenhouse_glass.json`. **Delete this line.** Regenerating then removes `src/generated/resources/data/cultivo/tags/block/greenhouse_glass.json`, because the generator wipes the folder first (`NEXT_SESSION.md:114`).
- **The `greenhouse_quality` GameTest** exists in three places that must be renamed or replaced together (for example to `grow_tent_quality`):
  - `manifest.json:200`
  - `core/ContentIds.java:112`
  - `src/generated/resources/data/cultivo/test_instance/greenhouse_quality.json:1-7`

  `DataConsistencyTest:383-386` and `GameTestRegistrationTest:25` enforce that the three match.
- **Crop loot**: `generate.mjs:266-305` (modifier `set_harvest_quality` at :283) produces 40 files `data/cultivo/loot_table/blocks/<v>_crop.json`. **No change** under either option.
- **Serene Seasons tags** (`data/sereneseasons/tags/{block,item}/{summer,autumn}_crops.json`): **no change.** Do NOT add the tent:
  - `CompatibilityTest:57-72` requires exactly the 40 crops;
  - `CompatibilityTest:76-89` forbids a Serene Seasons `greenhouse_glass` file.

## 3. JUnit tests
**core**
- **`core/GreenhouseRulesTest.java:1-83`** (8 tests): **delete.** Replace it with tests of the tent's pure rules, using literal expected values and including a negative test.
- **`core/VarietyAndGrowthTest.java:45-55`** (`greenhouseAndVarietyMultiply`: :47 expects 4×1.25×1.25 with `true`; :51-53 "estufa acelera") **and `:66`** (MORANGO_AZEDO 4×0.8×1.25 with `true`):
  - if the tent keeps the multiplier, keep these with the new meaning;
  - otherwise rewrite or remove them;
  - either way, add a literal test for the tent's speed without farmland.
- **`core/QualityRulesTest.java:13-16, 36-37, 47-48, 51-62`** (`routesNeverOverlap`): the numbers stay. Follow any renaming of the flag.
- **`core/CuringRulesTest.java:31-39` and `core/SmokeRulesTest.java:72-77`**: they only use the literals 4 against 3, so they stay valid. Their "estufa" comments now mean the tent route.

**data**
- **`data/DataConsistencyTest.java:370-378`** (`greenhouseGlassTagHasGlassAndPanes`, expects 35 entries): **delete.** Optionally add a negative check that the tag file no longer exists.
- **`DataConsistencyTest:298-320`** (crop loot uses `set_harvest_quality`): **stays.**
- **`CompatibilityTest:84`**: the comment is about the Serene Seasons `greenhouse_glass` tag. **Stays.**
- **`ClientIsolationTest:46`** requires more than 50 common sources. There are 59 today; removing the 2 classes leaves 57, so it still passes.
- **Mention glass but are unrelated, all stay:** `DataConsistencyTest:599-611` (jar), `TextureArtTest:88-137` (`jar_glass`), `PortugueseEverywhereTest:172` ("Pote de Vidro"), `AssetCompletenessTest:216`, and the glass ingredient in the bong, hookah and jar recipes (`ArticleRecipes:37,50,52,59`).

## 4. GameTests (`gametest/CultivoGameTests.java`)
- **`:5`** `GreenhouseDetector` import: remove.
- **`:108`** registration of `greenhouse_quality`: replace.
- **`:209-216`** `buildGreenhouse` (glass, lime stained glass, pane): remove, or keep it only as a negative fixture (a glass box must now give quality 2–3).
- **`:261`** `check(!GreenhouseDetector.isGreenhouse(...), "céu aberto não é estufa")` in `outdoor_growth`: remove.
- **`:277-305`** `greenhouseQuality`: replace with a tent test. It should:
  1. place the tent with both halves at PLANT (4,2,4); y+1 fits in `empty9x9x9`;
  2. plant with a real `player.gameMode.useItemOn(...)`, with no farmland and no water nearby;
  3. grow by random ticks to age 7;
  4. right-click and check: fresh flower with quality 4–5, seeds, tent empty and replantable;
  5. also put a glass box around a crop on farmland and check it gives 2–3.
- **`:1924`**: comment "estufa 4 → 5". Stays (wording only).
- **`:1297-1363`** (`harvest_drops`, `catalog_strain_grows_and_drops`, which expect outdoor 2–3): no change.
- **`serene_seasons_compat` (`:2933-3013`) STAYS.** The glass at `:2999-3005` is Serene Seasons' own column rule, not the Cultivo greenhouse:
  - a single glass block 3 above the plant, with no walls;
  - it is Serene Seasons' `isGlassAboveBlock` check against its `sereneseasons:greenhouse_glass` tag.

  Optionally add an assertion that a tent plant grows in early spring (see §7.3).
- **Test counts** ("62 GameTests"): `NEXT_SESSION.md:160` and `TODO.md:121`. Update them only after real evidence.

## 5. Visual probe and compat tools
- **`tools/visual-probe/gen-datapack.mjs:138-144`** is scene E: a hollow glass box (39..45 / 150..154 / 10..16), farmland at :140, water at :141, five crops at age 7 at :142-144. **Replace** it with a tent scene (empty tent, young plant, mature plant, ideally in the dark so the purple light shows).
- **Timeline and camera**: `gen-datapack.mjs:53` (`[1060,'cam_greenhouse']`) and `:190` (camera `tp 42.5 155.8 21.5`) must stay consistent with `run-probe.ps1:304` (`Wait-Step 'cam_greenhouse'`, `Shot '11_estufa'`). `validate.mjs:1-2` checks that markers, timeline and ps1 agree.
- **Generated datapack, regenerate with `gen-datapack.mjs` and never edit by hand:** `datapack/data/probe/function/build.mcfunction:93-101`, `tick.mcfunction:11`, `cam_greenhouse.mcfunction:1-2`.
- **Descriptions**: `run-probe.ps1:7` ("estufa") and `PRINTS.md:16` (row 11, "Vidro com chão de vidro…") should describe the tent.
- **`validate.mjs:13-29`** reads block properties from the blockstate JSON. The tent's `half` (and any age property) must appear there, or get a default added like `:26-29` does for other blocks.
- **Stay (Serene Seasons glass only):** `tools/compat/probes/seasons.txt:4,36-37,103+`, `tools/compat/README.md:12,116`, `tools/compat/gen-probes.mjs`.

## 6. Docs and CLAUDE.md
**`CLAUDE.md:12`**, "Rota de estufa com qualidade superior.": the text **stays** and the tent satisfies it (4–5 against outdoor 2–3). Optionally add a dated line like `:14-15` ("Em 25/10/2026 o dono pediu… estufinha") pointing to DECISIONS.

**`DECISIONS.md`**
- `:5-17`: add the owner's 25/10/2026 request, quoted.
- `:9`: "indoor em estufa" becomes "estufinha".
- `:66`: the formula "× 1,25 se estiver em estufa" must be rewritten.
- `:74-76`: wording only; the numbers stay.
- `:78-82`, section "Estufa (`GreenhouseRules`)": **replace** with a tent section covering:
  - the 2-tall block and its recipe;
  - the purple light;
  - no farmland or water;
  - right-click harvest that gives flowers and seeds and empties the tent;
  - glass gives nothing.

  Line `:82`, "Não exige luz artificial nem irrigação extra além da farmland", is directly contradicted by the new tent.
- `:390-394`, the Serene Seasons "Estufa" part:
  - `:391` stays;
  - `:392-393` delete (the Cultivo glass greenhouse no longer exists);
  - `:394` "continua vindo da GreenhouseRules" becomes the tent route;
  - add the Serene Seasons behaviour from §7.3.
- `:387`, `:400`, `:411`, `:413` are about Serene Seasons only. They stay.

**`PRODUCT_SPEC.md`**
- `:19`: "estufa construída pelo jogador" becomes a crafted estufinha.
- `:27`, `:59`, `:71-72`: wording only; the acceptance criteria are still met through the tent.
- Add an extension bullet near `:55-64`.

**`GUIA_INICIANTE.md`**
- `:8` and `:461` (probe scene 11).
- **Rewrite all of §5.5, `:749-788`:**
  - the rule;
  - the ASCII diagram at `:758-772`;
  - ×1,25 at `:776`;
  - the table at `:781-784`;
  - `:786`, "quebra a planta madura… estufa montada", becomes right-click harvest;
  - Serene Seasons at `:788`.
- `:1043` stays; `:1044` rewrite.
- Checklists `:1095` and `:1328`.
- Troubleshooting `:1630`: drop "estufa do Cultivo com teto de bloco de vidro".
- Glossary `:1702`: replace the example tag `cultivo:greenhouse_glass`.
- §7.2.3, `:1449-1467`: rows `:1457-1459`; tests `:1463`, `:1466-1467` (`GreenhouseRulesTest`, `buildGreenhouse`, "35 blocos").
- The recipe tables (around `:848-886`) gain the tent recipe.
- `:1319-1327`, `:1340`, `:1056` are Serene Seasons glass only. They stay.

**`NEXT_SESSION.md`**
- Change: `:9` (wording), `:133` (`GreenhouseRules`), `:143` ("vidro de estufa"), `:145` (`GreenhouseDetector`), `:160` (test count).
- Stay: `:35`, `:39`, `:91`, `:161`, `:284-285` (Serene Seasons glass) and `:259` (history).

**`TODO.md`**
- `:95-97`: line `:97`, "estufa: teto + 4 paredes de vidro até 6 blocos", becomes false.
- `:118`: `greenhouse_quality`.
- `:121`: "62 GameTests".
- Rewrite these only with new evidence (date, exact command, real output). The history rows `:22`, `:63`, `:66` and the Serene Seasons item `:195` stay.

**`README.md:3`, `CLOUD_PROMPT.md:14`, `TECHNICAL_RESEARCH.md:140`** ("regra da estufa", pointing to the DECISIONS "Estufa" section): update the wording and the section reference.

## 7. Decisions forced by removing the glass rule
1. **Growth speed without farmland.** `CannabisCropBlock.vanillaGrowthSpeed` (:61-93) returns 1.0 when there is no farmland, which makes the roll 1 in 26: about 4× slower than hydrated farmland. The tent needs a fixed base speed in `core`, for example 4.0 (isolated hydrated farmland, as in `VarietyAndGrowthTest:41`) × variety, plus ×1.25 if you keep the multiplier.
2. **Light.** Growth requires light ≥ 9 (`CannabisCropBlock:41`, `GrowthRules:8`). For the purple light, either the tent block gives off light ≥ 9 (`Properties.lightLevel`; no block in `ModBlocks` uses it yet), or tent growth ignores the surrounding light level. The GameTest should prove the tent grows with no sky.
3. **Serene Seasons (from the SS 26.1.2.0.7 bytecode).**
   - How it works:
     - `MixinBlockStateBase` hooks `randomTick` and calls `SeasonalCropGrowthHandler.onCropGrowth`;
     - that handler is gated by `ModFertility.isCrop`, which means "in one of the 4 season tags";
     - the bone-meal path uses the same gate;
     - this matches `NEXT_SESSION.md:91`.
   - So a tent block in no SS tag grows all year under Serene Seasons, like an SS greenhouse, with no extra work.
   - If the tent instead keeps a real `CannabisCropBlock` in the world, Serene Seasons applies to that crop, and SS's glass check would not count the tent.
   - Record this in DECISIONS.
4. **Storing the variety.** 40 varieties × 8 ages × 2 halves is too many block states. Either use one tent block per variety, or a BlockEntity (like `JarBlockEntity`) plus a renderer in `client/` that reuses the per-family crop models. Those models use the `minecraft:block/crop` parent (`generate.mjs:57-66`) and are one block tall, so a full-size plant fits in the lower half.
5. **Villagers.** `HarvestFarmland` only breaks `CropBlock`s (`NEXT_SESSION.md:224`), so villagers leave the tent alone, and `FarmerReplant` is unaffected.

## 8. Where the tent plugs in, enforced by existing tests (new code, not glass)
- **Block lists:** `ContentIds.allBlocks` (:188-190) and `stateProperties` (:195+), checked by the `block_states_match_ids` GameTest. The `ARTICLES` list (:30-32) is documented as smoking articles only, so the tent likely needs its own list.
- **Recipe:** `ArticleRecipes.java:48-66` plus the manifest recipes (:132-141). The count check at `DataConsistencyTest:81` (`11+3+40+1`) changes.
- **Sounds, if any:** `ContentIds.SOUNDS` (:65-81) and `SoundsTest:46` (expects 16); play them only through `ModSounds`.
- **Tooltip:** `TooltipHints.LINES` (:15-25).
- **Two-tall placement:** follow the 26.3 `DoublePlantBlock` pattern rather than extending it, because it extends `VegetationBlock` (`DoublePlantBlock.java:27`), which only accepts dirt-like ground. The pieces in `/tmp/forge-src/net/minecraft/world/level/block/DoublePlantBlock.java`:
  - `HALF` property (:28);
  - `getStateForPlacement` checks the block above (:59-62);
  - `setPlacedBy` places the upper half (:66-68);
  - `updateShape` (:36-52);
  - `playerWillDestroy` and `preventDropFromBottomPart` (:93-125).

  `DoorBlock` extends plain `Block` (`DoorBlock.java:41`) and is the other model to follow.
- **Rendering:** any rendering code goes only in `client/` (`ClientIsolationTest`).

---

I didn't modify any file or run Gradle or Minecraft. Every claim below comes from the code; the one fact taken only from docs is marked "(docs)".

Path roots used below:
- `C/` = `/home/user/minezada/src/main/java/io/github/peixeangola/cultivo/`
- `V/` = `/tmp/forge-src/net/minecraft/world/` (decompiled 26.3)
- `G/` = `/home/user/minezada/src/generated/resources/`

**Recommendation in short:** keep the plant as a strain and an age inside the tent's BlockEntity. Grow it with the tent block's own random tick, using the existing formula. Harvest by rolling the crop's own loot table with the tent BlockEntity in the loot context. `SetHarvestQualityFunction` then gives 4–5 when that BlockEntity is a tent and 2–3 otherwise.

## A. How a plant grows today

**Registration and placement**
- There is one `CannabisCropBlock` per strain, built with `ofFullCopy(Blocks.WHEAT)` (`C/registry/ModBlocks.java:33-37`).
- The seed is a `SeedItem`, which is the crop's `BlockItem` (`C/registry/ModItems.java:43-44`, `C/item/SeedItem.java:14-20`).
- It can only be placed on `FarmlandBlock` (`V/level/block/CropBlock.java:41-44`). It survives with raw light ≥ 8 (`CropBlock.java:138-145`).

**Ages and visual stages**
- Ages run 0–7 (`AGE_7`); the max age is 7 (`CropBlock.java:27-28, 50-64`; `C/core/GrowthRules.java:9`).
- It only random-ticks while not mature (`CropBlock.java:66-69`). The shape grows 2+2·age px (`CropBlock.java:29`).
- Visuals: the blockstate maps `age=N` to `<family>_crop_stageN`, giving 8 models per art family with parent `minecraft:block/crop` (`/home/user/minezada/tools/gen/generate.mjs:51-66`, `G/assets/cultivo/blockstates/brisa_crop.json`).
- The vanilla `crop.json` model spans y = −1 to 15.
- `GrowthRules.visualStage` (4 stages, `GrowthRules.java:31-37`) has no callers.

**Random tick** (`C/block/CannabisCropBlock.java:39-55`), in order:
1. Return if raw brightness < 9 (`GrowthRules.java:8, 27-29`).
2. Return if age ≥ 7.
3. Check the glass greenhouse (`:48`, then `C/block/GreenhouseDetector.java:13-22`). The rule (`C/core/GreenhouseRules.java:83, 97-129`) needs both:
   - the first non-air block above, within 6 blocks, is glass;
   - there is glass in each of the 4 directions, within 6 blocks, at the plant's height or one above.
4. Speed `v = vanillaGrowthSpeed × variety.growthMultiplier() × (greenhouse ? 1.25 : 1)` (`GrowthRules.java:10, 14-17`).
   - `vanillaGrowthSpeed` (`CannabisCropBlock.java:57-93`) starts at 1 and adds a value for each block of the 3×3 below: hydrated farmland 3, dry farmland 1, anything else 0.
   - The 8 neighbours count ÷4. The total is halved if the same crop is in both row directions, or on any diagonal.
   - It ports `CropBlock.getGrowthSpeed` (`CropBlock.java:94-136`).
   - A lone hydrated plot gives 4. A plot ringed by hydrated farmland with no crowding gives 10.
5. It grows if `random.nextInt((int)(25/v)+1) == 0` (`GrowthRules.java:19-25`).
6. `ForgeHooks.onCropsGrowPre`, then `setBlock(age+1, UPDATE_CLIENTS)`, then `onCropsGrowPost` (`CannabisCropBlock.java:51-54`; `/tmp/forge-src/net/minecraftforge/common/ForgeHooks.java:643-650`).

**Strains** (`C/core/Variety.java`)
- 40 strains: 3 native and 37 from the catalog (`Variety.java:31-72`). By growth class: 22 FAST, 14 MEDIUM, 4 SLOW.
- Growth multipliers: FAST ×1.25, MEDIUM ×1.0, SLOW ×0.8 (`Variety.java:85-99, 202-205`).
- At v=4 the tests expect roll bounds of 6 / 7 / 8 (`/home/user/minezada/src/test/java/io/github/peixeangola/cultivo/core/VarietyAndGrowthTest.java:39-43, 57-66`).
- (docs) Serene Seasons only affects blocks in its crop tags, through a randomTick mixin (`docs/DECISIONS.md:376`).

**Bone meal** is inherited unchanged, with no strain or greenhouse factor:
- Valid while not mature; success is always true; the age becomes `min(7, age + nextInt(2..5))` (`CropBlock.java:85-92, 172-185`).
- Flow: `BoneMealItem.useOn` → `applyBonemeal` → Forge event → `instanceof BonemealableBlock && isValidBonemealTarget`. This check also runs on the client as the click prediction (`V/item/BoneMealItem.java:35-47, 73-90`).
- Click order: the block's `useItemOn` runs first (`V/../server/level/ServerPlayerGameMode.java:446`).
  - `TRY_WITH_EMPTY_HAND` in the main hand leads to `useWithoutItem` (`:452-453`).
  - Otherwise the item's `useOn` runs (`:461-468`).
  - So a tent must return PASS for bone meal, as `V/level/block/SweetBerryBushBlock.java:111` does.

## B. How a plant is harvested today

**Loot table** `cultivo:blocks/<v>_crop` (written by `generate.mjs:266-305`; e.g. `G/data/cultivo/loot_table/blocks/brisa_crop.json`) has the same shape as vanilla `wheat.json`:
- Table modifier `explosion_decay`. It does nothing without `EXPLOSION_RADIUS` (`V/level/storage/loot/functions/ApplyExplosionDecay.java:27-28`).
- Pool 1, alternatives:
  - `<v>_fresh_flower`, if `match_block` age=7, with modifier `cultivo:set_harvest_quality`;
  - otherwise 1 × `<v>_seeds`.
- Pool 2, only at age 7: `<v>_seeds` with Fortune `binomial_with_bonus_count` (extra 3, p 0.5714286). That gives 1 + Bin(3+Fortune, 4/7) seeds (`ApplyBonusCount.java:63-70, 105-112`).
- Result: a mature plant gives 1 flower and 1–4 seeds; an immature one gives 1 seed (GameTest `harvestDrops`, `C/gametest/CultivoGameTests.java:308`).

**Loot context**
- `Block.getDrops(state, level, pos, be, breaker, tool)` sets ORIGIN = centre of `pos`, TOOL, and the optional THIS_ENTITY and BLOCK_ENTITY (`V/level/block/Block.java:372-386`).
- `BlockBehaviour.getDrops` adds BLOCK_STATE = `state` and rolls that block's own loot key (`V/level/block/state/BlockBehaviour.java:266-275`).
- The BLOCK parameter set requires BLOCK_STATE, ORIGIN and TOOL; THIS_ENTITY, BLOCK_ENTITY and EXPLOSION_RADIUS are optional (`LootContextParamSets.java:109-117`).
- `match_block` reads BLOCK_STATE (`MatchBlock.java:24-31`).

**`SetHarvestQualityFunction.run`** (`C/loot/SetHarvestQualityFunction.java:38-46`)
- It needs BLOCK_STATE to be a `CannabisCropBlock` (that gives the strain), plus ORIGIN.
- Greenhouse or outdoor is decided at roll time by `GreenhouseDetector.isGreenhouse(level, containing(origin))` (`:42`).
- It then stores `QualityRules.harvestQuality(variety, greenhouse, random.nextDouble())` in `cultivo:quality` (`:43-44`).
- It doesn't override `getReferencedContextParams`. It is registered as a MapCodec (`C/registry/ModLootFunctions.java:17-21`).

**`QualityRules`** (`C/core/QualityRules.java:9-33`)
- Base 2 outdoors, 4 in the greenhouse. Add +1 if the roll is below the strain's chance.
- The chance comes from potency 1 / 2 / 3: 0.20 / 0.35 / 0.50 (`Variety.java:154-161, 207-210`). The result is clamped to 1–5.
- So outdoor gives 2–3 and greenhouse 4–5. Curing in the jar adds +1 once (`C/core/CuringRules.java:30-32`).

**Items and components**
- `cultivo:quality` is an int 1–5, persistent and synced (`C/registry/ModComponents.java:19-24`). Only the fresh flower gets it, at harvest.
- `cultivo:strain` (`ModComponents.java:26-39`) is only for items that don't belong to one strain (joint, resin, hash).
- Seeds and flowers know their strain from the item class (`C/item/SeedItem.java:22-24`, `C/item/FlowerItem.java:23-25`, `ModItems.varietyOf` at `ModItems.java:106-115`). Seeds have no components.
- The generated tag `G/data/cultivo/tags/item/seeds.json` exists, but `C/registry/ModTags.java` has no TagKey for it.

**FarmerReplant** (`C/village/FarmerReplant.java:23-38`, wired at `C/Cultivo.java:47`)
- When a villager breaks a mature `CannabisCropBlock`, it replants age 0.
- Villagers only harvest a `CropBlock` sitting on farmland (`HarvestFarmland.java:85, 108-109`). A tent is not a `CropBlock`, so villagers never touch it and FarmerReplant needs no change.

**Handouts** (`C/block/Handouts.java:20-22`)
- Uses `placeItemBackInInventory(stack, false, PREDICTED)`; anything that doesn't fit drops at the player's feet, also in creative.
- The class is package-private (`:8`), so the tent block has to live in `block/`.

## C. Proposal for the tent

New pieces:
- `block/GrowTentBlock`: Block + EntityBlock + BonemealableBlock, with `DoubleBlockHalf HALF`.
- `block/GrowTentBlockEntity`.
- `client/GrowTentRenderer`.
- Additions to GrowthRules, plus registry and manifest entries (recipe, lang, sounds such as `grow_tent.plant` and `grow_tent.harvest`, art).

**1. Two-block shape (door pattern)**
- Placement needs the space above to be replaceable (`V/level/block/DoorBlock.java:129-143`). `setPlacedBy` places the upper half (`:146-148`).
- `updateShape` removes the other half (`:88-98`). In creative, `preventDropFromBottomPart` stops the drop (`:112-117`; `DoublePlantBlock.java:117-128`).
- The item is a `DoubleHighBlockItem` (`V/item/DoubleHighBlockItem.java:16-22`). The tent's loot table only drops on half=lower.
- Only the lower half gets a BlockEntity: `newBlockEntity` may return null (`V/level/chunk/LevelChunk.java:338-342`). The upper half sends every interaction to `pos.below()`.
- Purple light: `Properties.lightLevel(state -> …)` (`BlockBehaviour.java:1171`).
  - Optionally add a `PLANTED` boolean that the BlockEntity sets, the way `JarBlockEntity.java:144-154` sets FILLED. Changing it keeps the BlockEntity (`LevelChunk.java:304`).
  - Set its default to false explicitly.

**2. BlockEntity state:** `@Nullable Variety variety` and `int age` (0–7).
- Save the strain id and age; load the strain with `Variety.byId` (`Variety.java:128-131`).
- Send both to the client with `getUpdatePacket` and `getUpdateTag` (`JarBlockEntity.java:191-204`), and call `sendBlockUpdated` on every change (`:135-142`).
- The client needs them for the renderer, for click prediction, and for bone meal (`applyBonemeal` checks the target on the client too).

**3. Accepting a seed** in `useItemOn`:
- If the item is a `SeedItem` and the tent is empty: on the server, plant `seed.variety()`, use up 1 seed unless `hasInfiniteMaterials`, play a ModSounds sound, and return SUCCESS. The client predicts from the synced state (`JarBlock.java:86-118`).
- Bone meal returns PASS. Other cases return `TRY_WITH_EMPTY_HAND`.
- No farmland, water or light check.

**4. Growth: use the tent block's random tick, not a BlockEntity ticker.**

```java
protected boolean isRandomlyTicking(BlockState s){ return s.getValue(HALF)==LOWER /* && s.getValue(PLANTED) */; }
protected void randomTick(BlockState st, ServerLevel lvl, BlockPos pos, RandomSource r){
  if(!(lvl.getBlockEntity(pos) instanceof GrowTentBlockEntity t) || !t.canGrow()) return;
  float v = GrowthRules.tentSpeed(t.variety());
  boolean grows = r.nextInt(GrowthRules.growthRollBound(v)) == 0;
  if(ForgeHooks.onCropsGrowPre(lvl,pos,st,grows)){ t.setAge(t.age()+1); ForgeHooks.onCropsGrowPost(lvl,pos,st); }
}
```

Why random ticks:
- Same distribution and `randomTickSpeed` game rule as the crop, and the same formula.
- The GameTests already force growth with `state.randomTick` (`CultivoGameTests.java:265`).
- An idle tent costs nothing per tick.
- (docs) Serene Seasons ignores blocks outside its tags (`DECISIONS.md:376`).
- Note: `isRandomlyTicking` is cached per blockstate (`BlockBehaviour.java:522, 897`). To skip empty tents you need the PLANTED property.

**Speed:** with no soil, `tentSpeed` needs a constant.
- I propose `TENT_SOIL_SPEED = 4.0F` (a lone hydrated plot) × strain multiplier × 1.25. That keeps the greenhouse multiplier, renamed `TENT_MULTIPLIER`.
- That gives roll bounds of 5 / 6 / 7 for FAST / MEDIUM / SLOW, against 6 / 7 / 8 outdoors on a lone hydrated plot.
- Using 10.0F instead gives 2 / 3 / 3, which loses the difference between classes. The constant is a balance call for the owner; lock it with literal tests.

**5. Bone meal on the tent**
- `isValidBonemealTarget`: the lower BlockEntity is planted and age < 7.
- `isBonemealSuccess`: true.
- `performBonemeal`: `age = min(7, age + Mth.nextInt(random, 2, 5))`. This copies `CropBlock.java:85-92`, which is protected, so put the 2..5 constants in GrowthRules.
- Dispensers go through the same path (`BoneMealItem.java:65-71`).

**6. Harvest: roll the crop's own loot table with the tent BlockEntity in the context.**

```java
List<ItemStack> rollPlantDrops(ServerLevel lvl, @Nullable Entity who, ItemStack tool){
  BlockState plant = ModBlocks.CROPS.get(variety).get().getStateForAge(age);
  return Block.getDrops(plant, lvl, worldPosition, this, who, tool);
}
```

- `useWithoutItem` on a mature plant:
  - roll the drops, then clear the strain and age so the tent is empty;
  - give each stack with `Handouts.give`;
  - play the ModSounds harvest sound and send the BLOCK_CHANGE game event, as `SweetBerryBushBlock.java:118-138` does;
  - return SUCCESS.
- This gives exactly the JSON drops: 1 flower and 1 + Bin(3, 4/7) seeds. Forge global loot modifiers and datapack overrides still apply.
- The main hand is empty here, so the tool is `ItemStack.EMPTY` and Fortune doesn't apply.

**Change to `SetHarvestQualityFunction`.** It stays the only place that decides quality:

```java
BlockState s = context.getOptional(LootContextParams.BLOCK_STATE);
if (s != null && s.getBlock() instanceof CannabisCropBlock crop) {
  boolean tent = context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GrowTentBlockEntity;
  stack.set(ModComponents.QUALITY.get(), QualityRules.harvestQuality(crop.variety(), tent, context.getRandom().nextDouble()));
}
```

- Optionally override `getReferencedContextParams` to return `Set.of(BLOCK_STATE, BLOCK_ENTITY)`, so the loot validator checks them.
- Why read BLOCK_ENTITY instead of looking at the block at ORIGIN:
  - Outdoor crops have no BlockEntity, so they automatically get 2–3.
  - When the tent is broken, `preRemoveSideEffects` runs after the chunk already holds the new block (set at `LevelChunk.java:279`, side effects at `:311`). A lookup at ORIGIN would see air and give outdoor quality. The BlockEntity parameter is still `this`.
- In `QualityRules`, rename `greenhouse` to `tent` and `GREENHOUSE_BASE` to `TENT_BASE`; the value stays 4.

**Breaking the tent:** use `preRemoveSideEffects` (pattern at `JarBlockEntity.java:206-213`) to `popResource` each stack of `rollPlantDrops(lvl, null, EMPTY)`.
- An immature plant gives 1 seed; a mature one gives the flower at 4–5 plus seeds.
- The tent item drops from the tent's own loot table.

**Rejected: a pure Java rule** for the drops. It would copy the numbers from `generate.mjs:276-301` and need a parity test, and it would skip global loot modifiers, datapack overrides and Fortune.

**7. Client rendering**
- `client/GrowTentRenderer` copies the pattern of `C/client/PressPlatenRenderer.java:52-56, 85-112`:
  - in `extractRenderState`, call `models.update(state.plant, CROPS.get(v).get().getStateForAge(age), ctx)`;
  - in `submit`, draw it with `lightCoords`.
- Register it in `C/client/CultivoClientEvents.java:28-29`.
- This reuses the 8 existing stage models, so no new plant art is needed.
- Lift the plant by the soil tray height plus 1/16 (the crop model starts at y=−1). The tent model needs a cutout window or an open front so the plant shows, plus `noOcclusion`.
- A blockstate-only design (strain × age properties) would need about 642 states and a wrapper for Variety; I'd avoid it.

**8. Places to change so glass gives no bonus**
- Code: `CannabisCropBlock.java:48-49` and `SetHarvestQualityFunction.java:42`; drop `GreenhouseDetector`, `GreenhouseRules` and `GreenhouseRulesTest`.
- Tag: `ModTags.java:14`, `tools/gen/manifest.json:183`, `generate.mjs:463`.
- Tests: `CultivoGameTests.java:210-216, 261, 278-305`, the greenhouse=true cases in `VarietyAndGrowthTest.java:45-66`, and `ContentIds.java:112`.

**9. Tests to add**
- JUnit:
  - `tentSpeed` roll bounds 5 / 6 / 7 as literals;
  - tent quality 4–5.
- GameTests:
  - a tent grows under the test ceiling (no `sky_access`) with no farmland or water;
  - bone meal works on a tent;
  - right-click harvest gives a flower at quality 4–5 and at least 1 seed, and leaves the tent empty;
  - breaking a mature tent gives quality 4–5;
  - a crop under glass now gives 2–3.

---

# Mapa de padrões para a estufinha, com citações do código (só leitura)

Leitura concluída sem alterar nenhum arquivo e sem rodar `./gradlew` ou o Minecraft. Pontos que impactam o projeto da estufinha:
- O mod ainda não tem bloco de 2 de altura, então esse padrão vem do vanilla.
- A qualidade 4–5 da estufa de vidro hoje passa por quatro pontos (seção 0).
- A estufinha precisa de BlockEntity e renderer próprios (seção 12).
- Seis números fixos nos testes vão mudar (seção 15).

Os caminhos abaixo são relativos a `src/main/java/io/github/peixeangola/cultivo/`, salvo indicação.

## 0. Como a estufa de vidro funciona hoje (o que precisa deixar de dar bônus)
- **Regra pura:** `core/GreenhouseRules.java:26-58`, com alcance de 6 (`:12`).
- **Ligação com o mundo:** `block/GreenhouseDetector.java:13-23`, usando a tag `ModTags.GREENHOUSE_GLASS` (`registry/ModTags.java:14`).
- **A regra é consultada em dois lugares:**
  - no crescimento, `block/CannabisCropBlock.java:48-49` → `GrowthRules.effectiveSpeed` com multiplicador de 1,25 (`core/GrowthRules.java:10,14-17`);
  - na qualidade, `loot/SetHarvestQualityFunction.java:41-44` → `QualityRules.harvestQuality(variety, greenhouse, roll)`, com base 2 ao ar livre e 4 na estufa (`core/QualityRules.java:10-11,15-27`).
- **Essa função de loot só age se o bloco do loot for um `CannabisCropBlock`** (`:41`). Para a colheita da estufinha há dois caminhos:
  - chamar `QualityRules.harvestQuality(v, true, rnd)` direto;
  - ou rolar a tabela de loot da lavoura com uma detecção da estufinha no lugar do `GreenhouseDetector`.
- **A lavoura:** `CannabisCropBlock` é um `CropBlock` copiado do trigo (`registry/ModBlocks.java:36-37`).
  - Só sobrevive em farmland (`CropBlock.mayPlaceOn`, `/tmp/forge-src/.../CropBlock.java:42`; o GameTest confere em `CultivoGameTests.java:271-274`).
  - Exige luz de pelo menos 9 (`CannabisCropBlock.java:41`, `GrowthRules.java:27`).
  - Umidade e vizinhos entram por `vanillaGrowthSpeed` (`CannabisCropBlock.java:61-93`).
  - Uma planta sem farmland e sem água precisa ser um estado da própria estufinha (ou da BlockEntity), não um `CropBlock` de verdade.
- **Outros pontos que citam a estufa de vidro:**
  - GameTest `greenhouse_quality` em `CultivoGameTests.java:278-305` (usa `buildGreenhouse`, `:209-216`);
  - `outdoor_growth` em `:261`;
  - `serene_seasons_compat` em `:2999-3003` (vidro como estufa do Serene Seasons, não do Cultivo);
  - testes JUnit: `test/.../core/GreenhouseRulesTest.java`, `DataConsistencyTest.java:371` (tag), `CompatibilityTest.java:84`, `VarietyAndGrowthTest.java:46`;
  - manifesto `greenhouse_glass` em `tools/gen/manifest.json:183-197`, escrito em `generate.mjs:463`;
  - visual probe: `tools/visual-probe/{gen-datapack.mjs,run-probe.ps1,PRINTS.md}` e `datapack/.../cam_greenhouse.mcfunction`, `build.mcfunction`, `tick.mcfunction`.

## 1. Bloco com BlockEntity, inventário e sincronização com o cliente
**`JarBlock` / `JarBlockEntity`** (o modelo mais próximo: um slot e um blockstate visual):
- **Propriedade e padrão:** `FILLED`, com o padrão `false` definido explicitamente (`block/JarBlock.java:45,51`); `createBlockStateDefinition` em `:55-57`.
- **Ticker só no servidor:** `level.isClientSide()` devolve null e o tipo é conferido (`:74-80`).
- **Clique:**
  - `useItemOn` devolve `TRY_WITH_EMPTY_HAND` com a mão vazia, `PASS` se a BlockEntity não existe e a previsão no ramo do cliente (`:96-106`);
  - no servidor, só desconta a pilha se `!player.hasInfiniteMaterials()` (`:111-113`), toca o som (`:116`) e devolve `SUCCESS` (`:117`);
  - `useWithoutItem` → `Handouts.give` → som → `SUCCESS` (`:121-136`).
- **Vizinhos e comparador:** `affectNeighborsAfterRemoval` → `Containers.updateNeighboursAfterDestroy` (`:149-151`); comparador em `:139-146`.
- **BlockEntity:**
  - `NonNullList` de 1 slot (`block/JarBlockEntity.java:49`);
  - `contentsChanged()`: `setChanged`, `syncVisualState`, `level.sendBlockUpdated(..., UPDATE_CLIENTS)` só no servidor (`:135-142`);
  - `syncVisualState` só mexe no bloco se esta for a BlockEntity viva e o estado tiver a propriedade, com `setBlock(UPDATE_CLIENTS)` só quando o valor muda (`:145-154`);
  - o tick confere o estado visual (acerta dados vindos de pick-block, `/setblock` ou `/data`) e avança o contador (`:156-174`);
  - contador sem acordar comparadores: `level.blockEntityChanged` (`:172`);
  - salvar e carregar: `saveAdditional(ValueOutput)` + `ContainerHelper.saveAllItems` + `putInt`; `loadAdditional(ValueInput)` + `getIntOr` (`:177-189`);
  - `getUpdatePacket` = `ClientboundBlockEntityDataPacket.create(this)` (`:192-194`);
  - `getUpdateTag` só com os itens, via `ProblemReporter.ScopedCollector` + `TagValueOutput.createWithContext` + `saveAllItems(output, items, true)` (`:198-204`);
  - o drop ao quebrar fica em `preRemoveSideEffects` → `Containers.dropContents` (`:207-213`);
  - ganchos para os GameTests: `setItem` e `setCureProgress` (`:61-74`).

**`ProcessingStationBlock` / `ProcessingStationBlockEntity`:**
- **Bloco:**
  - `FACING` = `HorizontalDirectionalBlock.FACING` e `CONTENTS` = `EnumProperty.create("contents", StationContents.class)` (`block/ProcessingStationBlock.java:66-67`);
  - padrão em `:76`; `getStateForPlacement` com `getHorizontalDirection().getOpposite()` (`:106-108`);
  - formas por direção com `Shapes.rotateHorizontal` (`:75,111-113`); `rotate`/`mirror` em `:116-123`;
  - partículas só no cliente em `animateTick`, conforme o estado (`:138-170`); ticker em `:178-185`.
- **BlockEntity:**
  - dois slots e progresso (`block/ProcessingStationBlockEntity.java:57-62`);
  - `contentsChanged` e `syncVisualState` no mesmo padrão do pote (`:89-113`);
  - `serverTick` com regra pura (`ProcessingRules.advance`, `:249-284`);
  - sons de trabalho no ritmo de `StationAnimation` (`:287-301`), partículas no servidor com `sendParticles` + som de pronto (`:304-313`);
  - `getUpdateTag` só com os slots (`:339-345`); `preRemoveSideEffects` em `:348-354`.
- **`StationContents`:** enum `StringRepresentable` que espelha a regra pura `StationVisualRules.Contents` (`block/StationContents.java:10-34`). É o modelo para um enum de estado da estufinha (vazia / crescendo / madura).
- **Propriedade a mais numa subclasse:** `SieveBlock` sobrescreve `createBlockStateDefinition` e chama o super (`block/SieveBlock.java:64-68`), com o padrão booleano explícito (`:56`). A armadilha é que `createBlockStateDefinition` roda dentro do construtor de `Block` (javadoc em `:42-43`).
- **`Handouts.give`:** `placeItemBackInInventory(stack, false, Prediction.PREDICTED)`, que evita a armadilha do `Inventory#add` no criativo (`block/Handouts.java:20-22`). A classe é package-private (`:8`), então a estufinha precisa ficar em `block/`.
- **Registro:**
  - `ModBlockEntities.JAR` = `new BlockEntityType<>(JarBlockEntity::new, Set.of(ModBlocks.JAR.get()))` (`registry/ModBlockEntities.java:28-29`);
  - artigo colocável em `registry/ModBlocks.java:64-67`: `Properties.of().mapColor(...).strength(...).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED).setId(BLOCKS.key(id))`, mapa `PLACEABLE_ARTICLES` em `:76-79`;
  - a ordem dos DeferredRegister fica em `Cultivo.java:32-42`.

## 2. Previsão no cliente e SUCCESS vs SUCCESS_SERVER
- **Padrão de previsão:** o ramo do cliente devolve uma previsão estática e pura, feita com os slots sincronizados ou o blockstate:
  - `JarBlock.predictInsert` e `predictTake` (`block/JarBlock.java:86-93`);
  - `ProcessingStationBlock.predictUse` (`:198-205`), pública para os GameTests;
  - `SieveBlock.predictUse` (`:79-89`);
  - `RollingTrayBlock.predict` devolve `CONSUME` para a recusa (`block/RollingTrayBlock.java:97-109`).
- **Previsão por tag:** `couldInsert` usa uma tag de item, porque as tags chegam ao cliente e as receitas não:
  - estações: `ProcessingStationBlockEntity.java:145-152` com `ModTags.STATION_INPUTS` (`registry/ModTags.java:23-29`, gerado em `generate.mjs:471-478`);
  - pote: tag `jar_storable` (`JarBlockEntity.java:77-90`).
  - Para a estufinha, a tag `cultivo:seeds` já existe (`generate.mjs:466`).
- **O servidor sempre devolve `SUCCESS`:**
  - a previsão já balançou o braço; `SUCCESS_SERVER` repetiria o balanço (javadoc em `ProcessingStationBlock.java:49-52`);
  - GameTest `interactionsSwingOnce` em `CultivoGameTests.java:2332`, com `checkPredictedSwing` exigindo `Success.swingSource() == PREDICTED` (`:2322-2325`).
- **GameTest da previsão:** `client_prediction_matches_server` (`:2623`) monta a visão do cliente só com `getUpdateTag`:
  - `stationView` e `jarView` com `loadWithComponents(TagValueInput.create(...))` (`:2564-2580`);
  - `serverBlockResult` repete a ordem do `ServerPlayerGameMode` (`:2585-2596`).
- **Agachado:** `ServerPlayerGameMode` só chama o bloco com as duas mãos vazias (comentário em `ProcessingStationBlock.java:237`).

## 3. Artigo colocável com ícone plano
- **`ArticleBlockItem`:** é um `BlockItem` com chave de dica e `appendHoverText` → `TooltipItem.addHint` (`item/ArticleBlockItem.java:16-34`).
- **Criação:** em `ModItems.article()`, os artigos colocáveis viram `ArticleBlockItem(ModBlocks.PLACEABLE_ARTICLES.get(name).get(), props, "tooltip.cultivo."+name)` (`registry/ModItems.java:94-96`). Empilham 1, exceto os listados em `STACKABLE_ARTICLES` (`:31,92`). Todos saem de `ContentIds.ARTICLES` (`:57-58`).
- **Listas em `ContentIds`:** `ARTICLES` em `core/ContentIds.java:30-32` e `PLACEABLE_ARTICLES` em `:55`. `allBlocks()` = lavouras + `blocksWithItems` + colocáveis (`:188-190`).
  - `flatItems()` inclui `ARTICLES` (`:237-246`), então o ícone plano sai em `textures/item/<id>`.
  - Em `DataConsistencyTest.articlesArePartitionedByUse` (`test/.../data/DataConsistencyTest.java:535-550`), cada artigo tem exatamente um papel. A lista colocável fica fixa em `["rolling_tray","jar","ashtray"]` (`:549`).
  - Se a estufinha entrar em `ARTICLES` e `PLACEABLE_ARTICLES`, ela herda ícone, nome, loot, aba e as checagens desses testes.
- **Gerador:**
  - `placeables = M.articles.filter(a => a.placeable)` (`tools/gen/generate.mjs:46`);
  - modelos e blockstate (`:86-93`), modelo de item plano (`:96-99`), definição de item (`:141`);
  - nome `block.cultivo.<id>` (`:236`), loot `dropsSelf` (`:338-340`).
- **Manifesto:** entradas com `placeable: true` (`tools/gen/manifest.json:75-77`, comentário em `:79`) e `mineable` (`:81-85`).
- **Teste do ícone plano:** `AssetCompletenessTest.placeableArticlesKeepFlatIconsAndHaveBlockModels` (`:241-256`) exige:
  - o item `minecraft:item/generated`;
  - o bloco com parent `minecraft:block/block` e elementos;
  - só texturas `cultivo:block/`.

## 4. Dicas de tooltip (220 px)
- **`TooltipHints.LINES`** guarda o número de linhas por chave base (`core/TooltipHints.java:15-25`, o pote com 4 em `:24`). `lineKeys` lança exceção se a chave não estiver registrada (`:30-36`).
- **`TooltipItem.addHint`** escreve as linhas em `DARK_GRAY` (`item/TooltipItem.java:29-33`).
- **No manifesto**, a dica é uma lista em `lang.en_us` e `lang.pt_br` (pote em `manifest.json:301,345`), expandida em `<chave>.1..n` (`generate.mjs:221`).
- **Testes:** `TooltipHintsTest` com `MAX_LINE_PIXELS = 220` e medidor `guiWidth` (`:22-44`).
  - `tooltipLinesFitTheScreen` (`:102-130`) mede o pt gerado e o en do manifesto.
  - `hintsNameTheButtonAndTheGesture` (`:136`) pede que a dica diga o botão e o gesto, com nomes em minúsculas.
  - `DataConsistencyTest.everyNonSmokingArticleHasAUseTooltip` (`:573-596`).

## 5. Sons
- **Registro:** `ModSounds.register(id)` = `SoundEvent.createVariableRangeEvent(Cultivo.id(id))` (`registry/ModSounds.java:53-55`), com campos como `JAR_OPEN` (`:39`).
- **Uso:** sempre `level.playSound(null, pos, ModSounds.X.get(), SoundSource.BLOCKS, vol, pitch)` no servidor (`JarBlock.java:116`).
- **Lista de IDs:** `ContentIds.SOUNDS` (`core/ContentIds.java:65-81`).
- **Manifesto:** `sounds` em `manifest.json:96-113`, com `id`, `event` vanilla, `volume`, `pitch`, `en`, `pt`. O gerador escreve `sounds.json` com `"type":"event"` e a legenda `subtitles.cultivo.<id>` (`generate.mjs:238,256-263`).
- **Testes:**
  - `SoundsTest`: `ModSounds` registra exatamente `ContentIds.SOUNDS`, na ordem (`:45-47`), e confere o tamanho 16 (`:46`);
  - todo `ModSounds.X.get()` usado precisa estar registrado (`:93-110`);
  - nenhum `SoundEvents.` fora de `gametest/` (`:117-125`);
  - o GameTest `sound_events_registered` também confere o 16 (`CultivoGameTests.java:2070`).

## 6. Propriedades de blockstate em `ContentIds`
- **Valores comuns:** `HORIZONTAL_FACINGS` e `BOOLEAN_VALUES` (`core/ContentIds.java:87-89`); `STATION_STATE_PROPERTIES` (`:96-98`); `GRINDER_PROPERTY` (`:101`).
- **`stateProperties(blockId)`** é o espelho por bloco: o `switch` por artigo em `:214-220` (pote `filled`, cinzeiro `ash`, bandeja `facing`). A estufinha precisa de `half` (`lower`/`upper`) mais o estado da planta.
- **Quem lê esse espelho:**
  - `AssetCompletenessTest.multipartConditionsUseExistingProperties` (`:109-140`) e as asserções literais em `:143-147`;
  - o GameTest `blockStatesMatchIds` compara com a `StateDefinition` registrada (`CultivoGameTests.java:2821-2836`).
- **Detalhe:** o javadoc da lista `GAME_TESTS` está fora de lugar, em cima de `SERENE_SEASONS_CROP_SEASONS` (`ContentIds.java:103-110`).

## 7. Abas criativas
- **Aba "Cultivo":** `ModCreativeTabs.CULTIVO` com `displayItems` → `ContentIds.mainTabItems()` (`registry/ModCreativeTabs.java:23-29`, `accept` em `:41-43`). A ordem vem de `ContentIds.mainTabItems()` (`:256-262`): estações, planta silvestre, resina, haxixe, baseado e artigos.
- **GameTest `creativeTabsSplit`** (`CultivoGameTests.java:1474-1497`) exige que a aba seja igual a `mainTabItems()`.

## 8. Receitas e avanços de receita
- **Fonte pura:** `ArticleRecipes` (`core/ArticleRecipes.java:49-60` artigos, pote em `:59`; estações em `:63-66`; `ALL` em `:77-83`; `shaped` em `:87-89`).
- **Manifesto:** `crafting` (`manifest.json:131-...`, pote em `:141`).
- **Gerador:**
  - shaped, transmute ou shapeless (`generate.mjs:380-410`);
  - avanço `advancement/recipes/misc/<id>.json` com `has_<ingrediente>` + `has_the_recipe`, numa só lista de requisitos e `rewards.recipes` (`:413-435`).
- **Testes:**
  - `DataConsistencyTest.everyArticleStationAndJointHasCraftingRecipe` confere `ALL.size() == 11 + 3 + 40 + 1` (`:71-82`) e muda com uma receita nova;
  - `craftingJsonMatchesArticleRecipes` (`:86-128`), `sampleItemsSatisfyTagIngredients` (`:174`), `everyCraftingRecipeHasUnlockAdvancement` (`:406`);
  - GameTest `article_recipes` (helpers `toInput` em `CultivoGameTests.java:538`).

## 9. Tabelas de loot dos blocos colocáveis
- **`dropsSelf`** (`generate.mjs:321-325`): pool com `survives_explosion` e um item. É usado pelos colocáveis (`:338-340`).
- **Pool condicional:** existe o exemplo `match_block` + `state` na peneira (`:330-334`).
- **Bloco de 2 de altura (do vanilla, não existe no mod):** `data/minecraft/loot_table/blocks/oak_door.json`, no jar client-extra, coloca na entrada a condição `{"type":"minecraft:match_block","blocks":...,"state":{"half":"lower"}}`.
- **Testes:**
  - `DataConsistencyTest.everyBlockHasLootTable` (`:324-329`) e `lootTablesUse263FieldNames` (`:332`);
  - GameTest `placeableArticlesDropSelf` (`CultivoGameTests.java:2022-2058`): coloca pelo item, `destroyBlock`, e espera exatamente 1 do próprio item e 0 outros.
  - Uma estufinha com planta dentro precisa decidir se derruba a planta junto. Esse teste exige "só a si mesmo", mas o pote derruba o conteúdo via `preRemoveSideEffects`.

## 10. Bloco de 2 de altura (referência vanilla 26.3)
- **`DoublePlantBlock`** (`/tmp/forge-src/net/minecraft/world/level/block/DoublePlantBlock.java`):
  - `HALF = BlockStateProperties.DOUBLE_BLOCK_HALF` (`:28`);
  - `updateShape`: a metade some quando a outra sai (`:36-56`);
  - `getStateForPlacement` checa `pos.getY() < level.getMaxY()` e se o bloco de cima `canBeReplaced` (`:59-63`);
  - `setPlacedBy` coloca a metade `UPPER` (`:66-69`);
  - `canSurvive` da metade de cima exige a de baixo (`:72-80`);
  - `playerWillDestroy` + `preventDropFromBottomPart` evitam o drop duplo no criativo (`:93-128`);
  - `playerDestroy` passa AIR (`:106-115`).
- **`DoorBlock`** (`.../DoorBlock.java`): `HALF` em `:43`, `updateShape` em `:78-95`, `playerWillDestroy` em `:112`, `getStateForPlacement` em `:129`, `setPlacedBy` em `:146`, `canSurvive` em `:231-234`.
- **Blockstate:** `assets/minecraft/blockstates/peony.json` usa `variants` com `half=lower` / `half=upper`.
- **Luz roxa:** `BlockBehaviour.Properties#lightLevel(ToIntFunction<BlockState>)` (`.../BlockBehaviour.java:1171`).

## 11. GameTests: helpers e convenções
- **Registro:** `TESTS = DeferredRegister.create(Registries.TEST_FUNCTION, ...)` (`CultivoGameTests.java:103`), com `TESTS.register("<nome>", () -> CultivoGameTests::x)` em `:107-168`.
- **Lista de nomes:** `ContentIds.GAME_TESTS` (`ContentIds.java:110-172`).
- **Manifesto:** `game_tests` com `max_ticks` (`manifest.json:198-...`; `sky_access` opcional, escrito em `generate.mjs:564-573`). Testes que conferem: `DataConsistencyTest.everyGameTestHasInstance` (`:381`) e `GameTestRegistrationTest`.
- **Helpers:**
  - `check` lança `Failure` e `run` captura e chama `helper.fail(Component.literal(...))` (`:185-198`);
  - `SOIL` (4,1,4) e `PLANT` (`:171-172`); `plantOnFarmland` (`:204-207`);
  - `harvest` = `Block.getDrops(state, level, pos, null)` (`:218-222`); `withQuality` (`:235`).
- **Clique real:**
  - `rightClick` monta `BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)` e chama `player.gameMode.useItemOn(player, level, stack, hand, hit)`, tentando a mão principal e depois a secundária (`:1069-1081`);
  - colocar pelo item: `placeWithItem` mira o topo de uma pedra com `setYRot` (`:2004-2016`).
- **Jogadores:**
  - `helper.makeMockServerPlayer(GameType.SURVIVAL)` com cast para `ServerPlayer` (`:1091`);
  - criativo: `makeMockPlayer(GameType.CREATIVE)` + `GameType.CREATIVE.updatePlayerAbilities` (`:885-886`);
  - `capturingServerPlayer` guarda mensagens (`:1737`);
  - `setSelectedSlot(8)` mantém a mão principal vazia (`:1094`).
- **Assíncrono:** `helper.runAfterDelay` / `succeedWhen` (`:1197,1221`).

## 12. Pacote client: o renderer da prensa e os registros
- **`PressPlatenRenderer implements BlockEntityRenderer<ProcessingStationBlockEntity, PressPlatenRenderer.State>`** (`client/PressPlatenRenderer.java:43`). A API exata do 26.3:
  - `State extends BlockEntityRenderState`, com um `BlockModelRenderState` (`:59-63`); `createRenderState()` em `:71-73`;
  - construtor: `context.blockModelResolver()` (`:54-56`);
  - `extractRenderState(BE, State, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay)` chama primeiro `BlockEntityRenderer.super.extractRenderState(...)` (`:86-88`), depois `state.platen.clear()` ou `models.update(state.platen, blockState, DISPLAY_CONTEXT)` (`:94,100`);
  - `DISPLAY_CONTEXT = BlockDisplayContext.create()` (`:50`);
  - `submit(State, PoseStack, SubmitNodeCollector, CameraRenderState)` faz `pushPose`, `translate`, `state.platen.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0)` e `popPose` (`:104-112`);
  - `getViewDistance()` = distância de renderização × 16 (`:81-83`), conferido por `ClientIsolationTest.pressPlatenIsDrawnUpToTheRenderDistance` (`:176-183`).
  - `BlockModelResolver.update(renderState, BlockState, BlockDisplayContext)` (`/tmp/forge-src/.../BlockModelResolver.java:21`) aceita qualquer `BlockState` registrado. Com isso, a estufinha pode desenhar o estado real da lavoura (`crop(variety, age)`) sem modelos novos.
- **Estado sintético:** `PLATEN_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR).add(FACING).create(Block::defaultBlockState, BlockState::new)` (`:47-49`), com o nome `Cultivo.id("press_platen")` (`:45`).
- **`CultivoClientEvents`:**
  - `@Mod.EventBusSubscriber(modid = Cultivo.MOD_ID, value = Dist.CLIENT)` (`client/CultivoClientEvents.java:16`);
  - `FMLClientSetupEvent` (`:22-24`);
  - `EntityRenderersEvent.RegisterRenderers` → `event.registerBlockEntityRenderer(ModBlockEntities.PROCESSING_STATION.get(), PressPlatenRenderer::new)` (`:28-30`). Há um renderer por tipo de BlockEntity, então a estufinha precisa de tipo e renderer próprios;
  - `ModelEvent.RegisterModelStateDefinitions` → `event.register(PLATEN_STATES, PLATEN_DEFINITION)` (`:34-36`).
- **Gerador da placa:** `pressPlatenModel` e `pressPlatenBlockstate` (`tools/gen/models.mjs:614-630`), escritos em `generate.mjs:128-129`.
- **Isolamento do cliente (`ClientIsolationTest`):**
  - o código comum nunca cita `CultivoClientEvents` (`:94-103`);
  - a única ponte para `cultivo.client.` é `SmokingItem#initializeClient`, com `bridges == 1` (`:64-91`);
  - o bytecode comum não pode referenciar `net/minecraft/client` (`:120`).

## 13. Pipeline do gerador
- **`generate.mjs`:**
  - lê o manifesto antes de `rmSync(OUT)` (`:15,48`); texturas em `:49`;
  - lavoura: 8 modelos `minecraft:block/crop` por família, blockstate `age=0..7` (`:55-67`);
  - estações e artigos com modelos 3D (`:73-93`), itens planos (`:96-102`);
  - definições de item, inclusive `composite` e `select` (`:132-211`);
  - lang com checagem de paridade pt/en e `portuguese_everywhere` escrevendo pt no `en_us.json` (`:218-252`);
  - sons (`:256-263`); loot das lavouras com `set_harvest_quality` (`:266-306`);
  - tags (`:461-498`); `test_instance` (`:564-573`).
- **`models.mjs`:**
  - `box(from, to, texture, {grain, faces, uv, cull, rotation, shade})` coloca o cullface só nas bordas do bloco (`:54-74`);
  - `blockParent` = parent `minecraft:block/block` (`:82`); `whenOf` para multipart (`:228`);
  - `articleModels()` (`:256-346`): `base` e `parts` com `when`. O pote mostra vidro cutout e o conteúdo atrás dele (`:302-318`);
  - `articleBlockstate`: variantes por facing, ou multipart base + partes (`:352-370`);
  - `FACING_Y` em `:80`. Coordenadas de elementos ficam em [-16, 32] e UV em [0, 16] (`AssetCompletenessTest.modelElementsStayWithinLimits`, `:258-294`). Um modelo de bloco único só pode chegar a 32, então duas metades com `half` é o mais simples.
- **`textures.mjs`:**
  - grades 16×16, `.` = transparente, cores pela paleta (`tools/art/textures.mjs:3-31`). Itens ficam em `ITEMS` (`:640`, pote em `:839`), blocos em `BLOCKS` (`:988`; `jar_glass` cutout em `:1084-1104`), materiais por `material(seed, base, marks)` e `grainGrid` (`:1145-1156`, `:911`);
  - animação por `renderAnimation` (tira vertical + `.png.mcmeta` com `frametime`, `:40-63`) e registro em `ANIMATED` (`:1259-1267`);
  - `generateAll` (`:1304-1328`).
- **Regras de alfa:**
  - alfa só 0 ou 255 em `block/` e `item/` (`TextureArtTest.blockAndItemTexturesHaveNoPartialAlpha`, `:91-106`). Os ícones de efeito são exceção (`'#e8e8e8c8'` em `:1290`);
  - `noOrphanTextures` exige que toda textura seja citada por um modelo (`AssetCompletenessTest.java:451`);
  - `AnimatedTexturesTest.theRequestedAnimationsExist` compara com um `EXPECTED_FRAMES` fixo (`:47-49`), então animação nova (luz roxa pulsando) entra ali também;
  - a camada de render vem da transparência da textura (NEXT_SESSION §6). Não há modelo emissivo no mod; o brilho da luz roxa sairia de `lightLevel`.

## 14. Manifesto e português
Itens, blocos, legendas e dicas vão no manifesto em `en` e `pt` (`generate.mjs:234-238`; `lang.en_us` e `lang.pt_br` em `manifest.json:263,307`). O gerador para se uma chave faltar em uma das línguas (`generate.mjs:243-248`). Com `portuguese_everywhere: true` (`manifest.json:4`), o `en_us.json` sai igual ao pt.

## 15. Números fixos que mudam com a estufinha
- `DataConsistencyTest`: total de receitas `11 + 3 + 40 + 1` (`:81`) e a lista colocável literal (`:549`).
- Contagem de sons 16: `SoundsTest.java:46` e `CultivoGameTests.java:2070`.
- `AssetCompletenessTest.java:143-147`: propriedades de blockstate literais.
- `AnimatedTexturesTest`: `EXPECTED_FRAMES`.

---

# Grow tent ("estufinha"): Forge 26.3 / MC 26.3 API notes

Everything below comes from decompiled sources. `F` means `/tmp/forge-src/`. Vanilla JSON is read from `client-extra-26.3-20260918.230105.jar`, written `CE:<path>`. I modified no files and ran nothing.

## 1. A 2-block-tall block (door, tall flower, pitcher)

**Property**
- `BlockStateProperties.DOUBLE_BLOCK_HALF` is an `EnumProperty<DoubleBlockHalf>` (`F/net/minecraft/world/level/block/DoublePlantBlock.java:28`).
- `DoubleBlockHalf` has `UPPER` and `LOWER`, serialized as `"upper"` and `"lower"`, plus `getOtherHalf()` and `getDirectionToOther()` (`F/.../state/properties/DoubleBlockHalf.java:6-32`).
- Register it in `createBlockStateDefinition` (`DoublePlantBlock.java:131-133`). The default state is `LOWER` (`DoublePlantBlock.java:32`).
- Trap: `createBlockStateDefinition` runs inside `Block`'s constructor (`F/.../block/Block.java:233-236`). Use static properties only.

**Checking space above when placing**
- Pattern: `pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context) ? <state> : null` (`DoublePlantBlock.java:58-63`, `DoorBlock.java:129-143`, which sets `HALF=LOWER` at :139).
- `BlockItem` fails if `getPlacementState` returns null (`F/.../world/item/BlockItem.java:71-74`).

**Placing the upper half**
- `BlockItem` calls `setPlacedBy` after `placeBlock` (`BlockItem.java:76-89`).
- The door does `level.setBlockAndUpdate(pos.above(), state.setValue(HALF, UPPER))` (`DoorBlock.java:146-148`). `DoublePlantBlock` does the same with waterlogging copied (`DoublePlantBlock.java:65-69`).
- Vanilla doors and tall flowers use `DoubleHighBlockItem`. Before calling `super.placeBlock`, it clears the space above with flags 27 (`F/.../world/item/DoubleHighBlockItem.java:15-22`). It is registered at `Items.java:1336-1337` (oak door) and `Items.java:1038` (sunflower).
- `DoublePlantBlock.placeAt(level, state, lowerPos, flags)` places both halves for worldgen or commands (`DoublePlantBlock.java:82-86`).

**`updateShape`: each half dies without its partner**
- `DoublePlantBlock.java:35-56`:
  - If the neighbour on the "other half" side is no longer this block with the opposite half, return `AIR`.
  - If this is the lower half, the neighbour is below, and `!canSurvive`, return `AIR`.
- The door variant is at `DoorBlock.java:78-96`. It also copies the door's properties onto the other half (:94-95).
- Returning `AIR` reaches `Block.updateOrDestroy`, which calls `level.destroyBlock(pos, (flags & 32) == 0, ...)` and so drops loot unless flag 32 is set (`Block.java:214-231`).
- `Level.destroyBlock` passes the block entity to `dropResources` (`F/.../world/level/Level.java:297-316`, BE passed at :305).

**`playerWillDestroy` without duplicate drops**
- Door, the recommended model (`DoorBlock.java:111-118`):
  - On the server, only if `player.preventsBlockDrops()` (creative) or the player lacks the correct tool, it calls `DoublePlantBlock.preventDropFromBottomPart`.
  - That method (`DoublePlantBlock.java:117-128`) sets the lower half to air or water with flags `35` (3 | `UPDATE_SUPPRESS_DROPS` = 32, `Block.java:96`). It fires `levelEvent 2001` for the break particles.
  - In survival: breaking the upper half drops its own loot, which is empty because of the `half=lower` condition. The lower half then dies through `updateShape` and drops the item, once.
- `DoublePlantBlock` instead drops in `playerWillDestroy` (`:92-103`, calling `dropResources(state, level, pos, null, ...)` with a null BE). It then calls `super.playerDestroy` with `AIR` so nothing drops a second time (`:105-115`).
  - For a BE block, prefer the door pattern. The DoublePlant path passes `null` instead of the block entity.

**Loot tables** (both halves share one table; the item entry is conditioned on `half=lower`)
- `CE:data/minecraft/loot_table/blocks/oak_door.json` and `CE:.../sunflower.json` both have:
  - a pool with `"condition":{"type":"minecraft:survives_explosion"}`;
  - an entry with `"condition":{"type":"minecraft:match_block","blocks":"minecraft:oak_door","state":{"half":"lower"}}`.
- These use 26.3 field names (`condition`, `type`). `tall_grass.json` also adds a `location_check` with `offsetY` ±1 on each half's pool.

**`canSurvive`**
- Door: the lower half needs `belowState.isFaceSturdy(level, below, UP)`; the upper half needs `belowState.is(this)` (`DoorBlock.java:230-235`).
- `DoublePlantBlock`: the upper half checks for a lower half below. Forge's pre-check returns `super` when the state isn't placed yet (`DoublePlantBlock.java:71-80`, comment at :78).
- The base method is `BlockBehaviour.java:305`. Use the door rule for the tent: sturdy floor, no farmland or water check.

**Blockstate JSON:** `CE:assets/minecraft/blockstates/sunflower.json` maps `half=lower` to `sunflower_bottom` and `half=upper` to `sunflower_top`. Doors add facing (`oak_door.json`).

## 2. Block entity only on the lower half, clicks routed from the upper half

**Upper half without a BE**
- `BlockState.hasBlockEntity()` is simply `getBlock() instanceof EntityBlock` (`BlockBehaviour.java:881-883`).
- `LevelChunk.setBlockState` calls `newBlockEntity(pos, state)` and skips it when it returns null (`F/.../chunk/LevelChunk.java:330-341`). `createBlockEntity` does the same (:358-361).
- So `newBlockEntity` should return `null` when `state.getValue(HALF) == UPPER`. The interface is `EntityBlock.java:14`.
- `getTicker` can likewise return null for `UPPER` (`EntityBlock.java:16`, `BlockBehaviour.java:889-891`).

**Removal side effects**
- `preRemoveSideEffects` runs when the block changes, on the server, unless flag 256 (`UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS`, `Block.java:99`) is set (`LevelChunk.java:304-315`).
- The default implementation drops container contents (`F/.../block/entity/BlockEntity.java:243-247`). Campfire example: `CampfireBlockEntity.java:199-204`.
- Trap: the creative path in `preventDropFromBottomPart` uses flags 35, which does not include 256. `preRemoveSideEffects` still runs there, so a BE that drops its plant must handle that case itself.

**Routing clicks to the lower half**
- `useItemOn` and `useWithoutItem` receive the clicked position (`BlockBehaviour.java:191-197`, dispatch at :807-814).
- Vanilla pattern, `PitcherCropBlock.getLowerHalf` (`F/.../block/PitcherCropBlock.java:188-196`): if this is the lower half, use `pos`; otherwise use `pos.below()` if it is a lower half; otherwise null.
- `BedBlock` resolves its other half with `pos.relative(...)` (`BedBlock.java:49`).
- Do all reads and writes through `level.getBlockEntity(lowerPos)`.

**BE render culling**
- Forge's default `IForgeBlockEntity.getRenderBoundingBox()` is the lower block's collision-shape bounds (`F/net/minecraftforge/common/extensions/IForgeBlockEntity.java:78-106`).
- The frustum uses it (`F/.../client/renderer/extract/LevelExtractor.java:326,356`).
- For a renderer that draws into the upper block, override it to cover 1×2×1. `SignBlockEntity.java:285` is a vanilla override.

## 3. Light emission and full-bright faces

**World light**
- `BlockBehaviour.Properties.lightLevel(ToIntFunction<BlockState>)` (`BlockBehaviour.java:1171-1174`).
- The value is cached per state (`:475`, read at `:591`), so it can vary only by blockstate properties.
- Examples: torch `lightLevel(state -> 14)` (`Blocks.java:1349`), `litBlockEmission(13)` (`Blocks.java:1448`).
- The light engine re-checks when light properties change (`LevelChunk.java:295-301`).
- Forge also has `IForgeBlock.getLightEmission(state, level, pos)` (`IForgeBlock.java:105-106`). A state property is the safe route.

**Whole block full-bright**
- `Properties.emissiveRendering(Predicate<BlockState>)` (`BlockBehaviour.java:1269-1272`), used by magma at `Blocks.java:3865`.
- It forces light coordinates to 15728880 (`F/net/minecraft/util/LightCoordsUtil.java:112-114`). In BER paths it applies through `BlockModelResolver.java:24`.

**Per-face emissive in model JSON (new in 26.3)**
- An element-level `"light_emission": 0..15` field. It is parsed in `F/net/minecraft/client/resources/model/cuboid/CuboidModelElement.java:43-75` and throws outside 0..15 (:70).
- It is baked into `BakedQuad.MaterialInfo.lightEmission` (`FaceBakery.java:63,73-74`, `BakedQuad.java:64-99`).
- It is applied per vertex in both chunk and BER rendering (`F/com/mojang/blaze3d/vertex/VertexConsumer.java:70-76,87-93,105-113`, `QuadInstance.java:41`).
- Vanilla pattern, `CE:assets/minecraft/models/block/cross_emissive.json`:
  - two normal cross planes with `#cross`;
  - two duplicate planes with `"light_emission": 15` and texture `#cross_emissive` (mostly transparent, only the glowing pixels opaque);
  - `"shade_direction_override": "up"` on every element.
- Used by `firefly_bush.json` and `open_eyeblossom.json` (`flower_pot_cross_emissive.json` for the potted version). For the tent, put the purple LED strip on an element with `"light_emission": 15`.

**Animated textures**
- A vertical strip with `.png.mcmeta`, for example `CE:assets/minecraft/textures/block/firefly_bush_emissive.png.mcmeta` = `{"animation":{"frametime":3}}` and `sea_lantern.png.mcmeta` (`frametime` 5).
- Optional `{"texture":{"mipmap_strategy":"strict_cutout"}}` (`open_eyeblossom.png.mcmeta`).
- Animated sprites get a flag (`BakedQuad.java:105-109`).

## 4. Client particles

**`animateTick`**
- Signature: `public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random)`, a no-op by default (`Block.java:358-359`).
- Only the client calls it: `ClientLevel.animateTick` at `F/.../client/multiplayer/ClientLevel.java:569`, calling `getBlock().animateTick` at :607.
- It uses only `Level#addParticle`, no client classes, so it can stay in the block class. Examples: `RedStoneOreBlock.java:95-115`, `FireflyBushBlock.java:28-40`.

**`DustParticleOptions`**
- `new DustParticleOptions(int rgb, float scale)` (`F/net/minecraft/core/particles/DustParticleOptions.java:26-29`). The colour is a packed RGB24 int; the old `Vector3f` constructor is gone.
- Scale is clamped to 0.01–4.0 (`ScalableParticleOptionsBase.java:8-10`).
- Usage: `level.addParticle(new DustParticleOptions(color, 1.0F), x, y, z, 0, 0, 0)` (`RedstoneWireBlock.java:425`, `LeverBlock.java:101`).
- For purple, use something like `0x9B30FF`. `ColorParticleOption.create(type, argb)` also exists (`Blocks.java:715`).

## 5. Showing the strain's crop inside the tent

### Option A: block entity renderer

The project already does this in `src/main/java/io/github/peixeangola/cultivo/client/PressPlatenRenderer.java`:
- It takes `context.blockModelResolver()` (:54-56).
- It keeps a `BlockModelRenderState` in the render state (:59-63).
- In `extractRenderState` it calls `models.update(renderState, someBlockState, BlockDisplayContext.create())` (:50, :100).
- In `submit` it does `pushPose`, translate, `renderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0)`, `popPose` (:104-112).
- It is registered in `CultivoClientEvents.java:28-29`.

Supporting API:
- `BlockModelResolver.update` resolves any real `BlockState` through `modelManager.getBlockModelSet().get(blockState)` (`F/.../client/renderer/block/BlockModelResolver.java:21-25`).
- `MushroomCowRenderer.java:40-59` is a vanilla precedent for drawing another block's model; `CarriedBlockLayer.java:38` submits one.
- `BlockModelRenderState` picks a cutout or translucent item sheet (`BlockModelRenderState.java:59-62`). It merges external light with the block's own light (`:81,108`) and submits through `submitBlockModel` (`:112-115`).
- Base light comes from `BlockEntityRenderState.extractBase`, `lightCoords = LightCoordsUtil.getLightCoords(level, pos)` (`BlockEntityRenderState.java:21,30`).

For the tent:
- Pass `ModBlocks.CROPS.get(variety).get().defaultBlockState().setValue(CropBlock.AGE, age)`. `ModBlocks.CROPS` is an `EnumMap<Variety, RegistryObject<CannabisCropBlock>>` (`ModBlocks.java:29-36`).
- The existing 40 × 8 crop blockstates and models are reused (generated `assets/cultivo/blockstates/<strain>_crop.json`, `age=0..7`), with no new assets.
- The crop model spans y −1..15 (`CE:assets/minecraft/models/block/crop.json`). Translate by +1/16 to sit on the tent floor.
- Costs: the BE needs client sync (`getUpdatePacket`/`getUpdateTag` + `sendBlockUpdated`, as in NEXT_SESSION §6). Rendering is per frame, culled at the default 64-block view distance (`BlockEntityRenderer.java:34-40`). Override the render bounding box (see 2).
- The `crop` texture and 26.3 per-quad light both work in a BER (`VertexConsumer.java:70`).

### Option B: blockstate-driven (no renderer, rendered with the chunk)

- Properties on the tent block: `HALF`, plus a strain index as `IntegerProperty.create("strain", 0, 40)` (0 = empty; min ≥ 0 and max > min, `IntegerProperty.java:16-20,47`), plus `AGE` 0–7.
  - An `EnumProperty` would need a `StringRepresentable` enum, and `core/Variety` must stay free of Minecraft classes.
- That is 2 × 41 × 8 = 656 states, or ×4 with facing (2624).
- Blockstate JSON: a `multipart` like `CE:assets/minecraft/blockstates/chiseled_bookshelf.json` (`"when"` with `"AND"`). The shell model applies always per half. The crop model `cultivo:block/<strain>_crop_stage<age>` applies `when {half: lower, strain: N, age: A}`, for 320 entries in one generated file.
- No new models, no renderer, no BE sync for visuals, and free at runtime. Costs:
  - large generated JSON;
  - the `Variety` ↔ index mapping has to stay stable, and changing the strain list changes saved block states;
  - the gene list is baked into the block definition.
- The vanilla alternative is one block per contents (`FlowerPotBlock` with `POTTED_BY_CONTENT`, `FlowerPotBlock.java:31-38,69-110`). That would mean 40 tent blocks (×2 halves) swapped on planting, which is heavier on registry, loot and assets.

## 6. Random ticks vs a block entity ticker

**Random ticks**
- `Properties.randomTicks()` (`BlockBehaviour.java:1189-1190`), or override `protected boolean isRandomlyTicking(BlockState)` (`:407-408`) plus `protected void randomTick(BlockState, ServerLevel, BlockPos, RandomSource)` (`:337-338`).
- The result is cached per state in `initCache` (`BlockBehaviour.java:520-522`, read at :897-898). It can depend only on the state, not on BE data.
- `ServerLevel` picks random positions per section and calls `randomTick` only if `blockState.isRandomlyTicking()` (`F/.../server/level/ServerLevel.java:541-551`).
- Vanilla 2-tall example, `PitcherCropBlock`:
  - `isRandomlyTicking`: `HALF == LOWER && !isMaxAge` (`:135-138`);
  - `randomTick`: `random.nextInt((int)(25 / growthSpeed) + 1) == 0` (`:140-147`);
  - `grow` writes the lower half with flag 2 and updates the upper half (`:149-158`).
- The project's crop uses the same hook (`block/CannabisCropBlock.java:40-55`, `GrowthRules.growthRollBound`, `ForgeHooks.onCropsGrowPre/Post`).
- With random ticks, keep `age` (or `planted`) as a blockstate property so that `isRandomlyTicking` can be false when the tent is empty or the plant is mature. Inside `randomTick`, `level.getBlockEntity(pos)` gives the strain data.

**Block entity ticker**
- `EntityBlock.getTicker(level, state, type)` (`EntityBlock.java:16`) with `BaseEntityBlock.createTickerHelper(actual, expected, ticker)` (`BaseEntityBlock.java:30-34`).
- Server-only and state-gated: Campfire returns a ticker only on `ServerLevel` and only when `LIT` (`CampfireBlock.java:310-318`).
- The ticker is re-evaluated when the state changes (`LevelChunk.java:343-345`, `updateBlockEntityTicker`).
- A ticker runs every tick, which suits a deterministic counter. Random ticks give crop-like growth for free.

**Recommendation:** random ticks on the lower half, gated by state, which matches the "behaves like the crop" rule and PitcherCrop. Use the BE only for strain identity and client sync, or drop it entirely under option B.

## Hooks in this project to change (for the implementer)

- Greenhouse quality is decided in `loot/SetHarvestQualityFunction.java:42` (`GreenhouseDetector.isGreenhouse`).
- Greenhouse growth speed is decided in `block/CannabisCropBlock.java:48`.
- Quality constants: `core/QualityRules.java:10-11` (`OUTDOOR_BASE = 2`, `GREENHOUSE_BASE = 4`) and `harvestQuality(Variety, boolean, double)` at :22.
- Disabling the glass greenhouse means removing or neutralizing those two `GreenhouseDetector` calls and their GameTests (`gametest/CultivoGameTests.java:261,284,294`).

---

# Test and visual-probe checklist for a new 2-block `grow_tent`

Everything here comes from reading the code. Nothing was edited and nothing was run. Line numbers are for the current files. Two outputs printed several files together, so I adjusted those line numbers to each file: `ParodyNamesTest` (shown number minus 222) and `GameTestRegistrationTest` (minus 129).

## 1. ContentIds (`src/main/java/io/github/peixeangola/cultivo/core/ContentIds.java`)
- **Pick one category.** The tent has to live in one of two places:
  - **Option A (recommended): a new constant `GROW_TENT`** added to `blocksWithItems()` (181-185). It then flows into `allBlocks()` (188-190) and `allItems()` (248-250).
  - **Option B: `PLACEABLE_ARTICLES`** (55). This drags in the article rules (exactly one role, a mandatory recipe, a usage tooltip, a flat icon, a `minecraft:block/block` parent) and literal lists in four tests. CLAUDE.md also fixes the list of MVP articles, so A fits better.
- **`mainTabItems()` (256-262) is built by hand**, so the tent must be added there explicitly. `StrainCatalogTest.java:166-168` compares that list as a literal, and `:172` requires every item to be in some tab. The `creative_tabs_split` GameTest repeats the check (`CultivoGameTests.java:1487`). The tab itself fills from this list (`ModCreativeTabs.java:28`).
- **`stateProperties()` (202-222) needs a `case` for the tent.**
  - `half` takes `upper` and `lower`; the vanilla order is UPPER then LOWER (`DoubleBlockHalf.java:7-8, 27`).
  - Add any other property you create (for example `facing`, or a plant/age property).
  - The `block_states_match_ids` GameTest compares this against the registered `StateDefinition` for every block in `allBlocks()` (`CultivoGameTests.java:2821-2834`).
- **`GAME_TESTS` (110-172):** every new GameTest needs its name here, in the same order as the `TESTS.register` calls.
- **`SOUNDS` (65-81):** only touch it if the tent gets new sound events (see section 2, `SoundsTest`).
- **`STATION_STATE_PROPERTIES` (96-98) and `STATIONS` (83-84):** leave them alone. The stations logic expects `facing` + `contents`.

## 2. JUnit tests a new block runs into

### `AssetCompletenessTest` (`src/test/java/io/github/peixeangola/cultivo/data/`)
- **`everyBlockHasBlockstateModelsAndTextures` (54-60), `assertModelComplete` (829-861):** needs `assets/cultivo/blockstates/grow_tent.json`.
  - Its `variants` or `multipart` must point at `cultivo:` models only (830-831).
  - Model parents may only be `minecraft:block/crop`, `cross`, `item/generated` or `block/block`, and the child must define the parent's texture variable (40-44, 836-840). A model with no parent needs `elements`.
  - Every texture must be 16×16 (892-916). An animated texture is a vertical stack of square frames.
  - Every `#ref` must resolve (881-890), and `particle` must resolve (858-860).
- **`modelElementsStayWithinLimits` (258-293):**
  - Coordinates must stay within [-16, 32]. One 32-px-tall element is allowed, but splitting lower and upper models is more conventional.
  - Faces must be non-empty and use valid names, textures must start with `#`, UVs must be within 0–16, and face rotations must be 0/90/180/270.
  - `light_emission` on an element is accepted: it exists in 26.3 (`CuboidModelElement.java:43, 64`) and the test does not reject it.
- **`everyItemHasItemDefinitionModelAndTexture` (298-304):**
  - Needs `assets/cultivo/items/grow_tent.json`.
  - The parser only knows the types `model`, `condition`, `select`, `range_dispatch` and `composite` (747-779).
  - A station-style item is a model with `parent: cultivo:block/...`; vanilla's tall plants use a flat icon instead.
- **`noOrphanTextures` (451-474):** every PNG in `textures/` must be referenced by some model.
- **`everyIdIsTranslatedInBothLanguages` (559-572):** requires `block.cultivo.grow_tent`. Option A items use `useBlockDescriptionPrefix` (`ModItems.java:69`), so the block key is enough. Under option B the item key `item.cultivo.grow_tent` is needed too.
- **`languagesHaveSameKeys` (574-579)** and **`translationKeysUsedInJavaExist` (581-608):** every `"message|tooltip|effect.cultivo..."` literal in Java must exist in the lang files. A multi-line hint needs its `.1…n` keys.
- **`multipartConditionsUseExistingProperties` (108-148):** the block list is hard-coded (STATIONS, JAR, ASHTRAY at 110-111). A tent multipart would not be checked unless you add it here. Adding it is recommended, along with a specific test like `jarAndAshtrayPartsFollowTheirState` (217-234).
- **Option B only:** `placeableArticlesKeepFlatIconsAndHaveBlockModels` (240-255) requires a flat `item/generated` icon, a block model with parent `minecraft:block/block` plus elements, and only `cultivo:block/` textures.

### `DataConsistencyTest`
- **`everyBlockHasLootTable` (323-329):** needs `data/cultivo/loot_table/blocks/grow_tent.json` with `type: minecraft:block`.
  - **`lootTablesUse263FieldNames` (331-355):** no `conditions`, `functions`, `function` or `block_state_property` keys are allowed.
  - The model to copy is vanilla `peony.json`: a `minecraft:match_block` condition with `state: {half: lower}`, inside `condition` (singular).
- **`everyArticleStationAndJointHasCraftingRecipe` (70-83):** the expected set is exactly ARTICLES + STATIONS + JOINT, and the count is the literal `11 + 3 + 40 + 1` (81). A recipe for the tent changes both: add the tent to the expected set and bump the count.
- **`craftingJsonMatchesArticleRecipes` (85-124), `sampleItemsSatisfyTagIngredients` (173-184), `everyCraftingRecipeHasUnlockAdvancement` (405-447):** the recipe exists in two places: `ArticleRecipes.java` (`ALL` at 77-83) and `manifest.crafting` (`generate.mjs:368-385`). It also needs `data/cultivo/recipe/grow_tent.json` and `advancement/recipes/misc/grow_tent.json`, with `parent: minecraft:recipes/root`, an `inventory_changed` criterion per ingredient, and a single OR requirement group.
- **`everyGameTestHasInstance` (380-393):** each GameTest needs `test_instance/<name>.json`. It is generated from `manifest.game_tests`.
- **`woodenStationsMineFasterWithAnAxe` (602-618):** the axe and pickaxe tags are compared as exact sets (606-607). Putting the tent in `manifest.mineable` means updating those sets.
- **`cropTags` (359-368):** the `crops`, `maintains_farmland` and `bee_growables` tags must equal exactly the 40 crops, so the tent stays out of them.
- **`greenhouseGlassTagHasGlassAndPanes` (370-378):** asserts the `greenhouse_glass` tag with 35 entries. It must change or go if the tag goes. The tag comes from `manifest.greenhouse_glass` via `generate.mjs:463`.
- **Option B only:** `articlesArePartitionedByUse` (534-550, literal lists at 547-549), `manifestPlaceablesMatchContentIds` (621-631), `placeableArticlesDropThemselves` (668-678) and `everyNonSmokingArticleHasAUseTooltip` (572-595, which requires `TooltipHints` lines).

### Other tests
- **`CompatibilityTest`:**
  - `sereneSeasonsTagsListEveryCropAndSeed` (55-73) requires exactly the 40 crops, so the tent stays out of the Serene Seasons tags.
  - `onlyTheFourSereneSeasonsFilesExist` (75-90) forbids a Serene Seasons `greenhouse_glass` file.
  - The `serene_seasons_compat` GameTest uses glass, but that is Serene Seasons' own greenhouse, not Cultivo's bonus (`CultivoGameTests.java:2999-3003`). Leave it.
- **`GameTestRegistrationTest:25`:** the `TESTS.register("...")` calls in `CultivoGameTests.java` must equal `ContentIds.GAME_TESTS`, in the same order.
- **`SoundsTest`:**
  - `ContentIds.SOUNDS.size()` is a literal 16 (46).
  - `ModSounds` must register exactly that list, in order (47). The manifest `sounds` list and `sounds.json` must match (48-52).
  - Each event needs `type: event`, a `minecraft:` name, volume in (0, 1], pitch in [0.5, 2], and a translated `subtitles.cultivo.<id>` (59-89).
  - Every declared sound must actually be played via `ModSounds.X.get()` (93-113).
  - No `SoundEvents.` outside `gametest/` (116-128).
- **`PortugueseEverywhereTest`:**
  - `en_us` must equal `pt_br` (89-97).
  - The manifest sections that are checked for complete pt/en are hard-coded: `strains`, `generic_items`, `articles`, `stations`, `effects`, `sounds` (77, 105). A new manifest section for the tent must be added to both lists, or its English is never checked.
  - `noEnglishWordsLeftInGameTexts` (127-145): the Portuguese text must not contain any word that appears in the English sources, such as "tent", "grow" or "light". "Estufinha" is fine.
  - Optionally add the tent's name to `reviewedPortugueseNames` (158-180).
  - No `Component.literal` (205-221).
- **`ParodyNamesTest:142-170`:** scans `src/main`, `src/generated`, `tools/gen`, `tools/art` and `tools/visual-probe` (28-34). Comments and Portuguese text must not contain real names. Ones that clash with this theme include "Purple Haze", "Purple Kush", "Blue Purple", "Northern Lights", "Skywalker", "Ultra Violet" and "THC". "roxo" and "luz roxa" are safe.
- **`TooltipHintsTest`:** only applies if the tent gets a hint.
  - Add an entry to `TooltipHints.LINES` (`core/TooltipHints.java:15-25`) and a list of the same length in both `manifest.lang` tables (79-94).
  - Each line must be at most 220 px wide (102-123) and must not join phrases with " · " (124-128).
- **`AnimatedTexturesTest:48`:** the set of animated textures must equal `EXPECTED_FRAMES` (24-31) exactly. An animated purple light has to be added there and in `textures.mjs` `ANIMATED` (1259-1267). The `.mcmeta` may contain only `animation`, with no `interpolate` (55-61), and frames must actually change (78-92).
- **`TextureArtTest:91-105`:** block and item textures may only have alpha 0 or 255. A purple glow therefore needs a solid color plus `light_emission`, not translucency.
- **`ClientIsolationTest`:**
  - Common code must not reference `net.minecraft.client` or `blaze3d`, in source (44-57) or bytecode (120-140).
  - There is exactly one bridge into `client/`: `SmokingItem#initializeClient` (89).
  - If the plant inside the tent is drawn by a BlockEntityRenderer, it must live in `client/` and be registered from `CultivoClientEvents.onRegisterRenderers` (`client/CultivoClientEvents.java:28-29`).
- **Greenhouse rules, if the glass bonus is removed or moved:**
  - `core/GreenhouseRulesTest.java` (whole file) and the `greenhouse_quality` GameTest (`CultivoGameTests.java:108, 210-215, 278-294`, plus `:261` in `outdoor_growth`).
  - `QualityRulesTest:57-60` (4–5 versus 2–3, through a boolean), `VarietyAndGrowthTest:46-53` (the greenhouse speed multiplier) and `CuringRulesTest:33-38`. These can stay if the boolean becomes "in the tent".
  - Production code that uses glass detection: `loot/SetHarvestQualityFunction.java:42` and `block/GreenhouseDetector.java`.

## 3. Generator (sources only; regenerate the output)
- **`tools/gen/manifest.json`:** the tent's pt/en names (in `articles`, `stations` or a new section), its recipe in `crafting`, `game_tests`, lang/tooltip keys, and `sounds` / `mineable` if used.
  - `greenhouse_glass` is used only by `generate.mjs:463` and the test at `DataConsistencyTest:370-378`.
- **`tools/gen/generate.mjs`:**
  - Blockstates and models live at 74-92 (stations and placeables). The tent needs its own loop.
  - Item definitions at 211, lang block names at 236, block loot at 326-339, mineable at 495-497.
- **`tools/gen/models.mjs`:** a new model function (see `articleModels`, 256-350, and `articleBlockstate`, 352). Use the `box()` helper (54).
- **`tools/art/textures.mjs`:** textures go in `BLOCKS` (988), `ITEMS` (640) and `ANIMATED` (1259). They are written by `generateAll` (1304-1328).

## 4. Visual probe (`tools/visual-probe/`)
- **`gen-datapack.mjs`:**
  - The timeline `steps` (51-58) must keep increasing times.
  - Scene E, the glass greenhouse (138-144), sits at x 39-45, z 10-16, y 150-154. It is the natural spot to replace with tents.
  - The `cam_greenhouse` camera is at 190. The `forceload` area (78) only covers 0..47.
  - `random_tick_speed 0` (64) means the scene must set ripe plants through state or NBT, not by waiting for growth.
- **Placing the tent in the scene:** use two `setblock` calls, `cultivo:grow_tent[half=lower]` first and then `[half=upper]` (`setblock` does not run `setPlacedBy`). Show an empty tent, a growing one and a ripe one standing on plain stone, with no farmland or water.
- **Other places the tent should appear:**
  - The icon showcase (`row3`, 167-168).
  - The inventory at `inventory.18` (199-233; slots 0-17 are taken). Add a tooltip-hover entry to `$tips` in `run-probe.ps1:312-316` if the tent has a hint.
  - Optionally a real-click scene, modeled on `use_rack` (251-257), for planting and for harvesting from a ripe tent.
- **`validate.mjs`:**
  - Known blocks are the files in `blockstates/`, and properties are read from the JSON (13-25).
  - Values the JSON never lists must be added by hand (26-30). A client-only synthetic blockstate must be excluded the way `press_platen` is (13-14).
  - Allowed commands are listed at 32-33 (`kill`, `data` and `give` are not among them). Slots up to `inventory.26` are allowed (34).
  - Only the components `quality`, `strain`, `filtered` and `cured` are allowed (89-98).
  - Each step needs a `say probe:<name>` (109-112).
  - `Wait-Step` calls in `run-probe.ps1` must match the timeline in the same order (114-117).
  - **No marker may be a prefix of another** (119). For example, `use_tent` next to `use_tent_harvest` fails.
- **`run-probe.ps1`:** the scene list is at 296-308; `cam_greenhouse` and shot `11_estufa` are at 304. Clicks are at 330-338. Scanning the log for missing model/texture/sound problems is automatic (367).
- **`PRINTS.md`:** row 11 "estufa" (16) needs rewriting, and new rows for any new shots.

## 5. Files to add or edit
- **Edit:**
  - `src/main/java/io/github/peixeangola/cultivo/core/ContentIds.java`
  - `core/ArticleRecipes.java` (if the tent has a recipe)
  - `core/TooltipHints.java` (if it has a hint)
  - `registry/ModBlocks.java`, `registry/ModItems.java`, `registry/ModCreativeTabs.java` (only if it doesn't keep using `mainTabItems`), `registry/ModBlockEntities.java` (if the tent has a block entity)
  - `registry/ModSounds.java` (if sounds are added)
  - `loot/SetHarvestQualityFunction.java`, `block/GreenhouseDetector.java` and `core/GreenhouseRules.java` (glass bonus removed)
  - `client/CultivoClientEvents.java` (if a renderer is added)
  - `gametest/CultivoGameTests.java`
  - `tools/gen/manifest.json`, `tools/gen/generate.mjs`, `tools/gen/models.mjs`, `tools/art/textures.mjs`
  - `tools/visual-probe/gen-datapack.mjs`, `validate.mjs` (only if properties are implied), `run-probe.ps1`, `PRINTS.md`
- **Tests to edit:**
  - `src/test/.../data/DataConsistencyTest.java` (lines 81, 370-378, and 606-607 if a mineable tag is used)
  - `core/StrainCatalogTest.java:166-168`
  - `core/GreenhouseRulesTest.java`
  - `data/SoundsTest.java:46` (if sounds are added)
  - `data/AnimatedTexturesTest.java:24-31` (if animated)
  - `data/AssetCompletenessTest.java:110-111`, plus a new test for the tent's blockstate parts
  - `data/PortugueseEverywhereTest.java:77, 105` (if the tent gets its own manifest section)
- **Add:**
  - `block/GrowTentBlock.java`, plus a block entity if it stores the strain
  - a new pure rules class in `core/` with its own JUnit test (no farmland, no water, quality 4–5, harvest leaves the tent empty)
  - a renderer in `client/` if the plant is drawn that way
- **Regenerated output under `src/generated/resources/`:**
  - `assets/cultivo/blockstates/grow_tent.json`
  - `models/block/grow_tent_*.json`
  - `models/item/grow_tent.json`
  - `items/grow_tent.json`
  - `textures/block/grow_tent_*.png` (and `.mcmeta` if animated)
  - `lang/{pt_br,en_us}.json`
  - `data/cultivo/loot_table/blocks/grow_tent.json`
  - `recipe/grow_tent.json`
  - `advancement/recipes/misc/grow_tent.json`
  - `test_instance/<new tests>.json`
  - `greenhouse_glass.json` disappears if the glass tag is removed
