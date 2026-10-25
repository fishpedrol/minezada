package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules.Refusal;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.smoke.SmokeEffects;
import io.github.peixeangola.cultivo.smoke.SmokeSession;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Baseado, cachimbo, chillum, bong e narguilé. Segure o botão direito para fumar: precisa de isqueiro no
 * inventário e, exceto o baseado, de flor seca ou haxixe. O resultado é calculado no servidor
 * ({@link SmokeSession}). O bong também vai para o chão: agachado, o clique num bloco põe o bloco dele
 * ({@link #useOn}).
 *
 * <p>Animação: o uso é {@link ItemUseAnimation#NONE} para o vanilla não impor a pose da corneta; a pose do braço na 3ª
 * pessoa e a subida até a boca na 1ª vêm das extensões de cliente
 * ({@code io.github.peixeangola.cultivo.client.SmokingItemClient}, só carregada no cliente), com os números de
 * {@link io.github.peixeangola.cultivo.core.SmokeAnimation}. Partículas e sons ficam em {@link SmokeEffects}.
 */
public class SmokingItem extends Item {
    private final SmokeDevice device;
    /** Bloco que o aparelho põe no chão quando o jogador agacha e clica num bloco (o bong); null nos outros. */
    private final @Nullable Supplier<? extends Block> placed;

    public SmokingItem(SmokeDevice device, Properties properties) {
        this(device, properties, null);
    }

    public SmokingItem(SmokeDevice device, Properties properties, @Nullable Supplier<? extends Block> placed) {
        super(properties);
        this.device = device;
        this.placed = placed;
    }

    public SmokeDevice device() {
        return device;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Refusal refusal = SmokeSession.check(player, device);
        if (refusal != Refusal.NONE) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(refusal.messageKey()));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel serverLevel) {
            SmokeEffects.startSounds(serverLevel, player);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Agachado, o clique num bloco põe o aparelho no chão (só o bong; pedido do dono de 25/10/2026), como um BlockItem:
     * no lugar clicado ou na face, se couber, com o som de colocar do bloco, gastando o item (nada no criativo). Sem
     * agachar, ou sem bloco próprio, devolve PASS e o vanilla segue para {@link #use}, que fuma. O modo aventura já é
     * barrado antes, pelo ItemStack#useOn vanilla.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (placed == null || !context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return InteractionResult.FAIL;
        }
        Level level = place.getLevel();
        BlockPos pos = place.getClickedPos();
        Player player = place.getPlayer();
        BlockState state = placed.get().getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)
            || !level.isUnobstructed(state, pos, CollisionContext.placementContext(player))
            || !level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        BlockState placedState = level.getBlockState(pos);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, place.getItemInHand());
        }
        SoundType sound = placedState.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
            sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placedState));
        place.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /** Sem animação vanilla: a de fumar é do mod (cliente), e com NONE o vanilla pergunta a pose às extensões. */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    /**
     * Um tick segurando o botão: no cliente (de quem fuma e de quem está olhando), brasa, fumaça e bolhas; no servidor,
     * a tragada e o borbulhar, para todos ouvirem.
     */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        int elapsed = device.useTicks() - ticksRemaining;
        if (level instanceof ServerLevel serverLevel) {
            SmokeEffects.useTickSounds(serverLevel, user, device, elapsed);
        } else {
            SmokeEffects.useTickParticles(level, user, device, elapsed);
        }
    }

    /**
     * Ponte para o cliente: o Forge só chama isto no cliente físico (Item#initClient confere o Dist), então a classe do
     * pacote client nunca é carregada num servidor dedicado. Roda dentro do construtor de Item, antes de {@link #device}
     * existir: as extensões leem o aparelho do item na hora de desenhar.
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(io.github.peixeangola.cultivo.client.SmokingItemClient.EXTENSIONS);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return device.useTicks();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player
            && SmokeSession.smoke(serverLevel, player, stack, device).isEmpty()) {
            // A animação terminou mas o servidor recusou (isqueiro ou material saiu do inventário no meio do uso)
            SmokeSession.explainRefusal(player, device);
        }
        return stack;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Variety strain = stack.get(ModComponents.STRAIN.get());
        if (strain != null) {
            tooltip.accept(Component.translatable("tooltip.cultivo.strain",
                Component.translatable("tooltip.cultivo.variety." + strain.id())).withStyle(ChatFormatting.GRAY));
        }
        Integer quality = stack.get(ModComponents.QUALITY.get());
        if (quality != null) {
            tooltip.accept(Component.translatable("tooltip.cultivo.quality",
                Component.translatable("tooltip.cultivo.quality." + quality)).withStyle(QualityItem.color(quality)));
        }
        QualityItem.appendCured(stack, tooltip);
        if (stack.has(ModComponents.FILTERED.get())) {
            tooltip.accept(Component.translatable("tooltip.cultivo.filtered").withStyle(ChatFormatting.YELLOW));
        }
        TooltipItem.addHint(tooltip, device.needsFuel() ? "tooltip.cultivo.smoke.device" : "tooltip.cultivo.smoke.joint");
        if (placed != null) {
            TooltipItem.addHint(tooltip, "tooltip.cultivo.bong.place");
        }
    }
}
