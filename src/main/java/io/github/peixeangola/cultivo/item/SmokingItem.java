package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules.Refusal;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModComponents;
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

/**
 * Baseado, cachimbo, chillum, bong e narguilé. Segure o botão direito para fumar: precisa de isqueiro no
 * inventário e, exceto o baseado, de flor seca ou haxixe. O resultado é calculado no servidor
 * ({@link SmokeSession}).
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
                String key = refusal == Refusal.NEEDS_LIGHTER ? "message.cultivo.need_lighter" : "message.cultivo.need_fuel";
                player.sendOverlayMessage(Component.translatable(key));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return device.useTicks();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof Player player) {
            SmokeSession.smoke(serverLevel, player, stack, device);
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
        tooltip.accept(Component.translatable(device.needsFuel()
            ? "tooltip.cultivo.smoke.device" : "tooltip.cultivo.smoke.joint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
