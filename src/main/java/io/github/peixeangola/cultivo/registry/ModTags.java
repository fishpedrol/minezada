package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Vidros, vidros tingidos e painéis que formam teto e paredes da estufa. */
    public static final TagKey<Block> GREENHOUSE_GLASS = TagKey.create(Registries.BLOCK, Cultivo.id("greenhouse_glass"));
    public static final TagKey<Item> DRIED_FLOWERS = TagKey.create(Registries.ITEM, Cultivo.id("dried_flowers"));

    private ModTags() {}
}
