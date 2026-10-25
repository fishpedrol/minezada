package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.ProcessingRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.registry.ModItems;
import io.github.peixeangola.cultivo.registry.ModSounds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Peneira de bancada com o dichavador como melhoria (propriedade {@link #GRINDER} do blockstate).
 * <ul>
 *   <li>Clique direito com um dichavador numa peneira sem ele: instala (gasta 1, exceto no criativo).</li>
 *   <li>Agachado e de mãos vazias, com a peneira sem entrada: devolve o dichavador. Com flores na entrada, o mesmo gesto
 *       primeiro devolve as flores (como em toda estação), então segurar o botão agachado nunca tira o dichavador e
 *       depois puxa uma entrada em andamento. O vanilla só deixa o clique agachado chegar ao bloco quando as duas mãos
 *       estão vazias (ServerPlayerGameMode#useItemOn).</li>
 *   <li>Sem agachar, a mão vazia continua retirando só a saída, como nas outras estações.</li>
 * </ul>
 * Com o dichavador, cada ciclo rende +1 resina ({@link ProcessingRules#outputCount(StationKind, int, Integer, boolean)});
 * quebrar a peneira derruba o dichavador junto (pool extra em loot_table/blocks/sieve.json). A caixa de seleção e de
 * colisão cresce com o dichavador, que fica 2 px acima da moldura.
 *
 * <p>É uma subclasse porque createBlockStateDefinition roda dentro do construtor de Block, antes de o campo kind
 * existir: a estação base não tem como escolher as propriedades pelo tipo.
 */
public class SieveBlock extends ProcessingStationBlock {
    public static final BooleanProperty GRINDER = BooleanProperty.create(ContentIds.GRINDER_PROPERTY);
    /** O dichavador instalado, com a frente para o norte: as mesmas medidas da parte sieve_grinder do modelo. */
    static final VoxelShape GRINDER_BOX = Block.box(11.0D, 13.0D, 11.0D, 15.0D, 15.0D, 15.0D);
    /** Peneira com o dichavador, girada pelo facing como o modelo. */
    private static final Map<Direction, VoxelShape> SHAPES_WITH_GRINDER =
        Shapes.rotateHorizontal(Shapes.or(northShape(StationKind.SIEVING), GRINDER_BOX));

    public SieveBlock(Properties properties) {
        super(StationKind.SIEVING, properties);
        // O primeiro valor de um BooleanProperty é true: sem isto a peneira nasceria com dichavador
        registerDefaultState(defaultBlockState().setValue(GRINDER, false));
    }

    /** O estado é de uma peneira com o dichavador instalado (false para qualquer outro bloco). */
    public static boolean hasGrinder(BlockState state) {
        return state.hasProperty(GRINDER) && state.getValue(GRINDER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GRINDER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return hasGrinder(state) ? SHAPES_WITH_GRINDER.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
    }

    /**
     * A previsão da estação mais o dichavador: segurando um, SUCCESS se a peneira ainda não tem (instala) e PASS se já tem;
     * agachado e de mãos vazias, SUCCESS quando o gesto tira o dichavador ({@link #removesGrinder}).
     */
    @Override
    public InteractionResult predictUse(BlockState state, ProcessingStationBlockEntity station, ItemStack held,
                                        boolean sneaking) {
        if (held.is(ModItems.GRINDER.get())) {
            return state.getValue(GRINDER) ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && removesGrinder(state, station, sneaking)) {
            return InteractionResult.SUCCESS;
        }
        return super.predictUse(state, station, held, sneaking);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.GRINDER.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (state.getValue(GRINDER)) {
            // Já tem um: segue o vanilla, sem mexer na estação
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel)) {
            return level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station
                ? predictUse(state, station, stack, player.isSecondaryUseActive())
                : InteractionResult.PASS;
        }
        level.setBlock(pos, state.setValue(GRINDER, true), Block.UPDATE_ALL);
        stack.consume(1, player);
        level.playSound(null, pos, ModSounds.GRINDER_GRIND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /**
     * O gesto agachado e de mãos vazias tira o dichavador: só com ele instalado e sem entrada na peneira (com entrada, o
     * gesto devolve a entrada primeiro). Vale no cliente, com o blockstate e os slots sincronizados.
     */
    public static boolean removesGrinder(BlockState state, ProcessingStationBlockEntity station, boolean sneaking) {
        return sneaking && hasGrinder(state) && !station.hasInput();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ProcessingStationBlockEntity station)
            || !removesGrinder(state, station, player.isSecondaryUseActive())) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!(level instanceof ServerLevel)) {
            return predictUse(state, station, ItemStack.EMPTY, true);
        }
        level.setBlock(pos, state.setValue(GRINDER, false), Block.UPDATE_ALL);
        Handouts.give(player, new ItemStack(ModItems.GRINDER.get()));
        level.playSound(null, pos, ModSounds.GRINDER_GRIND.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.SUCCESS;
    }
}
