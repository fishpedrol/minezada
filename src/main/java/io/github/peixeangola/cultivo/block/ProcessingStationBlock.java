package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.StationVisualRules;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
 *   <li>Só a mão principal vazia retira: primeiro a saída; sem saída, devolve a entrada em andamento e zera o
 *       progresso.</li>
 * </ul>
 * No cliente o resultado é previsto com o que ele sabe: a tag cultivo:&lt;estação&gt;_inputs e os slots
 * sincronizados da BlockEntity. Assim a mão secundária e o uso do item não são engolidos por um SUCCESS falso.
 *
 * <p>Blockstate: {@link #FACING} (a frente olha para quem colocou) e {@link #CONTENTS} (vazia, trabalhando ou pronta,
 * calculado por {@link StationVisualRules} e gravado pela BlockEntity só quando muda). O modelo é multipart: a base
 * gira com o facing e as partes de conteúdo aparecem conforme contents. Uma subclasse pode somar outra propriedade
 * (por exemplo um booleano na peneira) sobrescrevendo {@link #createBlockStateDefinition} e chamando o super: o
 * construtor daqui não lê campos da subclasse, e a BlockEntity só troca contents, preservando as outras.
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
            // Só a mão vazia de verdade chega a useWithoutItem (o vanilla faz isso apenas na mão principal)
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return station.couldInsert(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        int moved = station.insert(serverLevel, stack);
        if (moved <= 0) {
            // Nunca cai na retirada: segurar um item que não entra não mexe na estação
            return InteractionResult.PASS;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(moved);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel)) {
            // Estação vazia: PASS deixa a mão secundária tentar inserir
            return station.hasContents() ? InteractionResult.SUCCESS : InteractionResult.PASS;
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
