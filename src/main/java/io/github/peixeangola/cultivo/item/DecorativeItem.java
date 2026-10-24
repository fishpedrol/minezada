package io.github.peixeangola.cultivo.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Artigos sem uso próprio (piteira, dichavador, bandeja, pote, cinzeiro): só decoração/coleção. */
public class DecorativeItem extends Item {
    public DecorativeItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cultivo.decorative").withStyle(ChatFormatting.DARK_GRAY));
    }
}
