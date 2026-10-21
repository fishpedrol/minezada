package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.loot.SetHarvestQualityFunction;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootFunctions {
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS =
        DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Cultivo.MOD_ID);

    public static final RegistryObject<MapCodec<SetHarvestQualityFunction>> SET_HARVEST_QUALITY =
        LOOT_FUNCTIONS.register("set_harvest_quality", () -> SetHarvestQualityFunction.CODEC);

    private ModLootFunctions() {}
}
