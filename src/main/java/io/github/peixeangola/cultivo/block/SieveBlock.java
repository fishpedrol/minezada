package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.ProcessingRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Peneira de bancada com o dichavador como melhoria (propriedade {@link #GRINDER} do blockstate).
 * <ul>
 *   <li>Clique direito com um dichavador numa peneira sem ele: instala (gasta 1, exceto no criativo).</li>
 *   <li>Agachado e com as duas mãos vazias: devolve o dichavador. O vanilla só deixa o clique agachado chegar ao bloco
 *       quando as duas mãos estão vazias (ServerPlayerGameMode#useItemOn).</li>
 *   <li>Sem agachar, a mão vazia continua retirando a saída, como nas outras estações.</li>
 * </ul>
 * Com o dichavador, cada ciclo rende +1 resina ({@link ProcessingRules#outputCount(StationKind, int, Integer, boolean)});
 * quebrar a peneira derruba o dichavador junto (pool extra em loot_table/blocks/sieve.json).
 *
 * <p>É uma subclasse porque createBlockStateDefinition roda dentro do construtor de Block, antes de o campo kind
 * existir: a estação base não tem como escolher as propriedades pelo tipo.
 */
public class SieveBlock extends ProcessingStationBlock {
    public static final BooleanProperty GRINDER = BooleanProperty.create(ContentIds.GRINDER_PROPERTY);

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
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(GRINDER, true), Block.UPDATE_ALL);
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !state.getValue(GRINDER)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(GRINDER, false), Block.UPDATE_ALL);
        ItemStack grinder = new ItemStack(ModItems.GRINDER.get());
        if (!player.getInventory().add(grinder)) {
            player.drop(grinder, false, Prediction.SERVER_ONLY);
        }
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
