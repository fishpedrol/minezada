package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.block.ProcessingStationBlockEntity;
import io.github.peixeangola.cultivo.core.StationKind;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Cultivo.MOD_ID);

    /** Uma única BlockEntity serve as três estações; o tipo de receita vem do bloco. */
    public static final RegistryObject<BlockEntityType<ProcessingStationBlockEntity>> PROCESSING_STATION =
        BLOCK_ENTITIES.register("processing_station", () -> new BlockEntityType<>(ProcessingStationBlockEntity::new,
            Arrays.stream(StationKind.values())
                .map(kind -> (Block) ModBlocks.STATIONS.get(kind).get())
                .collect(Collectors.toUnmodifiableSet())));

    private ModBlockEntities() {}
}
