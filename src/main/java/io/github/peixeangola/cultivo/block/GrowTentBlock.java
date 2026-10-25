package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.GrowTentRules;
import io.github.peixeangola.cultivo.item.SeedItem;
import io.github.peixeangola.cultivo.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Estufinha (grow tent): bloco de 2 de altura, como a porta vanilla, com luz roxa e um pé de planta dentro. A planta
 * (variedade e idade) fica na {@link GrowTentBlockEntity} da metade de baixo; a de cima só manda tudo para baixo.
 * <ul>
 *   <li>Planta sem farmland, sem água e sem luz de fora: cresce pelo random tick da metade de baixo
 *       ({@link GrowTentRules#rollBound}), em qualquer lugar onde o bloco fica em pé (chão firme embaixo).</li>
 *   <li>Clique direito ({@link GrowTentRules#use}): com uma semente na estufinha vazia, planta; com a planta madura,
 *       colhe flores e sementes pela tabela da lavoura (qualidade 4–5, com a fortuna da mão principal) e a estufinha
 *       fica vazia; antes disso nada, e a farinha de osso do vanilla segue (+2 a +5 idades).</li>
 *   <li>Quebrar qualquer metade derruba 1 estufinha (só a metade de baixo tem o item no loot) e a planta uma vez.</li>
 *   <li>Luz de bloco {@link GrowTentRules#LAMP_LIGHT} na metade de cima e partículas roxas (só no cliente).</li>
 * </ul>
 * O cliente prevê o clique com a mesma regra e os dados sincronizados; o servidor devolve SUCCESS, nunca
 * SUCCESS_SERVER (o braço já balançou pela previsão). Pistões não movem a estufinha (as metades se separariam).
 */
public class GrowTentBlock extends Block implements EntityBlock, BonemealableBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Roxo das partículas, o mesmo da lâmpada (textura grow_tent_lamp). */
    public static final int PURPLE = 0xB04DFF;

    private static final VoxelShape LOWER_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    private static final VoxelShape UPPER_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public GrowTentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    /** Posição da metade de baixo (a que tem a planta) a partir de qualquer metade. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    /** A BlockEntity da planta, clicando em qualquer metade (null se faltar, por dados corrompidos). */
    public static @Nullable GrowTentBlockEntity tentAt(BlockGetter level, BlockState state, BlockPos pos) {
        return level.getBlockEntity(lowerPos(state, pos)) instanceof GrowTentBlockEntity tent ? tent : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    // ------------------------------------------------------------------ duas metades (como DoorBlock)

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        level.setBlockAndUpdate(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER));
    }

    /** Cada metade some sem a outra; a de baixo também some sem chão firme. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (directionToNeighbour == half.getDirectionToOther()) {
            return neighbourState.is(this) && neighbourState.getValue(HALF) != half
                ? state
                : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
            ? belowState.isFaceSturdy(level, below, Direction.UP)
            : belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /**
     * No criativo, quebrar a metade de cima tira a de baixo sem drop do item (cópia de
     * DoublePlantBlock#preventDropFromBottomPart, que só o pacote vanilla enxerga). A planta ainda cai, pelo
     * preRemoveSideEffects da BlockEntity, como o conteúdo de um baú quebrado no criativo.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos bottomPos = pos.below();
            BlockState bottomState = level.getBlockState(bottomPos);
            if (bottomState.is(this) && bottomState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState replacement = bottomState.getFluidState().is(Fluids.WATER)
                    ? Blocks.WATER.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
                level.setBlock(bottomPos, replacement, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, bottomPos, Block.getId(bottomState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new GrowTentBlockEntity(pos, state) : null;
    }

    // ------------------------------------------------------------------ crescimento (só no servidor)

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /** Como a lavoura, mas sem luz, farmland nem vizinhos: só o sorteio da variedade na estufinha. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof GrowTentBlockEntity tent) || !tent.canGrow()) {
            return;
        }
        boolean grows = random.nextInt(GrowTentRules.rollBound(tent.variety())) == 0;
        if (ForgeHooks.onCropsGrowPre(level, pos, state, grows)) {
            tent.setAge(tent.age() + 1);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    /** Vale também no cliente (a farinha de osso prevê o clique), pelos dados sincronizados. */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        GrowTentBlockEntity tent = tentAt(level, state, pos);
        return tent != null && tent.canGrow();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        GrowTentBlockEntity tent = tentAt(level, state, pos);
        if (tent != null && tent.canGrow()) {
            int bonus = Mth.nextInt(random, GrowTentRules.BONE_MEAL_MIN, GrowTentRules.BONE_MEAL_MAX);
            tent.setAge(GrowTentRules.afterBoneMeal(tent.age(), bonus));
        }
    }

    // ------------------------------------------------------------------ clique direito

    /** O que o clique faz com o que o lado que chama sabe (no cliente, os dados sincronizados). */
    public static GrowTentRules.Use predict(GrowTentBlockEntity tent, ItemStack held) {
        return GrowTentRules.use(tent.hasPlant(), tent.age(), held.getItem() instanceof SeedItem);
    }

    /** Resultado do clique no cliente: SUCCESS quando o servidor vai plantar ou colher; senão PASS. */
    public static InteractionResult predictResult(GrowTentBlockEntity tent, ItemStack held) {
        return predict(tent, held) == GrowTentRules.Use.NONE ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        GrowTentBlockEntity tent = tentAt(level, state, pos);
        if (tent == null) {
            return InteractionResult.PASS;
        }
        if (predict(tent, stack) != GrowTentRules.Use.PLANT) {
            // Colher (com qualquer coisa na mão principal) ou nada: segue para useWithoutItem; a farinha de osso, sem
            // nada a fazer aqui, cai no uso do item
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        tent.plant(((SeedItem) stack.getItem()).variety());
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        BlockPos lower = tent.getBlockPos();
        level.playSound(null, lower, ModSounds.GROW_TENT_PLANT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_PLACE, lower, GameEvent.Context.of(player, level.getBlockState(lower)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        GrowTentBlockEntity tent = tentAt(level, state, pos);
        if (tent == null || !tent.isRipe()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        // A tabela da lavoura com a fortuna da mão principal, como quebrar a lavoura madura
        var drops = tent.rollPlantDrops(serverLevel, player, player.getMainHandItem());
        tent.clear();
        drops.forEach(drop -> Handouts.give(player, drop));
        BlockPos lower = tent.getBlockPos();
        level.playSound(null, lower, ModSounds.GROW_TENT_HARVEST.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, lower, GameEvent.Context.of(player, level.getBlockState(lower)));
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ partículas (só no cliente)

    /**
     * Pó roxo caindo da lâmpada, na metade de cima; com a planta madura, um ponto roxo a mais na metade de baixo, como
     * aviso de "pronta". O vanilla chama isto só no cliente, para blocos sorteados perto do jogador.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            if (random.nextInt(3) == 0) {
                level.addParticle(new DustParticleOptions(PURPLE, 0.6F), x + 0.25D + random.nextDouble() * 0.5D, y + 0.6D,
                    z + 0.25D + random.nextDouble() * 0.5D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (random.nextInt(5) == 0 && level.getBlockEntity(pos) instanceof GrowTentBlockEntity tent && tent.isRipe()) {
            level.addParticle(new DustParticleOptions(PURPLE, 0.8F), x + 0.3D + random.nextDouble() * 0.4D, y + 0.9D,
                z + 0.3D + random.nextDouble() * 0.4D, 0.0D, 0.0D, 0.0D);
        }
    }
}
