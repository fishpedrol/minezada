package io.github.peixeangola.cultivo.loot;

import io.github.peixeangola.cultivo.core.SeedSources;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;

/**
 * Acrescenta aos baús das casas de aldeia cobertas um pool que rola cultivo:inject/village_seeds
 * (vazio 50, brisa 30, aurora 14, eclipse 6). O evento só dispara no servidor ao carregar as loot tables.
 */
public final class SeedLootInjector {
    private static final Set<String> TARGETS = Set.copyOf(SeedSources.chestTables());
    public static final ResourceKey<LootTable> INJECT_TABLE =
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(SeedSources.INJECT_TABLE));

    private SeedLootInjector() {}

    public static boolean isTarget(Identifier table) {
        return TARGETS.contains(table.toString());
    }

    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!isTarget(event.getName())) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool()
            .name(SeedSources.INJECT_POOL_NAME)
            .setRolls(ConstantValue.exactly(SeedSources.CHEST_ROLLS))
            .add(NestedLootTable.lootTableReference(INJECT_TABLE))
            .build());
    }
}
