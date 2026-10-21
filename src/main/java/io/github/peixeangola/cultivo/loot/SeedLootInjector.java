package io.github.peixeangola.cultivo.loot;

import io.github.peixeangola.cultivo.core.SeedSources;
import io.github.peixeangola.cultivo.registry.ModItems;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraftforge.event.LootTableLoadEvent;

/**
 * Acrescenta aos baús das casas de aldeia cobertas um pool com 1 rolagem: vazio 50, brisa 30 (1–3),
 * aurora 14 (1–2), eclipse 6 (1), montado a partir de {@link SeedSources}. O evento só dispara no servidor
 * ao carregar as loot tables. No 26.3 o evento não dá acesso ao registro de loot tables, então o pool é
 * construído aqui em vez de referenciar cultivo:inject/village_seeds; o JSON espelha os mesmos pesos para
 * datapacks e testes.
 */
public final class SeedLootInjector {
    private static final Set<String> TARGETS = Set.copyOf(SeedSources.chestTables());

    private SeedLootInjector() {}

    public static boolean isTarget(Identifier table) {
        return TARGETS.contains(table.toString());
    }

    public static LootPool seedPool() {
        LootPool.Builder pool = LootPool.lootPool()
            .name(SeedSources.INJECT_POOL_NAME)
            .setRolls(ContextIntProviders.exactly(SeedSources.CHEST_ROLLS))
            .add(EmptyLootItem.emptyItem().setWeight(SeedSources.CHEST_EMPTY_WEIGHT));
        for (SeedSources.ChestEntry entry : SeedSources.CHEST_ENTRIES) {
            pool.add(LootItem.lootTableItem(ModItems.SEEDS.get(entry.variety()).get())
                .setWeight(entry.weight())
                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(entry.minCount(), entry.maxCount()))));
        }
        return pool.build();
    }

    public static void onLootTableLoad(LootTableLoadEvent event) {
        LootTable table = event.getTable();
        if (table != null && isTarget(event.getName())) {
            table.addPool(seedPool());
        }
    }
}
