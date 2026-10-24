package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Pote de vidro colocado no mundo: guarda uma pilha da tag cultivo:jar_storable (flores secas, resina, haxixe,
 * baseado) e cura flores secas ({@link JarBlockEntity}).
 * <ul>
 *   <li>Clique direito com um item da tag põe o quanto couber, só no mesmo item com os mesmos componentes. Um item
 *       que não entra devolve PASS: o vanilla segue (usar o item, colocar bloco) e o pote não muda.</li>
 *   <li>Mão vazia retira a pilha inteira.</li>
 *   <li>Comparador: ⌈15 × quantidade / pilha máxima⌉. Quebrar o pote derruba o conteúdo.</li>
 * </ul>
 * {@link #FILLED} só escolhe o modelo (com ou sem conteúdo atrás do vidro) e é gravado pela BlockEntity quando muda.
 */
public class JarBlock extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape SHAPE = Block.box(4.5D, 0.0D, 4.5D, 11.5D, 9.5D, 11.5D);

    public JarBlock(Properties properties) {
        super(properties);
        // O primeiro valor de um BooleanProperty é true: o pote nasce vazio
        registerDefaultState(stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JarBlockEntity(pos, state);
    }

    /** Só no servidor e só com algo dentro: um pote vazio não tem o que curar. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.JAR.get() || !state.getValue(FILLED)) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> ((JarBlockEntity) blockEntity).serverTick((ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof JarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel)) {
            // Previsão com o conteúdo sincronizado: a mão secundária e o uso do item não são engolidos
            return jar.couldInsert(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        int moved = jar.insert(stack);
        if (moved <= 0) {
            return InteractionResult.PASS;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(moved);
        }
        float fill = (float) jar.getItem().getCount() / jar.getItem().getMaxStackSize();
        level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.8F, 1.0F + 0.5F * fill);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof JarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel)) {
            return jar.getItem().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        ItemStack taken = jar.extract();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false, Prediction.SERVER_ONLY);
        }
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof JarBlockEntity jar ? jar.comparatorSignal() : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
