package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.effect.HighEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Cultivo.MOD_ID);

    public static final RegistryObject<MobEffect> HIGH = EFFECTS.register("high", HighEffect::new);

    private ModEffects() {}

    /** Holder do efeito, que é o tipo que MobEffectInstance pede no 26.3. */
    public static Holder<MobEffect> high() {
        return HIGH.getHolder().orElseThrow();
    }
}
