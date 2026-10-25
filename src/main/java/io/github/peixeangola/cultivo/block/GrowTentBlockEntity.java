package io.github.peixeangola.cultivo.block;

import com.mojang.logging.LogUtils;
import io.github.peixeangola.cultivo.core.GrowTentRules;
import io.github.peixeangola.cultivo.core.GrowthRules;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModBlocks;
import java.util.List;
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
import net.minecraft.world.phys.AABB;
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
 *   <li>Quebrar a estufinha (qualquer metade, por jogador, explosão ou comando) derruba a planta uma vez, aqui em
 *       {@link #preRemoveSideEffects}, como o pote derruba o conteúdo. Também no criativo, como os baús.</li>
 *   <li>Salva a variedade pelo id e a idade; um id desconhecido carrega como estufinha vazia.</li>
 * </ul>
 */
public class GrowTentBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private @Nullable Variety variety;
    private int age;

    public GrowTentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROW_TENT.get(), pos, state);
    }

    public boolean hasPlant() {
        return variety != null;
    }

    public @Nullable Variety variety() {
        return variety;
    }

    public int age() {
        return age;
    }

    /** Planta uma semente da variedade, na idade 0. */
    public void plant(Variety variety) {
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

    /** Esvazia a estufinha (depois da colheita). */
    public void clear() {
        variety = null;
        age = 0;
        changed();
    }

    /** O que a planta derruba agora: o loot da lavoura da variedade nesta idade, com esta BlockEntity no contexto. */
    public List<ItemStack> rollPlantDrops(ServerLevel level, @Nullable Entity breaker, ItemStack tool) {
        if (variety == null) {
            return List.of();
        }
        BlockState plant = ModBlocks.CROPS.get(variety).get().getStateForAge(age);
        return Block.getDrops(plant, level, worldPosition, this, breaker, tool);
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
        if (variety != null) {
            output.putString("variety", variety.id());
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
        variety = input.getString("variety").flatMap(Variety::byId).orElse(null);
        age = variety == null ? 0 : Mth.clamp(input.getIntOr("age", 0), 0, GrowthRules.MAX_AGE);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** O cliente recebe a variedade e a idade (sem planta, nada: a estufinha aparece vazia). */
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
        if (level instanceof ServerLevel serverLevel && variety != null) {
            rollPlantDrops(serverLevel, null, ItemStack.EMPTY).forEach(stack -> Block.popResource(serverLevel, pos, stack));
            variety = null;
            age = 0;
        }
    }
}
