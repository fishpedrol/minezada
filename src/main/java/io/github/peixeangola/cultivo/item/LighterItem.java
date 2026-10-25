package io.github.peixeangola.cultivo.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Isqueiro: precisa estar no inventário para fumar e perde 1 de durabilidade por uso. */
public class LighterItem extends Item {
    public LighterItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addHint(tooltip, "tooltip.cultivo.lighter");
    }
}
