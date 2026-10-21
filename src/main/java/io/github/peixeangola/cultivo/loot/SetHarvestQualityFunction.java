package io.github.peixeangola.cultivo.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import io.github.peixeangola.cultivo.block.GreenhouseDetector;
import io.github.peixeangola.cultivo.core.QualityRules;
import io.github.peixeangola.cultivo.registry.ModComponents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

/**
 * cultivo:set_harvest_quality — na hora da colheita (servidor), detecta se a planta está numa estufa e grava
 * a qualidade sorteada pelas regras puras no componente cultivo:quality.
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
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        if (state != null && origin != null && state.getBlock() instanceof CannabisCropBlock crop) {
            boolean greenhouse = GreenhouseDetector.isGreenhouse(context.getLevel(), BlockPos.containing(origin));
            int quality = QualityRules.harvestQuality(crop.variety(), greenhouse, context.getRandom().nextDouble());
            stack.set(ModComponents.QUALITY.get(), quality);
        }
        return stack;
    }
}
