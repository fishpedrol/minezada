package io.github.peixeangola.cultivo.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Item da estufinha: coloca as duas metades, como a porta vanilla (o {@link DoubleHighBlockItem} limpa o espaço de cima
 * antes), mantém o ícone plano e mostra a dica de uso em linhas curtas ({@link TooltipItem#addHint}).
 */
public class GrowTentItem extends DoubleHighBlockItem {
    public static final String TOOLTIP = "tooltip.cultivo.grow_tent";

    public GrowTentItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addHint(tooltip, TOOLTIP);
    }
}
