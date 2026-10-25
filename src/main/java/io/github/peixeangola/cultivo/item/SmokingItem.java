package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules.Refusal;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.smoke.SmokeEffects;
import io.github.peixeangola.cultivo.smoke.SmokeSession;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Baseado, cachimbo, chillum, bong e narguilé. Segure o botão direito para fumar: precisa de isqueiro no
 * inventário e, exceto o baseado, de flor seca ou haxixe. O resultado é calculado no servidor
 * ({@link SmokeSession}).
 *
 * <p>Animação: o uso é {@link ItemUseAnimation#NONE} para o vanilla não impor a pose da corneta; a pose do braço na 3ª
 * pessoa e a subida até a boca na 1ª vêm das extensões de cliente
 * ({@code io.github.peixeangola.cultivo.client.SmokingItemClient}, só carregada no cliente), com os números de
 * {@link io.github.peixeangola.cultivo.core.SmokeAnimation}. Partículas e sons ficam em {@link SmokeEffects}.
 */
public class SmokingItem extends Item {
    private final SmokeDevice device;

    public SmokingItem(SmokeDevice device, Properties properties) {
        super(properties);
        this.device = device;
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
        tooltip.accept(Component.translatable(device.needsFuel()
            ? "tooltip.cultivo.smoke.device" : "tooltip.cultivo.smoke.joint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
