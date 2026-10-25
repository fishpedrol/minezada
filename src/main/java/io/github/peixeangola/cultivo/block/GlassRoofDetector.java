package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.GlassRoofRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/**
 * Liga {@link GlassRoofRules} ao mundo. Vidro é a tag {@code c:glass_blocks} (vidro e os 16 vidros tingidos) menos o
 * vidro fosco ({@code c:glass_blocks/tinted}), que bloqueia a luz; vidraças não contam.
 */
public final class GlassRoofDetector {
    private GlassRoofDetector() {}

    public static boolean isCovered(BlockGetter level, BlockPos plant) {
        return GlassRoofRules.isCovered(dy -> {
            BlockPos pos = plant.above(dy);
            return classify(level.getBlockState(pos), level, pos);
        });
    }

    static GlassRoofRules.Cell classify(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.is(Tags.Blocks.GLASS_BLOCKS) && !state.is(Tags.Blocks.GLASS_BLOCKS_TINTED)) {
            return GlassRoofRules.Cell.GLASS;
        }
        return state.getCollisionShape(level, pos).isEmpty() ? GlassRoofRules.Cell.PASSABLE : GlassRoofRules.Cell.SOLID;
    }
}
