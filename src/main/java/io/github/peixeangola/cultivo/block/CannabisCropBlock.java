package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.GrowthRules;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/**
 * Lavoura das variedades. Herda tudo do trigo (farmland, luz ≥ 9, idades 0–7, farinha de osso, pisoteio),
 * sem nenhum filtro de bioma. Só o random tick muda: velocidade vanilla × variedade × 1,25 na estufa.
 * Crescimento e colheita rodam apenas no servidor.
 */
public class CannabisCropBlock extends CropBlock {
    private final Variety variety;

    public CannabisCropBlock(Variety variety, Properties properties) {
        super(properties);
        this.variety = variety;
    }

    public Variety variety() {
        return variety;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.SEEDS.get(variety).get();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GrowthRules.hasEnoughLight(level.getRawBrightness(pos, 0))) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge()) {
            return;
        }
        boolean greenhouse = GreenhouseDetector.isGreenhouse(level, pos);
        float speed = GrowthRules.effectiveSpeed(vanillaGrowthSpeed(this, level, pos), variety, greenhouse);
        boolean grows = random.nextInt(GrowthRules.growthRollBound(speed)) == 0;
        if (ForgeHooks.onCropsGrowPre(level, pos, state, grows)) {
            level.setBlock(pos, getStateForAge(age + 1), Block.UPDATE_CLIENTS);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    /**
     * Porta de CropBlock#getGrowthSpeed do vanilla: farmland seca vale 1 e hidratada 3 (vizinhas valem 1/4),
     * e plantar a mesma cultura em linhas cruzadas ou diagonais divide a velocidade por 2.
     */
    static float vanillaGrowthSpeed(Block block, BlockGetter level, BlockPos pos) {
        float speed = 1.0F;
        BlockPos below = pos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                float soil = 0.0F;
                BlockState ground = level.getBlockState(below.offset(dx, 0, dz));
                if (ground.getBlock() instanceof FarmBlock) {
                    soil = ground.getValue(FarmBlock.MOISTURE) > 0 ? 3.0F : 1.0F;
                }
                if (dx != 0 || dz != 0) {
                    soil /= 4.0F;
                }
                speed += soil;
            }
        }
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        boolean rowX = level.getBlockState(west).is(block) || level.getBlockState(east).is(block);
        boolean rowZ = level.getBlockState(north).is(block) || level.getBlockState(south).is(block);
        if (rowX && rowZ) {
            speed /= 2.0F;
        } else {
            boolean diagonal = level.getBlockState(west.north()).is(block) || level.getBlockState(east.north()).is(block)
                || level.getBlockState(east.south()).is(block) || level.getBlockState(west.south()).is(block);
            if (diagonal) {
                speed /= 2.0F;
            }
        }
        return speed;
    }
}
