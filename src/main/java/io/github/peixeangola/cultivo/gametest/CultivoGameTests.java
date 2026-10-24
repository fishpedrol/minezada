package io.github.peixeangola.cultivo.gametest;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.GreenhouseDetector;
import io.github.peixeangola.cultivo.block.ProcessingStationBlockEntity;
import io.github.peixeangola.cultivo.core.ArticleRecipes;
import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModEffects;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
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
import net.minecraft.world.level.block.FarmlandBlock;
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

    /** Cada SmokeDevice está registrado como item fumável e funciona (inclui chillum e narguilé). */
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
            for (String article : List.of("rolling_paper", "filter_tip", "grinder", "rolling_tray", "jar", "ashtray")) {
                check(ModItems.ARTICLES.get(article).get() instanceof io.github.peixeangola.cultivo.item.DecorativeItem,
                    article + " deveria ser decorativo");
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
}
