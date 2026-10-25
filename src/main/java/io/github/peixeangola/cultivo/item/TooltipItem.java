package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.TooltipHints;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Item simples com uma dica de uso própria: folha de seda e piteira (ingredientes) e dichavador (melhoria). */
public class TooltipItem extends Item {
    private final String tooltipKey;

    public TooltipItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    public String tooltipKey() {
        return tooltipKey;
    }

    /**
     * Uma dica de uso em linhas curtas ({@code <chave>.1} … {@code <chave>.n}, quantas {@link TooltipHints} disser), em
     * cinza escuro como as dicas vanilla. Cada linha cabe na tela mesmo na escala de GUI automática.
     */
    public static void addHint(Consumer<Component> tooltip, String key) {
        for (String line : TooltipHints.lineKeys(key)) {
            tooltip.accept(Component.translatable(line).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        addHint(tooltip, tooltipKey);
    }
}
