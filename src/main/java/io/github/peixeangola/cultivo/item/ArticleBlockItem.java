package io.github.peixeangola.cultivo.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/**
 * Artigo que vira bloco ao ser colocado (bandeja, pote, cinzeiro). Continua com o ícone plano no inventário, como o
 * vaso de flor vanilla, e mostra a dica com o uso (em linhas curtas, {@link TooltipItem#addHint}).
 */
public class ArticleBlockItem extends BlockItem {
    private final String tooltipKey;

    public ArticleBlockItem(Block block, Properties properties, String tooltipKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        TooltipItem.addHint(tooltip, tooltipKey);
    }
}
