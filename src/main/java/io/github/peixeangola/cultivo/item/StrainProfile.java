package io.github.peixeangola.cultivo.item;

import io.github.peixeangola.cultivo.core.Variety;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Linha de perfil da variedade no tooltip de sementes e flores, por exemplo
 * "Comum · Automática · Sativa · Potência ★★☆". A raridade vem colorida (comum branca, incomum amarela, rara rosa).
 */
public final class StrainProfile {
    private StrainProfile() {}

    private static String key(String group, Enum<?> value) {
        return "tooltip.cultivo." + group + "." + value.name().toLowerCase(Locale.ROOT);
    }

    public static ChatFormatting rarityColor(Variety.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> ChatFormatting.WHITE;
            case UNCOMMON -> ChatFormatting.YELLOW;
            case RARE -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    public static Component line(Variety variety) {
        return Component.translatable("tooltip.cultivo.strain_profile",
            Component.translatable(key("rarity", variety.rarity())).withStyle(rarityColor(variety.rarity())),
            Component.translatable(key("cycle", variety.cycle())),
            Component.translatable(key("lineage", variety.lineage())),
            Component.translatable("tooltip.cultivo.potency." + variety.potency())).withStyle(ChatFormatting.GRAY);
    }
}
