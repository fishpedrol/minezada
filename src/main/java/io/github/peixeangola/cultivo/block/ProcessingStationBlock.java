package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.StationVisualRules;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Suporte de secagem, peneira de bancada ou prensa manual. Toda mudança de inventário acontece no servidor.
 * <ul>
 *   <li>Clique direito com um ingrediente insere na entrada.</li>
 *   <li>Um item que não entra (outro item, outra qualidade, entrada cheia) devolve PASS: o vanilla segue para o uso
 *       do item ou a colocação de bloco, e nada sai da estação.</li>
 *   <li>Mão principal vazia: tira só a saída pronta; sem saída, PASS. Segurar o botão (o vanilla repete o clique a cada
 *       4 ticks) ou o clique de outro jogador logo depois nunca puxa a entrada em andamento.</li>
 *   <li>Agachado e de mãos vazias (o gesto de propósito): devolve a entrada e zera o progresso.</li>
 * </ul>
 * No cliente o resultado é previsto com o que ele sabe ({@link #predictUse}): a tag
 * cultivo:&lt;estação&gt;_inputs, o agachar e os slots sincronizados da BlockEntity. Assim a mão secundária e o uso do
 * item não são engolidos por um SUCCESS falso. O servidor devolve SUCCESS (balanço já previsto pelo cliente), nunca
 * SUCCESS_SERVER, que faria o servidor repetir o balanço do braço para quem clicou.
 *
 * <p>Blockstate: {@link #FACING} (a frente olha para quem colocou) e {@link #CONTENTS} (vazia, carregada, trabalhando
 * ou pronta, calculado por {@link StationVisualRules} e gravado pela BlockEntity só quando muda). O modelo é
 * multipart: a base gira com o facing e as partes de conteúdo aparecem conforme contents. Uma subclasse soma outra
 * propriedade sobrescrevendo {@link #createBlockStateDefinition} e chamando o super, como a {@link SieveBlock} faz com
 * o dichavador: o construtor daqui não lê campos da subclasse, e a BlockEntity só troca contents, preservando as outras.
 *
 * <p>Animação: só com o ciclo andando (contents=working) o maço do suporte balança e as flores da peneira tremem
 * (texturas animadas), a placa da prensa sobe e desce (desenhada no cliente) e {@link #animateTick} solta partículas só
 * no cliente. Carregada (a receita não anda), a entrada aparece parada. O som de trabalho e o sopro do fim de ciclo vêm
 * do servidor ({@link ProcessingStationBlockEntity}).
 */
public class ProcessingStationBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<StationContents> CONTENTS = EnumProperty.create("contents", StationContents.class);

    private final StationKind kind;
    private final Map<Direction, VoxelShape> shapes;

    public ProcessingStationBlock(StationKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.shapes = Shapes.rotateHorizontal(northShape(kind));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENTS, StationContents.EMPTY));
    }

    public StationKind kind() {
        return kind;
    }

    /**
     * Caixa de colisão/seleção com a frente para o norte (as mesmas medidas dos modelos 3D): o suporte de secagem é
     * um cavalete fino, a peneira é uma bancada e a prensa ocupa quase o bloco todo.
     */
    static VoxelShape northShape(StationKind kind) {
        return switch (kind) {
            case DRYING -> Block.box(0.0D, 0.0D, 6.0D, 16.0D, 15.0D, 10.0D);
            case SIEVING -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);
            case PRESSING -> Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        };
    }

    /** O estado com contents trocado pelo valor da regra; as demais propriedades ficam como estão. */
    public static BlockState withContents(BlockState state, StationVisualRules.Contents contents) {
        return state.setValue(CONTENTS, StationContents.of(contents));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONTENTS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Pó âmbar que cai da tela da peneira na bandeja (a cor vem do mapa do bloco de favo: laranja). */
    private static final ParticleOptions SIEVE_DUST =
        new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.HONEYCOMB_BLOCK.defaultBlockState());
    /** Farelo verde que cai dos maços do suporte de secagem (cor do mapa do bloco de musgo). */
    private static final ParticleOptions LEAF_DUST =
        new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MOSS_BLOCK.defaultBlockState());

    /**
     * Só no cliente, com a estação trabalhando: a peneira solta pó âmbar da tela (y 11) para a bandeja de baixo; a prensa
     * pinga resina pelas beiradas da placa; o suporte de secagem solta um farelo verde dos maços. O vanilla chama isto
     * para blocos sorteados perto do jogador (umas 0,4 vez por segundo por bloco), então cada chamada solta um punhado.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTENTS) != StationContents.WORKING) {
            return;
        }
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        switch (kind) {
            case SIEVING -> {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(SIEVE_DUST, x + 0.2D + random.nextDouble() * 0.6D, y + 0.66D,
                        z + 0.2D + random.nextDouble() * 0.6D, 0.0D, 0.0D, 0.0D);
                }
            }
            case PRESSING -> {
                // Um ponto da borda da placa (x ou z em 4/16 ou 12/16), na altura da resina apertada
                double along = 0.25D + random.nextDouble() * 0.5D;
                double edge = random.nextBoolean() ? 0.24D : 0.76D;
                boolean alongX = random.nextBoolean();
                level.addParticle(ParticleTypes.DRIPPING_HONEY, x + (alongX ? along : edge), y + 0.27D,
                    z + (alongX ? edge : along), 0.0D, 0.0D, 0.0D);
            }
            case DRYING -> {
                if (random.nextBoolean()) {
                    // Os maços ficam na linha do meio do cavalete, que gira com o facing
                    double along = 0.25D + random.nextDouble() * 0.5D;
                    boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
                    level.addParticle(LEAF_DUST, x + (alongX ? along : 0.5D), y + 0.35D + random.nextDouble() * 0.2D,
                        z + (alongX ? 0.5D : along), 0.0D, 0.0D, 0.0D);
                }
            }
        }
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

    /**
     * O que o cliente prevê para um clique (é o que os ramos do cliente de {@link #useItemOn} e {@link #useWithoutItem}
     * devolvem), com o que ele sabe: o blockstate, os slots sincronizados da BlockEntity, a pilha na mão e o agachar.
     * <ul>
     *   <li>Com um item: SUCCESS se a pilha entra (tag de entradas e slots); senão PASS, para o vanilla seguir com o uso
     *       do item, a colocação de bloco ou a mão secundária.</li>
     *   <li>Mão vazia: agachado, SUCCESS se há entrada para devolver; sem agachar, SUCCESS se há saída pronta; senão PASS
     *       (a mão secundária pode inserir).</li>
     * </ul>
     * Pública para os GameTests compararem com o resultado do servidor numa visão montada só com a tag de atualização.
     */
    public InteractionResult predictUse(BlockState state, ProcessingStationBlockEntity station, ItemStack held,
                                        boolean sneaking) {
        if (held.isEmpty()) {
            boolean something = sneaking ? station.hasInput() : station.hasOutput();
            return something ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return station.couldInsert(held) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            // Só a mão vazia de verdade chega a useWithoutItem (o vanilla faz isso apenas na mão principal)
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return predictUse(state, station, stack, player.isSecondaryUseActive());
        }
        int moved = station.insert(serverLevel, stack);
        if (moved <= 0) {
            // Nunca cai na retirada: segurar um item que não entra não mexe na estação
            return InteractionResult.PASS;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(moved);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        // Agachado o vanilla só chega aqui com as duas mãos vazias (ServerPlayerGameMode#useItemOn)
        boolean sneaking = player.isSecondaryUseActive();
        if (!(level instanceof ServerLevel)) {
            return predictUse(state, station, ItemStack.EMPTY, sneaking);
        }
        ItemStack taken = sneaking ? station.takeInput() : station.takeOutput();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        Handouts.give(player, taken);
        return InteractionResult.SUCCESS;
    }
}
