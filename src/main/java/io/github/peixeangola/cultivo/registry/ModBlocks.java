package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.CannabisCropBlock;
import io.github.peixeangola.cultivo.block.ProcessingStationBlock;
import io.github.peixeangola.cultivo.block.WildPlantBlock;
import io.github.peixeangola.cultivo.core.ContentIds;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.Variety;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
            STATIONS.put(kind, BLOCKS.register(name, () -> new ProcessingStationBlock(kind,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().setId(BLOCKS.key(name)))));
        }
    }

    public static final RegistryObject<WildPlantBlock> WILD_PLANT = BLOCKS.register(ContentIds.WILD_PLANT,
        () -> new WildPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)
            .mapColor(MapColor.PLANT)
            .setId(BLOCKS.key(ContentIds.WILD_PLANT))));

    private ModBlocks() {}
}
