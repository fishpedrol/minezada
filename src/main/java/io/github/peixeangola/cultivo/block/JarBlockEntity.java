package io.github.peixeangola.cultivo.block;

import com.mojang.logging.LogUtils;
import io.github.peixeangola.cultivo.core.CuringRules;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

/**
 * Conteúdo do pote: uma pilha e o contador da cura, salvos em NBT como os slots das estações. Tudo roda no servidor.
 * <ul>
 *   <li>Entra só o que está na tag cultivo:jar_storable, no mesmo item com os mesmos componentes.</li>
 *   <li>Cura ({@link CuringRules}): uma pilha de flores secas sem cultivo:cured que fica
 *       {@link CuringRules#CURE_TICKS} ticks sem mudar ganha +1 de qualidade (até 5) e cultivo:cured, uma vez só. O
 *       contador zera a cada mudança no conteúdo; como os componentes mudam, a pilha curada não junta com flores não
 *       curadas.</li>
 *   <li>A pilha vai para o cliente a cada mudança (para o bloco prever o clique); o contador não.</li>
 *   <li>A propriedade filled do bloco acompanha a pilha, gravada com setBlock(UPDATE_CLIENTS) só quando muda; o bloco é
 *       o mesmo, então esta BlockEntity continua no lugar.</li>
 * </ul>
 */
public class JarBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int cureProgress;

    public JarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JAR.get(), pos, state);
    }

    public ItemStack getItem() {
        return items.get(0);
    }

    /** Substitui a pilha diretamente (usado pelos GameTests). Conta como mudança de conteúdo. */
    public void setItem(ItemStack stack) {
        items.set(0, stack);
        contentsChanged();
    }

    public int cureProgress() {
        return cureProgress;
    }

    /** Ajusta o contador da cura (usado pelos GameTests para não esperar 2400 ticks). */
    public void setCureProgress(int progress) {
        this.cureProgress = progress;
        setChanged();
    }

    /** A pilha está na tag do pote (flores secas, resina, haxixe, baseado). */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.JAR_STORABLE);
    }

    /** Cabe pelo menos 1: pote vazio, ou mesmo item e componentes com espaço. Vale no cliente (pilha sincronizada). */
    public boolean couldInsert(ItemStack stack) {
        if (!accepts(stack)) {
            return false;
        }
        ItemStack stored = getItem();
        return stored.isEmpty()
            || (ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() < stored.getMaxStackSize());
    }

    /** Põe o quanto couber da pilha. Não altera {@code stack}; quem chama desconta a quantidade retornada. */
    public int insert(ItemStack stack) {
        if (!couldInsert(stack)) {
            return 0;
        }
        ItemStack stored = getItem();
        int moved;
        if (stored.isEmpty()) {
            moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            items.set(0, stack.copyWithCount(moved));
        } else {
            moved = Math.min(stack.getCount(), stored.getMaxStackSize() - stored.getCount());
            stored.grow(moved);
        }
        contentsChanged();
        return moved;
    }

    /** Retira a pilha inteira (vazia se não havia nada). */
    public ItemStack extract() {
        ItemStack stored = getItem();
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(0, ItemStack.EMPTY);
        contentsChanged();
        return stored;
    }

    /** A pilha atual pode ser curada: flor seca ainda sem cultivo:cured. */
    public boolean curable() {
        ItemStack stored = getItem();
        return CuringRules.canCure(stored.is(ModTags.DRIED_FLOWERS), stored.has(ModComponents.CURED.get()));
    }

    public int comparatorSignal() {
        ItemStack stored = getItem();
        return CuringRules.comparatorSignal(stored.getCount(), stored.getMaxStackSize());
    }

    /**
     * Conteúdo mudou: zera a cura, salva, avisa o comparador (BlockEntity#setChanged), atualiza filled e manda a pilha
     * ao cliente.
     */
    private void contentsChanged() {
        cureProgress = 0;
        setChanged();
        if (level != null && !level.isClientSide()) {
            syncVisualState();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Grava filled no blockstate se mudou. Só a BlockEntity viva no mundo mexe no bloco. */
    private void syncVisualState() {
        BlockState current = level.getBlockState(worldPosition);
        if (level.getBlockEntity(worldPosition) != this || !current.hasProperty(JarBlock.FILLED)) {
            return;
        }
        boolean filled = !getItem().isEmpty();
        if (current.getValue(JarBlock.FILLED) != filled) {
            level.setBlock(worldPosition, current.setValue(JarBlock.FILLED, filled), Block.UPDATE_CLIENTS);
        }
    }

    public void serverTick(ServerLevel level) {
        CuringRules.Tick tick = CuringRules.advance(cureProgress, curable());
        if (tick.completed()) {
            ItemStack stored = getItem();
            stored.set(ModComponents.QUALITY.get(), CuringRules.curedQuality(stored.get(ModComponents.QUALITY.get())));
            stored.set(ModComponents.CURED.get(), Unit.INSTANCE);
            contentsChanged();
            return;
        }
        if (tick.progress() != cureProgress) {
            cureProgress = tick.progress();
            // Só o contador mudou: marca para salvar sem acordar comparadores a cada tick
            level.blockEntityChanged(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("cure_progress", cureProgress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.set(0, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        cureProgress = input.getIntOr("cure_progress", 0);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Só a pilha vai ao cliente, como nas estações. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            ContainerHelper.saveAllItems(output, items, true);
            return output.buildResult();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, items);
            items.set(0, ItemStack.EMPTY);
        }
    }
}
