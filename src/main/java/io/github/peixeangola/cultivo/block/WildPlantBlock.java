package io.github.peixeangola.cultivo.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Planta silvestre gerada no mundo (selvas e planícies). Fica em grama/terra como outras plantas e,
 * ao ser quebrada, dá sementes das três variedades nativas conforme loot_table/blocks/wild_plant.json (as do
 * catálogo vêm de baús de aldeia e do mascate).
 */
public class WildPlantBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public WildPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
