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
import io.github.peixeangola.cultivo.core.AshtrayRules;
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
import io.github.peixeangola.cultivo.registry.ModSounds;
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
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
        TESTS.register("sound_events_registered", () -> CultivoGameTests::soundEventsRegistered);
        TESTS.register("smoke_use_effects_run", () -> CultivoGameTests::smokeUseEffectsRun);
        TESTS.register("station_effects_run", () -> CultivoGameTests::stationEffectsRun);
        TESTS.register("station_stalled_visual", () -> CultivoGameTests::stationStalledVisual);
        TESTS.register("station_hold_click_keeps_input", () -> CultivoGameTests::stationHoldClickKeepsInput);
        TESTS.register("tray_rolls_from_off_hand", () -> CultivoGameTests::trayRollsFromOffHand);
        TESTS.register("interactions_swing_once", () -> CultivoGameTests::interactionsSwingOnce);
        TESTS.register("full_inventory_drops_handouts", () -> CultivoGameTests::fullInventoryDropsHandouts);
        TESTS.register("creative_keeps_held_stacks", () -> CultivoGameTests::creativeKeepsHeldStacks);
        TESTS.register("jar_reload_resumes", () -> CultivoGameTests::jarReloadResumes);
        TESTS.register("client_prediction_matches_server", () -> CultivoGameTests::clientPredictionMatchesServer);
        TESTS.register("sieve_grinder_shape", () -> CultivoGameTests::sieveGrinderShape);
        TESTS.register("ashtray_search_edges", () -> CultivoGameTests::ashtraySearchEdges);
        TESTS.register("block_states_match_ids", () -> CultivoGameTests::blockStatesMatchIds);
        TESTS.register("tooltips_show_profile_and_marks", () -> CultivoGameTests::tooltipsShowProfileAndMarks);
        TESTS.register("serene_seasons_compat", () -> CultivoGameTests::sereneSeasonsCompat);
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
            ItemStack bonus = station.takeOutput();
            check(bonus.is(ModItems.RESIN.get()) && bonus.getCount() == 2, "qualidade 4 deveria render 2: " + bonus);
            check(Integer.valueOf(4).equals(quality(bonus)), "qualidade da resina");
            station.insert(helper.getLevel(), withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 3));
            tick(helper, station, StationKind.SIEVING.defaultTicks());
            ItemStack plain = station.takeOutput();
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

            ItemStack taken = station.takeOutput();
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
     * As tabelas de baú das 4 aldeias cobertas, com o pool que o jogo injeta de verdade (SeedLootInjector), geram sementes
     * de todas as 40 variedades, nativas (Brisa, Aurora, Eclipse) e do catálogo: cerca de metade dos baús tem semente; as
     * pilhas têm 1–3 sementes nas comuns (e o 3 aparece), 1–2 nas incomuns (e o 2 aparece) e sempre 1 nas raras; e, por
     * variedade, a média fica a ±25% do esperado em 1600 baús: 1600 × 4/238 × 2 = 53,8 (comum), 1600 × 2/238 × 1,5 = 20,2
     * (incomum) e 1600 × 1/238 × 1 = 6,7 (rara). O sorteio usa uma semente fixa, então o resultado é sempre o mesmo.
     */
    public static void villageChestSeeds(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            RandomSource random = RandomSource.create(20_261_024L);
            Map<Variety, Integer> totals = new EnumMap<>(Variety.class);
            Map<Variety.Rarity, Set<Integer>> stackSizes = new EnumMap<>(Variety.Rarity.class);
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
                            stackSizes.computeIfAbsent(seed.variety().rarity(), r -> new java.util.TreeSet<>()).add(stack.getCount());
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
            check(totals.keySet().containsAll(List.of(Variety.BRISA, Variety.AURORA, Variety.ECLIPSE)),
                "as nativas sumiram dos baús: " + totals.keySet());
            check(totals.size() == 40, "só " + totals.size() + " das 40 variedades apareceram: " + totals.keySet());
            // Quantidades por pilha: comum 1–3, incomum 1–2, rara 1
            check(Set.of(1, 2, 3).equals(stackSizes.get(Variety.Rarity.COMMON)), "pilhas comuns: " + stackSizes);
            check(Set.of(1, 2).equals(stackSizes.get(Variety.Rarity.UNCOMMON)), "pilhas incomuns: " + stackSizes);
            check(Set.of(1).equals(stackSizes.get(Variety.Rarity.RARE)), "pilhas raras: " + stackSizes);
            Map<Variety.Rarity, Integer> byRarity = new EnumMap<>(Variety.Rarity.class);
            totals.forEach((variety, n) -> byRarity.merge(variety.rarity(), n, Integer::sum));
            // Média por variedade: 25 comuns, 4 incomuns e 11 raras
            double common = byRarity.getOrDefault(Variety.Rarity.COMMON, 0) / 25.0;
            double uncommon = byRarity.getOrDefault(Variety.Rarity.UNCOMMON, 0) / 4.0;
            double rare = byRarity.getOrDefault(Variety.Rarity.RARE, 0) / 11.0;
            String averages = "sementes por variedade: comum " + common + ", incomum " + uncommon + ", rara " + rare;
            check(common >= 53.8 * 0.75 && common <= 53.8 * 1.25, averages);
            check(uncommon >= 20.2 * 0.75 && uncommon <= 20.2 * 1.25, averages);
            check(rare >= 6.7 * 0.75 && rare <= 6.7 * 1.25, averages);
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
            ItemStack resin = sieve.takeOutput();
            check(resin.getCount() == 2 && resin.get(ModComponents.STRAIN.get()) == Variety.ECLIPSE, "resina " + resin);

            ProcessingStationBlockEntity press = placeStation(helper, StationKind.PRESSING);
            press.insert(helper.getLevel(), resin);
            tick(helper, press, StationKind.PRESSING.defaultTicks());
            ItemStack hash = press.takeOutput();
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
     * progresso, e a terra é colocada ao lado como no vanilla; a mão principal vazia tira só a saída; agachado e de mãos
     * vazias a entrada volta; com a estação vazia a mão secundária insere.
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
            // Sem saída, a mão vazia não faz nada: a entrada em andamento fica e o progresso também
            check(!rightClick(helper, player, SOIL).consumesAction(), "mão vazia sem saída tratou o clique");
            check(station.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 16 && station.progress() == 590,
                "mão vazia sem saída puxou a entrada ou zerou o progresso");
            // Agachado e de mãos vazias (o gesto de propósito): a entrada volta e o progresso zera
            player.setShiftKeyDown(true);
            check(rightClick(helper, player, SOIL).consumesAction(), "agachado não devolveu a entrada");
            player.setShiftKeyDown(false);
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
     * Anota em {@code firstSeen[i]} o primeiro tick do teste em que a saída da estação i aparece. Roda a cada tick: o
     * sucesso do teste não pode pular a conferência de "nada sai antes da hora" (depois do succeed, o vanilla para de rodar
     * os eventos agendados, então um runAfterDelay posterior ao fim nunca rodaria).
     */
    private static long[] recordFirstOutputs(GameTestHelper helper, ProcessingStationBlockEntity... stations) {
        long[] firstSeen = new long[stations.length];
        java.util.Arrays.fill(firstSeen, -1L);
        helper.onEachTick(() -> {
            for (int i = 0; i < stations.length; i++) {
                if (firstSeen[i] < 0 && !stations[i].getItem(ProcessingStationBlockEntity.OUTPUT).isEmpty()) {
                    firstSeen[i] = helper.getTick();
                }
            }
        });
        return firstSeen;
    }

    /**
     * As três estações processam só com os ticks do mundo (o ticker real da BlockEntity), sem chamar serverTick:
     * secagem 600, peneira 200 e prensa 300 ticks. Nada sai antes da hora: o tick em que cada saída aparece é anotado e
     * conferido no sucesso (margem de 2 ticks para a ordem entre o tick do mundo e o do teste), então um ciclo mais
     * curto falha mesmo que todos terminem cedo.
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

        long[] firstSeen = recordFirstOutputs(helper, drying, sieve, press);
        helper.succeedWhen(() -> {
            helper.assertTrue(drying.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get()),
                "ainda secando");
            helper.assertTrue(sieve.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.RESIN.get()), "ainda peneirando");
            helper.assertTrue(press.getItem(ProcessingStationBlockEntity.OUTPUT).is(ModItems.HASH.get()), "ainda prensando");
            helper.assertTrue(firstSeen[0] >= 0 && firstSeen[1] >= 0 && firstSeen[2] >= 0, "saídas ainda não anotadas");
            helper.assertTrue(firstSeen[0] >= 598, "secagem terminou antes de 600 ticks: " + firstSeen[0]);
            helper.assertTrue(firstSeen[1] >= 198, "peneira terminou antes de 200 ticks: " + firstSeen[1]);
            helper.assertTrue(firstSeen[2] >= 298, "prensa terminou antes de 300 ticks: " + firstSeen[2]);
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
            ItemStack dried = drying.takeOutput();
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
            ItemStack resin = sieve.takeOutput();
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
     * O blockstate mostra o conteúdo: ciclo andando → working, só saída → done, nada → empty; a BlockEntity é a mesma o
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

            ItemStack taken = station.takeOutput();
            check(taken.getCount() == 1, "retirada da saída: " + taken);
            check(contentsAt(helper, SOIL) == StationContents.EMPTY, "depois de retirar deveria estar vazia");

            // Entrada e saída juntas: com a saída aceitando o resultado (mesma resina) o ciclo anda (working); com a saída
            // de outra qualidade ele para e a estação mostra o pronto (done); sem a saída volta a trabalhar
            ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, SOIL.east(2));
            ItemStack auroraResin = withQuality(ModItems.RESIN.get(), 1, 3);
            auroraResin.set(ModComponents.STRAIN.get(), Variety.AURORA);
            sieve.setItem(ProcessingStationBlockEntity.OUTPUT, auroraResin);
            check(contentsAt(helper, SOIL.east(2)) == StationContents.DONE, "peneira com saída deveria estar pronta");
            sieve.insert(level, withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 2, 3));
            check(contentsAt(helper, SOIL.east(2)) == StationContents.WORKING, "entrada + saída que empilha: trabalhando");
            sieve.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(ModItems.RESIN.get(), 1, 4));
            check(contentsAt(helper, SOIL.east(2)) == StationContents.DONE, "saída de outra qualidade bloqueia: pronta");
            sieve.takeOutput();
            check(contentsAt(helper, SOIL.east(2)) == StationContents.WORKING, "sem a saída, a entrada volta a trabalhar");

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
            ItemStack bonus = sieve.takeOutput();
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
            check(sieve.takeOutput().getCount() == 1, "sem dichavador a qualidade 2 rende 1");

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
            // O ticker existe com ou sem conteúdo: é ele que acerta filled de um pote carregado de dados (jar_reload_resumes)
            check(level.getBlockState(pos).getTicker(level, ModBlockEntities.JAR.get()) != null, "pote vazio sem ticker");

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

    // ------------------------------------------------------------------ sons, partículas e animação (estágio 4)

    /**
     * Os 16 eventos de som do mod estão registrados no servidor, e cada evento vanilla que o sounds.json gerado
     * reaproveita existe no registro (um nome errado tocaria silêncio no cliente sem erro nenhum). O borbulhar usa o evento
     * do próprio aparelho: o narguilé tem o seu (legenda "Narguilé borbulha"), não o do bong.
     */
    public static void soundEventsRegistered(GameTestHelper helper) {
        run(helper, () -> {
            check(ContentIds.SOUNDS.size() == 16, "sons: " + ContentIds.SOUNDS.size());
            check(io.github.peixeangola.cultivo.smoke.SmokeEffects.bubbleSound(SmokeDevice.HOOKAH)
                .equals(Optional.of(ModSounds.HOOKAH_BUBBLE.get())), "o narguilé borbulha com o evento do bong");
            check(io.github.peixeangola.cultivo.smoke.SmokeEffects.bubbleSound(SmokeDevice.BONG)
                .equals(Optional.of(ModSounds.BONG_BUBBLE.get())), "o bong perdeu o borbulhar");
            for (SmokeDevice dry : List.of(SmokeDevice.JOINT, SmokeDevice.PIPE, SmokeDevice.CHILLUM)) {
                check(io.github.peixeangola.cultivo.smoke.SmokeEffects.bubbleSound(dry).isEmpty(), dry + " borbulha");
            }
            for (String id : ContentIds.SOUNDS) {
                check(BuiltInRegistries.SOUND_EVENT.containsKey(Cultivo.id(id)), "som não registrado: " + id);
            }
            com.google.gson.JsonObject sounds;
            try (java.io.InputStream in = CultivoGameTests.class.getResourceAsStream("/assets/cultivo/sounds.json")) {
                check(in != null, "sounds.json fora do classpath");
                sounds = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException e) {
                throw new Failure("sounds.json ilegível: " + e.getMessage());
            }
            check(sounds.keySet().equals(Set.copyOf(ContentIds.SOUNDS)), "sounds.json com eventos " + sounds.keySet());
            for (String id : ContentIds.SOUNDS) {
                for (com.google.gson.JsonElement sound : sounds.getAsJsonObject(id).getAsJsonArray("sounds")) {
                    Identifier vanilla = Identifier.parse(sound.getAsJsonObject().get("name").getAsString());
                    check(BuiltInRegistries.SOUND_EVENT.containsKey(vanilla), id + " aponta para evento inexistente " + vanilla);
                }
            }
        });
    }

    /**
     * Fumar cada aparelho tick a tick como no jogo (use → onUseTick em todos os ticks → finishUsingItem), no servidor:
     * os sons do isqueiro, da tragada e do borbulhar e a baforada final não atrapalham a sessão, e as partículas do uso
     * (só do cliente) não fazem nada no servidor. Sem a animação vanilla (NONE): a pose vem das extensões de cliente.
     */
    public static void smokeUseEffectsRun(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos ashtray = new BlockPos(3, 1, 4);
            helper.setBlock(ashtray, ModBlocks.ASHTRAY.get().defaultBlockState());
            for (SmokeDevice device : SmokeDevice.values()) {
                ItemStack held = device == SmokeDevice.JOINT
                    ? joint(Variety.BRISA, 2)
                    : new ItemStack(ModItems.ARTICLES.get(device.itemId()).get());
                Player player = smoker(helper, held, lighter(),
                    withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 1, 2));
                player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 4))));
                ItemStack stack = player.getMainHandItem();
                check(stack.getUseAnimation() == net.minecraft.world.item.ItemUseAnimation.NONE, device + " com animação vanilla");
                check(stack.getUseDuration(player) == device.useTicks(), device + " duração " + stack.getUseDuration(player));
                check(stack.getItem().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME, device + " recusado");
                for (int remaining = device.useTicks(); remaining > 0; remaining--) {
                    stack.getItem().onUseTick(level, player, stack, remaining);
                    io.github.peixeangola.cultivo.smoke.SmokeEffects.useTickParticles(level, player, device,
                        device.useTicks() - remaining);
                }
                stack.getItem().finishUsingItem(stack, level, player);
                player.stopUsingItem();
                check(highTicks(player) > 0, device + ": a sessão não aplicou o chapado");
                check(countInInventory(player, ModItems.DRIED_FLOWERS.get(Variety.AURORA).get()) == (device.needsFuel() ? 0 : 1),
                    device + ": material gasto errado");
            }
            check(ashAt(helper, ashtray) == AshtrayRules.MAX_ASH, "cinzeiro: " + ashAt(helper, ashtray));
        });
    }

    /**
     * Com os enfeites (som de trabalho no ritmo da animação, sopro de partículas e som de pronto), as estações ainda
     * terminam pelo ticker real: a prensa a 45 ticks do fim passa por um aperto (ciclo de 40) e entrega o haxixe; a
     * peneira com dichavador a 35 ticks do fim entrega 2 resinas (qualidade 2 + dichavador).
     */
    public static void stationEffectsRun(GameTestHelper helper) {
        BlockPos pressRel = SOIL.west(2);
        BlockPos sieveRel = SOIL.east(2);
        ProcessingStationBlockEntity press = placeStationAt(helper, StationKind.PRESSING, pressRel);
        ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, sieveRel);
        ServerLevel level = helper.getLevel();
        BlockPos sievePos = helper.absolutePos(sieveRel);
        level.setBlock(sievePos, level.getBlockState(sievePos).setValue(SieveBlock.GRINDER, true), Block.UPDATE_ALL);
        press.setItem(ProcessingStationBlockEntity.INPUT, withQuality(ModItems.RESIN.get(), 2, 2));
        press.setProgress(StationKind.PRESSING.defaultTicks() - 45);
        sieve.setItem(ProcessingStationBlockEntity.INPUT, withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 2));
        sieve.setProgress(StationKind.SIEVING.defaultTicks() - 35);
        helper.assertTrue(level.getBlockState(helper.absolutePos(pressRel)).getValue(ProcessingStationBlock.CONTENTS)
            == StationContents.WORKING, "prensa deveria estar trabalhando");
        long[] firstSeen = recordFirstOutputs(helper, press, sieve);
        helper.succeedWhen(() -> {
            ItemStack hash = press.getItem(ProcessingStationBlockEntity.OUTPUT);
            helper.assertTrue(hash.is(ModItems.HASH.get()) && hash.getCount() == 1, "ainda prensando: " + hash);
            helper.assertTrue(firstSeen[0] >= 0 && firstSeen[1] >= 0, "saídas ainda não anotadas");
            // Faltavam 45 ticks na prensa e 35 na peneira (margem de 2 ticks)
            helper.assertTrue(firstSeen[0] >= 43, "prensa terminou antes da hora: " + firstSeen[0]);
            helper.assertTrue(firstSeen[1] >= 33, "peneira terminou antes da hora: " + firstSeen[1]);
            helper.assertTrue(level.getBlockState(helper.absolutePos(pressRel)).getValue(ProcessingStationBlock.CONTENTS)
                == StationContents.DONE, "prensa pronta deveria mostrar o haxixe");
            ItemStack resin = sieve.getItem(ProcessingStationBlockEntity.OUTPUT);
            helper.assertTrue(resin.is(ModItems.RESIN.get()) && resin.getCount() == 2, "peneira com dichavador: " + resin);
        });
    }

    // ------------------------------------------------------------------ revisão: estado das estações e cliques

    /**
     * Regressão: o blockstate segue o ciclo de verdade, não só a entrada. 3 resinas na prensa: depois de um ciclo sobra 1
     * (precisa de 2) e a prensa mostra o haxixe pronto (done, o modelo press_hash), não a placa apertando para sempre;
     * parada, o progresso não anda. Tirado o haxixe, a resina parada aparece carregada (loaded); com a 2ª resina volta a
     * trabalhar. O suporte com a saída de outra qualidade fica pronto (done), parado, até a saída sair.
     */
    public static void stationStalledVisual(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ProcessingStationBlockEntity press = placeStation(helper, StationKind.PRESSING);
            check(press.insert(level, withQuality(ModItems.RESIN.get(), 3, 3)) == 3, "a prensa recusou 3 resinas");
            check(contentsAt(helper, SOIL) == StationContents.WORKING, "3 resinas deveriam trabalhar");
            press.setProgress(StationKind.PRESSING.defaultTicks() - 1);
            press.serverTick(level);
            ItemStack hash = press.getItem(ProcessingStationBlockEntity.OUTPUT);
            check(hash.is(ModItems.HASH.get()) && hash.getCount() == 1, "o ciclo não terminou: " + hash);
            check(press.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 1, "deveria sobrar 1 resina");
            check(!press.canProgress(level), "1 resina não deveria andar");
            check(contentsAt(helper, SOIL) == StationContents.DONE,
                "com 1 resina parada e o haxixe pronto deveria mostrar o haxixe: " + contentsAt(helper, SOIL));
            tick(helper, press, 60);
            check(press.progress() == 0 && contentsAt(helper, SOIL) == StationContents.DONE,
                "parada: progresso " + press.progress() + ", estado " + contentsAt(helper, SOIL));

            check(press.takeOutput().is(ModItems.HASH.get()), "a mão vazia deveria tirar o haxixe");
            check(contentsAt(helper, SOIL) == StationContents.LOADED, "1 resina sem saída: " + contentsAt(helper, SOIL));
            tick(helper, press, 5);
            check(contentsAt(helper, SOIL) == StationContents.LOADED && press.progress() == 0, "a resina parada andou");
            check(press.insert(level, withQuality(ModItems.RESIN.get(), 1, 3)) == 1, "não aceitou a 2ª resina");
            check(contentsAt(helper, SOIL) == StationContents.WORKING, "com 2 resinas deveria voltar a trabalhar");

            // Só 1 resina desde o começo: carregada, nunca trabalhando
            ProcessingStationBlockEntity single = placeStationAt(helper, StationKind.PRESSING, SOIL.west(2));
            check(single.insert(level, withQuality(ModItems.RESIN.get(), 1, 2)) == 1, "a prensa recusou 1 resina");
            tick(helper, single, 20);
            check(contentsAt(helper, SOIL.west(2)) == StationContents.LOADED && single.progress() == 0,
                "1 resina: " + contentsAt(helper, SOIL.west(2)) + " progresso " + single.progress());

            // Suporte com a saída de outra qualidade: parado e pronto (o jogador vê as flores secas e sabe que deve tirar)
            ProcessingStationBlockEntity rack = placeStationAt(helper, StationKind.DRYING, SOIL.east(2));
            rack.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 2));
            check(rack.insert(level, withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 1, 3)) == 1, "não aceitou a flor");
            tick(helper, rack, 10);
            check(contentsAt(helper, SOIL.east(2)) == StationContents.DONE && rack.progress() == 0,
                "saída bloqueada: " + contentsAt(helper, SOIL.east(2)) + " progresso " + rack.progress());
            rack.takeOutput();
            check(contentsAt(helper, SOIL.east(2)) == StationContents.WORKING, "sem a saída bloqueando deveria trabalhar");
            tick(helper, rack, 3);
            check(rack.progress() == 3, "o ciclo não andou depois de liberar a saída: " + rack.progress());
        });
    }

    /**
     * Regressão: segurar o botão com a mão vazia (o vanilla repete o clique a cada 4 ticks) tira a saída pronta e para: os
     * cliques seguintes, e o clique de outro jogador logo depois, não puxam a entrada em andamento nem zeram o progresso.
     * Só agachado e de mãos vazias a entrada volta. Na peneira com dichavador, segurar agachado devolve primeiro as flores
     * e só depois o dichavador, então tirar o dichavador nunca puxa uma entrada em andamento.
     */
    public static void stationHoldClickKeepsInput(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            player.getInventory().setSelectedSlot(8);
            Item fresh = ModItems.FRESH_FLOWERS.get(Variety.BRISA).get();
            Item dried = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            ProcessingStationBlockEntity rack = placeStation(helper, StationKind.DRYING);
            check(rack.insert(level, withQuality(fresh, 8, 2)) == 8, "preparo: entrada");
            rack.setProgress(550);
            rack.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(dried, 3, 2));

            check(rightClick(helper, player, SOIL).consumesAction(), "a mão vazia não tirou a saída");
            check(countInInventory(player, dried) == 3, "a saída não veio: " + countInInventory(player, dried));
            for (int repeat = 1; repeat <= 5; repeat++) {
                check(!rightClick(helper, player, SOIL).consumesAction(), "o clique repetido " + repeat + " tratou o clique");
            }
            check(rack.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 8 && rack.progress() == 550,
                "segurar o botão puxou a entrada ou zerou o progresso: " + rack.getItem(ProcessingStationBlockEntity.INPUT)
                    + ", progresso " + rack.progress());
            check(countInInventory(player, fresh) == 0, "segurar o botão devolveu flores frescas");

            ServerPlayer other = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            other.getInventory().setSelectedSlot(8);
            check(!rightClick(helper, other, SOIL).consumesAction(), "o clique do outro jogador tratou o clique");
            check(rack.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 8 && rack.progress() == 550
                && countInInventory(other, fresh) == 0, "o outro jogador puxou a entrada em andamento");

            player.setShiftKeyDown(true);
            check(rightClick(helper, player, SOIL).consumesAction(), "agachado não devolveu a entrada");
            check(countInInventory(player, fresh) == 8 && rack.getItem(ProcessingStationBlockEntity.INPUT).isEmpty()
                && rack.progress() == 0, "agachado: a entrada deveria voltar inteira e o progresso zerar");
            check(!rightClick(helper, player, SOIL).consumesAction(), "agachado com a estação vazia tratou o clique");

            // Peneira com dichavador trabalhando: agachado, o 1º clique devolve as flores e só o 2º tira o dichavador
            BlockPos sieveRel = SOIL.east(2);
            ProcessingStationBlockEntity sieve = placeStationAt(helper, StationKind.SIEVING, sieveRel);
            BlockPos sievePos = helper.absolutePos(sieveRel);
            level.setBlock(sievePos, level.getBlockState(sievePos).setValue(SieveBlock.GRINDER, true), Block.UPDATE_ALL);
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            check(sieve.insert(level, withQuality(aurora, 2, 3)) == 2, "preparo: flores na peneira");
            sieve.setProgress(150);
            check(rightClick(helper, player, sieveRel).consumesAction(), "agachado não tratou a peneira");
            check(countInInventory(player, aurora) == 2 && grinderAt(helper, sieveRel),
                "o 1º clique agachado deveria devolver as flores e manter o dichavador");
            check(rightClick(helper, player, sieveRel).consumesAction(), "o 2º clique agachado não tirou o dichavador");
            check(!grinderAt(helper, sieveRel) && countInInventory(player, ModItems.GRINDER.get()) == 1, "o dichavador não voltou");
            player.setShiftKeyDown(false);
        });
    }

    /**
     * Regressão: 1 flor Aurora q3 na mão principal, 10 na secundária e 5 folhas: a principal passa o clique e a
     * secundária enrola (gasta 2 flores dela e 3 folhas), sem aviso. Com folhas de menos, o aviso das folhas sai da mão
     * secundária, uma vez só, e nada é gasto. O cliente prevê o mesmo para cada mão (RollingTrayBlock#predict).
     */
    public static void trayRollsFromOffHand(GameTestHelper helper) {
        run(helper, () -> {
            helper.setBlock(SOIL, ModBlocks.ROLLING_TRAY.get().defaultBlockState());
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            Item paper = ModItems.ROLLING_PAPER.get();
            Item joint = ModItems.JOINT.get();
            List<String> keys = new ArrayList<>();
            ServerPlayer player = capturingServerPlayer(helper, GameType.SURVIVAL, keys);
            player.getInventory().setSelectedSlot(8);
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 1, 3));
            player.setItemInHand(InteractionHand.OFF_HAND, withQuality(aurora, 10, 3));
            player.getInventory().add(new ItemStack(paper, 5));
            check(RollingTrayBlock.predict(player, InteractionHand.MAIN_HAND, player.getMainHandItem()) == InteractionResult.PASS,
                "o cliente deveria passar o clique da mão principal");
            check(RollingTrayBlock.predict(player, InteractionHand.OFF_HAND, player.getOffhandItem()) == InteractionResult.SUCCESS,
                "o cliente deveria prever a mão secundária enrolando");

            check(rightClick(helper, player, SOIL).consumesAction(), "a mão secundária não enrolou");
            check(keys.isEmpty(), "aviso com uma mão capaz de enrolar: " + keys);
            check(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 8,
                "flores: principal " + player.getMainHandItem() + ", secundária " + player.getOffhandItem());
            check(countInInventory(player, paper) == 2 && countInInventory(player, joint) == 3,
                "folhas " + countInInventory(player, paper) + ", baseados " + countInInventory(player, joint));
            ItemStack rolled = firstStack(player, joint);
            check(rolled.get(ModComponents.STRAIN.get()) == Variety.AURORA && Integer.valueOf(3).equals(quality(rolled)),
                "baseado da mão secundária: " + rolled);

            // Folhas de menos (2): a principal passa e a secundária avisa das folhas, uma vez só, sem gastar nada
            keys.clear();
            check(rightClick(helper, player, SOIL).consumesAction(), "a recusa deveria tratar o clique");
            check(keys.equals(List.of("message.cultivo.tray_need_papers")), "aviso: " + keys);
            check(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 8
                && countInInventory(player, paper) == 2 && countInInventory(player, joint) == 3, "a recusa gastou algo");
        });
    }

    /** O servidor devolve SUCCESS (balanço já previsto pelo cliente), nunca SUCCESS_SERVER (balanço repetido). */
    private static void checkPredictedSwing(InteractionResult result, String what) {
        check(result instanceof InteractionResult.Success success
            && success.swingSource() == InteractionResult.SwingSource.PREDICTED, what + ": " + result);
    }

    /**
     * Regressão: cada interação que dá certo nas estações, na peneira (dichavador), no pote e na bandeja devolve, no
     * servidor, SUCCESS com o balanço previsto pelo cliente. Com SUCCESS_SERVER o servidor mandava um segundo balanço do
     * braço para quem clicou (balanço duplo com ping). A recusa da bandeja não balança (CONSUME), nos dois lados.
     */
    public static void interactionsSwingOnce(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            // A recusa da bandeja manda uma mensagem: o jogador de teste precisa guardá-la (o mock comum não tem conexão)
            ServerPlayer player = capturingServerPlayer(helper, GameType.SURVIVAL, new ArrayList<>());
            player.getInventory().setSelectedSlot(8);
            Item brisa = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            ProcessingStationBlockEntity sieve = placeStation(helper, StationKind.SIEVING);
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(brisa, 1, 2));
            checkPredictedSwing(rightClick(helper, player, SOIL), "inserir na peneira");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRINDER.get()));
            checkPredictedSwing(rightClick(helper, player, SOIL), "instalar o dichavador");
            sieve.setProgress(StationKind.SIEVING.defaultTicks() - 1);
            sieve.serverTick(level);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            checkPredictedSwing(rightClick(helper, player, SOIL), "tirar a saída");
            sieve.insert(level, withQuality(brisa, 1, 2));
            player.setShiftKeyDown(true);
            checkPredictedSwing(rightClick(helper, player, SOIL), "devolver a entrada agachado");
            checkPredictedSwing(rightClick(helper, player, SOIL), "tirar o dichavador");
            player.setShiftKeyDown(false);

            placeJar(helper, SOIL.east(2));
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(brisa, 4, 2));
            checkPredictedSwing(rightClick(helper, player, SOIL.east(2)), "guardar no pote");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            checkPredictedSwing(rightClick(helper, player, SOIL.east(2)), "tirar do pote");

            helper.setBlock(SOIL.west(2), ModBlocks.ROLLING_TRAY.get().defaultBlockState());
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 2, 3));
            player.getInventory().add(new ItemStack(ModItems.ROLLING_PAPER.get(), 3));
            checkPredictedSwing(rightClick(helper, player, SOIL.west(2)), "enrolar na bandeja");
            player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 1, 3));
            InteractionResult refused = rightClick(helper, player, SOIL.west(2));
            check(refused instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.NONE, "a recusa da bandeja balançou: " + refused);
            check(RollingTrayBlock.predict(player, InteractionHand.MAIN_HAND, player.getMainHandItem()) == InteractionResult.CONSUME,
                "o cliente deveria prever a recusa sem balançar");
        });
    }

    // ------------------------------------------------------------------ revisão: inventário cheio e criativo

    /** Enche os 36 slots do inventário com pilhas cheias de terra, menos os slots indicados. */
    private static void fillInventory(Player player, Set<Integer> keep) {
        for (int slot = 0; slot < 36; slot++) {
            if (!keep.contains(slot)) {
                player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
            }
        }
    }

    /** Jogador de teste parado num canto da área (os itens derrubados nascem aos pés dele). */
    private static ServerPlayer placedPlayer(GameTestHelper helper, GameType type, BlockPos rel) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(type);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(rel)));
        player.getInventory().setSelectedSlot(8);
        return player;
    }

    private static List<ItemEntity> dropsAround(GameTestHelper helper, Player player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3.0D));
    }

    /**
     * Regressão: com o inventário cheio, o que o bloco entrega cai aos pés do jogador em vez de sumir, também no criativo
     * (o Inventory#add do criativo zerava a pilha e dizia que tinha guardado). A bandeja (a mão segura as flores) derruba
     * os 3 baseados com variedade e qualidade. A mão vazia sempre tem lugar, então a pilha do pote (64 flores curadas), a
     * saída da estação e o dichavador vão inteiros para a mão, sem perder nada nem componentes.
     */
    public static void fullInventoryDropsHandouts(GameTestHelper helper) {
        run(helper, () -> {
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            Item paper = ModItems.ROLLING_PAPER.get();
            helper.setBlock(SOIL, ModBlocks.ROLLING_TRAY.get().defaultBlockState());
            for (GameType type : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                ServerPlayer player = placedPlayer(helper, type, new BlockPos(4, 1, 1));
                fillInventory(player, Set.of(0, 8));
                player.getInventory().setItem(0, new ItemStack(paper, 4));
                player.setItemInHand(InteractionHand.MAIN_HAND, withQuality(aurora, 3, 3));
                dropsAround(helper, player).forEach(ItemEntity::discard);
                check(rightClick(helper, player, SOIL).consumesAction(), type + ": a bandeja não enrolou");
                List<ItemEntity> drops = dropsAround(helper, player);
                int joints = 0;
                for (ItemEntity entity : drops) {
                    ItemStack stack = entity.getItem();
                    check(stack.is(ModItems.JOINT.get()) && stack.get(ModComponents.STRAIN.get()) == Variety.AURORA
                        && Integer.valueOf(3).equals(quality(stack)), type + ": derrubou " + stack);
                    joints += stack.getCount();
                }
                check(joints == 3, type + ": baseados derrubados " + joints);
                check(countInInventory(player, ModItems.JOINT.get()) == 0, type + ": baseado dentro do inventário cheio");
                drops.forEach(ItemEntity::discard);
            }

            for (GameType type : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                ServerPlayer player = placedPlayer(helper, type, new BlockPos(4, 1, 1));
                fillInventory(player, Set.of(8));
                dropsAround(helper, player).forEach(ItemEntity::discard);

                JarBlockEntity jar = placeJar(helper, SOIL.east(2));
                ItemStack cured = withQuality(aurora, 64, 4);
                cured.set(ModComponents.CURED.get(), Unit.INSTANCE);
                jar.setItem(cured.copy());
                check(rightClick(helper, player, SOIL.east(2)).consumesAction(), type + ": o pote não entregou");
                check(ItemStack.matches(player.getMainHandItem(), cured), type + ": o pote deu " + player.getMainHandItem());
                check(jar.getItem().isEmpty(), type + ": o pote deveria esvaziar");

                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ProcessingStationBlockEntity press = placeStationAt(helper, StationKind.PRESSING, SOIL.west(2));
                ItemStack hash = withQuality(ModItems.HASH.get(), 3, 5);
                hash.set(ModComponents.STRAIN.get(), Variety.ECLIPSE);
                press.setItem(ProcessingStationBlockEntity.OUTPUT, hash.copy());
                check(rightClick(helper, player, SOIL.west(2)).consumesAction(), type + ": a prensa não entregou");
                check(ItemStack.matches(player.getMainHandItem(), hash), type + ": a prensa deu " + player.getMainHandItem());

                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                BlockPos sieveRel = new BlockPos(4, 1, 7);
                placeStationAt(helper, StationKind.SIEVING, sieveRel);
                BlockPos sievePos = helper.absolutePos(sieveRel);
                helper.getLevel().setBlock(sievePos, helper.getLevel().getBlockState(sievePos).setValue(SieveBlock.GRINDER, true),
                    Block.UPDATE_ALL);
                player.setShiftKeyDown(true);
                check(rightClick(helper, player, sieveRel).consumesAction(), type + ": não tirou o dichavador");
                player.setShiftKeyDown(false);
                check(player.getMainHandItem().is(ModItems.GRINDER.get()) && player.getMainHandItem().getCount() == 1,
                    type + ": o dichavador deu " + player.getMainHandItem());
                check(dropsAround(helper, player).isEmpty(), type + ": derrubou com a mão livre: " + dropsAround(helper, player));
            }
        });
    }

    /**
     * No criativo nada que o jogador segura é gasto: instalar o dichavador, guardar no pote e inserir na estação deixam a
     * pilha da mão igual, e o efeito acontece.
     */
    public static void creativeKeepsHeldStacks(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer creative = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
            creative.getInventory().setSelectedSlot(8);
            check(creative.hasInfiniteMaterials(), "preparo: jogador criativo");
            placeStation(helper, StationKind.SIEVING);
            creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRINDER.get(), 2));
            check(rightClick(helper, creative, SOIL).consumesAction(), "o criativo não instalou o dichavador");
            check(grinderAt(helper, SOIL) && creative.getMainHandItem().is(ModItems.GRINDER.get())
                && creative.getMainHandItem().getCount() == 2,
                "instalar gastou o dichavador do criativo: " + creative.getMainHandItem());

            JarBlockEntity jar = placeJar(helper, SOIL.east(2));
            ItemStack flowers = withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 10, 2);
            creative.setItemInHand(InteractionHand.MAIN_HAND, flowers.copy());
            check(rightClick(helper, creative, SOIL.east(2)).consumesAction(), "o criativo não guardou no pote");
            check(jar.getItem().getCount() == 10 && ItemStack.matches(creative.getMainHandItem(), flowers),
                "guardar no pote gastou as flores do criativo: " + creative.getMainHandItem());

            ProcessingStationBlockEntity rack = placeStationAt(helper, StationKind.DRYING, SOIL.west(2));
            ItemStack fresh = withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 5, 3);
            creative.setItemInHand(InteractionHand.MAIN_HAND, fresh.copy());
            check(rightClick(helper, creative, SOIL.west(2)).consumesAction(), "o criativo não inseriu na estação");
            check(rack.getItem(ProcessingStationBlockEntity.INPUT).getCount() == 5
                && ItemStack.matches(creative.getMainHandItem(), fresh),
                "inserir gastou as flores do criativo: " + creative.getMainHandItem());
        });
    }

    // ------------------------------------------------------------------ revisão: pote recarregado

    /**
     * Regressão: o pote salvo e recarregado (o caminho do carregamento de chunk, e também o de um pote copiado com Ctrl +
     * pick-block, que chega com filled=false) mantém a pilha com os componentes e o contador da cura, ganha ticker mesmo
     * marcado como vazio, acerta filled no primeiro tick e termina a cura pelo ticker do mundo (faltavam 20 ticks; nada de
     * curar antes). A marca cultivo:cured sobrevive a salvar (NBT/JSON) e à rede.
     */
    public static void jarReloadResumes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(SOIL);
        Item brisa = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
        JarBlockEntity jar = placeJar(helper, SOIL);
        ItemStack flowers = withQuality(brisa, 10, 2);
        helper.assertTrue(jar.insert(flowers.copy()) == 10, "preparo: o pote não guardou as flores");
        jar.setCureProgress(CuringRules.CURE_TICKS - 20);

        ItemStack cured = withQuality(brisa, 3, 3);
        cured.set(ModComponents.CURED.get(), Unit.INSTANCE);
        var ops = level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        com.google.gson.JsonElement json = ItemStack.CODEC.encodeStart(ops, cured).getOrThrow();
        helper.assertTrue(json.getAsJsonObject().getAsJsonObject("components").has("cultivo:cured"),
            "cultivo:cured não foi salvo: " + json);
        helper.assertTrue(ItemStack.matches(cured, ItemStack.CODEC.parse(ops, json).getOrThrow()),
            "a pilha curada não persistiu: " + json);
        net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, cured);
        helper.assertTrue(ItemStack.matches(cured, ItemStack.STREAM_CODEC.decode(buf)), "a pilha curada não sincronizou");

        CompoundTag saved = jar.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(pos);
        level.setBlock(pos, ModBlocks.JAR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        helper.assertTrue(loaded instanceof JarBlockEntity, "o pote não recarregou");
        level.setBlockEntity(loaded);
        JarBlockEntity after = (JarBlockEntity) loaded;
        helper.assertTrue(level.getBlockEntity(pos) == after && after != jar, "o pote recarregado não está no mundo");
        helper.assertTrue(!level.getBlockState(pos).getValue(JarBlock.FILLED), "preparo: o estado deveria chegar com filled=false");
        helper.assertTrue(level.getBlockState(pos).getTicker(level, ModBlockEntities.JAR.get()) != null,
            "pote marcado como vazio sem ticker: nunca curaria");
        helper.assertTrue(ItemStack.matches(after.getItem(), flowers), "a pilha não persistiu: " + after.getItem());
        helper.assertTrue(after.cureProgress() == CuringRules.CURE_TICKS - 20,
            "o contador da cura não persistiu: " + after.cureProgress());

        long[] curedAt = {-1L};
        helper.onEachTick(() -> {
            if (curedAt[0] < 0 && level.getBlockEntity(pos) instanceof JarBlockEntity now
                && now.getItem().has(ModComponents.CURED.get())) {
                curedAt[0] = helper.getTick();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockEntity(pos) == after, "o pote recarregado saiu do mundo");
            helper.assertTrue(level.getBlockState(pos).getValue(JarBlock.FILLED), "o tick não acertou filled");
            helper.assertTrue(curedAt[0] >= 0, "ainda curando");
            helper.assertTrue(curedAt[0] >= 18, "curou antes da hora: tick " + curedAt[0]);
            ItemStack stack = after.getItem();
            helper.assertTrue(stack.getCount() == 10 && Integer.valueOf(3).equals(quality(stack))
                && stack.has(ModComponents.CURED.get()), "cura pelo ticker do mundo: " + stack);
        });
    }

    // ------------------------------------------------------------------ revisão: previsão do cliente

    /** Visão do cliente: uma estação nova carregada só com a tag de atualização que o servidor manda (getUpdateTag). */
    private static ProcessingStationBlockEntity stationView(GameTestHelper helper, ProcessingStationBlockEntity station) {
        var registries = helper.getLevel().registryAccess();
        ProcessingStationBlockEntity view = new ProcessingStationBlockEntity(station.getBlockPos(), station.getBlockState());
        view.loadWithComponents(TagValueInput.create(
            ProblemReporter.DISCARDING, registries, station.getUpdateTag(registries)));
        return view;
    }

    /** Visão do cliente do pote, só com a tag de atualização. */
    private static JarBlockEntity jarView(GameTestHelper helper, JarBlockEntity jar) {
        var registries = helper.getLevel().registryAccess();
        JarBlockEntity view = new JarBlockEntity(jar.getBlockPos(), jar.getBlockState());
        view.loadWithComponents(TagValueInput.create(
            ProblemReporter.DISCARDING, registries, jar.getUpdateTag(registries)));
        return view;
    }

    /**
     * O que o servidor faz com um clique no bloco, na ordem do ServerPlayerGameMode#useItemOn: o useItemOn do bloco e, com
     * a mão principal vazia (TRY_WITH_EMPTY_HAND), o useWithoutItem. O uso do item depois disso não entra.
     */
    private static InteractionResult serverBlockResult(GameTestHelper helper, BlockPos rel, Player player,
                                                       InteractionHand hand, ItemStack held) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        BlockState state = level.getBlockState(pos);
        InteractionResult result = state.useItemOn(held, level, player, hand, hit);
        if (result instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
            result = state.useWithoutItem(level, player, hit);
        }
        return result;
    }

    private static ItemStack stationInput(StationKind kind, int quality) {
        return switch (kind) {
            case DRYING -> withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 1, quality);
            case SIEVING -> withQuality(ModItems.DRIED_FLOWERS.get(Variety.AURORA).get(), 1, quality);
            case PRESSING -> withQuality(ModItems.RESIN.get(), 1, quality);
        };
    }

    private static ItemStack stationOutput(StationKind kind) {
        return switch (kind) {
            case DRYING -> withQuality(ModItems.DRIED_FLOWERS.get(Variety.BRISA).get(), 1, 3);
            case SIEVING -> withQuality(ModItems.RESIN.get(), 1, 3);
            case PRESSING -> withQuality(ModItems.HASH.get(), 1, 3);
        };
    }

    /**
     * Regressão das previsões do cliente, que nenhum teste rodava: numa visão montada só com a tag de atualização
     * (o que o cliente recebe), a previsão que os ramos do cliente devolvem é igual ao resultado do servidor, numa matriz
     * de estados e itens na mão. Estações (com e sem dichavador na peneira): vazia, com entrada, com a entrada cheia (64),
     * só com saída e com as duas; na mão a entrada certa, outra qualidade, terra, cachimbo, dichavador e a mão vazia
     * (agachado ou não). Pote: vazio, com 10, cheio (64) e com flores curadas; na mão flor seca igual e de outra
     * qualidade, resina, flor fresca, terra, cachimbo e a mão vazia. Bandeja: cada mão em várias combinações de flores
     * e folhas. Uma previsão SUCCESS onde o servidor dá PASS engoliria o uso da outra mão ou do item (o bug da auditoria).
     */
    public static void clientPredictionMatchesServer(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            // As recusas da bandeja mandam mensagens: o jogador de teste precisa guardá-las (o mock comum não tem conexão)
            ServerPlayer player = capturingServerPlayer(helper, GameType.SURVIVAL, new ArrayList<>());
            List<String> mismatches = new ArrayList<>();
            int checks = 0;

            for (StationKind kind : StationKind.values()) {
                ProcessingStationBlockEntity station = placeStation(helper, kind);
                BlockPos pos = helper.absolutePos(SOIL);
                ItemStack valid = stationInput(kind, 3);
                ItemStack output = stationOutput(kind);
                List<ItemStack[]> contents = List.of(
                    new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY},
                    new ItemStack[] {valid.copyWithCount(4), ItemStack.EMPTY},
                    new ItemStack[] {valid.copyWithCount(valid.getMaxStackSize()), ItemStack.EMPTY},
                    new ItemStack[] {ItemStack.EMPTY, output.copyWithCount(2)},
                    new ItemStack[] {valid.copyWithCount(4), output.copyWithCount(2)});
                List<ItemStack> helds = List.of(valid.copyWithCount(1), stationInput(kind, 4), new ItemStack(Items.DIRT),
                    new ItemStack(ModItems.ARTICLES.get("pipe").get()), new ItemStack(ModItems.GRINDER.get()), ItemStack.EMPTY);
                for (boolean grinder : kind == StationKind.SIEVING ? List.of(false, true) : List.of(false)) {
                    for (ItemStack[] slots : contents) {
                        for (ItemStack held : helds) {
                            for (boolean sneaking : held.isEmpty() ? List.of(false, true) : List.of(false)) {
                                if (level.getBlockState(pos).hasProperty(SieveBlock.GRINDER)) {
                                    level.setBlock(pos, level.getBlockState(pos).setValue(SieveBlock.GRINDER, grinder),
                                        Block.UPDATE_ALL);
                                }
                                station.setItem(ProcessingStationBlockEntity.INPUT, slots[0].copy());
                                station.setItem(ProcessingStationBlockEntity.OUTPUT, slots[1].copy());
                                BlockState state = level.getBlockState(pos);
                                InteractionResult predicted = ((ProcessingStationBlock) state.getBlock())
                                    .predictUse(state, stationView(helper, station), held, sneaking);
                                player.getInventory().clearContent();
                                player.getInventory().setSelectedSlot(8);
                                player.setShiftKeyDown(sneaking);
                                InteractionResult actual =
                                    serverBlockResult(helper, SOIL, player, InteractionHand.MAIN_HAND, held.copy());
                                checks++;
                                if (!predicted.equals(actual)) {
                                    mismatches.add(kind + (grinder ? " com dichavador" : "") + ", entrada " + slots[0] + ", saída "
                                        + slots[1] + ", mão " + held + (sneaking ? " agachado" : "") + ": cliente " + predicted
                                        + ", servidor " + actual);
                                }
                            }
                        }
                    }
                }
            }
            player.setShiftKeyDown(false);

            Item brisa = ModItems.DRIED_FLOWERS.get(Variety.BRISA).get();
            JarBlockEntity jar = placeJar(helper, SOIL.east(2));
            ItemStack curedFlowers = withQuality(brisa, 10, 3);
            curedFlowers.set(ModComponents.CURED.get(), Unit.INSTANCE);
            List<ItemStack> stored = List.of(ItemStack.EMPTY, withQuality(brisa, 10, 2), withQuality(brisa, 64, 2), curedFlowers);
            List<ItemStack> jarHelds = List.of(withQuality(brisa, 1, 2), withQuality(brisa, 1, 3),
                withQuality(ModItems.RESIN.get(), 1, 2), withQuality(ModItems.FRESH_FLOWERS.get(Variety.BRISA).get(), 1, 2),
                new ItemStack(Items.DIRT), new ItemStack(ModItems.ARTICLES.get("pipe").get()), ItemStack.EMPTY);
            for (ItemStack inJar : stored) {
                for (ItemStack held : jarHelds) {
                    jar.setItem(inJar.copy());
                    JarBlockEntity view = jarView(helper, jar);
                    InteractionResult predicted = held.isEmpty() ? JarBlock.predictTake(view) : JarBlock.predictInsert(view, held);
                    player.getInventory().clearContent();
                    player.getInventory().setSelectedSlot(8);
                    InteractionResult actual =
                        serverBlockResult(helper, SOIL.east(2), player, InteractionHand.MAIN_HAND, held.copy());
                    checks++;
                    if (!predicted.equals(actual)) {
                        mismatches.add("pote com " + inJar + ", mão " + held + ": cliente " + predicted + ", servidor " + actual);
                    }
                }
            }

            BlockPos trayRel = SOIL.west(2);
            helper.setBlock(trayRel, ModBlocks.ROLLING_TRAY.get().defaultBlockState());
            Item aurora = ModItems.DRIED_FLOWERS.get(Variety.AURORA).get();
            record Hands(ItemStack main, ItemStack off, int papers) {}
            List<Hands> trays = List.of(
                new Hands(withQuality(aurora, 1, 3), withQuality(aurora, 10, 3), 5),
                new Hands(withQuality(aurora, 1, 3), withQuality(aurora, 10, 3), 2),
                new Hands(withQuality(aurora, 2, 3), ItemStack.EMPTY, 3),
                new Hands(withQuality(aurora, 2, 3), ItemStack.EMPTY, 2),
                new Hands(withQuality(aurora, 1, 3), withQuality(aurora, 1, 2), 3),
                new Hands(withQuality(aurora, 1, 3), ItemStack.EMPTY, 5),
                new Hands(new ItemStack(Items.DIRT), withQuality(aurora, 10, 3), 5));
            for (Hands hands : trays) {
                for (InteractionHand hand : InteractionHand.values()) {
                    player.getInventory().clearContent();
                    player.getInventory().setSelectedSlot(8);
                    player.setItemInHand(InteractionHand.MAIN_HAND, hands.main().copy());
                    player.setItemInHand(InteractionHand.OFF_HAND, hands.off().copy());
                    if (hands.papers() > 0) {
                        player.getInventory().add(new ItemStack(ModItems.ROLLING_PAPER.get(), hands.papers()));
                    }
                    InteractionResult predicted = RollingTrayBlock.predict(player, hand, player.getItemInHand(hand));
                    InteractionResult actual = serverBlockResult(helper, trayRel, player, hand, player.getItemInHand(hand));
                    checks++;
                    if (!predicted.equals(actual)) {
                        mismatches.add("bandeja " + hands + " mão " + hand + ": cliente " + predicted + ", servidor " + actual);
                    }
                }
            }
            check(mismatches.isEmpty(), mismatches.size() + " previsões erradas: " + mismatches);
            // Estações: suporte, peneira sem e com dichavador e prensa × 5 conteúdos × 7 mãos; pote 4 × 7; bandeja 7 × 2
            check(checks == 4 * 5 * 7 + 4 * 7 + 7 * 2, "combinações conferidas: " + checks);
        });
    }

    // ------------------------------------------------------------------ revisão: forma, alcance, estados e tooltips

    /**
     * Regressão: com o dichavador instalado a caixa de seleção e de colisão da peneira cresce até o topo do dichavador
     * (y 15, 2 px acima da moldura), na posição do modelo girada pelo facing; sem ele, a peneira vai até y 13.
     */
    public static void sieveGrinderShape(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(SOIL);
            // A parte sieve_grinder do modelo, com a frente para o norte: [11, 13, 11] → [15, 15, 15]
            Map<Direction, VoxelShape> grinderBoxes =
                Shapes.rotateHorizontal(Block.box(11.0D, 13.0D, 11.0D, 15.0D, 15.0D, 15.0D));
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState without = ModBlocks.STATIONS.get(StationKind.SIEVING).get().defaultBlockState()
                    .setValue(ProcessingStationBlock.FACING, facing);
                helper.setBlock(SOIL, without);
                VoxelShape plain = level.getBlockState(pos).getShape(level, pos);
                helper.setBlock(SOIL, without.setValue(SieveBlock.GRINDER, true));
                BlockState with = level.getBlockState(pos);
                VoxelShape installed = with.getShape(level, pos);
                check(Math.abs(plain.max(Direction.Axis.Y) - 13.0D / 16.0D) < 1.0E-6,
                    facing + ": peneira sem dichavador até " + plain.max(Direction.Axis.Y));
                check(Math.abs(installed.max(Direction.Axis.Y) - 15.0D / 16.0D) < 1.0E-6,
                    facing + ": a caixa não cobre o dichavador: até " + installed.max(Direction.Axis.Y));
                check(!Shapes.joinIsNotEmpty(grinderBoxes.get(facing), installed,
                    BooleanOp.ONLY_FIRST), facing + ": parte do dichavador fora da caixa");
                check(!Shapes.joinIsNotEmpty(plain, installed,
                    BooleanOp.ONLY_FIRST), facing + ": a peneira perdeu parte da caixa");
                check(Math.abs(with.getCollisionShape(level, pos).max(Direction.Axis.Y) - 15.0D / 16.0D) < 1.0E-6,
                    facing + ": a colisão não cobre o dichavador");
            }
        });
    }

    /**
     * O alcance do cinzeiro nas bordas, pela sessão de fumo real (AshtrayBlock#catchAsh): a 4 blocos na horizontal, a 2
     * acima e no canto (4, 2, 4) pega a cinza; a 5 blocos e a 3 acima não. Uma sessão no criativo também deixa cinza e não
     * gasta o baseado.
     */
    public static void ashtraySearchEdges(GameTestHelper helper) {
        run(helper, () -> {
            BlockPos smokerRel = new BlockPos(3, 1, 4);
            record Case(BlockPos rel, String where, boolean catches) {}
            List<Case> cases = List.of(
                new Case(new BlockPos(8, 1, 4), "5 blocos em x", false),
                new Case(new BlockPos(7, 1, 4), "4 blocos em x", true),
                new Case(new BlockPos(3, 1, 0), "4 blocos em z", true),
                new Case(new BlockPos(3, 4, 4), "3 acima", false),
                new Case(new BlockPos(3, 3, 4), "2 acima", true),
                new Case(new BlockPos(7, 3, 8), "canto (4, 2, 4)", true));
            for (Case c : cases) {
                helper.setBlock(c.rel(), ModBlocks.ASHTRAY.get().defaultBlockState());
                Player player = smoker(helper, joint(Variety.BRISA, 2), lighter());
                player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(smokerRel)));
                check(smokeOnce(helper, player) == InteractionResult.CONSUME, c.where() + ": sessão recusada");
                int ash = ashAt(helper, c.rel());
                check(ash == (c.catches() ? 1 : 0), "cinzeiro a " + c.where() + ": cinza " + ash);
                helper.setBlock(c.rel(), Blocks.AIR.defaultBlockState());
            }
            BlockPos near = new BlockPos(5, 1, 4);
            helper.setBlock(near, ModBlocks.ASHTRAY.get().defaultBlockState());
            Player creative = helper.makeMockPlayer(GameType.CREATIVE);
            GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
            check(creative.hasInfiniteMaterials(), "preparo: jogador criativo");
            creative.setItemInHand(InteractionHand.MAIN_HAND, joint(Variety.BRISA, 2));
            creative.getInventory().add(lighter());
            creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(smokerRel)));
            check(smokeOnce(helper, creative) == InteractionResult.CONSUME, "sessão do criativo recusada");
            check(ashAt(helper, near) == 1, "a sessão no criativo não deixou cinza: " + ashAt(helper, near));
            check(creative.getMainHandItem().getCount() == 1, "o criativo gastou o baseado");
        });
    }

    private static <T extends Comparable<T>> Set<String> valueNames(Property<T> property) {
        Set<String> names = new java.util.TreeSet<>();
        for (T value : property.getPossibleValues()) {
            names.add(property.getName(value));
        }
        return names;
    }

    /**
     * O espelho ContentIds.stateProperties (que os testes dos blockstates JSON usam) bate com a StateDefinition registrada
     * de cada bloco do mod: mesmos nomes de propriedade e mesmos valores. Um nome trocado no Java (filled virar full, uma
     * propriedade nova) deixaria o JSON apontando para um estado que não existe, com todos os testes dos modelos verdes.
     */
    public static void blockStatesMatchIds(GameTestHelper helper) {
        run(helper, () -> {
            for (String id : ContentIds.allBlocks()) {
                Block block = BuiltInRegistries.BLOCK.getValue(Cultivo.id(id));
                check(block != Blocks.AIR, "bloco não registrado: " + id);
                Map<String, Set<String>> actual = new java.util.TreeMap<>();
                for (Property<?> property : block.getStateDefinition().getProperties()) {
                    actual.put(property.getName(), valueNames(property));
                }
                Map<String, Set<String>> expected = new java.util.TreeMap<>();
                ContentIds.stateProperties(id).forEach((name, values) -> expected.put(name, new java.util.TreeSet<>(values)));
                check(actual.equals(expected), id + ": o bloco tem " + actual + " e o espelho diz " + expected);
            }
        });
    }

    /** As linhas de tooltip de uma pilha, como o cliente monta (Item#appendHoverText). */
    @SuppressWarnings("deprecation")
    private static List<Component> tooltipOf(GameTestHelper helper, ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()),
            TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
        return lines;
    }

    /** A chave de tradução de uma linha ou argumento ("?" se não é texto traduzido). */
    private static String keyOf(Object component) {
        return component instanceof Component c && c.getContents() instanceof TranslatableContents t
            ? t.getKey() : "?";
    }

    private static List<String> keysOf(List<Component> lines) {
        return lines.stream().map(CultivoGameTests::keyOf).toList();
    }

    private static List<String> argKeys(Component line) {
        return line.getContents() instanceof TranslatableContents t
            ? java.util.Arrays.stream(t.getArgs()).map(CultivoGameTests::keyOf).toList() : List.of();
    }

    /**
     * Os tooltips pedidos, montados pelo item como no cliente: a semente do catálogo mostra o perfil "raridade · ciclo ·
     * genética · potência" nessa ordem (Manga Gelada: comum, fotoperíodo, híbrida, ★★☆); a flor seca curada mostra o
     * perfil, a qualidade e "Cura concluída"; o baseado com piteira mostra a variedade, a qualidade, "Com piteira" e a dica
     * em linhas curtas; a estação, a função e a dica de uso em 4 linhas.
     */
    public static void tooltipsShowProfileAndMarks(GameTestHelper helper) {
        run(helper, () -> {
            List<Component> seed = tooltipOf(helper, new ItemStack(ModItems.SEEDS.get(Variety.MANGA_GELADA).get()));
            check(keysOf(seed).equals(List.of("tooltip.cultivo.strain_profile")), "semente: " + keysOf(seed));
            check(argKeys(seed.get(0)).equals(List.of("tooltip.cultivo.rarity.common", "tooltip.cultivo.cycle.photoperiod",
                "tooltip.cultivo.lineage.hybrid", "tooltip.cultivo.potency.2")), "perfil da semente: " + argKeys(seed.get(0)));

            ItemStack cured = withQuality(ModItems.DRIED_FLOWERS.get(Variety.ECLIPSE).get(), 1, 4);
            cured.set(ModComponents.CURED.get(), Unit.INSTANCE);
            List<Component> flower = tooltipOf(helper, cured);
            check(keysOf(flower).equals(
                List.of("tooltip.cultivo.strain_profile", "tooltip.cultivo.quality", "tooltip.cultivo.cured")),
                "flor seca curada: " + keysOf(flower));
            check(argKeys(flower.get(0)).equals(List.of("tooltip.cultivo.rarity.rare", "tooltip.cultivo.cycle.photoperiod",
                "tooltip.cultivo.lineage.indica", "tooltip.cultivo.potency.3")), "perfil da Eclipse: " + argKeys(flower.get(0)));
            check(argKeys(flower.get(1)).equals(List.of("tooltip.cultivo.quality.4")), "qualidade: " + argKeys(flower.get(1)));

            List<Component> joint = tooltipOf(helper, craftFilter(helper, joint(Variety.AURORA, 4)));
            check(keysOf(joint).equals(List.of("tooltip.cultivo.strain", "tooltip.cultivo.quality", "tooltip.cultivo.filtered",
                "tooltip.cultivo.smoke.joint.1", "tooltip.cultivo.smoke.joint.2")), "baseado com piteira: " + keysOf(joint));
            check(argKeys(joint.get(0)).equals(List.of("tooltip.cultivo.variety.aurora")), "variedade: " + argKeys(joint.get(0)));

            List<Component> station = tooltipOf(helper, new ItemStack(ModItems.STATIONS.get(StationKind.SIEVING).get()));
            check(keysOf(station).equals(List.of("tooltip.cultivo.station.sieving", "tooltip.cultivo.station.usage.1",
                "tooltip.cultivo.station.usage.2", "tooltip.cultivo.station.usage.3", "tooltip.cultivo.station.usage.4")),
                "peneira: " + keysOf(station));
            List<Component> bong = tooltipOf(helper, new ItemStack(ModItems.ARTICLES.get("bong").get()));
            check(keysOf(bong).equals(List.of("tooltip.cultivo.smoke.device.1", "tooltip.cultivo.smoke.device.2",
                "tooltip.cultivo.smoke.device.3")), "bong: " + keysOf(bong));
            List<Component> jarTip = tooltipOf(helper, new ItemStack(ModItems.ARTICLES.get(ContentIds.JAR).get()));
            check(keysOf(jarTip).equals(List.of("tooltip.cultivo.jar.1", "tooltip.cultivo.jar.2", "tooltip.cultivo.jar.3",
                "tooltip.cultivo.jar.4")), "pote: " + keysOf(jarTip));
        });
    }

    // ------------------------------------------------------------------ compatibilidade: Serene Seasons

    /** Estações do Serene Seasons, escritas como na especificação (DECISIONS.md, "Compatibilidade: Serene Seasons"). */
    private static final List<String> SS_SEASONS = List.of("spring", "summer", "autumn", "winter");
    private static final List<String> SS_FERTILE_LIKE_WHEAT = List.of("summer", "autumn");

    private static net.minecraft.tags.TagKey<Block> ssBlockTag(String path) {
        return net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("sereneseasons", path));
    }

    private static net.minecraft.tags.TagKey<Item> ssItemTag(String path) {
        return net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("sereneseasons", path));
    }

    private static void command(GameTestHelper helper, String command) {
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Dá random ticks na planta (passando pelo BlockState, onde o Serene Seasons se pendura) até a idade 7. */
    private static int tickUntilMature(GameTestHelper helper, BlockPos relative, int maxTicks) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        int ticks = 0;
        while (ticks < maxTicks && level.getBlockState(pos).getValue(CropBlock.AGE) < 7) {
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
            ticks++;
        }
        return level.getBlockState(pos).getValue(CropBlock.AGE);
    }

    /**
     * Serene Seasons (mod opcional de terceiros). Sempre: as 40 lavouras estão nas tags de bloco de verão e de outono
     * do Serene Seasons, e só nelas, as 40 sementes nas tags de item correspondentes, e a planta silvestre em nenhuma
     * (sem o Serene Seasons instalado, essas tags existem só pelos dados do Cultivo e nada as lê).
     *
     * <p>Com o Serene Seasons na pasta de mods (tools/compat/README.md: runGameTestServer com
     * -Pcultivo_run_dir=run-compat-ss e out_of_season_crop_behavior = 1), confere também o efeito, como no single
     * player: o trigo e a lavoura do Cultivo pertencem às mesmas estações; no início da primavera, num bioma temperado,
     * nenhum dos dois cresce; sob um bloco de vidro os dois crescem; no meio do verão os dois crescem. O Forge 26.3 só
     * dispara o TagsUpdatedEvent no cliente, então o teste o dispara como o cliente do single player faria.
     *
     * <p>Muda estado global: depois dele, até o fim da execução, o Serene Seasons segue com as listas montadas e a
     * estação fica no meio do verão (fértil para tudo que ele lista), então os outros testes crescem normalmente.
     */
    public static void sereneSeasonsCompat(GameTestHelper helper) {
        run(helper, () -> {
            Block wild = ModBlocks.WILD_PLANT.get();
            for (String season : SS_SEASONS) {
                boolean fertile = SS_FERTILE_LIKE_WHEAT.contains(season);
                for (Variety variety : Variety.values()) {
                    check(crop(variety, 0).is(ssBlockTag(season + "_crops")) == fertile,
                        variety.cropId() + " em sereneseasons:" + season + "_crops deveria ser " + fertile);
                    check(new ItemStack(ModItems.SEEDS.get(variety).get()).is(ssItemTag(season + "_crops")) == fertile,
                        variety.seedsId() + " na tag de item " + season + "_crops deveria ser " + fertile);
                }
                check(!wild.defaultBlockState().is(ssBlockTag(season + "_crops")), "planta silvestre em " + season);
            }
            check(!crop(Variety.BRISA, 0).is(ssBlockTag("unbreakable_infertile_crops")), "o trigo não está nessa tag");

            if (!net.minecraftforge.fml.ModList.isLoaded("sereneseasons")) {
                return;
            }
            // Com o Serene Seasons: o trigo está nas mesmas estações que o Cultivo (paridade).
            for (String season : SS_SEASONS) {
                check(Blocks.WHEAT.defaultBlockState().is(ssBlockTag(season + "_crops")) == SS_FERTILE_LIKE_WHEAT.contains(season),
                    "o trigo mudou de estações no Serene Seasons (" + season + "): reveja a paridade");
            }
            int behavior;
            try {
                Object fertility = Class.forName("sereneseasons.init.ModConfig").getField("fertility").get(null);
                behavior = fertility.getClass().getField("outOfSeasonCropBehavior").getInt(fertility);
            } catch (ReflectiveOperationException e) {
                throw new Failure("não consegui ler a config do Serene Seasons: " + e);
            }
            check(behavior == 1, "rode com out_of_season_crop_behavior = 1 (tools/compat/config-cant-grow), veio " + behavior);

            // O que o cliente do single player faz ao receber as tags: o Serene Seasons monta as listas de lavouras.
            net.minecraftforge.event.ForgeEventFactory.onTagsUpdated(helper.getLevel().registryAccess(), true, true);
            // A partir daqui o Serene Seasons fica ativo até o fim da execução (não há como desfazer o evento). O finally
            // termina no meio do verão, fértil para o Cultivo e para o trigo, para não travar testes que rodem depois.
            try {
                BlockPos min = helper.absolutePos(BlockPos.ZERO);
                BlockPos max = helper.absolutePos(new BlockPos(8, 8, 8));
                command(helper, "fillbiome " + min.getX() + " " + min.getY() + " " + min.getZ() + " "
                    + max.getX() + " " + max.getY() + " " + max.getZ() + " minecraft:plains");
                check(helper.getLevel().getBiome(helper.absolutePos(PLANT)).is(net.minecraft.world.level.biome.Biomes.PLAINS),
                    "o bioma da área não virou plains");
                check(helper.getLevel().canSeeSky(helper.absolutePos(PLANT)), "a planta precisa ver o céu");

                BlockPos wheatSoil = SOIL.east(2);
                BlockPos wheat = PLANT.east(2);
                Runnable plantBoth = () -> {
                    plantOnFarmland(helper, Variety.BRISA, 0);
                    helper.setBlock(wheatSoil, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
                    helper.setBlock(wheat, Blocks.WHEAT.defaultBlockState());
                };

                command(helper, "season set early_spring");
                plantBoth.run();
                check(tickUntilMature(helper, PLANT, 300) == 0, "Cultivo cresceu fora de estação (primavera)");
                check(tickUntilMature(helper, wheat, 300) == 0, "trigo cresceu fora de estação (primavera): o Serene Seasons não está ativo");

                // Um bloco de vidro até 16 acima é a "estufa" do Serene Seasons: os dois crescem mesmo fora de estação.
                helper.setBlock(PLANT.above(3), Blocks.GLASS.defaultBlockState());
                helper.setBlock(wheat.above(3), Blocks.GLASS.defaultBlockState());
                check(tickUntilMature(helper, PLANT, 20_000) == 7, "Cultivo sob vidro não cresceu na primavera");
                check(tickUntilMature(helper, wheat, 20_000) == 7, "trigo sob vidro não cresceu na primavera");
                helper.setBlock(PLANT.above(3), Blocks.AIR.defaultBlockState());
                helper.setBlock(wheat.above(3), Blocks.AIR.defaultBlockState());

                command(helper, "season set mid_summer");
                plantBoth.run();
                check(tickUntilMature(helper, PLANT, 20_000) == 7, "Cultivo não cresceu no verão");
                check(tickUntilMature(helper, wheat, 20_000) == 7, "trigo não cresceu no verão");
            } finally {
                command(helper, "season set mid_summer");
            }
        });
    }
}
