package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.AshtrayRules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cinzeiro de terracota colocado no mundo, no estilo da composteira ({@link AshtrayRules}). A cinza ({@link #ASH},
 * 0–4) sobe 1 a cada sessão de fumo que termina por perto ({@link #catchAsh}, chamado pela sessão no servidor). Cheio,
 * a mão vazia tira 1 farinha de osso e ele volta a 0; segurar qualquer item não esvazia. Comparador: cinza × 3.
 * Quebrado, derruba só a si mesmo (a cinza se perde).
 */
public class AshtrayBlock extends Block {
    public static final IntegerProperty ASH = IntegerProperty.create("ash", 0, AshtrayRules.MAX_ASH);
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 2.5D, 12.0D);

    public AshtrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ASH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ASH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Uma sessão de fumo terminou com o jogador em {@code center}: o cinzeiro mais próximo que ainda não está cheio,
     * até {@link AshtrayRules#RADIUS_XZ} blocos na horizontal e {@link AshtrayRules#RADIUS_Y} na vertical, ganha 1 de
     * cinza. Usa UPDATE_ALL para o comparador acompanhar. Retorna onde a cinza caiu, se caiu.
     */
    public static Optional<BlockPos> catchAsh(ServerLevel level, BlockPos center) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-AshtrayRules.RADIUS_XZ, -AshtrayRules.RADIUS_Y, -AshtrayRules.RADIUS_XZ),
            center.offset(AshtrayRules.RADIUS_XZ, AshtrayRules.RADIUS_Y, AshtrayRules.RADIUS_XZ))) {
            if (!AshtrayRules.inRange(pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AshtrayBlock) || AshtrayRules.isFull(state.getValue(ASH))) {
                continue;
            }
            double distance = pos.distSqr(center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos.immutable();
            }
        }
        if (nearest == null) {
            return Optional.empty();
        }
        BlockState state = level.getBlockState(nearest);
        level.setBlock(nearest, state.setValue(ASH, AshtrayRules.next(state.getValue(ASH))), Block.UPDATE_ALL);
        level.playSound(null, nearest, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.3F, 1.0F);
        return Optional.of(nearest);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        // Só a mão vazia esvazia: com um item na mão o vanilla segue (usar o item, colocar bloco)
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!AshtrayRules.isFull(state.getValue(ASH))) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            Block.popResourceFromFace(level, pos, Direction.UP,
                new ItemStack(Items.BONE_MEAL, AshtrayRules.BONE_MEAL_PER_EMPTYING));
            level.setBlock(pos, state.setValue(ASH, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AshtrayRules.comparatorSignal(state.getValue(ASH));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
