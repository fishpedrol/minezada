package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.Variety;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Duas abas: "Cultivo" (estações, planta silvestre, processados e artigos) e "Cultivo: Genéticas" (semente, flor
 * fresca e flor seca de cada variedade, por origem e depois raridade). A ordem vem de {@link ContentIds}.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cultivo.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CULTIVO = CREATIVE_TABS.register("cultivo",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cultivo"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ModItems.JOINT.get().getDefaultInstance())
            .displayItems((parameters, output) -> accept(output, ContentIds.mainTabItems()))
            .build());

    public static final RegistryObject<CreativeModeTab> STRAINS = CREATIVE_TABS.register("strains",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cultivo.strains"))
            .withTabsBefore(CULTIVO.getKey())
            .icon(() -> ModItems.FRESH_FLOWERS.get(Variety.BRISA).get().getDefaultInstance())
            .displayItems((parameters, output) -> accept(output, ContentIds.strainTabItems()))
            .build());

    private ModCreativeTabs() {}

    private static void accept(CreativeModeTab.Output output, List<String> ids) {
        ids.forEach(id -> output.accept(BuiltInRegistries.ITEM.getValue(Cultivo.id(id))));
    }
}
