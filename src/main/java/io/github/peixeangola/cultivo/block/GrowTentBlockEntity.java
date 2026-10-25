package io.github.peixeangola.cultivo.block;

import com.mojang.logging.LogUtils;
import io.github.peixeangola.cultivo.core.GrowTentRules;
import io.github.peixeangola.cultivo.core.GrowthRules;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * O pé de planta da estufinha, só na metade de baixo: a variedade (ou nenhuma, estufinha vazia) e a idade de 0 a 7.
 * <ul>
 *   <li>Tudo muda no servidor ({@link GrowTentBlock}: plantar, random tick, farinha de osso, colher). A cada mudança os
 *       dois campos vão ao cliente, que desenha a planta e prevê o clique e a farinha de osso com eles.</li>
 *   <li>Os drops da planta são os da própria lavoura ({@link #rollPlantDrops}): a tabela cultivo:blocks/&lt;v&gt;_crop
 *       rolada com esta BlockEntity no contexto, que é o que faz a função de loot dar qualidade 4–5. Madura: 1 flor
 *       fresca e sementes; antes disso: 1 semente.</li>
 *   <li>Quebrar a estufinha (qualquer metade, por jogador ou comando) derruba a planta uma vez, aqui em
 *       {@link #preRemoveSideEffects}, como o pote derruba o conteúdo. Na explosão, a planta sai pelo
 *       {@link GrowTentBlock} (com o decaimento da explosão); no criativo, não sai, como a lavoura quebrada no criativo.</li>
 *   <li>Salva a variedade pelo id e a idade. Um id desconhecido (genética renomeada, mod mais antigo) carrega como
 *       estufinha <b>inerte</b>: ocupada, sem crescer, sem colher, sem replantar, sem desenho e sem drop, e o id volta
 *       igual ao salvar, para não apagar a planta de ninguém.</li>
 * </ul>
 */
public class GrowTentBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private @Nullable Variety variety;
    /** O id salvo quando não corresponde a nenhuma variedade (estufinha inerte); null no resto. */
    private @Nullable String rawVariety;
    private int age;

    public GrowTentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROW_TENT.get(), pos, state);
    }

    public boolean hasPlant() {
        return variety != null;
    }

    /** Tem planta ou um id desconhecido: não aceita outra semente. */
    public boolean occupied() {
        return variety != null || rawVariety != null;
    }

    public @Nullable Variety variety() {
        return variety;
    }

    public int age() {
        return age;
    }

    /** Planta uma semente da variedade, na idade 0 (só na estufinha desocupada). */
    public void plant(Variety variety) {
        if (occupied()) {
            return;
        }
        this.variety = variety;
        this.age = 0;
        changed();
    }

    /** Ajusta a idade (0 a 7; o random tick, a farinha de osso e os GameTests). Sem planta, não faz nada. */
    public void setAge(int age) {
        if (variety == null) {
            return;
        }
        this.age = Mth.clamp(age, 0, GrowthRules.MAX_AGE);
        changed();
    }

    /** Esvazia a estufinha (depois da colheita; também tira um id desconhecido). */
    public void clear() {
        forget();
        changed();
    }

    /** Esquece a planta sem salvar nem sincronizar: o bloco está saindo (quebra, explosão ou criativo), como o pote. */
    void forget() {
        variety = null;
        rawVariety = null;
        age = 0;
    }

    /**
     * O que a planta derruba agora: o loot da lavoura da variedade nesta idade, com esta BlockEntity no contexto (os
     * mesmos parâmetros de Block#getDrops; com {@code explosionRadius}, o decaimento da explosão da tabela vale).
     */
    public List<ItemStack> rollPlantDrops(ServerLevel level, @Nullable Entity breaker, ItemStack tool,
                                          @Nullable Float explosionRadius) {
        if (variety == null) {
            return List.of();
        }
        BlockState plant = ModBlocks.CROPS.get(variety).get().getStateForAge(age);
        LootParams.Builder params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
            .withParameter(LootContextParams.TOOL, tool)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this)
            .withOptionalParameter(LootContextParams.EXPLOSION_RADIUS, explosionRadius);
        return plant.getDrops(params);
    }

    /** Estado da lavoura que o cliente desenha dentro da estufinha (null se vazia). */
    public @Nullable BlockState plantState() {
        return variety == null ? null : ModBlocks.CROPS.get(variety).get().getStateForAge(age);
    }

    public boolean canGrow() {
        return GrowTentRules.canGrow(hasPlant(), age);
    }

    public boolean isRipe() {
        return GrowTentRules.isRipe(hasPlant(), age);
    }

    /** Salva e manda os dois campos ao cliente (só no servidor). */
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void writePlant(ValueOutput output) {
        String id = variety != null ? variety.id() : rawVariety;
        if (id != null) {
            output.putString("variety", id);
            output.putInt("age", age);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writePlant(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Sempre os três campos: a tag do cliente sem planta também apaga a planta que ele via
        Optional<String> id = input.getString("variety");
        variety = id.flatMap(Variety::byId).orElse(null);
        rawVariety = variety == null ? id.orElse(null) : null;
        if (rawVariety != null && (level == null || !level.isClientSide())) {
            LOGGER.warn("Estufinha em {} com variedade desconhecida '{}': fica inerte até ser quebrada", worldPosition, rawVariety);
        }
        age = occupied() ? Mth.clamp(input.getIntOr("age", 0), 0, GrowthRules.MAX_AGE) : 0;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** O cliente recebe a variedade e a idade (sem planta, nada: a estufinha aparece vazia; inerte, o id desconhecido). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            writePlant(output);
            return output.buildResult();
        }
    }

    /**
     * A planta madura passa 3 px para dentro da metade de cima: a caixa de desenho cobre as duas metades (a padrão do
     * Forge é a colisão da metade de baixo, e a planta sumiria olhando de baixo para cima).
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
            worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) {
            rollPlantDrops(serverLevel, null, ItemStack.EMPTY, null).forEach(stack -> Block.popResource(serverLevel, pos, stack));
            forget();
        }
    }
}
