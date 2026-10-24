package io.github.peixeangola.cultivo.gametest;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.AshtrayBlock;
import io.github.peixeangola.cultivo.block.GreenhouseDetector;
import io.github.peixeangola.cultivo.block.JarBlock;
import io.github.peixeangola.cultivo.block.JarBlockEntity;
import io.github.peixeangola.cultivo.block.ProcessingStationBlock;
import io.github.peixeangola.cultivo.block.ProcessingStationBlockEntity;
import io.github.peixeangola.cultivo.block.RollingTrayBlock;
import io.github.peixeangola.cultivo.block.SieveBlock;
import io.github.peixeangola.cultivo.block.StationContents;
import io.github.peixeangola.cultivo.core.ArticleRecipes;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.CuringRules;
import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.item.ArticleBlockItem;
import io.github.peixeangola.cultivo.item.SeedItem;
import io.github.peixeangola.cultivo.item.TooltipItem;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModCreativeTabs;
import io.github.peixeangola.cultivo.registry.ModEffects;
import io.github.peixeangola.cultivo.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/**
 * GameTests do Cultivo. Cada função tem um data/cultivo/test_instance/&lt;nome&gt;.json com a estrutura
 * forge:empty9x9x9. Rodar com {@code ./gradlew runGameTestServer}. As verificações são síncronas.
 */
public final class CultivoGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TESTS =
        DeferredRegister.create(Registries.TEST_FUNCTION, Cultivo.MOD_ID);

    static {
        TESTS.register("outdoor_growth", () -> CultivoGameTests::outdoorGrowth);
        TESTS.register("greenhouse_quality", () -> CultivoGameTests::greenhouseQuality);
        TESTS.register("harvest_drops", () -> CultivoGameTests::harvestDrops);
        TESTS.register("drying_station", () -> CultivoGameTests::dryingStation);
        TESTS.register("sieve_station", () -> CultivoGameTests::sieveStation);
        TESTS.register("press_station", () -> CultivoGameTests::pressStation);
        TESTS.register("station_output_full", () -> CultivoGameTests::stationOutputFull);
        TESTS.register("station_persistence", () -> CultivoGameTests::stationPersistence);
        TESTS.register("village_chest_seeds", () -> CultivoGameTests::villageChestSeeds);
        TESTS.register("article_recipes", () -> CultivoGameTests::articleRecipes);
        TESTS.register("joint_recipe_copies_quality", () -> CultivoGameTests::jointRecipeCopiesQuality);
        TESTS.register("smoke_joint_effects", () -> CultivoGameTests::smokeJointEffects);
        TESTS.register("smoke_requires_lighter_and_fuel", () -> CultivoGameTests::smokeRequiresLighterAndFuel);
        TESTS.register("smoke_pipe_uses_fuel", () -> CultivoGameTests::smokePipeUsesFuel);
        TESTS.register("smoke_bong_hash_stronger", () -> CultivoGameTests::smokeBongHashStronger);
        TESTS.register("smoke_stacking_and_cap", () -> CultivoGameTests::smokeStackingAndCap);
        TESTS.register("smoke_resmoke_after_heavy", () -> CultivoGameTests::smokeResmokeAfterHeavy);
        TESTS.register("smoke_at_cap_refused", () -> CultivoGameTests::smokeAtCapRefused);
        TESTS.register("smoke_hash_multiplier", () -> CultivoGameTests::smokeHashMultiplier);
        TESTS.register("smoke_offhand_fuel_first", () -> CultivoGameTests::smokeOffhandFuelFirst);
        TESTS.register("smoke_lighter_breaks", () -> CultivoGameTests::smokeLighterBreaks);
        TESTS.register("smoke_creative_consumes_nothing", () -> CultivoGameTests::smokeCreativeConsumesNothing);
        TESTS.register("smoke_joint_without_strain", () -> CultivoGameTests::smokeJointWithoutStrain);
        TESTS.register("smoke_refusal_messages", () -> CultivoGameTests::smokeRefusalMessages);
        TESTS.register("joint_strain_persists", () -> CultivoGameTests::jointStrainPersists);
        TESTS.register("hash_keeps_strain", () -> CultivoGameTests::hashKeepsStrain);
        TESTS.register("smoke_devices_registered", () -> CultivoGameTests::smokeDevicesRegistered);
        TESTS.register("smoke_keeps_potion_effects", () -> CultivoGameTests::smokeKeepsPotionEffects);
        TESTS.register("smoke_infinite_high_refused", () -> CultivoGameTests::smokeInfiniteHighRefused);
        TESTS.register("station_block_interaction", () -> CultivoGameTests::stationBlockInteraction);
        TESTS.register("station_real_ticker", () -> CultivoGameTests::stationRealTicker);
        TESTS.register("station_reload_resumes", () -> CultivoGameTests::stationReloadResumes);
        TESTS.register("smoke_refused_at_finish_explains", () -> CultivoGameTests::smokeRefusedAtFinishExplains);
        TESTS.register("villager_replants_mod_crop", () -> CultivoGameTests::villagerReplantsModCrop);
        TESTS.register("catalog_strain_grows_and_drops", () -> CultivoGameTests::catalogStrainGrowsAndDrops);
        TESTS.register("catalog_joint_carries_strain", () -> CultivoGameTests::catalogJointCarriesStrain);
        TESTS.register("wandering_trader_catalog_seeds", () -> CultivoGameTests::wanderingTraderCatalogSeeds);
        TESTS.register("creative_tabs_split", () -> CultivoGameTests::creativeTabsSplit);
        TESTS.register("station_visual_state", () -> CultivoGameTests::stationVisualState);
        TESTS.register("joint_filter_recipe", () -> CultivoGameTests::jointFilterRecipe);
        TESTS.register("smoke_filtered_joint", () -> CultivoGameTests::smokeFilteredJoint);
        TESTS.register("sieve_grinder", () -> CultivoGameTests::sieveGrinder);
        TESTS.register("tray_rolls_joints", () -> CultivoGameTests::trayRollsJoints);
        TESTS.register("jar_cures_flowers", () -> CultivoGameTests::jarCuresFlowers);
        TESTS.register("ashtray_collects_ash", () -> CultivoGameTests::ashtrayCollectsAsh);
        TESTS.register("placeable_articles_drop_self", () -> CultivoGameTests::placeableArticlesDropSelf);
    }

    private static final BlockPos SOIL = new BlockPos(4, 1, 4);
    private static final BlockPos PLANT = SOIL.above();

    private CultivoGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** Falha explícita do teste com mensagem. */
    private static final class Failure extends RuntimeException {
        Failure(String message) {
            super(message);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new Failure(message);
        }
    }

    private static void run(GameTestHelper helper, Runnable body) {
        try {
            body.run();
            helper.succeed();
        } catch (Failure failure) {
            helper.fail(Component.literal(failure.getMessage()));
        }
    }

    private static BlockState crop(Variety variety, int age) {
        return ModBlocks.CROPS.get(variety).get().getStateForAge(age);
    }

    private static void plantOnFarmland(GameTestHelper helper, Variety variety, int age) {
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
        helper.setBlock(PLANT, crop(variety, age));
    }

    /** Teto de vidro 2 blocos acima e paredes de vidro a 2 blocos, na altura da planta. */
    private static void buildGreenhouse(GameTestHelper helper) {
        helper.setBlock(PLANT.above(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.east(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.west(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.north(2), Blocks.STAINED_GLASS.lime().defaultBlockState());
        helper.setBlock(PLANT.south(2), Blocks.GLASS_PANE.defaultBlockState());
    }

    private static List<ItemStack> harvest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(PLANT);
        return Block.getDrops(level.getBlockState(pos), level, pos, null);
    }

    private static int count(List<ItemStack> drops, Item item) {
        return drops.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static ProcessingStationBlockEntity placeStation(GameTestHelper helper, StationKind kind) {
        helper.setBlock(SOIL, ModBlocks.STATIONS.get(kind).get().defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(SOIL));
        check(be instanceof ProcessingStationBlockEntity, "estação sem BlockEntity: " + kind);
        return (ProcessingStationBlockEntity) be;
    }

    private static ItemStack withQuality(Item item, int count, int quality) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModComponents.QUALITY.get(), quality);
        return stack;
    }

    private static void tick(GameTestHelper helper, ProcessingStationBlockEntity station, int ticks) {
        for (int i = 0; i < ticks; i++) {
            station.serverTick(helper.getLevel());
        }
    }

    private static Integer quality(ItemStack stack) {
        return stack.get(ModComponents.QUALITY.get());
    }

    // ------------------------------------------------------------------ cultivo

    /** Planta em farmland a céu aberto (sem filtro de bioma) e cresce até a idade 7 só com random ticks. */
    public static void outdoorGrowth(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(PLANT);
            for (Variety variety : Variety.values()) {
                plantOnFarmland(helper, variety, 0);
                check(level.getBlockState(pos).canSurvive(level, pos), variety + " não sobrevive em farmland");
                check(!GreenhouseDetector.isGreenhouse(level, pos), "céu aberto não é estufa");
                check(level.getRawBrightness(pos, 0) >= 9, "luz insuficiente no ambiente de teste");
                int ticks = 0;
                while (ticks < 20_000 && level.getBlockState(pos).getValue(CropBlock.AGE) < 7) {
                    level.getBlockState(pos).randomTick(level, pos, level.getRandom());
                    ticks++;
                }
                check(level.getBlockState(pos).getValue(CropBlock.AGE) == 7, variety + " não amadureceu");
            }
            // Fora da farmland a planta não sobrevive, como o trigo
            helper.setBlock(SOIL, Blocks.STONE.defaultBlockState());
            helper.setBlock(PLANT, crop(Variety.BRISA, 0));
            check(!level.getBlockState(pos).canSurvive(level, pos), "sobreviveu em pedra");
        });
    }

    /** Estufa produz qualidade 4–5 e outdoor 2–3, na mesma colheita servidor. */
    public static void greenhouseQuality(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            for (Variety variety : Variety.values()) {
                plantOnFarmland(helper, variety, 7);
                buildGreenhouse(helper);
                check(GreenhouseDetector.isGreenhouse(level, helper.absolutePos(PLANT)), "estufa não detectada");
                for (int i = 0; i < 30; i++) {
                    for (ItemStack drop : harvest(helper)) {
                        if (drop.is(ModItems.FRESH_FLOWERS.get(variety).get())) {
                            Integer q = quality(drop);
                            check(q != null && q >= 4 && q <= 5, "estufa deu qualidade " + q);
                        }
                    }
                }
                helper.setBlock(PLANT.above(2), Blocks.AIR.defaultBlockState());
                check(!GreenhouseDetector.isGreenhouse(level, helper.absolutePos(PLANT)), "sem teto ainda é estufa");
                for (int i = 0; i < 30; i++) {
                    for (ItemStack drop : harvest(helper)) {
                        if (drop.is(ModItems.FRESH_FLOWERS.get(variety).get())) {
                            Integer q = quality(drop);
                            check(q != null && q >= 2 && q <= 3, "outdoor deu qualidade " + q);
                        }
                    }
                }
            }
        });
    }

    /** Madura: flor fresca com qualidade + sementes. Imatura: exatamente 1 semente. */
    public static void harvestDrops(GameTestHelper helper) {
        run(helper, () -> {
            for (Variety variety : Variety.values()) {
                Item flower = ModItems.FRESH_FLOWERS.get(variety).get();
                Item seeds = ModItems.SEEDS.get(variety).get();
                plantOnFarmland(helper, variety, 7);
                List<ItemStack> mature = harvest(helper);
                check(count(mature, flower) == 1, variety + " madura sem flor");
                check(count(mature, seeds) >= 1, variety + " madura sem semente");
                plantOnFarmland(helper, variety, 3);
                List<ItemStack> young = harvest(helper);
                check(count(young, flower) == 0, "imatura deu flor");
                check(count(young, seeds) == 1, "imatura deu " + count(young, seeds) + " sementes");
            }
            // Planta silvestre: 1 semente por colheita, sempre de uma variedade nativa (as do catálogo não brotam no mato)
            helper.setBlock(SOIL, Blocks.GRASS_BLOCK.defaultBlockState());
            helper.setBlock(PLANT, ModBlocks.WILD_PLANT.get().defaultBlockState());
            int seeds = 0;
            for (int i = 0; i < 40; i++) {
                for (ItemStack drop : harvest(helper)) {
                    check(drop.getItem() instanceof SeedItem, "planta silvestre deu " + drop);
                    Variety variety = ((SeedItem) drop.getItem()).variety();
                    check(variety.isNative(), "planta silvestre deu semente do catálogo: " + variety);
                    seeds += drop.getCount();
                }
            }
            check(seeds == 40, "planta silvestre deu " + seeds + " sementes em 40 colheitas");
        });
    }

    // ------------------------------------------------------------------ estações

    public static void dryingStation(GameTestHelper helper) {
        run(helper, () -> {
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.DRYING);
            ItemStack fresh = withQuality(ModItems.FRESH_FLOWERS.get(Variety.AURORA).get(), 2, 3);
            check(station.insert(helper.getLevel(), fresh) == 2, "não aceitou flor fresca");
            check(station.insert(helper.getLevel(), new ItemStack(Items.DIRT)) == 0, "aceitou terra");
            tick(helper, station, StationKind.DRYING.defaultTicks() - 1);
            check(station.getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty(), "secou antes do tempo");
            tick(helper, station, 1);
            ItemStack out = station.getItem(ProcessingStationBlockEntity.OUTPUT);
            check(out.is(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get()) && out.getCount() == 1, "saída errada " + out);
            check(Integer.valueOf(3).equals(quality(out)), "qualidade não copiada");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 1, "entrada não consumida");
        });
    }

    public static void sieveStation(GameTestHelper helper) {
        run(helper, () -> {
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.SIEVING);
            station.insert(helper.getLevel(), withQuality(ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get(), 1, 4));
            tick(helper, station, StationKind.SIEVING.defaultTicks());
            ItemStack bonus = station.extract();
            check(bonus.is(ModItems.RESIN.get()) && bonus.getCount() == 2, "qualidade 4 deveria render 2: " + bonus);
            check(Integer.valueOf(4).equals(quality(bonus)), "qualidade da resina");
            station.insert(helper.getLevel(), withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 3));
            tick(helper, station, StationKind.SIEVING.defaultTicks());
            ItemStack plain = station.extract();
            check(plain.is(ModItems.RESIN.get()) && plain.getCount() == 1, "qualidade 3 deveria render 1: " + plain);
        });
    }

    public static void pressStation(GameTestHelper helper) {
        run(helper, () -> {
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.PRESSING);
            station.insert(helper.getLevel(), withQuality(ModItems.RESIN.get(), 1, 5));
            tick(helper, station, StationKind.PRESSING.defaultTicks() * 2);
            check(station.getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty(), "prensou com 1 resina");
            station.insert(helper.getLevel(), withQuality(ModItems.RESIN.get(), 1, 5));
            tick(helper, station, StationKind.PRESSING.defaultTicks());
            ItemStack out = station.getItem(ProcessingStationBlockEntity.OUTPUT);
            check(out.is(ModItems.HASH.get()) && out.getCount() == 1, "2 resinas → 1 haxixe: " + out);
            check(Integer.valueOf(5).equals(quality(out)), "qualidade do haxixe");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).isEmpty(), "resinas não consumidas");
        });
    }

    /** Saída cheia ou de outra qualidade congela o progresso sem perder nem duplicar itens. */
    public static void stationOutputFull(GameTestHelper helper) {
        run(helper, () -> {
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.DRYING);
            Item dried = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            station.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(dried, 64, 3));
            station.insert(helper.getLevel(), withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 1, 3));
            tick(helper, station, StationKind.DRYING.defaultTicks() * 3);
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 1, "entrada perdida");
            check(station.getItem(ProcessingStationBlockEntity.OUTPUT).getCount() == 64, "saída alterada");
            check(station.progress() <= StationKind.DRYING.defaultTicks(), "progresso passou do limite");

            station.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(dried, 1, 2));
            tick(helper, station, StationKind.DRYING.defaultTicks() * 2);
            check(station.getItem(ProcessingStationBlockEntity.OUTPUT).getCount() == 1, "misturou qualidades");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 1, "entrada perdida (qualidade)");

            ItemStack taken = station.extract();
            check(taken.getCount() == 1 && Integer.valueOf(2).equals(quality(taken)), "retirada da saída");
            tick(helper, station, StationKind.DRYING.defaultTicks());
            ItemStack out = station.getItem(ProcessingStationBlockEntity.OUTPUT);
            check(out.getCount() == 1 && Integer.valueOf(3).equals(quality(out)), "não retomou após liberar a saída");
        });
    }

    /** Progresso e itens sobrevivem a salvar/carregar a BE; quebrar o bloco derruba tudo. */
    public static void stationPersistence(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.SIEVING);
            station.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 5, 4));
            // A resina da peneira guarda a variedade da flor; a saída pré-carregada precisa ser igual para empilhar
            ItemStack presetResin = withQuality(ModItems.RESIN.get(), 3, 4);
            presetResin.set(ModComponents.STRAIN.get(), Variety.AURORA);
            station.setItem(ProcessingStationBlockEntity.OUTPUT, presetResin);
            tick(helper, station, 77);

            CompoundTag saved = station.saveWithFullMetadata(level.registryAccess());
            BlockPos pos = helper.absolutePos(SOIL);
            BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            check(loaded instanceof ProcessingStationBlockEntity, "BE não recarregou");
            ProcessingStationBlockEntity copy = (ProcessingStationBlockEntity) loaded;
            check(copy.progress() == 77, "progresso salvo " + copy.progress());
            check(ItemStack.matches(copy.getItem(ProcessingStationBlockEntity.INPUT),
                station.getItem(ProcessingStationBlockEntity.INPUT)), "entrada não persistiu");
            check(ItemStack.matches(copy.getItem(ProcessingStationBlockEntity.OUTPUT),
                station.getItem(ProcessingStationBlockEntity.OUTPUT)), "saída não persistiu");

            level.destroyBlock(pos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            int flowers = 0;
            int resin = 0;
            int stations = 0;
            for (ItemEntity entity : drops) {
                ItemStack stack = entity.getItem();
                if (stack.is(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get())) {
                    flowers += stack.getCount();
                } else if (stack.is(ModItems.RESIN.get())) {
                    resin += stack.getCount();
                } else if (stack.is(ModItems.STATIONS.get(StationKind.SIEVING).get())) {
                    stations += stack.getCount();
                }
            }
            check(flowers == 5 && resin == 3, "drops ao quebrar: " + flowers + " flores, " + resin + " resinas");
            check(stations == 1, "a estação deve dropar a si mesma");
            drops.forEach(ItemEntity::discard);
        });
    }

    // ------------------------------------------------------------------ fontes e receitas

    /**
     * As tabelas de baú das 4 aldeias cobertas geram sementes de todas as variedades, nativas e do catálogo: cerca de
     * metade dos baús tem semente e, por variedade, as comuns rendem mais que as incomuns, que rendem mais que as raras.
     * O sorteio usa uma semente fixa, então o resultado é sempre o mesmo e as margens (esperado: 50% dos baús; 54, 20 e
     * 7 sementes por variedade comum, incomum e rara em 1600 baús) ficam longe dos limites.
     */
    public static void villageChestSeeds(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            RandomSource random = RandomSource.create(20_261_024L);
            Map<Variety, Integer> totals = new EnumMap<>(Variety.class);
            int chests = 0;
            int chestsWithSeeds = 0;
            for (String tableId : SeedSources.chestTables()) {
                LootTable table = level.getServer().reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(tableId)));
                LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(SOIL)))
                    .create(LootContextParamSets.CHEST);
                int found = 0;
                for (int roll = 0; roll < 400; roll++) {
                    chests++;
                    int seedStacks = 0;
                    for (ItemStack stack : table.getRandomItems(params, random)) {
                        if (stack.getItem() instanceof SeedItem seed) {
                            totals.merge(seed.variety(), stack.getCount(), Integer::sum);
                            seedStacks++;
                        }
                    }
                    check(seedStacks <= 1, "a rolagem injetada deu " + seedStacks + " pilhas de semente num baú");
                    chestsWithSeeds += seedStacks;
                    found += seedStacks;
                }
                check(found > 0, "sem sementes em " + tableId);
            }
            double share = chestsWithSeeds / (double) chests;
            check(share > 0.45 && share < 0.55, "baús com semente: " + chestsWithSeeds + "/" + chests);
            long catalogSeen = totals.keySet().stream().filter(v -> !v.isNative()).count();
            check(catalogSeen >= 30, "só " + catalogSeen + " variedades do catálogo apareceram");
            Map<Variety.Rarity, Integer> byRarity = new EnumMap<>(Variety.Rarity.class);
            totals.forEach((variety, n) -> byRarity.merge(variety.rarity(), n, Integer::sum));
            // Média por variedade: 25 comuns, 4 incomuns e 11 raras
            double common = byRarity.getOrDefault(Variety.Rarity.COMMON, 0) / 25.0;
            double uncommon = byRarity.getOrDefault(Variety.Rarity.UNCOMMON, 0) / 4.0;
            double rare = byRarity.getOrDefault(Variety.Rarity.RARE, 0) / 11.0;
            check(common > uncommon && uncommon > rare && rare > 0,
                "sementes por variedade: comum " + common + ", incomum " + uncommon + ", rara " + rare);
        });
    }

    /** Cada grade de artigo/estação/baseado resolve para o item do mod, sem conflito com receitas vanilla. */
    public static void articleRecipes(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            for (ArticleRecipes.Recipe recipe : ArticleRecipes.ALL) {
                CraftingInput input = toInput(recipe);
                Optional<RecipeHolder<CraftingRecipe>> found =
                    level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                check(found.isPresent(), "sem receita para " + recipe.result());
                ItemStack result = found.get().value().assemble(input);
                Item expected = item("cultivo:" + recipe.result());
                check(result.is(expected), recipe.result() + " resolveu para " + result);
                check(result.getCount() == recipe.count(), recipe.result() + " quantidade " + result.getCount());
            }
        });
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static CraftingInput toInput(ArticleRecipes.Recipe recipe) {
        if (!recipe.shaped()) {
            List<ItemStack> stacks = new ArrayList<>();
            recipe.ingredients().forEach(key -> stacks.add(new ItemStack(item(key.sampleItem()))));
            return CraftingInput.of(stacks.size(), 1, stacks);
        }
        int height = recipe.pattern().size();
        int width = recipe.pattern().stream().mapToInt(String::length).max().orElse(0);
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(width * height, ItemStack.EMPTY));
        for (int y = 0; y < height; y++) {
            String row = recipe.pattern().get(y);
            for (int x = 0; x < row.length(); x++) {
                char symbol = row.charAt(x);
                if (symbol != ' ') {
                    stacks.set(y * width + x, new ItemStack(item(recipe.key().get(symbol).sampleItem())));
                }
            }
        }
        return CraftingInput.of(width, height, stacks);
    }

    // ------------------------------------------------------------------ fumar (mecânica fictícia de jogo)

    /** Baseado feito pela receita transmute herda a qualidade da flor seca e grava a variedade. */
    public static void jointRecipeCopiesQuality(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            for (Variety variety : Variety.values()) {
                ItemStack flower = withQuality(ModItems.DRIED_FLOWERS.get(variety).get(), 1, 5);
                CraftingInput input = CraftingInput.of(2, 1,
                    List.of(flower, new ItemStack(ModItems.ARTICLES.get("rolling_paper").get())));
                Optional<RecipeHolder<CraftingRecipe>> found =
                    level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                check(found.isPresent(), "sem receita de baseado para " + variety);
                ItemStack joint = found.get().value().assemble(input);
                check(joint.is(ModItems.JOINT.get()) && joint.getCount() == 1, "resultado " + joint);
                check(Integer.valueOf(5).equals(quality(joint)), "qualidade não passou para o baseado: " + quality(joint));
                check(joint.get(ModComponents.STRAIN.get()) == variety, "variedade gravada " + joint.get(ModComponents.STRAIN.get()));
            }
        });
    }

    private static Player smoker(GameTestHelper helper, ItemStack inHand, ItemStack... inventory) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, inHand);
        for (ItemStack stack : inventory) {
            player.getInventory().add(stack);
        }
        return player;
    }

    private static ItemStack lighter() {
        return new ItemStack(ModItems.LIGHTER.get());
    }

    /**
     * Inventory#add guarda uma CÓPIA e zera a pilha original, então os testes sempre releem o inventário.
     * Retorna quantos itens daquele tipo o jogador tem (inventário + mãos).
     */
    private static int countInInventory(Player player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int lighterDamage(Player player) {
        ItemStack lighter = io.github.peixeangola.cultivo.smoke.SmokeSession.findLighter(player);
        check(!lighter.isEmpty(), "isqueiro sumiu do inventário");
        return lighter.getDamageValue();
    }

    private static ItemStack joint(Variety variety, int quality) {
        ItemStack joint = withQuality(ModItems.JOINT.get(), 1, quality);
        joint.set(ModComponents.STRAIN.get(), variety);
        return joint;
    }

    /** Simula segurar o botão até o fim: use() precisa aceitar e finishUsingItem() roda no servidor. */
    private static InteractionResult smokeOnce(GameTestHelper helper, Player player) {
        ServerLevel level = helper.getLevel();
        ItemStack held = player.getMainHandItem();
        InteractionResult result = held.getItem().use(level, player, InteractionHand.MAIN_HAND);
        if (result == InteractionResult.CONSUME) {
            held.getItem().finishUsingItem(held, level, player);
        }
        player.stopUsingItem();
        return result;
    }

    private static int highTicks(Player player) {
        MobEffectInstance high = player.getEffect(ModEffects.high());
        return high == null ? 0 : high.getDuration();
    }

    /** Baseado índica de qualidade 5: aplica chapado/larica/náusea/lentidão, some e gasta o isqueiro. */
    public static void smokeJointEffects(GameTestHelper helper) {
        run(helper, () -> {
            Player player = smoker(helper, joint(Variety.ECLIPSE, 5), lighter());
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "uso recusado");
            int expected = SmokeRules.totalHighTicks(0,
                SmokeRules.sessionTicks(SmokeDevice.JOINT, new SmokeRules.Material(Variety.ECLIPSE, 5, false)));
            check(highTicks(player) == expected, "chapado " + highTicks(player) + " esperado " + expected);
            check(player.hasEffect(MobEffects.HUNGER), "sem larica");
            check(player.hasEffect(MobEffects.NAUSEA), "sem náusea");
            check(player.hasEffect(MobEffects.SLOWNESS), "índica deveria dar lentidão");
            check(player.hasEffect(MobEffects.REGENERATION), "índica deveria dar regeneração");
            check(!player.hasEffect(MobEffects.SPEED), "índica não dá velocidade");
            check(player.getMainHandItem().isEmpty(), "o baseado deveria ser consumido");
            check(lighterDamage(player) == SmokeRules.LIGHTER_DAMAGE_PER_USE, "isqueiro não gastou: " + lighterDamage(player));
        });
    }

    /** Sem isqueiro nada acontece; artigo sem flor seca/haxixe também é recusado e nada é consumido. */
    public static void smokeRequiresLighterAndFuel(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            Player noLighter = smoker(helper, joint(Variety.BRISA, 3));
            check(smokeOnce(helper, noLighter) == InteractionResult.FAIL, "fumou sem isqueiro");
            check(highTicks(noLighter) == 0, "efeito sem isqueiro");
            check(noLighter.getMainHandItem().getCount() == 1, "consumiu baseado sem fumar");
            // Mesmo chamando direto o fim do uso (cliente malicioso), o servidor revalida
            ItemStack held = noLighter.getMainHandItem();
            held.getItem().finishUsingItem(held, level, noLighter);
            check(highTicks(noLighter) == 0 && held.getCount() == 1, "finishUsingItem ignorou a regra do isqueiro");

            Player noFuel = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter());
            check(smokeOnce(helper, noFuel) == InteractionResult.FAIL, "cachimbo sem material");
            ItemStack pipe = noFuel.getMainHandItem();
            pipe.getItem().finishUsingItem(pipe, level, noFuel);
            check(highTicks(noFuel) == 0, "efeito sem material");
            check(lighterDamage(noFuel) == 0, "isqueiro gasto sem fumar");
        });
    }

    /** Cachimbo queima 1 flor seca por vez, continua inteiro e usa o perfil da variedade (sativa → velocidade). */
    public static void smokePipeUsesFuel(GameTestHelper helper) {
        run(helper, () -> {
            Item flower = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            Player player = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(),
                withQuality(flower, 2, 3));
            check(countInInventory(player, flower) == 2, "preparo: 2 flores no inventário");
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "uso recusado");
            check(countInInventory(player, flower) == 1, "deveria sobrar 1 flor, sobrou " + countInInventory(player, flower));
            check(player.getMainHandItem().is(ModItems.ARTICLES.get("pipe").get()), "o cachimbo sumiu");
            check(player.hasEffect(MobEffects.SPEED), "sativa deveria dar velocidade");
            check(!player.hasEffect(MobEffects.SLOWNESS), "sativa não dá lentidão");
            smokeOnce(helper, player);
            check(countInInventory(player, flower) == 0, "segunda sessão não queimou a última flor");
            check(smokeOnce(helper, player) == InteractionResult.FAIL, "fumou sem material");
        });
    }

    /** Bong com haxixe rende mais tempo que cachimbo com flor seca da mesma qualidade. */
    public static void smokeBongHashStronger(GameTestHelper helper) {
        run(helper, () -> {
            Player bong = smoker(helper, new ItemStack(ModItems.ARTICLES.get("bong").get()), lighter(),
                withQuality(ModItems.HASH.get(), 1, 4));
            Player pipe = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(),
                withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 1, 4));
            smokeOnce(helper, bong);
            smokeOnce(helper, pipe);
            check(highTicks(bong) > highTicks(pipe), "bong+haxixe " + highTicks(bong) + " vs cachimbo " + highTicks(pipe));
            check(bong.hasEffect(MobEffects.SLOWNESS), "haxixe sem variedade dá lentidão leve");
        });
    }

    /**
     * Fumar de novo soma tempo e passa ao nível II; perto do limite o jogo recusa e não gasta nada.
     * Bong + haxixe q5 rende 3456 ticks: 3456 → 6912 (nível II) → recusado (passaria do limite).
     */
    public static void smokeStackingAndCap(GameTestHelper helper) {
        run(helper, () -> {
            Player player = smoker(helper, new ItemStack(ModItems.ARTICLES.get("bong").get()), lighter(),
                withQuality(ModItems.HASH.get(), 64, 5));
            smokeOnce(helper, player);
            int first = highTicks(player);
            check(first == 3456, "primeira sessão " + first);
            smokeOnce(helper, player);
            check(highTicks(player) == 6912, "não somou: " + first + " → " + highTicks(player));
            MobEffectInstance high = player.getEffect(ModEffects.high());
            check(high != null && high.getAmplifier() == 1, "deveria estar no nível II");
            for (int i = 0; i < 20; i++) {
                check(smokeOnce(helper, player) == InteractionResult.FAIL, "deveria recusar perto do limite");
            }
            check(highTicks(player) <= SmokeRules.MAX_HIGH_TICKS, "passou do limite: " + highTicks(player));
            int left = countInInventory(player, ModItems.HASH.get());
            check(left == 62, "haxixe consumido " + (64 - left));
            check(lighterDamage(player) == 2, "isqueiro deveria ter 2 de dano, tem " + lighterDamage(player));
        });
    }

    /** O "chapado" atual tem um "efeito escondido" do vanilla por baixo? (Não deveria: seria tempo perdido.) */
    private static boolean hasHiddenHigh(GameTestHelper helper, Player player) {
        MobEffectInstance high = player.getEffect(ModEffects.high());
        if (high == null) {
            return false;
        }
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        return MobEffectInstance.CODEC.encodeStart(ops, high).getOrThrow().getAsJsonObject().has("hidden_effect");
    }

    /** Jogador falso que guarda as chaves das mensagens mostradas acima da barra de itens. */
    private static Player capturingPlayer(GameTestHelper helper, List<String> keys) {
        return new Player(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "smoke-msg")) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }

            @Override
            public void sendOverlayMessage(Component message) {
                if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                    keys.add(t.getKey());
                }
            }
        };
    }

    /** Fumar enquanto um nível II acaba: cada sessão soma de verdade (sem "efeito escondido" do vanilla). */
    public static void smokeResmokeAfterHeavy(GameTestHelper helper) {
        run(helper, () -> {
            ItemStack joints = joint(Variety.AURORA, 2);
            joints.setCount(2);
            Player player = smoker(helper, joints, lighter());
            player.addEffect(new MobEffectInstance(ModEffects.high(), 500, 1)); // fim de um chapado forte
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "1ª sessão recusada");
            check(highTicks(player) == 1400 && player.getEffect(ModEffects.high()).getAmplifier() == 0,
                "500 + 900 = 1400 no nível I, veio " + highTicks(player));
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "2ª sessão recusada");
            check(highTicks(player) == 2300, "1400 + 900 = 2300, veio " + highTicks(player));
            check(!hasHiddenHigh(helper, player), "sobrou um efeito escondido do vanilla");

            // Subindo: cachimbo com 3 flores Brisa q2 (900 cada) passa ao nível II só na 3ª
            Item flower = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            Player up = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(), withQuality(flower, 3, 2));
            smokeOnce(helper, up);
            smokeOnce(helper, up);
            check(highTicks(up) == 1800 && up.getEffect(ModEffects.high()).getAmplifier() == 0, "1800 no nível I");
            smokeOnce(helper, up);
            check(highTicks(up) == 2700 && up.getEffect(ModEffects.high()).getAmplifier() == 1, "2700 no nível II");
        });
    }

    /** No limite o uso é recusado e nada é gasto; abaixo da margem ainda dá para fumar, e o total é cortado em 6 min. */
    public static void smokeAtCapRefused(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            Player full = smoker(helper, joint(Variety.BRISA, 3), lighter());
            full.addEffect(new MobEffectInstance(ModEffects.high(), SmokeRules.MAX_HIGH_TICKS, 1));
            check(smokeOnce(helper, full) == InteractionResult.FAIL, "fumou no limite");
            ItemStack held = full.getMainHandItem();
            held.getItem().finishUsingItem(held, level, full);
            check(held.getCount() == 1, "gastou o baseado no limite");
            check(lighterDamage(full) == 0, "gastou o isqueiro no limite");

            int edge = SmokeRules.MAX_HIGH_TICKS - SmokeRules.AT_MAX_MARGIN_TICKS;
            Player nearly = smoker(helper, new ItemStack(ModItems.ARTICLES.get("bong").get()), lighter(),
                withQuality(ModItems.HASH.get(), 1, 5));
            nearly.addEffect(new MobEffectInstance(ModEffects.high(), edge, 1));
            check(smokeOnce(helper, nearly) == InteractionResult.CONSUME, "deveria aceitar na margem");
            check(highTicks(nearly) == SmokeRules.MAX_HIGH_TICKS, "deveria cortar no limite: " + highTicks(nearly));
        });
    }

    /** Mesmo artigo e mesma qualidade: haxixe dura exatamente 1,5× a flor seca. */
    public static void smokeHashMultiplier(GameTestHelper helper) {
        run(helper, () -> {
            Player hash = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(),
                withQuality(ModItems.HASH.get(), 1, 4));
            Player flower = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(),
                withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 1, 4));
            smokeOnce(helper, hash);
            smokeOnce(helper, flower);
            check(highTicks(flower) == 1260, "flor q4 no cachimbo: " + highTicks(flower));
            check(highTicks(hash) == 1890, "haxixe q4 no cachimbo: " + highTicks(hash));
            check(hash.hasEffect(MobEffects.SLOWNESS) && !hash.hasEffect(MobEffects.REGENERATION),
                "haxixe sem variedade: perfil desconhecido");
        });
    }

    /** A flor da mão secundária queima primeiro e define variedade e qualidade. */
    public static void smokeOffhandFuelFirst(GameTestHelper helper) {
        run(helper, () -> {
            Item brisa = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            Item eclipse = ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get();
            Player player = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(), withQuality(brisa, 3, 3));
            player.setItemInHand(InteractionHand.OFF_HAND, withQuality(eclipse, 1, 5));
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "uso recusado");
            check(player.getOffhandItem().isEmpty(), "a flor da mão secundária deveria queimar");
            check(countInInventory(player, brisa) == 3, "queimou a flor errada");
            int expected = SmokeRules.sessionTicks(SmokeDevice.PIPE, new SmokeRules.Material(Variety.ECLIPSE, 5, false));
            check(highTicks(player) == expected, "duração " + highTicks(player) + " esperada " + expected);
            check(player.hasEffect(MobEffects.SLOWNESS) && !player.hasEffect(MobEffects.SPEED), "perfil índica");
        });
    }

    /** O isqueiro quebra no 64º uso (no inventário e na mão secundária) e depois não dá mais para fumar. */
    public static void smokeLighterBreaks(GameTestHelper helper) {
        run(helper, () -> {
            check(new ItemStack(ModItems.LIGHTER.get()).getMaxDamage() == SmokeRules.LIGHTER_DURABILITY, "durabilidade");
            ItemStack worn = lighter();
            worn.setDamageValue(SmokeRules.LIGHTER_DURABILITY - 1);
            ItemStack joints = joint(Variety.BRISA, 2);
            joints.setCount(2);
            List<String> keys = new ArrayList<>();
            Player player = capturingPlayer(helper, keys);
            player.setItemInHand(InteractionHand.MAIN_HAND, joints);
            player.getInventory().add(worn);
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "último uso recusado");
            check(keys.contains("message.cultivo.lighter_broke"), "sem aviso de isqueiro quebrado: " + keys);
            check(io.github.peixeangola.cultivo.smoke.SmokeSession.findLighter(player).isEmpty(), "isqueiro deveria quebrar");
            check(smokeOnce(helper, player) == InteractionResult.FAIL, "fumou com isqueiro quebrado");
            check(player.getMainHandItem().getCount() == 1, "consumiu baseado sem isqueiro");

            ItemStack wornOffhand = lighter();
            wornOffhand.setDamageValue(SmokeRules.LIGHTER_DURABILITY - 1);
            Player offhand = smoker(helper, joint(Variety.AURORA, 2));
            offhand.setItemInHand(InteractionHand.OFF_HAND, wornOffhand);
            check(smokeOnce(helper, offhand) == InteractionResult.CONSUME, "último uso (mão secundária) recusado");
            check(offhand.getOffhandItem().isEmpty(), "isqueiro da mão secundária deveria quebrar");

            // Com 62 de dano ainda sobra um uso: não quebra antes da hora e não avisa
            ItemStack almost = lighter();
            almost.setDamageValue(SmokeRules.LIGHTER_DURABILITY - 2);
            List<String> none = new ArrayList<>();
            Player early = capturingPlayer(helper, none);
            early.setItemInHand(InteractionHand.MAIN_HAND, joint(Variety.BRISA, 2));
            early.getInventory().add(almost);
            check(smokeOnce(helper, early) == InteractionResult.CONSUME, "uso recusado com isqueiro bom");
            check(lighterDamage(early) == SmokeRules.LIGHTER_DURABILITY - 1, "dano " + lighterDamage(early));
            check(none.isEmpty(), "avisou cedo demais: " + none);
        });
    }

    /** No criativo fuma normalmente, mas não gasta flor, baseado nem isqueiro. */
    public static void smokeCreativeConsumesNothing(GameTestHelper helper) {
        run(helper, () -> {
            Player player = helper.makeMockPlayer(GameType.CREATIVE);
            GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
            check(player.hasInfiniteMaterials(), "preparo: jogador criativo");
            Item flower = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARTICLES.get("pipe").get()));
            player.getInventory().add(lighter());
            player.getInventory().add(withQuality(flower, 1, 3));
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "uso recusado");
            check(highTicks(player) > 0, "sem efeito no criativo");
            check(countInInventory(player, flower) == 1, "criativo gastou flor");
            check(lighterDamage(player) == 0, "criativo gastou isqueiro");
            player.setItemInHand(InteractionHand.MAIN_HAND, joint(Variety.ECLIPSE, 3));
            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "baseado recusado no criativo");
            check(player.hasEffect(MobEffects.SLOWNESS), "o baseado índica não fez efeito");
            check(player.getMainHandItem().getCount() == 1, "criativo consumiu o baseado");
            check(lighterDamage(player) == 0, "criativo gastou isqueiro (baseado)");
        });
    }

    /** Baseado sem variedade (/give) usa o perfil desconhecido; craftar e fumar funciona de ponta a ponta. */
    public static void smokeJointWithoutStrain(GameTestHelper helper) {
        run(helper, () -> {
            Player legacy = smoker(helper, new ItemStack(ModItems.JOINT.get()), lighter());
            check(smokeOnce(helper, legacy) == InteractionResult.CONSUME, "baseado sem variedade recusado");
            check(highTicks(legacy) == 900, "duração padrão " + highTicks(legacy));
            check(legacy.hasEffect(MobEffects.SLOWNESS) && !legacy.hasEffect(MobEffects.SPEED)
                && !legacy.hasEffect(MobEffects.REGENERATION), "perfil desconhecido");
            check(legacy.getMainHandItem().isEmpty(), "baseado não foi consumido");

            ServerLevel level = helper.getLevel();
            CraftingInput input = CraftingInput.of(2, 1, List.of(
                withQuality(ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get(), 1, 4),
                new ItemStack(ModItems.ARTICLES.get("rolling_paper").get())));
            ItemStack crafted = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElseThrow().value().assemble(input);
            Player smoker = smoker(helper, crafted, lighter());
            smokeOnce(helper, smoker);
            check(highTicks(smoker) == 1260, "craftar → fumar: " + highTicks(smoker));
            check(smoker.hasEffect(MobEffects.SLOWNESS) && smoker.hasEffect(MobEffects.REGENERATION), "perfil índica");
        });
    }

    /** As mensagens de recusa saem na ordem certa: isqueiro, material, limite. */
    public static void smokeRefusalMessages(GameTestHelper helper) {
        run(helper, () -> {
            List<String> keys = new ArrayList<>();
            Player player = capturingPlayer(helper, keys);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARTICLES.get("pipe").get()));
            smokeOnce(helper, player);
            player.getInventory().add(lighter());
            smokeOnce(helper, player);
            player.getInventory().add(withQuality(ModItems.HASH.get(), 1, 3));
            player.addEffect(new MobEffectInstance(ModEffects.high(), SmokeRules.MAX_HIGH_TICKS, 1));
            smokeOnce(helper, player);
            check(keys.equals(List.of("message.cultivo.need_lighter", "message.cultivo.need_fuel", "message.cultivo.too_high")),
                "mensagens " + keys);
        });
    }

    /** cultivo:strain sobrevive a salvar (NBT) e à sincronização de rede, no formato "brisa"/"aurora"/"eclipse". */
    public static void jointStrainPersists(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            var ops = level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            for (Variety variety : Variety.values()) {
                ItemStack joint = joint(variety, 4);
                com.google.gson.JsonElement json = ItemStack.CODEC.encodeStart(ops, joint).getOrThrow();
                String saved = json.getAsJsonObject().getAsJsonObject("components").get("cultivo:strain").getAsString();
                check(variety.id().equals(saved), "formato salvo: " + json);
                check(ItemStack.matches(joint, ItemStack.CODEC.parse(ops, json).getOrThrow()), "não persistiu: " + json);
                net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.buffer(), level.registryAccess());
                ItemStack.STREAM_CODEC.encode(buf, joint);
                check(ItemStack.matches(joint, ItemStack.STREAM_CODEC.decode(buf)), "não sincronizou " + variety);
            }
        });
    }

    /** Peneira e prensa passam a variedade adiante: o haxixe de Eclipse tem o perfil índica ao fumar. */
    public static void hashKeepsStrain(GameTestHelper helper) {
        run(helper, () -> {
            ProcessingStationBlockEntity sieve = placeStation(helper, StationKind.SIEVING);
            sieve.insert(helper.getLevel(), withQuality(ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get(), 1, 4));
            tick(helper, sieve, StationKind.SIEVING.defaultTicks());
            ItemStack resin = sieve.extract();
            check(resin.getCount() == 2 && resin.get(ModComponents.STRAIN.get()) == Variety.ECLIPSE, "resina " + resin);

            ProcessingStationBlockEntity press = placeStation(helper, StationKind.PRESSING);
            press.insert(helper.getLevel(), resin);
            tick(helper, press, StationKind.PRESSING.defaultTicks());
            ItemStack hash = press.extract();
            check(hash.is(ModItems.HASH.get()) && hash.get(ModComponents.STRAIN.get()) == Variety.ECLIPSE
                && Integer.valueOf(4).equals(quality(hash)), "haxixe " + hash);

            Player player = smoker(helper, new ItemStack(ModItems.ARTICLES.get("pipe").get()), lighter(), hash);
            smokeOnce(helper, player);
            check(highTicks(player) == 1890, "haxixe q4 no cachimbo: " + highTicks(player));
            check(player.hasEffect(MobEffects.SLOWNESS) && player.hasEffect(MobEffects.REGENERATION), "perfil índica");
        });
    }

    /**
     * Cada SmokeDevice está registrado como item fumável e funciona (inclui chillum e narguilé); os outros artigos têm
     * dica de uso ou colocam o próprio bloco (nenhum é só decorativo).
     */
    public static void smokeDevicesRegistered(GameTestHelper helper) {
        run(helper, () -> {
            for (SmokeDevice device : SmokeDevice.values()) {
                Item registered = item("cultivo:" + device.itemId());
                check(registered instanceof io.github.peixeangola.cultivo.item.SmokingItem smoking && smoking.device() == device,
                    device + " não é SmokingItem");
                if (!device.needsFuel()) {
                    continue;
                }
                Player player = smoker(helper, new ItemStack(registered), lighter(),
                    withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 1, 2));
                check(smokeOnce(helper, player) == InteractionResult.CONSUME, device + " recusado");
                int expected = SmokeRules.sessionTicks(device, new SmokeRules.Material(Variety.AURORA, 2, false));
                check(highTicks(player) == expected, device + ": " + highTicks(player) + " esperado " + expected);
            }
            check(ModItems.LIGHTER.get() instanceof io.github.peixeangola.cultivo.item.LighterItem, "isqueiro");
            // Nenhum artigo é só decorativo: folha de seda, piteira e dichavador têm dica com o uso
            for (String article : List.of("rolling_paper", "filter_tip", "grinder")) {
                Item tipItem = ModItems.ARTICLES.get(article).get();
                check(tipItem instanceof TooltipItem tip && tip.tooltipKey().equals("tooltip.cultivo." + article),
                    article + " sem a dica de uso");
            }
            // Bandeja, pote e cinzeiro colocam o próprio bloco (e também têm dica)
            for (String article : ContentIds.PLACEABLE_ARTICLES) {
                Item blockItem = ModItems.ARTICLES.get(article).get();
                check(blockItem instanceof ArticleBlockItem placed
                    && placed.getBlock() == ModBlocks.PLACEABLE_ARTICLES.get(article).get()
                    && placed.tooltipKey().equals("tooltip.cultivo." + article), article + " não coloca o bloco certo");
            }
        });
    }

    /** Fumar não apaga efeitos de poção: Lentidão IV (Mestre Tartaruga) e Velocidade longa continuam. */
    public static void smokeKeepsPotionEffects(GameTestHelper helper) {
        run(helper, () -> {
            Player turtle = smoker(helper, joint(Variety.BRISA, 3), lighter());
            turtle.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 800, 3));
            check(smokeOnce(helper, turtle) == InteractionResult.CONSUME, "uso recusado");
            MobEffectInstance slowness = turtle.getEffect(MobEffects.SLOWNESS);
            check(slowness != null && slowness.getAmplifier() == 3, "a Lentidão IV da poção sumiu");
            check(turtle.hasEffect(MobEffects.SPEED), "a sativa ainda dá Velocidade");

            Player swift = smoker(helper, joint(Variety.ECLIPSE, 3), lighter());
            swift.addEffect(new MobEffectInstance(MobEffects.SPEED, 9600, 0));
            check(smokeOnce(helper, swift) == InteractionResult.CONSUME, "uso recusado");
            MobEffectInstance speed = swift.getEffect(MobEffects.SPEED);
            check(speed != null && speed.getDuration() == 9600, "a Velocidade da poção foi apagada ou encurtada");
            check(swift.hasEffect(MobEffects.SLOWNESS), "a índica ainda dá Lentidão");
        });
    }

    /** Um "chapado" infinito (por comando) conta como no limite: recusa e não gasta nada nem o encurta. */
    public static void smokeInfiniteHighRefused(GameTestHelper helper) {
        run(helper, () -> {
            Player player = smoker(helper, joint(Variety.AURORA, 3), lighter());
            player.addEffect(new MobEffectInstance(ModEffects.high(), MobEffectInstance.INFINITE_DURATION, 1));
            check(smokeOnce(helper, player) == InteractionResult.FAIL, "fumou com efeito infinito");
            ItemStack held = player.getMainHandItem();
            check(io.github.peixeangola.cultivo.smoke.SmokeSession.smoke(helper.getLevel(), player, held, SmokeDevice.JOINT).isEmpty(),
                "o servidor aceitou fumar");
            check(player.getEffect(ModEffects.high()).isInfiniteDuration(), "o efeito infinito foi encurtado");
            check(held.getCount() == 1 && lighterDamage(player) == 0, "gastou itens");
        });
    }

    // ------------------------------------------------------------------ estações pelo caminho real do jogo

    private static ProcessingStationBlockEntity placeStationAt(GameTestHelper helper, StationKind kind, BlockPos rel) {
        helper.setBlock(rel, ModBlocks.STATIONS.get(kind).get().defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        check(be instanceof ProcessingStationBlockEntity, "estação sem BlockEntity: " + kind);
        return (ProcessingStationBlockEntity) be;
    }

    /**
     * Clique direito como o cliente faz: tenta a mão principal e depois a secundária, cada uma pelo
     * ServerPlayerGameMode#useItemOn do vanilla (useItemOn do bloco → mão vazia → uso do item/colocar bloco).
     * Para no primeiro resultado que consome a ação. Mira a face norte do bloco.
     */
    private static InteractionResult rightClick(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        InteractionResult last = InteractionResult.PASS;
        for (InteractionHand hand : InteractionHand.values()) {
            last = player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit);
            if (last.consumesAction()) {
                return last;
            }
        }
        return last;
    }

    /**
     * Regressões do clique: item que não entra (terra, cachimbo, flor de outra qualidade) nunca retira nem zera o
     * progresso, e a terra é colocada ao lado como no vanilla; só a mão principal vazia retira (saída primeiro);
     * com a estação vazia a mão secundária insere.
     */
    public static void stationBlockInteraction(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            // O que sai da estação vai para o primeiro slot livre (0); a mão principal fica no 8 para continuar vazia
            player.getInventory().setSelectedSlot(8);
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.DRYING);
            Item fresh = ModItems.FRESH_FLOWERS.get(Variety.BRISA).get();
            Item dried = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();

            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(fresh, 16, 2));
            check(rightClick(helper, player, SOIL).consumesAction(), "não inseriu pelo clique");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16, "entrada após inserir");
            check(player.getMainHandItem().isEmpty(), "a mão deveria esvaziar");
            station.setProgress(590);

            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(fresh, 4, 3));
            rightClick(helper, player, SOIL);
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16
                && Integer.valueOf(2).equals(quality(station.getItem(ProcessingStationBlockEntity.INPUT))),
                "flor de outra qualidade mexeu na entrada");
            check(station.progress() == 590, "flor de outra qualidade zerou o progresso: " + station.progress());
            check(player.getMainHandItem().getCount() == 4 && countInInventory(player, fresh) == 4,
                "flor de outra qualidade: a mão ou o inventário mudou");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 3));
            rightClick(helper, player, SOIL);
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16 && station.progress() == 590,
                "segurar terra retirou a entrada ou zerou o progresso");
            check(level.getBlockState(helper.absolutePos(SOIL).north()).is(Blocks.DIRT),
                "PASS deveria deixar o vanilla colocar a terra ao lado");
            check(player.getMainHandItem().getCount() == 2, "a terra colocada não saiu da mão");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARTICLES.get("pipe").get()));
            rightClick(helper, player, SOIL);
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16 && station.progress() == 590,
                "segurar um cachimbo retirou a entrada");
            check(countInInventory(player, fresh) == 0, "segurar um item devolveu flores ao inventário");

            // Mão vazia: primeiro a saída, sem tocar na entrada em andamento
            station.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(dried, 1, 2));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(rightClick(helper, player, SOIL).consumesAction(), "mão vazia não retirou");
            check(countInInventory(player, dried) == 1, "a saída não veio para o inventário");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16 && station.progress() == 590,
                "retirar a saída mexeu na entrada");
            // Sem saída, a mão vazia devolve a entrada em andamento (comportamento documentado) e zera o progresso
            check(rightClick(helper, player, SOIL).consumesAction(), "mão vazia não devolveu a entrada");
            check(countInInventory(player, fresh) == 16, "entrada não voltou: " + countInInventory(player, fresh));
            check(station.getItem(ProcessingStationBlockEntity.INPUT).isEmpty() && station.progress() == 0,
                "a estação deveria ficar vazia");

            // Estação vazia + mão principal vazia: a mão secundária insere
            player.setItemInHand(InteractionHand.OFF_HAND, withQuality(fresh, 5, 3));
            check(rightClick(helper, player, SOIL).consumesAction(), "mão secundária não inseriu");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 5, "entrada pela mão secundária");
            check(player.getOffhandItem().isEmpty(), "a mão secundária deveria esvaziar");
        });
    }

    /**
     * As três estações processam só com os ticks do mundo (o ticker real da BlockEntity), sem chamar serverTick:
     * secagem 600, peneira 200 e prensa 300 ticks. Nada sai antes da hora.
     */
    public static void stationRealTicker(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos dryingPos = SOIL;
        BlockPos sievePos = SOIL.east(2);
        BlockPos pressPos = SOIL.west(2);
        ProcessingStationBlockEntity drying = placeStationAt(helper, StationKind.DRYING, dryingPos);
        ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, sievePos);
        ProcessingStationBlockEntity press = placeStationAt(helper, StationKind.PRESSING, pressPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.FRESH_FLOWERS.get(Variety.AURORA).get(), 1, 3));
        rightClick(helper, player, dryingPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 3));
        rightClick(helper, player, sievePos);
        player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.RESIN.get(), 2, 3));
        rightClick(helper, player, pressPos);
        helper.assertTrue(!drying.getItem(ProcessingStationBlockEntity.INPUT).isEmpty()
            && !sieve.getItem(ProcessingStationBlockEntity.INPUT).isEmpty()
            && press.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 2, "inserção pelo clique falhou");

        helper.runAfterDelay(150, () -> helper.assertTrue(sieve.getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty(),
            "peneira terminou antes de 200 ticks"));
        helper.runAfterDelay(250, () -> helper.assertTrue(press.getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty(),
            "prensa terminou antes de 300 ticks"));
        helper.runAfterDelay(550, () -> helper.assertTrue(drying.getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty(),
            "secagem terminou antes de 600 ticks"));
        helper.succeedWhen(() -> {
            helper.assertTrue(drying.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get()),
                "ainda secando");
            helper.assertTrue(sieve.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.RESIN.get()), "ainda peneirando");
            helper.assertTrue(press.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.HASH.get()), "ainda prensando");
        });
    }

    /**
     * Progresso sobrevive a recarregar a BlockEntity no mundo (o mesmo caminho do carregamento de chunk:
     * BlockEntity.loadStatic + Level#setBlockEntity, que registra o ticker de novo) e continua de onde parou.
     * A peneira leva 200 ticks: recarregada aos 120, termina por volta dos 200; se recomeçasse do zero, só
     * terminaria depois dos 320 e passaria do max_ticks de 300.
     */
    public static void stationReloadResumes(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        placeStationAt(helper, StationKind.SIEVING, SOIL);
        player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get(), 1, 3));
        rightClick(helper, player, SOIL);
        BlockPos pos = helper.absolutePos(SOIL);
        helper.runAfterDelay(120, () -> {
            ServerLevel level = helper.getLevel();
            ProcessingStationBlockEntity before = (ProcessingStationBlockEntity) level.getBlockEntity(pos);
            int progress = before.progress();
            helper.assertTrue(progress >= 100 && progress < 200, "progresso antes de recarregar: " + progress);
            CompoundTag saved = before.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(pos);
            BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            helper.assertTrue(loaded instanceof ProcessingStationBlockEntity, "BE não recarregou");
            level.setBlockEntity(loaded);
            helper.assertTrue(level.getBlockEntity(pos) == loaded && loaded != before, "BE recarregada não está no mundo");
            ProcessingStationBlockEntity after = (ProcessingStationBlockEntity) loaded;
            helper.assertTrue(after.progress() == progress, "progresso " + progress + " → " + after.progress());
            helper.assertTrue(after.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 1, "entrada perdida na recarga");
        });
        helper.succeedWhen(() -> {
            BlockEntity be = helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(be instanceof ProcessingStationBlockEntity station
                && station.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.RESIN.get())
                && Integer.valueOf(3).equals(quality(station.getItem(ProcessingStationBlockEntity.OUTPUT))), "ainda peneirando");
        });
    }

    // ------------------------------------------------------------------ fumar: recusa no fim do uso

    private static void removeAll(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Se o material ou o isqueiro some durante a animação, o servidor recusa no fim e diz por quê. */
    public static void smokeRefusedAtFinishExplains(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            List<String> keys = new ArrayList<>();
            Player player = capturingPlayer(helper, keys);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ARTICLES.get("pipe").get()));
            player.getInventory().add(lighter());
            player.getInventory().add(withQuality(ModItems.HASH.get(), 1, 3));
            ItemStack pipe = player.getMainHandItem();
            check(pipe.getItem().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME, "início recusado");
            check(keys.isEmpty(), "mensagem no início: " + keys);
            removeAll(player, ModItems.HASH.get());
            pipe.getItem().finishUsingItem(pipe, level, player);
            player.stopUsingItem();
            check(keys.equals(List.of("message.cultivo.need_fuel")), "sem material no fim: " + keys);
            check(highTicks(player) == 0 && lighterDamage(player) == 0, "fumou ou gastou o isqueiro sem material");

            keys.clear();
            player.setItemInHand(InteractionHand.MAIN_HAND, joint(Variety.BRISA, 3));
            ItemStack joint = player.getMainHandItem();
            check(joint.getItem().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME, "baseado recusado");
            removeAll(player, ModItems.LIGHTER.get());
            joint.getItem().finishUsingItem(joint, level, player);
            player.stopUsingItem();
            check(keys.equals(List.of("message.cultivo.need_lighter")), "sem isqueiro no fim: " + keys);
            check(joint.getCount() == 1 && highTicks(player) == 0, "consumiu o baseado sem fumar");
        });
    }

    // ------------------------------------------------------------------ aldeias

    /**
     * O fazendeiro vanilla (HarvestFarmland) colhe com {@code level.destroyBlock(pos, true, aldeão)}. Aqui o teste
     * faz a mesma chamada: a lavoura madura do mod volta na idade 0 da mesma variedade (não vira trigo) e a colheita
     * cai no chão. Sem aldeão, ou com a planta imatura, nada é replantado.
     */
    public static void villagerReplantsModCrop(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(PLANT);
            Villager villager = helper.spawn(EntityTypes.VILLAGER, PLANT.north());

            plantOnFarmland(helper, Variety.AURORA, 7);
            check(level.destroyBlock(pos, true, villager), "o aldeão não quebrou a lavoura");
            BlockState after = level.getBlockState(pos);
            check(after.is(ModBlocks.CROPS.get(Variety.AURORA).get()) && after.getValue(CropBlock.AGE) == 0,
                "não replantou aurora na idade 0: " + after);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            check(drops.stream().anyMatch(e -> e.getItem().is(ModItems.FRESH_FLOWERS.get(Variety.AURORA).get())),
                "a colheita do aldeão deveria derrubar a flor");
            drops.forEach(ItemEntity::discard);

            plantOnFarmland(helper, Variety.ECLIPSE, 3);
            level.destroyBlock(pos, true, villager);
            check(level.getBlockState(pos).isAir(), "replantou uma planta imatura");

            plantOnFarmland(helper, Variety.BRISA, 7);
            level.destroyBlock(pos, true, null);
            check(level.getBlockState(pos).isAir(), "replantou sem aldeão");
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D)).forEach(ItemEntity::discard);
            villager.discard();
        });
    }

    // ------------------------------------------------------------------ variedades do catálogo

    /**
     * Uma variedade do catálogo (Manga Gelada) segue o caminho real do jogo: a semente é plantada pelo clique na
     * farmland, cresce só com random ticks e, quebrada madura, derruba a própria flor fresca (qualidade outdoor 2–3) e
     * as próprias sementes, sem itens de outra variedade.
     */
    public static void catalogStrainGrowsAndDrops(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            Variety strain = Variety.MANGA_GELADA;
            check(!strain.isNative(), "preparo: variedade do catálogo");
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
            helper.setBlock(PLANT, Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SEEDS.get(strain).get(), 4));
            BlockPos soil = helper.absolutePos(SOIL);
            BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(soil).add(0.0D, 0.5D, 0.0D), Direction.UP, soil, false);
            InteractionResult planted = player.gameMode.useItemOn(player, level, player.getMainHandItem(),
                InteractionHand.MAIN_HAND, top);
            check(planted.consumesAction(), "a semente não foi plantada: " + planted);
            BlockPos pos = helper.absolutePos(PLANT);
            BlockState seedling = level.getBlockState(pos);
            check(seedling.is(ModBlocks.CROPS.get(strain).get()) && seedling.getValue(CropBlock.AGE) == 0,
                "plantou " + seedling);
            check(player.getMainHandItem().getCount() == 3, "a semente plantada não saiu da mão");

            int ticks = 0;
            while (ticks < 20_000 && level.getBlockState(pos).getValue(CropBlock.AGE) < 7) {
                level.getBlockState(pos).randomTick(level, pos, level.getRandom());
                ticks++;
            }
            check(level.getBlockState(pos).getValue(CropBlock.AGE) == 7, strain + " não amadureceu");

            check(level.destroyBlock(pos, true, player), "não colheu");
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            int flowers = 0;
            int seeds = 0;
            for (ItemEntity entity : drops) {
                ItemStack stack = entity.getItem();
                Variety dropped = ModItems.varietyOf(stack);
                check(dropped == strain, "a colheita deu item de outra variedade: " + stack);
                if (stack.is(ModItems.FRESH_FLOWERS.get(strain).get())) {
                    Integer q = quality(stack);
                    check(q != null && q >= 2 && q <= 3, "qualidade outdoor " + q);
                    flowers += stack.getCount();
                } else if (stack.is(ModItems.SEEDS.get(strain).get())) {
                    seeds += stack.getCount();
                }
            }
            check(flowers == 1, "flores da colheita: " + flowers);
            check(seeds >= 1, "a colheita madura não deu sementes");
            drops.forEach(ItemEntity::discard);
        });
    }

    /**
     * Flor do catálogo pela cadeia toda: a secagem mantém a variedade e a qualidade, o baseado da receita grava a
     * variedade e a qualidade, e fumar usa o perfil da genética (Bananona é híbrida: só Regeneração). A peneira também
     * guarda a variedade na resina. Outras genéticas do catálogo: Amnésia Neblina (sativa) e Luzes do Norte (índica).
     */
    public static void catalogJointCarriesStrain(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            Variety strain = Variety.BANANONA;
            ProcessingStationBlockEntity drying = placeStation(helper, StationKind.DRYING);
            check(drying.insert(level, withQuality(ModItems.FRESH_FLOWERS.get(strain).get(), 2, 4)) == 2, "secagem recusou");
            tick(helper, drying, StationKind.DRYING.defaultTicks());
            ItemStack dried = drying.extract();
            check(dried.is(ModItems.DRIED_FLOWERS.get(strain).get()) && dried.getCount() == 1, "secou em " + dried);
            check(Integer.valueOf(4).equals(quality(dried)), "a secagem mudou a qualidade: " + quality(dried));

            ItemStack joint = craftJoint(helper, dried);
            check(joint.get(ModComponents.STRAIN.get()) == strain, "variedade do baseado: " + joint.get(ModComponents.STRAIN.get()));
            check(Integer.valueOf(4).equals(quality(joint)), "qualidade do baseado: " + quality(joint));
            Player hybrid = smoker(helper, joint, lighter());
            check(smokeOnce(helper, hybrid) == InteractionResult.CONSUME, "uso recusado");
            check(highTicks(hybrid) == 1260, "baseado q4: " + highTicks(hybrid));
            check(hybrid.hasEffect(MobEffects.REGENERATION) && !hybrid.hasEffect(MobEffects.SPEED)
                && !hybrid.hasEffect(MobEffects.SLOWNESS), "perfil híbrido");

            ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, SOIL.east(2));
            sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(strain).get(), 1, 3));
            tick(helper, sieve, StationKind.SIEVING.defaultTicks());
            ItemStack resin = sieve.extract();
            check(resin.is(ModItems.RESIN.get()) && resin.get(ModComponents.STRAIN.get()) == strain, "resina " + resin);

            ItemStack sativaJoint = craftJoint(helper, withQuality(ModItems.DRIED_FLOWERS.get(Variety.AMNESIA_NEBLINA).get(), 1, 5));
            check(sativaJoint.get(ModComponents.STRAIN.get()) == Variety.AMNESIA_NEBLINA
                && Integer.valueOf(5).equals(quality(sativaJoint)), "baseado sativa " + sativaJoint);
            Player sativa = smoker(helper, sativaJoint, lighter());
            smokeOnce(helper, sativa);
            check(highTicks(sativa) == 1440, "baseado q5: " + highTicks(sativa));
            check(sativa.hasEffect(MobEffects.SPEED) && !sativa.hasEffect(MobEffects.SLOWNESS), "perfil sativa");

            Player indica = smoker(helper, craftJoint(helper,
                withQuality(ModItems.DRIED_FLOWERS.get(Variety.LUZES_DO_NORTE).get(), 1, 2)), lighter());
            smokeOnce(helper, indica);
            check(highTicks(indica) == 900, "baseado q2: " + highTicks(indica));
            check(indica.hasEffect(MobEffects.SLOWNESS) && indica.hasEffect(MobEffects.REGENERATION)
                && !indica.hasEffect(MobEffects.SPEED), "perfil índica");
        });
    }

    /** Monta o baseado pela receita de crafting (flor seca + folha de seda), como na bancada. */
    private static ItemStack craftJoint(GameTestHelper helper, ItemStack driedFlower) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = CraftingInput.of(2, 1,
            List.of(driedFlower, new ItemStack(ModItems.ARTICLES.get("rolling_paper").get())));
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(recipe.isPresent(), "sem receita de baseado para " + driedFlower);
        ItemStack joint = recipe.get().value().assemble(input);
        check(joint.is(ModItems.JOINT.get()) && joint.getCount() == 1, "resultado " + joint);
        return joint;
    }

    /**
     * Mascate novo (o mesmo caminho do spawn natural: finalizeSpawn com o evento do Forge) ganha 2 ofertas de sementes
     * do catálogo, de variedades diferentes, 2–3 sementes por esmeraldas pela raridade (comum 2, incomum 4, rara 8),
     * além das ofertas vanilla.
     */
    public static void wanderingTraderCatalogSeeds(GameTestHelper helper) {
        run(helper, () -> {
            Map<Variety.Rarity, Integer> price = Map.of(Variety.Rarity.COMMON, 2, Variety.Rarity.UNCOMMON, 4,
                Variety.Rarity.RARE, 8);
            for (int i = 0; i < 8; i++) {
                WanderingTrader trader = helper.spawn(EntityTypes.WANDERING_TRADER, PLANT, EntitySpawnReason.EVENT);
                List<MerchantOffer> seedOffers = trader.getOffers().stream()
                    .filter(offer -> offer.getResult().getItem() instanceof SeedItem)
                    .toList();
                check(seedOffers.size() == 2, "ofertas de semente: " + seedOffers.size());
                check(trader.getOffers().size() > 2, "as ofertas vanilla sumiram");
                Set<Variety> strains = EnumSet.noneOf(Variety.class);
                for (MerchantOffer offer : seedOffers) {
                    Variety variety = ((SeedItem) offer.getResult().getItem()).variety();
                    strains.add(variety);
                    check(!variety.isNative(), "o mascate vendeu uma nativa: " + variety);
                    int count = offer.getResult().getCount();
                    check(count == 2 || count == 3, variety + ": " + count + " sementes");
                    check(offer.getItemCostA().item().value() == Items.EMERALD, "custo em " + offer.getItemCostA());
                    check(offer.getItemCostA().count() == price.get(variety.rarity()),
                        variety + " (" + variety.rarity() + ") custa " + offer.getItemCostA().count());
                    check(offer.getItemCostB().isEmpty(), "segundo custo inesperado");
                    check(offer.getMaxUses() == 4, "usos " + offer.getMaxUses());
                }
                check(strains.size() == 2, "as duas ofertas são da mesma variedade: " + strains);
                trader.discard();
            }
        });
    }

    /**
     * Aba "Cultivo": estações, planta silvestre, processados e artigos, sem sementes nem flores. Aba "Cultivo:
     * Genéticas": as 120 sementes e flores, nativas antes do catálogo e comuns antes das raras.
     */
    public static void creativeTabsSplit(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            CreativeModeTab.ItemDisplayParameters parameters =
                new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess());
            CreativeModeTab main = ModCreativeTabs.CULTIVO.get();
            CreativeModeTab strains = ModCreativeTabs.STRAINS.get();
            main.buildContents(parameters);
            strains.buildContents(parameters);
            List<String> mainIds = main.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).toList();
            List<String> strainIds = strains.getDisplayItems().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).toList();
            check(mainIds.equals(ContentIds.mainTabItems()), "aba Cultivo: " + mainIds);
            check(strainIds.equals(ContentIds.strainTabItems()), "aba Genéticas: " + strainIds);
            check(strainIds.size() == 120, "itens na aba Genéticas: " + strainIds.size());
            check(strainIds.get(0).equals("brisa_seeds") && strainIds.get(9).equals("manga_gelada_seeds")
                && strainIds.get(119).equals("bananona_dried_flower"), "ordem da aba Genéticas");
            check(main.getDisplayItems().stream().noneMatch(stack -> ModItems.varietyOf(stack) != null),
                "semente ou flor na aba Cultivo");
            check(strains.getDisplayItems().stream().allMatch(stack -> ModItems.varietyOf(stack) != null),
                "item sem variedade na aba Genéticas");
        });
    }

    // ------------------------------------------------------------------ estado visual das estações

    private static StationContents contentsAt(GameTestHelper helper, BlockPos rel) {
        return helper.getLevel().getBlockState(helper.absolutePos(rel)).getValue(ProcessingStationBlock.CONTENTS);
    }

    /** Coloca uma estação pelo caminho real (item na mão, clique no topo de um bloco) com o jogador olhando para yRot. */
    private static BlockPos placeStationFacing(GameTestHelper helper, StationKind kind, BlockPos floorRel, float yRot) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setYRot(yRot);
        helper.setBlock(floorRel, Blocks.STONE.defaultBlockState());
        BlockPos floor = helper.absolutePos(floorRel);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STATIONS.get(kind).get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0D, 0.5D, 0.0D), Direction.UP, floor, false);
        InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
            InteractionHand.MAIN_HAND, hit);
        check(result.consumesAction(), "a estação não foi colocada: " + result);
        check(helper.getLevel().getBlockState(floor.above()).is(ModBlocks.STATIONS.get(kind).get()), "estação ausente");
        return floorRel.above();
    }

    /**
     * O blockstate mostra o conteúdo: entrada → working, só saída → done, nada → empty; a BlockEntity é a mesma o
     * tempo todo (a troca de estado não derruba nem recria os itens). A frente da estação olha para quem a colocou.
     */
    public static void stationVisualState(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ProcessingStationBlockEntity station = placeStation(helper, StationKind.DRYING);
            BlockPos pos = helper.absolutePos(SOIL);
            check(contentsAt(helper, SOIL) == StationContents.EMPTY, "estação nova deveria estar vazia");

            check(station.insert(level, withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 1, 3)) == 1,
                "não inseriu a flor fresca");
            check(contentsAt(helper, SOIL) == StationContents.WORKING, "com entrada deveria estar trabalhando");
            check(level.getBlockEntity(pos) == station, "a troca de estado recriou a BlockEntity");

            station.setProgress(StationKind.DRYING.defaultTicks() - 1);
            station.serverTick(level);
            check(station.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get()),
                "o ciclo não terminou");
            check(contentsAt(helper, SOIL) == StationContents.DONE, "só com saída deveria estar pronta");
            check(level.getBlockEntity(pos) == station, "a BlockEntity mudou ao terminar o ciclo");
            check(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D)).isEmpty(),
                "a troca de estado derrubou itens");

            ItemStack taken = station.extract();
            check(taken.getCount() == 1, "retirada da saída: " + taken);
            check(contentsAt(helper, SOIL) == StationContents.EMPTY, "depois de retirar deveria estar vazia");

            // Entrada e saída juntas: a entrada manda (working); retirar a saída mantém working
            ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, SOIL.east(2));
            sieve.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(ModItems.RESIN.get(), 1, 3));
            check(contentsAt(helper, SOIL.east(2)) == StationContents.DONE, "peneira com saída deveria estar pronta");
            sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 2, 3));
            check(contentsAt(helper, SOIL.east(2)) == StationContents.WORKING, "entrada + saída deveria estar trabalhando");
            sieve.extract();
            check(contentsAt(helper, SOIL.east(2)) == StationContents.WORKING, "sem a saída, a entrada segue trabalhando");

            // Colocação real: olhando para o sul (yRot 0) a frente fica para o norte; olhando para o oeste, para o leste
            BlockPos north = placeStationFacing(helper, StationKind.PRESSING, new BlockPos(1, 1, 7), 0.0F);
            check(level.getBlockState(helper.absolutePos(north)).getValue(ProcessingStationBlock.FACING) == Direction.NORTH,
                "jogador olhando para o sul deveria deixar facing=north");
            check(contentsAt(helper, north) == StationContents.EMPTY, "estação colocada deveria estar vazia");
            BlockPos east = placeStationFacing(helper, StationKind.SIEVING, new BlockPos(7, 1, 7), 90.0F);
            check(level.getBlockState(helper.absolutePos(east)).getValue(ProcessingStationBlock.FACING) == Direction.EAST,
                "jogador olhando para o oeste deveria deixar facing=east");
        });
    }

    // ------------------------------------------------------------------ usos dos artigos (mecânica fictícia de jogo)

    /** Baseado + piteira na bancada (transmute joint_filtered); confere que a receita é essa e devolve o resultado. */
    private static ItemStack craftFilter(GameTestHelper helper, ItemStack joint) {
        ServerLevel level = helper.getLevel();
        CraftingInput input = CraftingInput.of(2, 1, List.of(joint, new ItemStack(ModItems.FILTER_TIP.get())));
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        check(recipe.isPresent(), "sem receita de piteira para " + joint);
        check(recipe.get().id().identifier().equals(Cultivo.id("joint_filtered")), "receita " + recipe.get().id());
        ItemStack result = recipe.get().value().assemble(input);
        check(result.is(ModItems.JOINT.get()) && result.getCount() == 1, "resultado " + result);
        return result;
    }

    private static boolean filtered(ItemStack stack) {
        return stack.has(ModComponents.FILTERED.get());
    }

    /**
     * Piteira na bancada: o baseado de qualquer variedade sai com cultivo:filtered e mantém variedade e qualidade (o
     * transmute copia os componentes da entrada). Um baseado que já tem piteira não aceita outra: a receita não casa.
     */
    public static void jointFilterRecipe(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            for (Variety variety : Variety.values()) {
                ItemStack result = craftFilter(helper, joint(variety, 4));
                check(filtered(result), variety + ": sem cultivo:filtered");
                check(result.get(ModComponents.STRAIN.get()) == variety, variety + ": variedade " + result.get(ModComponents.STRAIN.get()));
                check(Integer.valueOf(4).equals(quality(result)), variety + ": qualidade " + quality(result));
            }
            ItemStack aurora = craftFilter(helper, joint(Variety.AURORA, 4));
            CraftingInput again = CraftingInput.of(2, 1, List.of(aurora, new ItemStack(ModItems.FILTER_TIP.get())));
            check(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, again, level).isEmpty(),
                "um baseado com piteira aceitou outra piteira");
            // Baseado sem variedade (de /give) também ganha piteira e continua sem variedade
            ItemStack legacy = craftFilter(helper, new ItemStack(ModItems.JOINT.get()));
            check(filtered(legacy) && legacy.get(ModComponents.STRAIN.get()) == null, "baseado sem variedade: " + legacy);
            // A marca sobrevive a salvar (NBT) e à rede, como {} no JSON
            var ops = level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            com.google.gson.JsonElement json = ItemStack.CODEC.encodeStart(ops, aurora).getOrThrow();
            check(json.getAsJsonObject().getAsJsonObject("components").get("cultivo:filtered").isJsonObject(), "formato salvo: " + json);
            check(ItemStack.matches(aurora, ItemStack.CODEC.parse(ops, json).getOrThrow()), "não persistiu: " + json);
            net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), level.registryAccess());
            ItemStack.STREAM_CODEC.encode(buf, aurora);
            check(ItemStack.matches(aurora, ItemStack.STREAM_CODEC.decode(buf)), "não sincronizou o baseado com piteira");
        });
    }

    private static int nauseaTicks(Player player) {
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        return nausea == null ? 0 : nausea.getDuration();
    }

    /**
     * Fumar o baseado com piteira (Brisa qualidade 2): chapado 1125 (900 × 1,25) e náusea 80 (160 ÷ 2); o comum dá 900
     * e 160. Variedade do catálogo com piteira segue o perfil da genética com a sessão mais longa.
     */
    public static void smokeFilteredJoint(GameTestHelper helper) {
        run(helper, () -> {
            Player filtered = smoker(helper, craftFilter(helper, joint(Variety.BRISA, 2)), lighter());
            check(smokeOnce(helper, filtered) == InteractionResult.CONSUME, "uso recusado");
            check(highTicks(filtered) == 1125, "chapado com piteira: " + highTicks(filtered));
            check(nauseaTicks(filtered) == 80, "náusea com piteira: " + nauseaTicks(filtered));
            check(filtered.hasEffect(MobEffects.SPEED), "a sativa ainda dá Velocidade");
            check(filtered.getMainHandItem().isEmpty(), "o baseado com piteira deveria ser consumido");
            check(lighterDamage(filtered) == 1, "isqueiro não gastou");

            Player plain = smoker(helper, joint(Variety.BRISA, 2), lighter());
            check(smokeOnce(helper, plain) == InteractionResult.CONSUME, "uso recusado (comum)");
            check(highTicks(plain) == 900 && nauseaTicks(plain) == 160,
                "baseado comum: " + highTicks(plain) + " / " + nauseaTicks(plain));

            Player indica = smoker(helper, craftFilter(helper, joint(Variety.MIRTILO, 2)), lighter());
            check(smokeOnce(helper, indica) == InteractionResult.CONSUME, "uso recusado (catálogo)");
            MobEffectInstance slowness = indica.getEffect(MobEffects.SLOWNESS);
            check(highTicks(indica) == 1125 && slowness != null && slowness.getDuration() == 563,
                "índica do catálogo com piteira: " + highTicks(indica) + " / " + slowness);
        });
    }

    private static boolean grinderAt(GameTestHelper helper, BlockPos rel) {
        return SieveBlock.hasGrinder(helper.getLevel().getBlockState(helper.absolutePos(rel)));
    }

    /**
     * Dichavador pelo clique real: instalar gasta 1 e liga grinder=true (um segundo não entra); flor qualidade 2 rende
     * 2 resinas e qualidade 4 rende 3; sem agachar a mão vazia ainda retira a saída; agachado e de mãos vazias o
     * dichavador volta ao inventário; quebrar a peneira com ele derruba a peneira e o dichavador.
     */
    public static void sieveGrinder(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            player.getInventory().setSelectedSlot(8);
            ProcessingStationBlockEntity sieve = placeStation(helper, StationKind.SIEVING);
            BlockPos pos = helper.absolutePos(SOIL);
            check(!grinderAt(helper, SOIL), "peneira nova já com dichavador");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRINDER.get(), 2));
            check(rightClick(helper, player, SOIL).consumesAction(), "não instalou o dichavador");
            check(grinderAt(helper, SOIL), "grinder deveria virar true");
            check(player.getMainHandItem().getCount() == 1, "instalar deveria gastar 1 dichavador");
            check(level.getBlockEntity(pos) == sieve, "instalar recriou a BlockEntity");
            rightClick(helper, player, SOIL);
            check(player.getMainHandItem().getCount() == 1, "gastou um segundo dichavador");

            check(sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 2)) == 1, "recusou a flor");
            check(contentsAt(helper, SOIL) == StationContents.WORKING && grinderAt(helper, SOIL),
                "trabalhando deveria manter o dichavador no blockstate");
            sieve.setProgress(StationKind.SIEVING.defaultTicks() - 1);
            sieve.serverTick(level);
            ItemStack resin = sieve.getItem(ProcessingStationBlockEntity.OUTPUT);
            check(resin.is(ModItems.RESIN.get()) && resin.getCount() == 2 && Integer.valueOf(2).equals(quality(resin)),
                "qualidade 2 com dichavador deveria render 2: " + resin);
            check(grinderAt(helper, SOIL), "terminar o ciclo tirou o dichavador do blockstate");

            // Sem agachar, a mão vazia retira a saída e o dichavador fica
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(rightClick(helper, player, SOIL).consumesAction(), "mão vazia não retirou a resina");
            check(countInInventory(player, ModItems.RESIN.get()) == 2 && grinderAt(helper, SOIL), "a mão vazia mexeu no dichavador");

            // Qualidade 4 de uma variedade do catálogo: 1 + bônus de qualidade + dichavador = 3
            sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.MANGA_GELADA).get(), 1, 4));
            sieve.setProgress(StationKind.SIEVING.defaultTicks() - 1);
            sieve.serverTick(level);
            ItemStack bonus = sieve.extract();
            check(bonus.getCount() == 3 && bonus.get(ModComponents.STRAIN.get()) == Variety.MANGA_GELADA,
                "qualidade 4 com dichavador deveria render 3: " + bonus);

            player.setShiftKeyDown(true);
            check(rightClick(helper, player, SOIL).consumesAction(), "agachado de mãos vazias não tirou o dichavador");
            check(!grinderAt(helper, SOIL), "grinder deveria voltar a false");
            check(countInInventory(player, ModItems.GRINDER.get()) == 1, "o dichavador não voltou ao inventário");
            player.setShiftKeyDown(false);

            // Sem dichavador a mesma flor qualidade 2 volta a render 1
            sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 2));
            sieve.setProgress(StationKind.SIEVING.defaultTicks() - 1);
            sieve.serverTick(level);
            check(sieve.extract().getCount() == 1, "sem dichavador a qualidade 2 rende 1");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRINDER.get()));
            rightClick(helper, player, SOIL);
            check(grinderAt(helper, SOIL), "não reinstalou o dichavador");
            level.destroyBlock(pos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            int sieves = 0;
            int grinders = 0;
            for (ItemEntity entity : drops) {
                if (entity.getItem().is(ModItems.STATIONS.get(StationKind.SIEVING).get())) {
                    sieves += entity.getItem().getCount();
                } else if (entity.getItem().is(ModItems.GRINDER.get())) {
                    grinders += entity.getItem().getCount();
                }
            }
            check(sieves == 1 && grinders == 1, "quebrar com dichavador: " + sieves + " peneira(s), " + grinders + " dichavador(es)");
            drops.forEach(ItemEntity::discard);
        });
    }

    /** ServerPlayer de teste que guarda as chaves das mensagens acima da barra de itens (o mock comum não tem conexão). */
    private static ServerPlayer capturingServerPlayer(GameTestHelper helper, GameType type, List<String> keys) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tray-msg"),
            net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public GameType gameMode() {
                return type;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }

            @Override
            public void sendOverlayMessage(Component message) {
                if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
                    keys.add(t.getKey());
                }
            }
        };
        type.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static ItemStack firstStack(Player player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Bandeja pelo clique real: 3 flores Aurora qualidade 3 na mão e 4 folhas de seda → gasta 2 flores e 3 folhas e
     * entrega 3 baseados Aurora qualidade 3, iguais (mesma pilha) ao baseado da bancada feito da mesma flor. Com 1 flor,
     * com 2 folhas ou com qualidades misturadas nas mãos, avisa e não gasta nada. No criativo não gasta nada.
     */
    public static void trayRollsJoints(GameTestHelper helper) {
        run(helper, () -> {
            helper.setBlock(SOIL, ModBlocks.ROLLING_TRAY.get().defaultBlockState());
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            Item paper = ModItems.ROLLING_PAPER.get();
            Item joint = ModItems.JOINT.get();
            List<String> keys = new ArrayList<>();
            ServerPlayer player = capturingServerPlayer(helper, GameType.SURVIVAL, keys);
            player.getInventory().setSelectedSlot(8);
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 3, 3));
            player.getInventory().add(new ItemStack(paper, 4));

            check(rightClick(helper, player, SOIL).consumesAction(), "a bandeja não enrolou");
            check(keys.isEmpty(), "aviso ao enrolar: " + keys);
            check(player.getMainHandItem().getCount() == 1, "deveria gastar 2 flores, sobrou " + player.getMainHandItem());
            check(countInInventory(player, paper) == 1, "deveria gastar 3 folhas, sobraram " + countInInventory(player, paper));
            check(countInInventory(player, joint) == 3, "baseados: " + countInInventory(player, joint));
            ItemStack rolled = firstStack(player, joint);
            check(rolled.get(ModComponents.STRAIN.get()) == Variety.AURORA && Integer.valueOf(3).equals(quality(rolled)),
                "baseado da bandeja: " + rolled);
            check(ItemStack.isSameItemSameComponents(rolled, craftJoint(helper, withQuality(aurora, 1, 3))),
                "o baseado da bandeja não empilha com o da bancada");

            // Só 1 flor na mão: avisa e não gasta
            keys.clear();
            check(rightClick(helper, player, SOIL).consumesAction(), "a recusa deveria tratar o clique");
            check(keys.equals(List.of("message.cultivo.tray_need_flowers")), "aviso com 1 flor: " + keys);
            check(player.getMainHandItem().getCount() == 1 && countInInventory(player, paper) == 1
                && countInInventory(player, joint) == 3, "gastou algo com 1 flor");

            // 2 flores e só 2 folhas: avisa e não gasta
            keys.clear();
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 2, 3));
            player.getInventory().add(new ItemStack(paper, 1));
            rightClick(helper, player, SOIL);
            check(keys.equals(List.of("message.cultivo.tray_need_papers")), "aviso com 2 folhas: " + keys);
            check(player.getMainHandItem().getCount() == 2 && countInInventory(player, paper) == 2
                && countInInventory(player, joint) == 3, "gastou algo com 2 folhas");

            // Qualidades misturadas nas duas mãos (1 de cada): não junta pilhas diferentes, avisa e não gasta
            keys.clear();
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 1, 3));
            player.setItemInHand(InteractionHand.OFF_HAND, withQuality(aurora, 1, 2));
            player.getInventory().add(new ItemStack(paper, 3));
            rightClick(helper, player, SOIL);
            check(keys.equals(List.of("message.cultivo.tray_need_flowers")), "aviso com qualidades misturadas: " + keys);
            check(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 1
                && countInInventory(player, paper) == 5 && countInInventory(player, joint) == 3, "misturou qualidades");

            // Criativo: enrola uma variedade do catálogo sem gastar flores nem folhas
            List<String> creativeKeys = new ArrayList<>();
            ServerPlayer creative = capturingServerPlayer(helper, GameType.CREATIVE, creativeKeys);
            creative.getInventory().setSelectedSlot(8);
            check(creative.hasInfiniteMaterials(), "preparo: jogador criativo");
            creative.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.DRIED_FLOWERS.get(Variety.MANGA_GELADA).get(), 2, 4));
            creative.getInventory().add(new ItemStack(paper, 3));
            check(rightClick(helper, creative, SOIL).consumesAction(), "a bandeja não enrolou no criativo");
            check(creativeKeys.isEmpty(), "aviso no criativo: " + creativeKeys);
            check(creative.getMainHandItem().getCount() == 2 && countInInventory(creative, paper) == 3, "o criativo gastou itens");
            ItemStack creativeJoints = firstStack(creative, joint);
            check(creativeJoints.getCount() == 3 && creativeJoints.get(ModComponents.STRAIN.get()) == Variety.MANGA_GELADA
                && Integer.valueOf(4).equals(quality(creativeJoints)), "baseados do criativo: " + creativeJoints);
        });
    }

    private static JarBlockEntity placeJar(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, ModBlocks.JAR.get().defaultBlockState());
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        check(be instanceof JarBlockEntity, "pote sem BlockEntity");
        return (JarBlockEntity) be;
    }

    private static boolean jarFilled(GameTestHelper helper, BlockPos rel) {
        return helper.getLevel().getBlockState(helper.absolutePos(rel)).getValue(JarBlock.FILLED);
    }

    private static int comparator(GameTestHelper helper, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        return helper.getLevel().getBlockState(pos).getAnalogOutputSignal(helper.getLevel(), pos, Direction.NORTH);
    }

    /**
     * Pote pelo clique real: guarda 10 flores Brisa qualidade 2 (filled=true, comparador 3, ticker ligado); o contador
     * no penúltimo tick cura a pilha inteira para qualidade 3 com cultivo:cured, uma vez só; a pilha curada não junta
     * com flores não curadas; a mão vazia retira tudo; mudar o conteúdo zera o contador; resina não cura; uma variedade
     * do catálogo vai de 4 a 5; quebrar o pote derruba a pilha.
     */
    public static void jarCuresFlowers(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            player.getInventory().setSelectedSlot(8);
            JarBlockEntity jar = placeJar(helper, SOIL);
            BlockPos pos = helper.absolutePos(SOIL);
            Item brisa = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            check(!jarFilled(helper, SOIL) && comparator(helper, SOIL) == 0, "pote novo deveria estar vazio");
            check(level.getBlockState(pos).getTicker(level, ModBlockEntities.JAR.get()) == null, "pote vazio não precisa de ticker");

            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(brisa, 10, 2));
            check(rightClick(helper, player, SOIL).consumesAction(), "o pote não guardou as flores");
            check(jar.getItem().getCount() == 10 && player.getMainHandItem().isEmpty(), "guardou " + jar.getItem());
            check(jarFilled(helper, SOIL), "filled deveria virar true");
            check(level.getBlockEntity(pos) == jar, "filled recriou a BlockEntity");
            check(comparator(helper, SOIL) == 3, "comparador com 10 de 64: " + comparator(helper, SOIL));
            check(level.getBlockState(pos).getTicker(level, ModBlockEntities.JAR.get()) != null, "pote cheio sem ticker");

            // Flor fresca não entra no pote (não está na tag)
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 3, 2));
            rightClick(helper, player, SOIL);
            check(jar.getItem().getCount() == 10 && player.getMainHandItem().getCount() == 3, "flor fresca entrou no pote");

            jar.setCureProgress(CuringRules.CURE_TICKS - 1);
            jar.serverTick(level);
            ItemStack cured = jar.getItem();
            check(cured.getCount() == 10 && Integer.valueOf(3).equals(quality(cured)) && cured.has(ModComponents.CURED.get()),
                "a cura deveria dar qualidade 3 e cultivo:cured: " + cured);
            check(jar.cureProgress() == 0, "o contador deveria zerar depois da cura");

            jar.setCureProgress(CuringRules.CURE_TICKS - 1);
            jar.serverTick(level);
            check(Integer.valueOf(3).equals(quality(jar.getItem())) && jar.cureProgress() == 0, "curou duas vezes: " + jar.getItem());

            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(brisa, 5, 3));
            rightClick(helper, player, SOIL);
            check(jar.getItem().getCount() == 10 && player.getMainHandItem().getCount() == 5,
                "a pilha curada juntou com flores não curadas");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(rightClick(helper, player, SOIL).consumesAction(), "a mão vazia não retirou");
            check(countInInventory(player, brisa) == 10 && firstStack(player, brisa).has(ModComponents.CURED.get()),
                "a pilha curada não voltou inteira");
            check(jar.getItem().isEmpty() && !jarFilled(helper, SOIL) && comparator(helper, SOIL) == 0, "o pote deveria esvaziar");

            // Mudar o conteúdo zera o contador
            jar.insert(withQuality(brisa, 4, 2));
            jar.setCureProgress(2000);
            check(jar.insert(withQuality(brisa, 1, 2)) == 1 && jar.cureProgress() == 0, "inserir não zerou o contador");

            // Resina não cura
            jar.setItem(withQuality(ModItems.RESIN.get(), 4, 2));
            jar.setCureProgress(CuringRules.CURE_TICKS - 1);
            jar.serverTick(level);
            check(Integer.valueOf(2).equals(quality(jar.getItem())) && !jar.getItem().has(ModComponents.CURED.get())
                && jar.cureProgress() == 0, "resina curou: " + jar.getItem());

            // Variedade do catálogo: estufa 4 → 5
            jar.setItem(withQuality(ModItems.DRIED_FLOWERS.get(Variety.GELATO_AZUL_42).get(), 3, 4));
            jar.setCureProgress(CuringRules.CURE_TICKS - 1);
            jar.serverTick(level);
            check(Integer.valueOf(5).equals(quality(jar.getItem())) && jar.getItem().has(ModComponents.CURED.get()),
                "catálogo 4 → 5: " + jar.getItem());

            jar.setItem(withQuality(brisa, 7, 2));
            level.destroyBlock(pos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            int flowers = 0;
            int jars = 0;
            for (ItemEntity entity : drops) {
                if (entity.getItem().is(brisa)) {
                    flowers += entity.getItem().getCount();
                } else if (entity.getItem().is(ModItems.ARTICLES.get(ContentIds.JAR).get())) {
                    jars += entity.getItem().getCount();
                }
            }
            check(flowers == 7 && jars == 1, "quebrar o pote: " + flowers + " flores, " + jars + " pote(s)");
            drops.forEach(ItemEntity::discard);
        });
    }

    private static int ashAt(GameTestHelper helper, BlockPos rel) {
        return helper.getLevel().getBlockState(helper.absolutePos(rel)).getValue(AshtrayBlock.ASH);
    }

    /**
     * Cinzeiro a 2 blocos: 4 baseados Brisa qualidade 2 (4 × 900 = 3600 ticks, abaixo do limite de 6600) enchem até
     * ash=4 (comparador 12). A 5ª sessão não passa de 4 e cai no próximo cinzeiro mais perto (a 3 blocos); o de 6
     * blocos fica em 0. Segurar um item não esvazia; a mão vazia tira 1 farinha de osso e zera.
     */
    public static void ashtrayCollectsAsh(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos near = new BlockPos(3, 1, 4);
            BlockPos next = new BlockPos(1, 1, 7);
            BlockPos far = new BlockPos(7, 1, 4);
            for (BlockPos rel : List.of(near, next, far)) {
                helper.setBlock(rel, ModBlocks.ASHTRAY.get().defaultBlockState());
            }
            ItemStack joints = joint(Variety.BRISA, 2);
            joints.setCount(5);
            Player player = smoker(helper, joints, lighter());
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 4))));
            for (int session = 1; session <= 4; session++) {
                check(smokeOnce(helper, player) == InteractionResult.CONSUME, "sessão " + session + " recusada");
                check(ashAt(helper, near) == session, "sessão " + session + ": cinza " + ashAt(helper, near));
            }
            check(highTicks(player) == 3600, "4 sessões de 900: " + highTicks(player));
            check(comparator(helper, near) == 12, "comparador cheio: " + comparator(helper, near));
            check(ashAt(helper, next) == 0, "a cinza foi para o cinzeiro mais longe antes de encher o mais perto");

            check(smokeOnce(helper, player) == InteractionResult.CONSUME, "5ª sessão recusada");
            check(ashAt(helper, near) == 4, "passou de 4: " + ashAt(helper, near));
            check(ashAt(helper, next) == 1, "a 5ª sessão deveria cair no próximo cinzeiro: " + ashAt(helper, next));
            check(ashAt(helper, far) == 0, "o cinzeiro a 6 blocos pegou cinza: " + ashAt(helper, far));

            ServerPlayer emptier = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            emptier.getInventory().setSelectedSlot(8);
            emptier.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            rightClick(helper, emptier, near);
            check(ashAt(helper, near) == 4, "segurar um graveto esvaziou o cinzeiro");

            BlockPos pos = helper.absolutePos(near);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D)).forEach(ItemEntity::discard);
            emptier.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(rightClick(helper, emptier, near).consumesAction(), "a mão vazia não esvaziou o cinzeiro cheio");
            check(ashAt(helper, near) == 0 && comparator(helper, near) == 0, "o cinzeiro deveria zerar");
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
            int boneMeal = drops.stream().filter(e -> e.getItem().is(Items.BONE_MEAL)).mapToInt(e -> e.getItem().getCount()).sum();
            check(boneMeal == 1, "farinha de osso: " + boneMeal);
            drops.forEach(ItemEntity::discard);
            check(!rightClick(helper, emptier, next).consumesAction(), "esvaziou um cinzeiro que não estava cheio");
            check(ashAt(helper, next) == 1, "a mão vazia mexeu num cinzeiro com 1 de cinza");
        });
    }

    /** Coloca um item de bloco pelo caminho real (clique no topo de um bloco de pedra) com o jogador olhando para yRot. */
    private static BlockPos placeWithItem(GameTestHelper helper, ItemStack stack, BlockPos floorRel, float yRot) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setYRot(yRot);
        helper.setBlock(floorRel, Blocks.STONE.defaultBlockState());
        BlockPos floor = helper.absolutePos(floorRel);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0D, 0.5D, 0.0D), Direction.UP, floor, false);
        InteractionResult result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
            InteractionHand.MAIN_HAND, hit);
        check(result.consumesAction(), stack + " não foi colocado: " + result);
        return floorRel.above();
    }

    /**
     * Bandeja, pote e cinzeiro: o item coloca o bloco pelo caminho real (a bandeja olha para quem colocou), o bloco
     * devolve o item ao ser escolhido (pick) e, quebrado, derruba exatamente o próprio item. O cinzeiro cheio derruba
     * só a si mesmo: a cinza se perde.
     */
    public static void placeableArticlesDropSelf(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            int x = 1;
            for (String article : ContentIds.PLACEABLE_ARTICLES) {
                Item item = ModItems.ARTICLES.get(article).get();
                Block block = ModBlocks.PLACEABLE_ARTICLES.get(article).get();
                check(block.asItem() == item, article + ": o bloco não aponta para o item");
                BlockPos placed = placeWithItem(helper, new ItemStack(item), new BlockPos(x, 1, 2), 0.0F);
                x += 3;
                BlockPos pos = helper.absolutePos(placed);
                BlockState state = level.getBlockState(pos);
                check(state.is(block), article + " colocou " + state);
                if (state.hasProperty(RollingTrayBlock.FACING)) {
                    check(state.getValue(RollingTrayBlock.FACING) == Direction.NORTH, "bandeja olhando para " + state);
                }
                level.destroyBlock(pos, true);
                List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D));
                int self = drops.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
                int others = drops.stream().filter(e -> !e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
                check(self == 1 && others == 0, article + " derrubou " + self + " de si e " + others + " outro(s)");
                drops.forEach(ItemEntity::discard);
            }
            BlockPos east = placeWithItem(helper, new ItemStack(ModItems.ARTICLES.get(ContentIds.ROLLING_TRAY).get()),
                new BlockPos(1, 1, 6), 90.0F);
            check(level.getBlockState(helper.absolutePos(east)).getValue(RollingTrayBlock.FACING) == Direction.EAST,
                "jogador olhando para o oeste deveria deixar a bandeja com facing=east");

            BlockPos full = new BlockPos(6, 1, 6);
            helper.setBlock(full, ModBlocks.ASHTRAY.get().defaultBlockState().setValue(AshtrayBlock.ASH, 4));
            BlockPos fullPos = helper.absolutePos(full);
            level.destroyBlock(fullPos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(fullPos).inflate(2.0D));
            check(drops.size() == 1 && drops.get(0).getItem().is(ModItems.ARTICLES.get(ContentIds.ASHTRAY).get())
                && drops.get(0).getItem().getCount() == 1, "cinzeiro cheio derrubou " + drops);
            drops.forEach(ItemEntity::discard);
        });
    }
}
