package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.AshtrayBlock;
import io.github.peixeangola.cultivo.block.BongBlock;
import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import io.github.peixeangola.cultivo.block.GrowTentBlock;
import io.github.peixeangola.cultivo.block.JarBlock;
import io.github.peixeangola.cultivo.block.ProcessingStationBlock;
import io.github.peixeangola.cultivo.block.RollingTrayBlock;
import io.github.peixeangola.cultivo.block.SieveBlock;
import io.github.peixeangola.cultivo.block.WildPlantBlock;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.GrowTentRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Cultivo.MOD_ID);

    public static final Map<Variety, RegistryObject<CannabisCropBlock>> CROPS = new EnumMap<>(Variety.class);
    public static final Map<StationKind, RegistryObject<ProcessingStationBlock>> STATIONS = new EnumMap<>(StationKind.class);

    static {
        for (Variety variety : Variety.values()) {
            String name = variety.cropId();
            // Mesmas propriedades do trigo: sem colisão, random ticks, quebra instantânea, som de lavoura
            CROPS.put(variety, BLOCKS.register(name, () -> new CannabisCropBlock(variety,
                BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).setId(BLOCKS.key(name)))));
        }
        for (StationKind kind : StationKind.values()) {
            String name = kind.blockId();
            // A peneira é uma subclasse: tem a propriedade do dichavador
            STATIONS.put(kind, BLOCKS.register(name, () -> {
                BlockBehaviour.Properties properties =
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().setId(BLOCKS.key(name));
                return kind == StationKind.SIEVING ? new SieveBlock(properties) : new ProcessingStationBlock(kind, properties);
            }));
        }
    }

    public static final RegistryObject<WildPlantBlock> WILD_PLANT = BLOCKS.register(ContentIds.WILD_PLANT,
        () -> new WildPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)
            .mapColor(MapColor.PLANT)
            .setId(BLOCKS.key(ContentIds.WILD_PLANT))));

    // Artigos colocáveis: pequenos, sem oclusão e estourados por pistão (como o vaso de flor e o pote decorado)

    /** Bandeja de enrolar: metal, gira com o facing. */
    public static final RegistryObject<RollingTrayBlock> ROLLING_TRAY = BLOCKS.register(ContentIds.ROLLING_TRAY,
        () -> new RollingTrayBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(0.5F).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.POPPED)
            .setId(BLOCKS.key(ContentIds.ROLLING_TRAY))));

    /** Pote de vidro: guarda uma pilha e cura flores secas (BlockEntity própria). */
    public static final RegistryObject<JarBlock> JAR = BLOCKS.register(ContentIds.JAR,
        () -> new JarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED)
            .setId(BLOCKS.key(ContentIds.JAR))));

    /** Cinzeiro de terracota: junta cinza das sessões de fumo por perto. */
    public static final RegistryObject<AshtrayBlock> ASHTRAY = BLOCKS.register(ContentIds.ASHTRAY,
        () -> new AshtrayBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.5F).sound(SoundType.DECORATED_POT).noOcclusion()
            .pushReaction(PushReaction.POPPED).setId(BLOCKS.key(ContentIds.ASHTRAY))));

    /**
     * Estufinha: tecido, 2 de altura, luz roxa na metade de cima. Pistões não a movem nem a estouram: fica de pé, como a
     * mesa de encantamento, em vez de virar item com a planta junto a cada máquina de redstone por perto.
     */
    public static final RegistryObject<GrowTentBlock> GROW_TENT = BLOCKS.register(ContentIds.GROW_TENT,
        () -> new GrowTentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE).strength(0.8F).sound(SoundType.WOOL).noOcclusion()
            .pushReaction(PushReaction.IMMOVEABLE)
            .lightLevel(state -> state.getValue(GrowTentBlock.HALF) == DoubleBlockHalf.UPPER ? GrowTentRules.LAMP_LIGHT : 0)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .setId(BLOCKS.key(ContentIds.GROW_TENT))));

    /**
     * Bong no chão (pedido do dono de 25/10/2026): vidro, posto agachado com o item, que continua fumando; estourado por
     * pistão como os artigos colocáveis.
     */
    public static final RegistryObject<BongBlock> BONG = BLOCKS.register(ContentIds.BONG,
        () -> new BongBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).strength(0.3F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED)
            .setId(BLOCKS.key(ContentIds.BONG))));

    /** Bloco de cada aparelho que vai para o chão, pelo ID (o mesmo do item). */
    public static final Map<String, RegistryObject<? extends Block>> PLACED_DEVICES = Map.of(ContentIds.BONG, BONG);

    /** Bloco de cada artigo colocável, pelo ID (o mesmo do item). */
    public static final Map<String, RegistryObject<? extends Block>> PLACEABLE_ARTICLES = Map.of(
        ContentIds.ROLLING_TRAY, ROLLING_TRAY,
        ContentIds.JAR, JAR,
        ContentIds.ASHTRAY, ASHTRAY);

    private ModBlocks() {}
}
