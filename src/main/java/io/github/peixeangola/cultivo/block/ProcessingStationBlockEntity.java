package io.github.peixeangola.cultivo.block;

import io.github.peixeangola.cultivo.core.ProcessingRules;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.recipe.ProcessingRecipe;
import io.github.peixeangola.cultivo.registry.ModBlockEntities;
import io.github.peixeangola.cultivo.registry.ModComponents;
import io.github.peixeangola.cultivo.registry.ModRecipes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Inventário comum às três estações: slot 0 entrada, slot 1 saída e progresso em ticks (salvo em NBT).
 * A lógica roda só no servidor. Com saída cheia ou de outra qualidade o progresso para, sem perda nem
 * duplicação; ao quebrar o bloco, os itens caem no chão.
 */
public class ProcessingStationBlockEntity extends BlockEntity {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

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
        setChanged();
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
            setChanged();
        }
        return moved;
    }

    /** Retira a saída inteira; se não houver, retira a entrada e zera o progresso. */
    public ItemStack extract() {
        ItemStack output = items.get(OUTPUT);
        if (!output.isEmpty()) {
            items.set(OUTPUT, ItemStack.EMPTY);
            setChanged();
            return output;
        }
        ItemStack input = items.get(INPUT);
        if (!input.isEmpty()) {
            items.set(INPUT, ItemStack.EMPTY);
            progress = 0;
            setChanged();
        }
        return input;
    }

    /** Resultado que o ciclo atual produziria, com a qualidade da entrada copiada. */
    ItemStack resultFor(ProcessingRecipe recipe, ItemStack input) {
        ItemStack result = recipe.assemble(new SingleRecipeInput(input));
        Integer quality = input.get(ModComponents.QUALITY.get());
        if (quality != null) {
            result.set(ModComponents.QUALITY.get(), quality);
        }
        result.setCount(ProcessingRules.outputCount(kind(), result.getCount(), quality));
        return result;
    }

    public void serverTick(ServerLevel level) {
        ItemStack input = items.get(INPUT);
        Optional<RecipeHolder<ProcessingRecipe>> holder = findRecipe(level, input);
        int previous = progress;
        if (holder.isEmpty()) {
            progress = 0;
        } else {
            ProcessingRecipe recipe = holder.get().value();
            ItemStack result = resultFor(recipe, input);
            ItemStack output = items.get(OUTPUT);
            boolean accepts = ProcessingRules.outputAccepts(output.isEmpty(),
                ItemStack.isSameItemSameComponents(output, result), output.getCount(), result.getCount(),
                result.getMaxStackSize());
            ProcessingRules.Tick tick = ProcessingRules.advance(progress, recipe.processingTime(), true, accepts);
            progress = tick.progress();
            if (tick.completed()) {
                input.shrink(recipe.inputCount());
                if (output.isEmpty()) {
                    items.set(OUTPUT, result);
                } else {
                    output.grow(result.getCount());
                }
                setChanged();
                return;
            }
        }
        if (progress != previous) {
            setChanged();
        }
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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, items);
            items.clear();
        }
    }
}
