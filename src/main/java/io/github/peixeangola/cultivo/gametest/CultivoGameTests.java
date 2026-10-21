package io.github.peixeangola.cultivo.gametest;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.GreenhouseDetector;
import io.github.peixeangola.cultivo.block.ProcessingStationBlockEntity;
import io.github.peixeangola.cultivo.core.ArticleRecipes;
import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
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
        helper.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        helper.setBlock(PLANT, crop(variety, age));
    }

    /** Teto de vidro 2 blocos acima e paredes de vidro a 2 blocos, na altura da planta. */
    private static void buildGreenhouse(GameTestHelper helper) {
        helper.setBlock(PLANT.above(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.east(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.west(2), Blocks.GLASS.defaultBlockState());
        helper.setBlock(PLANT.north(2), Blocks.LIME_STAINED_GLASS.defaultBlockState());
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
            // Planta silvestre dá alguma semente do mod
            helper.setBlock(SOIL, Blocks.GRASS_BLOCK.defaultBlockState());
            helper.setBlock(PLANT, ModBlocks.WILD_PLANT.get().defaultBlockState());
            List<ItemStack> wild = harvest(helper);
            int seeds = 0;
            for (Variety variety : Variety.values()) {
                seeds += count(wild, ModItems.SEEDS.get(variety).get());
            }
            check(seeds >= 1, "planta silvestre sem sementes");
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
            station.setItem(ProcessingStationBlockEntity.OUTPUT, withQuality(ModItems.RESIN.get(), 3, 4));
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

    /** As tabelas de baú das 4 aldeias cobertas geram sementes, com Brisa mais comum que Eclipse. */
    public static void villageChestSeeds(GameTestHelper helper) {
        run(helper, () -> {
            ServerLevel level = helper.getLevel();
            Map<Variety, Integer> totals = new EnumMap<>(Variety.class);
            for (String tableId : SeedSources.chestTables()) {
                LootTable table = level.getServer().reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(tableId)));
                LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(SOIL)))
                    .create(LootContextParamSets.CHEST);
                int found = 0;
                for (int roll = 0; roll < 400; roll++) {
                    for (ItemStack stack : table.getRandomItems(params)) {
                        for (Variety variety : Variety.values()) {
                            if (stack.is(ModItems.SEEDS.get(variety).get())) {
                                totals.merge(variety, stack.getCount(), Integer::sum);
                                found++;
                            }
                        }
                    }
                }
                check(found > 0, "sem sementes em " + tableId);
            }
            int brisa = totals.getOrDefault(Variety.BRISA, 0);
            int eclipse = totals.getOrDefault(Variety.ECLIPSE, 0);
            check(eclipse > 0, "eclipse nunca apareceu");
            check(brisa > eclipse * 3, "raridade: brisa " + brisa + " vs eclipse " + eclipse);
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
}
