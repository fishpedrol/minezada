package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.item.DecorativeItem;
import io.github.peixeangola.cultivo.item.QualityItem;
import io.github.peixeangola.cultivo.item.SeedItem;
import io.github.peixeangola.cultivo.item.StationBlockItem;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Cultivo.MOD_ID);

    /** Artigos que fazem sentido empilhar; os demais empilham 1. */
    private static final List<String> STACKABLE_ARTICLES = List.of("rolling_paper", "filter_tip");

    public static final Map<Variety, RegistryObject<Item>> SEEDS = new EnumMap<>(Variety.class);
    public static final Map<Variety, RegistryObject<Item>> FRESH_FLOWERS = new EnumMap<>(Variety.class);
    public static final Map<Variety, RegistryObject<Item>> DRIED_FLOWERS = new EnumMap<>(Variety.class);
    public static final Map<StationKind, RegistryObject<Item>> STATIONS = new EnumMap<>(StationKind.class);
    public static final Map<String, RegistryObject<Item>> ARTICLES = new LinkedHashMap<>();

    static {
        for (Variety variety : Variety.values()) {
            String seeds = variety.seedsId();
            // useItemDescriptionPrefix: o item-semente é o BlockItem do crop, mas usa item.cultivo.<v>_seeds
            SEEDS.put(variety, ITEMS.register(seeds, () -> new SeedItem(variety, ModBlocks.CROPS.get(variety).get(),
                new Item.Properties().setId(ITEMS.key(seeds)).useItemDescriptionPrefix())));
            FRESH_FLOWERS.put(variety, quality(variety.freshFlowerId()));
            DRIED_FLOWERS.put(variety, quality(variety.driedFlowerId()));
        }
    }

    public static final RegistryObject<Item> RESIN = quality(ContentIds.RESIN);
    public static final RegistryObject<Item> HASH = quality(ContentIds.HASH);
    public static final RegistryObject<Item> JOINT = ITEMS.register(ContentIds.JOINT,
        () -> new DecorativeItem(new Item.Properties().setId(ITEMS.key(ContentIds.JOINT))));

    static {
        for (String article : ContentIds.ARTICLES) {
            ARTICLES.put(article, ITEMS.register(article, () -> new DecorativeItem(new Item.Properties()
                .setId(ITEMS.key(article))
                .stacksTo(STACKABLE_ARTICLES.contains(article) ? 64 : 1))));
        }
        for (StationKind kind : StationKind.values()) {
            String name = kind.blockId();
            STATIONS.put(kind, ITEMS.register(name, () -> new StationBlockItem(kind, ModBlocks.STATIONS.get(kind).get(),
                new Item.Properties().setId(ITEMS.key(name)).useBlockDescriptionPrefix())));
        }
    }

    public static final RegistryObject<Item> WILD_PLANT = ITEMS.register(ContentIds.WILD_PLANT,
        () -> new BlockItem(ModBlocks.WILD_PLANT.get(),
            new Item.Properties().setId(ITEMS.key(ContentIds.WILD_PLANT)).useBlockDescriptionPrefix()));

    private ModItems() {}

    private static RegistryObject<Item> quality(String name) {
        return ITEMS.register(name, () -> new QualityItem(new Item.Properties().setId(ITEMS.key(name))));
    }
}
