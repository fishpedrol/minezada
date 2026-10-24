package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Suporte de secagem, peneira de bancada ou prensa manual. Clique direito com item insere na entrada;
 * mão vazia retira primeiro a saída. Toda mudança de inventário acontece no servidor.
 */
public class ProcessingStationBlock extends Block implements EntityBlock {
    private final StationKind kind;

    public ProcessingStationBlock(StationKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public StationKind kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessingStationBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.PROCESSING_STATION.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
            ((ProcessingStationBlockEntity) blockEntity).serverTick((ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int moved = station.insert(serverLevel, stack);
        if (moved <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(moved);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        ItemStack taken = station.extract();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false, Prediction.SERVER_ONLY);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
