package io.github.peixeangola.cultivo.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * cultivo:high ("Chapado"). Não faz nada sozinho: é o indicador visível (ícone e tempo) da sessão. Os efeitos
 * de jogo vêm de efeitos vanilla aplicados junto (fome, náusea, velocidade/lentidão, regeneração).
 */
public class HighEffect extends MobEffect {
    public static final int COLOR = 0x6AA84F;

    public HighEffect() {
        super(MobEffectCategory.NEUTRAL, COLOR);
    }
}
