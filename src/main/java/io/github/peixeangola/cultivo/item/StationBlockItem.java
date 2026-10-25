package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.StationKind;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Item das estações, com uma linha explicando a função e a dica de uso em linhas curtas. */
public class StationBlockItem extends BlockItem {
    private final StationKind kind;

    public StationBlockItem(StationKind kind, Block block, Properties properties) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.cultivo.station." + kind.recipeTypeId())
            .withStyle(ChatFormatting.GRAY));
        TooltipItem.addHint(tooltip, "tooltip.cultivo.station.usage");
    }
}
