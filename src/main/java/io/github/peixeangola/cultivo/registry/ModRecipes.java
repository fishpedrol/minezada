package io.github.peixeangola.cultivo.registry;

import io.github.peixeangola.cultivo.Cultivo;
import io.github.peixeangola.cultivo.core.StationKind;
import io.github.peixeangola.cultivo.recipe.ProcessingRecipe;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Tipos e serializers cultivo:drying, cultivo:sieving e cultivo:pressing. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Cultivo.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Cultivo.MOD_ID);

    public static final Map<StationKind, RegistryObject<RecipeType<ProcessingRecipe>>> TYPE = new EnumMap<>(StationKind.class);
    public static final Map<StationKind, RegistryObject<RecipeSerializer<ProcessingRecipe>>> SERIALIZER =
        new EnumMap<>(StationKind.class);

    static {
        for (StationKind kind : StationKind.values()) {
            String name = kind.recipeTypeId();
            TYPE.put(kind, TYPES.register(name, () -> new RecipeType<ProcessingRecipe>() {
                @Override
                public String toString() {
                    return Cultivo.MOD_ID + ":" + name;
                }
            }));
            SERIALIZER.put(kind, SERIALIZERS.register(name,
                () -> new RecipeSerializer<>(ProcessingRecipe.codec(kind), ProcessingRecipe.streamCodec(kind))));
        }
    }

    private ModRecipes() {}
}
