package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.StationKind;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Vidros, vidros tingidos e painéis que formam teto e paredes da estufa. */
    public static final TagKey<Block> GREENHOUSE_GLASS = TagKey.create(Registries.BLOCK, Cultivo.id("greenhouse_glass"));
    public static final TagKey<Item> DRIED_FLOWERS = TagKey.create(Registries.ITEM, Cultivo.id("dried_flowers"));

    /**
     * Entradas aceitas por estação (cultivo:&lt;estação&gt;_inputs), geradas a partir das receitas. As tags
     * chegam ao cliente; as receitas não.
     */
    public static final Map<StationKind, TagKey<Item>> STATION_INPUTS = new EnumMap<>(StationKind.class);

    static {
        for (StationKind kind : StationKind.values()) {
            STATION_INPUTS.put(kind, TagKey.create(Registries.ITEM, Cultivo.id(kind.inputTagId())));
        }
    }

    private ModTags() {}
}
