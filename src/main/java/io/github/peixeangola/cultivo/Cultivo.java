package io.github.peixeangola.cultivo;

import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.gametest.CultivoGameTests;
import io.github.peixeangola.cultivo.loot.SeedLootInjector;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModCreativeTabs;
import io.github.peixeangola.cultivo.registry.ModEffects;
import io.github.peixeangola.cultivo.registry.ModItems;
import io.github.peixeangola.cultivo.registry.ModLootFunctions;
import io.github.peixeangola.cultivo.registry.ModRecipes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// O valor precisa coincidir com o modId em META-INF/mods.toml
@Mod(Cultivo.MOD_ID)
public final class Cultivo {
    public static final String MOD_ID = ContentIds.MOD_ID;

    public Cultivo(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModComponents.COMPONENTS.register(modBusGroup);
        ModBlockEntities.BLOCK_ENTITIES.register(modBusGroup);
        ModRecipes.TYPES.register(modBusGroup);
        ModRecipes.SERIALIZERS.register(modBusGroup);
        ModLootFunctions.LOOT_FUNCTIONS.register(modBusGroup);
        ModEffects.EFFECTS.register(modBusGroup);
        ModCreativeTabs.CREATIVE_TABS.register(modBusGroup);
        CultivoGameTests.TESTS.register(modBusGroup);

        // Injeção de sementes nos baús de aldeia (só roda no servidor)
        LootTableLoadEvent.BUS.addListener(SeedLootInjector::onLootTableLoad);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
