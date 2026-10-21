package io.github.peixeangola.cultivo.registry;

import com.mojang.serialization.Codec;
import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.QualityRules;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Cultivo.MOD_ID);

    /** cultivo:quality — inteiro de 1 a 5, persistente e sincronizado (tooltip no cliente). */
    public static final RegistryObject<DataComponentType<Integer>> QUALITY = COMPONENTS.register("quality",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(QualityRules.MIN, QualityRules.MAX))
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build());

    private ModComponents() {}
}
