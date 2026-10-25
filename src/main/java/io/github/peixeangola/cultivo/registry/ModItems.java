package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.SmokeDevice;
import io.github.peixeangola.cultivo.core.SmokeRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.item.ArticleBlockItem;
import io.github.peixeangola.cultivo.item.FlowerItem;
import io.github.peixeangola.cultivo.item.GrowTentItem;
import io.github.peixeangola.cultivo.item.LighterItem;
import io.github.peixeangola.cultivo.item.QualityItem;
import io.github.peixeangola.cultivo.item.SeedItem;
import io.github.peixeangola.cultivo.item.SmokingItem;
import io.github.peixeangola.cultivo.item.StationBlockItem;
import io.github.peixeangola.cultivo.item.TooltipItem;
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
            FRESH_FLOWERS.put(variety, flower(variety, variety.freshFlowerId()));
            DRIED_FLOWERS.put(variety, flower(variety, variety.driedFlowerId()));
        }
    }

    public static final RegistryObject<Item> RESIN = quality(ContentIds.RESIN);
    public static final RegistryObject<Item> HASH = quality(ContentIds.HASH);
    /** Baseado: fumável, consome a si mesmo. Empilha 16 (pilhas só juntam com a mesma variedade e qualidade). */
    public static final RegistryObject<Item> JOINT = ITEMS.register(ContentIds.JOINT,
        () -> new SmokingItem(SmokeDevice.JOINT, new Item.Properties().setId(ITEMS.key(ContentIds.JOINT)).stacksTo(16)));

    static {
        for (String article : ContentIds.ARTICLES) {
            ARTICLES.put(article, ITEMS.register(article, () -> article(article)));
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

    /** Estufinha: coloca as duas metades e tem o ícone plano; empilha 64, como as portas. */
    public static final RegistryObject<Item> GROW_TENT = ITEMS.register(ContentIds.GROW_TENT,
        () -> new GrowTentItem(ModBlocks.GROW_TENT.get(), new Item.Properties().setId(ITEMS.key(ContentIds.GROW_TENT))));

    private ModItems() {}

    /** Isqueiro: durabilidade própria (e por isso empilha 1). */
    public static final RegistryObject<Item> LIGHTER = ARTICLES.get(ContentIds.LIGHTER);
    /** Folha de seda: ingrediente do baseado na bancada e na bandeja. */
    public static final RegistryObject<Item> ROLLING_PAPER = ARTICLES.get(ContentIds.ROLLING_PAPER);
    /** Piteira: ingrediente do baseado com piteira. */
    public static final RegistryObject<Item> FILTER_TIP = ARTICLES.get(ContentIds.FILTER_TIP);
    /** Dichavador: instala na peneira. */
    public static final RegistryObject<Item> GRINDER = ARTICLES.get(ContentIds.GRINDER);

    /**
     * Cada artigo tem um papel ({@link ContentIds#ARTICLES}): cachimbo, chillum, bong e narguilé fumam; o isqueiro tem
     * durabilidade; folha de seda, piteira e dichavador têm uma dica com o uso; bandeja, pote e cinzeiro colocam o
     * bloco (mantendo o ícone plano) e também mostram a dica. O bong fuma e, agachado, vai para o chão.
     */
    private static Item article(String name) {
        Item.Properties properties = new Item.Properties().setId(ITEMS.key(name));
        if (name.equals(ContentIds.LIGHTER)) {
            return new LighterItem(properties.durability(SmokeRules.LIGHTER_DURABILITY));
        }
        properties.stacksTo(STACKABLE_ARTICLES.contains(name) ? 64 : 1);
        String tooltip = "tooltip.cultivo." + name;
        if (ContentIds.PLACEABLE_ARTICLES.contains(name)) {
            return new ArticleBlockItem(ModBlocks.PLACEABLE_ARTICLES.get(name).get(), properties, tooltip);
        }
        if (ContentIds.INGREDIENT_ARTICLES.contains(name) || ContentIds.UPGRADE_ARTICLES.contains(name)) {
            return new TooltipItem(properties, tooltip);
        }
        return SmokeDevice.byItemId(name)
            .<Item>map(device -> ContentIds.PLACED_DEVICES.contains(name)
                ? new SmokingItem(device, properties, ModBlocks.PLACED_DEVICES.get(name))
                : new SmokingItem(device, properties))
            .orElseThrow(() -> new IllegalStateException("artigo sem papel: " + name));
    }

    /** Variedade de uma semente, flor fresca ou flor seca pelo item; null para outros itens. */
    public static Variety varietyOf(net.minecraft.world.item.ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SeedItem seed) {
            return seed.variety();
        }
        if (item instanceof FlowerItem flower) {
            return flower.variety();
        }
        return null;
    }

    private static RegistryObject<Item> quality(String name) {
        return ITEMS.register(name, () -> new QualityItem(new Item.Properties().setId(ITEMS.key(name))));
    }

    /** Flor fresca ou seca: já sabe a variedade (perfil no tooltip) e carrega cultivo:quality como os outros. */
    private static RegistryObject<Item> flower(Variety variety, String name) {
        return ITEMS.register(name, () -> new FlowerItem(variety, new Item.Properties().setId(ITEMS.key(name))));
    }
}
