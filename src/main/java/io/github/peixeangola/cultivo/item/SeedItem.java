package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.Variety;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Semente: BlockItem do crop, com raridade e perfil (ciclo e genética) no tooltip. */
public class SeedItem extends BlockItem {
    private final Variety variety;

    public SeedItem(Variety variety, Block crop, Properties properties) {
        super(crop, properties);
        this.variety = variety;
    }

    public Variety variety() {
        return variety;
    }

    private static String key(String group, Enum<?> value) {
        return "tooltip.cultivo." + group + "." + value.name().toLowerCase(Locale.ROOT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        ChatFormatting rarityColor = switch (variety.rarity()) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.YELLOW;
            case RARE -> ChatFormatting.LIGHT_PURPLE;
        };
        tooltip.accept(Component.translatable("tooltip.cultivo.seed_profile",
            Component.translatable(key("rarity", variety.rarity())).withStyle(rarityColor),
            Component.translatable(key("cycle", variety.cycle())),
            Component.translatable(key("lineage", variety.lineage()))).withStyle(ChatFormatting.GRAY));
    }
}
