package io.github.peixeangola.cultivo.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.QualityRules;
import io.github.peixeangola.cultivo.core.Variety;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Unit;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Cultivo.MOD_ID);

    /** cultivo:quality — inteiro de 1 a 5, persistente e sincronizado (tooltip no cliente). */
    public static final RegistryObject<DataComponentType<Integer>> QUALITY = COMPONENTS.register("quality",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(QualityRules.MIN, QualityRules.MAX))
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build());

    /**
     * cultivo:strain — variedade de itens que não são específicos de variedade (baseado, resina, haxixe). Gravado
     * pela receita transmute do baseado e pela peneira/prensa; no JSON é o ID da variedade ("brisa", "manga_gelada",
     * ...), um dos IDs da tabela {@link Variety}.
     */
    public static final RegistryObject<DataComponentType<Variety>> STRAIN = COMPONENTS.register("strain",
        () -> DataComponentType.<Variety>builder()
            .persistent(Codec.STRING.comapFlatMap(
                id -> Variety.byId(id).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "variedade desconhecida: " + id)),
                Variety::id))
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(
                id -> Variety.byId(id).orElseThrow(), Variety::id))
            .build());

    /**
     * cultivo:filtered — marca do baseado com piteira (receita transmute joint_filtered). Sem valor: no JSON é
     * {@code {}}. Persistente e sincronizado (o modelo do item e o tooltip mudam no cliente).
     */
    public static final RegistryObject<DataComponentType<Unit>> FILTERED = COMPONENTS.register("filtered",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

    /**
     * cultivo:cured — marca da flor seca que já passou pela cura no pote (+1 de qualidade uma vez só). Sem valor;
     * persistente e sincronizado. Como muda os componentes, uma pilha curada não junta com flores não curadas.
     */
    public static final RegistryObject<DataComponentType<Unit>> CURED = COMPONENTS.register("cured",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

    private ModComponents() {}
}
