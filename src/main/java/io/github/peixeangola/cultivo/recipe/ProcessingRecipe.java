package io.github.peixeangola.cultivo.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.registry.ModRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Receita fictícia de estação por ticks. JSON:
 * {@code {"type":"cultivo:drying","ingredient":"...","input_count":1,"result":{"id":"...","count":1},"processing_time":600}}
 */
public record ProcessingRecipe(StationKind kind, Ingredient ingredient, int inputCount, Item result, int resultCount,
                               int processingTime) implements Recipe<SingleRecipeInput> {

    private record Result(Item item, int count) {
        static final Codec<Result> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(Result::item),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Result::count)
        ).apply(i, Result::new));
    }

    public static MapCodec<ProcessingRecipe> codec(StationKind kind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ProcessingRecipe::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(ProcessingRecipe::inputCount),
            Result.CODEC.fieldOf("result").forGetter(r -> new Result(r.result(), r.resultCount())),
            Codec.intRange(1, 72_000).optionalFieldOf("processing_time", kind.defaultTicks())
                .forGetter(ProcessingRecipe::processingTime)
        ).apply(i, (ingredient, inputCount, result, time) ->
            new ProcessingRecipe(kind, ingredient, inputCount, result.item(), result.count(), time)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec(StationKind kind) {
        return StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ProcessingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, ProcessingRecipe::inputCount,
            ByteBufCodecs.registry(Registries.ITEM), ProcessingRecipe::result,
            ByteBufCodecs.VAR_INT, ProcessingRecipe::resultCount,
            ByteBufCodecs.VAR_INT, ProcessingRecipe::processingTime,
            (ingredient, inputCount, result, resultCount, time) ->
                new ProcessingRecipe(kind, ingredient, inputCount, result, resultCount, time));
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return new ItemStack(result, resultCount);
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<ProcessingRecipe> getSerializer() {
        return ModRecipes.SERIALIZER.get(kind).get();
    }

    @Override
    public RecipeType<ProcessingRecipe> getType() {
        return ModRecipes.TYPE.get(kind).get();
    }

    @Override
    public PlacementInfo placementInfo() {
        // Com NOT_PLACEABLE o RecipeManager descarta a receita dos índices (e avisa no log)
        return PlacementInfo.create(ingredient);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
