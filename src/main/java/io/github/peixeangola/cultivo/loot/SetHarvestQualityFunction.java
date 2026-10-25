package io.github.peixeangola.cultivo.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import io.github.peixeangola.cultivo.block.GlassRoofDetector;
import io.github.peixeangola.cultivo.block.GrowTentBlockEntity;
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
 * cultivo:set_harvest_quality — na hora da colheita (servidor), grava a qualidade sorteada pelas regras puras no
 * componente cultivo:quality. A rota sai do contexto do loot: a estufinha rola a tabela da lavoura passando a própria
 * BlockEntity (4–5); uma lavoura no mundo não tem BlockEntity, e o teto acima da origem decide entre teto de vidro (3–4)
 * e ar livre (2–3). Lê a BlockEntity, e não o bloco na origem, porque ao quebrar a estufinha o bloco já virou ar quando
 * a planta cai.
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
            int quality = QualityRules.harvestQuality(crop.variety(), route(context), context.getRandom().nextDouble());
            stack.set(ModComponents.QUALITY.get(), quality);
        }
        return stack;
    }

    private static QualityRules.Route route(LootContext context) {
        if (context.getOptional(LootContextParams.BLOCK_ENTITY) instanceof GrowTentBlockEntity) {
            return QualityRules.Route.TENT;
        }
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        if (origin != null && GlassRoofDetector.isCovered(context.getLevel(), BlockPos.containing(origin))) {
            return QualityRules.Route.GLASS_ROOF;
        }
        return QualityRules.Route.OUTDOOR;
    }
}
