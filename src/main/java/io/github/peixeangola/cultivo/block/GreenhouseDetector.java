package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.GreenhouseRules;
import io.github.peixeangola.cultivo.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Liga a regra pura de estufa ao mundo: classifica blocos em ar, vidro de estufa ou outro. */
public final class GreenhouseDetector {
    private GreenhouseDetector() {}

    public static boolean isGreenhouse(BlockGetter level, BlockPos plant) {
        return GreenhouseRules.isGreenhouse((dx, dy, dz) -> classify(level.getBlockState(plant.offset(dx, dy, dz))));
    }

    static GreenhouseRules.Cell classify(BlockState state) {
        if (state.is(ModTags.GREENHOUSE_GLASS)) {
            return GreenhouseRules.Cell.GLASS;
        }
        return state.isAir() ? GreenhouseRules.Cell.AIR : GreenhouseRules.Cell.OTHER;
    }
}
