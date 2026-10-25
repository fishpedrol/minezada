package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.RollingRules;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModItems;
import io.github.peixeangola.cultivo.registry.ModSounds;
import io.github.peixeangola.cultivo.registry.ModTags;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bandeja de enrolar colocada no mundo ({@link #FACING}: a frente olha para quem colocou). Clique direito com flores
 * secas iguais na mão (pelo menos {@link RollingRules#FLOWERS_PER_ROLL} na mesma pilha) e
 * {@link RollingRules#PAPERS_PER_ROLL} folhas de seda no inventário: gasta as flores e as folhas e entrega
 * {@link RollingRules#JOINTS_PER_ROLL} baseados. Os baseados levam os componentes da flor (qualidade, cura), como o
 * transmute da bancada, e a variedade em cultivo:strain, então servem para qualquer uma das variedades. Os baseados vão
 * para o inventário, e o que não couber cai aos pés do jogador (também no criativo). Sem o bastante, avisa acima da
 * barra de itens e não gasta nada; no criativo não gasta nada. Outros itens seguem o vanilla (PASS).
 *
 * <p>A mão principal sem flores bastantes deixa a mão secundária tentar quando ela segura flores secas
 * ({@link RollingRules#defersToOffHand}): o aviso só sai quando nenhuma das mãos consegue enrolar. Tudo é decidido no
 * servidor; o cliente prevê com a mesma regra ({@link #predict}), pelas pilhas das mãos e as folhas do inventário, que
 * ele conhece.
 */
public class RollingTrayBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Caixa com a frente para o norte (as medidas do modelo), girada pelo facing. */
    private static final Map<Direction, VoxelShape> SHAPES =
        Shapes.rotateHorizontal(Block.box(2.0D, 0.0D, 3.0D, 14.0D, 2.0D, 13.0D));

    public RollingTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** O que o clique de uma mão com flores secas faz na bandeja, pela regra pura (igual no cliente e no servidor). */
    static RollingRules.Refusal refusal(Player player, ItemStack stack) {
        return RollingRules.check(stack.getCount(), countPapers(player));
    }

    /** A mão principal sem flores bastantes passa o clique para a secundária, que segura flores secas. */
    static boolean defers(Player player, InteractionHand hand, RollingRules.Refusal refusal) {
        return RollingRules.defersToOffHand(hand == InteractionHand.MAIN_HAND, refusal,
            player.getOffhandItem().is(ModTags.DRIED_FLOWERS));
    }

    /**
     * Previsão do cliente (é o que o ramo do cliente de {@link #useItemOn} devolve) para o clique de uma mão: PASS se a
     * mão não segura flores secas ou se o clique passa para a outra mão; SUCCESS se enrola; CONSUME se o servidor vai
     * recusar e avisar (sem balançar o braço, como o servidor). Pública para os GameTests compararem com o servidor.
     */
    public static InteractionResult predict(Player player, InteractionHand hand, ItemStack stack) {
        if (!stack.is(ModTags.DRIED_FLOWERS)) {
            return InteractionResult.PASS;
        }
        RollingRules.Refusal refusal = refusal(player, stack);
        if (defers(player, hand, refusal)) {
            return InteractionResult.PASS;
        }
        return refusal == RollingRules.Refusal.NONE ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel)) {
            // Enrolar ou avisar acontecem no servidor; o cliente só decide se trata o clique ou deixa a outra mão tentar
            return predict(player, hand, stack);
        }
        if (!stack.is(ModTags.DRIED_FLOWERS)) {
            return InteractionResult.PASS;
        }
        RollingRules.Refusal refusal = refusal(player, stack);
        if (defers(player, hand, refusal)) {
            // A mão secundária enrola ou dá o próprio aviso
            return InteractionResult.PASS;
        }
        if (refusal != RollingRules.Refusal.NONE) {
            player.sendOverlayMessage(Component.translatable(refusal.messageKey()));
            // O clique foi tratado (com o aviso): a outra mão não tenta de novo
            return InteractionResult.CONSUME;
        }
        ItemStack joints = rollJoints(stack);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(RollingRules.FLOWERS_PER_ROLL);
            removePapers(player, RollingRules.PAPERS_PER_ROLL);
        }
        Handouts.give(player, joints);
        level.playSound(null, pos, ModSounds.TRAY_ROLL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /**
     * Os baseados de uma enrolada: copiam os componentes da flor (qualidade e, se houver, a cura), como o
     * TransmuteRecipe da bancada faz, e gravam a variedade da flor em cultivo:strain. Assim um baseado da bandeja junta
     * na mesma pilha com um da bancada feito da mesma flor.
     */
    public static ItemStack rollJoints(ItemStack flowers) {
        ItemStack joints = flowers.transmuteCopy(ModItems.JOINT.get(), RollingRules.JOINTS_PER_ROLL);
        Variety variety = ModItems.varietyOf(flowers);
        if (variety != null) {
            joints.set(ModComponents.STRAIN.get(), variety);
        }
        return joints;
    }

    /** Folhas de seda no inventário todo (mãos incluídas). */
    static int countPapers(Player player) {
        Inventory inventory = player.getInventory();
        int papers = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ROLLING_PAPER.get())) {
                papers += stack.getCount();
            }
        }
        return papers;
    }

    private static void removePapers(Player player, int amount) {
        Inventory inventory = player.getInventory();
        int left = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ROLLING_PAPER.get())) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
        inventory.setChanged();
    }
}
