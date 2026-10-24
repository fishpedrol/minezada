package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Flores, resina e haxixe: mostram o componente cultivo:quality no tooltip. */
public class QualityItem extends Item {
    public QualityItem(Properties properties) {
        super(properties);
    }

    public static ChatFormatting color(int quality) {
        return switch (quality) {
            case 1 -> ChatFormatting.DARK_GRAY;
            case 2 -> ChatFormatting.GRAY;
            case 3 -> ChatFormatting.GREEN;
            case 4 -> ChatFormatting.AQUA;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Integer quality = stack.get(ModComponents.QUALITY.get());
        if (quality != null) {
            tooltip.accept(Component.translatable("tooltip.cultivo.quality",
                Component.translatable("tooltip.cultivo.quality." + quality)).withStyle(color(quality)));
        }
    }
}
