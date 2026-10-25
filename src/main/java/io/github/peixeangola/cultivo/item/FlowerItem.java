package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.Variety;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Flor fresca ou seca de uma variedade: o item já é da variedade (não precisa de cultivo:strain). O tooltip mostra o
 * perfil da variedade e, como nos outros itens com qualidade, a linha de qualidade.
 */
public class FlowerItem extends QualityItem {
    private final Variety variety;

    public FlowerItem(Variety variety, Properties properties) {
        super(properties);
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
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
