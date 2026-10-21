package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.Variety;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cultivo.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CULTIVO = CREATIVE_TABS.register("cultivo",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cultivo"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ModItems.FRESH_FLOWERS.get(Variety.BRISA).get().getDefaultInstance())
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {}
}
