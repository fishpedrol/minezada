package io.github.peixeangola.cultivo.block;

import com.mojang.logging.LogUtils;
import io.github.peixeangola.cultivo.core.ProcessingRules;
import io.github.peixeangola.cultivo.core.StationAnimation;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.core.StationVisualRules;
import io.github.peixeangola.cultivo.core.Variety;
import io.github.peixeangola.cultivo.recipe.ProcessingRecipe;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModItems;
import io.github.peixeangola.cultivo.registry.ModRecipes;
import io.github.peixeangola.cultivo.registry.ModSounds;
import io.github.peixeangola.cultivo.registry.ModTags;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

/**
 * Inventário comum às três estações: slot 0 entrada, slot 1 saída e progresso em ticks (salvo em NBT).
 * A lógica roda só no servidor. Com saída cheia ou de outra qualidade o progresso para, sem perda nem
 * duplicação; ao quebrar o bloco, os itens caem no chão. Os dois slots (sem o progresso) são sincronizados com o
 * cliente a cada mudança de conteúdo, para o bloco prever o clique sem esperar o servidor.
 *
 * <p>A propriedade contents do bloco ({@link StationVisualRules}) segue o que o ciclo faz de verdade: trabalhando só
 * enquanto a receita casa (com a quantidade mínima) e a saída aceita o resultado; parada, a estação mostra a saída
 * pronta ou, sem saída, a entrada sem movimento. É gravada a cada mudança de conteúdo e a cada tick (o que também acerta
 * mundos salvos antes dela) com setBlock(UPDATE_CLIENTS) só quando o valor muda, como o LIT da fornalha: o bloco é o
 * mesmo, então esta BlockEntity continua no lugar.
 *
 * <p>Enfeites do servidor, para todos por perto: enquanto o progresso anda, o som de trabalho de cada estação no ritmo
 * de {@link StationAnimation} (o aperto da prensa bate quando a placa animada chega embaixo); no fim do ciclo, um sopro
 * de partículas e o som de pronto.
 */
public class ProcessingStationBlockEntity extends BlockEntity {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;

    public ProcessingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSING_STATION.get(), pos, state);
    }

    public StationKind kind() {
        return ((ProcessingStationBlock) getBlockState().getBlock()).kind();
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /** Substitui um slot diretamente (usado pelos GameTests). */
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        contentsChanged();
    }

    /** Ajusta o progresso em ticks (usado pelos GameTests para não esperar o ciclo inteiro). */
    public void setProgress(int progress) {
        this.progress = progress;
        setChanged();
    }

    /** Marca para salvar e manda os slots ao cliente (o progresso não vai, para não gerar um pacote por tick). */
    private void contentsChanged() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            syncVisualState(canProgress(serverLevel));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Grava contents (vazia, carregada, trabalhando, pronta) no blockstate se mudou. Só a BlockEntity viva no mundo mexe
     * no bloco: uma cópia carregada à parte (ou já removida) nunca recoloca a estação.
     *
     * @param progressing o ciclo anda: receita para a entrada e saída que aceita o resultado
     */
    private void syncVisualState(boolean progressing) {
        BlockState current = level.getBlockState(worldPosition);
        if (level.getBlockEntity(worldPosition) != this || !current.hasProperty(ProcessingStationBlock.CONTENTS)) {
            return;
        }
        BlockState wanted = ProcessingStationBlock.withContents(current,
            StationVisualRules.contents(items.get(INPUT).isEmpty(), items.get(OUTPUT).isEmpty(), progressing));
        if (wanted != current) {
            level.setBlock(worldPosition, wanted, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * O ciclo anda com o conteúdo atual: há receita para a entrada (com a quantidade mínima, como no serverTick) e a
     * saída aceita o resultado inteiro. É a mesma conta do tick, para o blockstate nunca mostrar trabalho parado.
     */
    public boolean canProgress(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        return findRecipe(level, input).map(holder -> outputAccepts(resultFor(holder.value(), input))).orElse(false);
    }

    /** A saída recebe o resultado inteiro: vazia, ou o mesmo item com os mesmos componentes e espaço na pilha. */
    private boolean outputAccepts(ItemStack result) {
        ItemStack output = items.get(OUTPUT);
        return ProcessingRules.outputAccepts(output.isEmpty(), ItemStack.isSameItemSameComponents(output, result),
            output.getCount(), result.getCount(), result.getMaxStackSize());
    }

    /** Há saída pronta para a mão vazia tirar. Vale no cliente, com os slots sincronizados. */
    public boolean hasOutput() {
        return !items.get(OUTPUT).isEmpty();
    }

    /** Há entrada para devolver (agachado, de mãos vazias). Vale no cliente, com os slots sincronizados. */
    public boolean hasInput() {
        return !items.get(INPUT).isEmpty();
    }

    /**
     * Previsão do cliente para inserir: a pilha está na tag de entradas da estação e cabe na entrada (vazia, ou
     * mesmo item e componentes com espaço). O servidor decide de verdade pelas receitas em {@link #insert}.
     */
    public boolean couldInsert(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModTags.STATION_INPUTS.get(kind()))) {
            return false;
        }
        ItemStack input = items.get(INPUT);
        return input.isEmpty()
            || (ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < input.getMaxStackSize());
    }

    public int progress() {
        return progress;
    }

    private Optional<RecipeHolder<ProcessingRecipe>> findRecipe(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(ModRecipes.TYPE.get(kind()).get(), new SingleRecipeInput(input), level);
    }

    /** O item é ingrediente de alguma receita desta estação (ignorando a quantidade mínima). */
    public boolean accepts(ServerLevel level, ItemStack stack) {
        return findRecipe(level, stack.copyWithCount(stack.getMaxStackSize())).isPresent();
    }

    /**
     * Move o máximo possível da pilha para a entrada. Não altera {@code stack}; quem chama desconta a
     * quantidade retornada.
     */
    public int insert(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty() || !accepts(level, stack)) {
            return 0;
        }
        ItemStack input = items.get(INPUT);
        int moved;
        if (input.isEmpty()) {
            moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            items.set(INPUT, stack.copyWithCount(moved));
        } else if (ItemStack.isSameItemSameComponents(input, stack)) {
            moved = Math.min(stack.getCount(), input.getMaxStackSize() - input.getCount());
            input.grow(moved);
        } else {
            return 0;
        }
        if (moved > 0) {
            contentsChanged();
        }
        return moved;
    }

    /**
     * Retira a saída inteira (vazia se não havia). É o clique de mão vazia: nunca toca na entrada, então segurar o botão,
     * ou o clique de outro jogador logo depois, não puxa o que ainda está processando nem zera o progresso.
     */
    public ItemStack takeOutput() {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(OUTPUT, ItemStack.EMPTY);
        contentsChanged();
        return output;
    }

    /**
     * Devolve a entrada inteira e zera o progresso (vazia se não havia). Só o gesto de propósito chega aqui: agachado e
     * de mãos vazias.
     */
    public ItemStack takeInput() {
        ItemStack input = items.get(INPUT);
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        items.set(INPUT, ItemStack.EMPTY);
        progress = 0;
        contentsChanged();
        return input;
    }

    /**
     * Resultado que o ciclo atual produziria, com a qualidade da entrada copiada. Na peneira e na prensa a
     * variedade também passa adiante (cultivo:strain), para a resina e o haxixe lembrarem de qual flor vieram. A
     * peneira com dichavador (blockstate grinder=true) rende +1 resina.
     */
    ItemStack resultFor(ProcessingRecipe recipe, ItemStack input) {
        ItemStack result = recipe.assemble(new SingleRecipeInput(input));
        Integer quality = input.get(ModComponents.QUALITY.get());
        if (quality != null) {
            result.set(ModComponents.QUALITY.get(), quality);
        }
        if (kind() != StationKind.DRYING) {
            Variety strain = input.get(ModComponents.STRAIN.get());
            if (strain == null) {
                strain = ModItems.varietyOf(input);
            }
            if (strain != null) {
                result.set(ModComponents.STRAIN.get(), strain);
            }
        }
        result.setCount(ProcessingRules.outputCount(kind(), result.getCount(), quality,
            SieveBlock.hasGrinder(getBlockState())));
        return result;
    }

    public void serverTick(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        Optional<RecipeHolder<ProcessingRecipe>> holder = findRecipe(level, input);
        int previous = progress;
        boolean progressing = false;
        if (holder.isEmpty()) {
            progress = 0;
        } else {
            ProcessingRecipe recipe = holder.get().value();
            ItemStack result = resultFor(recipe, input);
            boolean accepts = outputAccepts(result);
            ProcessingRules.Tick tick = ProcessingRules.advance(progress, recipe.processingTime(), true, accepts);
            progress = tick.progress();
            if (tick.completed()) {
                input.shrink(recipe.inputCount());
                ItemStack output = items.get(OUTPUT);
                if (output.isEmpty()) {
                    items.set(OUTPUT, result);
                } else {
                    output.grow(result.getCount());
                }
                // Recalcula o estado com o que sobrou: 1 resina na prensa não basta, e a barra de haxixe aparece
                contentsChanged();
                finishEffects(level);
                return;
            }
            progressing = accepts;
        }
        if (progress != previous) {
            setChanged();
            if (progress > previous) {
                workSounds(level);
            }
        }
        syncVisualState(progressing);
    }

    /** Som de trabalho no ritmo da estação; a peneira com dichavador também mói. */
    private void workSounds(ServerLevel level) {
        if (!StationAnimation.playsWorkSound(kind(), level.getGameTime(), worldPosition.asLong())) {
            return;
        }
        SoundEvent sound = switch (kind()) {
            case DRYING -> ModSounds.DRYING_RACK_RUSTLE.get();
            case SIEVING -> ModSounds.SIEVE_SIFT.get();
            case PRESSING -> ModSounds.PRESS_SQUEEZE.get();
        };
        float pitch = 0.9F + level.getRandom().nextFloat() * 0.2F;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.0F, pitch);
        if (SieveBlock.hasGrinder(getBlockState())) {
            level.playSound(null, worldPosition, ModSounds.GRINDER_GRIND.get(), SoundSource.BLOCKS, 0.6F, pitch);
        }
    }

    /** Fim de ciclo: um sopro de partículas onde está o produto (todos veem) e o som de pronto. */
    private void finishEffects(ServerLevel level) {
        double height = switch (kind()) {
            case DRYING -> 0.6D;
            case SIEVING -> 0.75D;
            case PRESSING -> 0.35D;
        };
        level.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5D, worldPosition.getY() + height,
            worldPosition.getZ() + 0.5D, 5, 0.18D, 0.05D, 0.18D, 0.01D);
        level.playSound(null, worldPosition, ModSounds.STATION_FINISH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Só os slots vão ao cliente, como na fogueira vanilla. */
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
            items.clear();
        }
    }
}
