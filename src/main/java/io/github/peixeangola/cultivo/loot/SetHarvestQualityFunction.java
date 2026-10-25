package io.github.peixeangola.cultivo.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import io.github.peixeangola.cultivo.block.GrowTentBlockEntity;
import io.github.peixeangola.cultivo.core.QualityRules;
import io.github.peixeangola.cultivo.registry.ModComponents;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * cultivo:set_harvest_quality — na hora da colheita (servidor), grava a qualidade sorteada pelas regras puras no
 * componente cultivo:quality. A rota sai do contexto do loot: a estufinha rola a tabela da lavoura passando a própria
 * BlockEntity (4–5); uma lavoura no mundo não tem BlockEntity (2–3), com ou sem vidro em volta. Lê a BlockEntity, e
 * não o bloco na origem, porque ao quebrar a estufinha o bloco já virou ar quando a planta cai.
 */
public final class SetHarvestQualityFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetHarvestQualityFunction> CODEC =
        RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, SetHarvestQualityFunction::new));

    private SetHarvestQualityFunction(Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }

    @Override
    public MapCodec<SetHarvestQualityFunction> codec() {
        return CODEC;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state != null && state.getBlock() instanceof CannabisCropBlock crop) {
            boolean tent = context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GrowTentBlockEntity;
            int quality = QualityRules.harvestQuality(crop.variety(), tent, context.getRandom().nextDouble());
            stack.set(ModComponents.QUALITY.get(), quality);
        }
        return stack;
    }
}
