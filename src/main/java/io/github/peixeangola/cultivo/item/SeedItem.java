package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.Variety;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Semente: BlockItem do crop, com o perfil da variedade (raridade, ciclo, genética e potência) no tooltip. */
public class SeedItem extends BlockItem {
    private final Variety variety;

    public SeedItem(Variety variety, Block crop, Properties properties) {
        super(crop, properties);
        this.variety = variety;
    }

    public Variety variety() {
        return variety;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(StrainProfile.line(variety));
    }
}
