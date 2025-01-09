package mopsy.productions.nexo.REICompat;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import mopsy.productions.nexo.recipes.NEXORecipe;
import mopsy.productions.nexo.util.NFluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class NEXODisplay implements Display {
    public NEXORecipe recipe;
    public NEXODisplay(NEXORecipe recipe){
        this.recipe=  recipe;

    }
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return null;
    }
    @Override
    public List<EntryIngredient> getInputEntries() {
        List<EntryIngredient> res = new ArrayList<>();
        for(Ingredient ingredient : recipe.inputs){
            for(ItemStack item : ingredient.getMatchingStacks()){
                res.add(EntryIngredients.of(item.getItem()));
            }
        }
        for(NFluidStack stack : recipe.inputFluids){
            res.add(EntryIngredients.of(stack.toFluidStack()));
        }
        return res;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        List<EntryIngredient> res = new ArrayList<>();
        for(ItemStack stack : recipe.outputs){
            res.add(EntryIngredients.of(stack));
        }
        for(NFluidStack stack : recipe.outputFluids){
            res.add(EntryIngredients.of(stack.toFluidStack()));
        }

        return res;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(recipe.id);
    }

    protected static <D extends NEXODisplay> DisplaySerializer<D> buildSerializer(Function<D,NEXORecipe> recipeGetter, Function<NEXORecipe,D> displayConstructor){
        return  new DisplaySerializer<D>() {
            @Override
            public NbtCompound save(NbtCompound tag, D display) {
                return
            }

            @Override
            public D read(NbtCompound tag) {
                return null;
            }
        }
                DisplaySerializer.of(RecordCodecBuilder.mapCodec(in -> in.group(
                        NEXORecipe.Serializer.CODEC.forGetter(recipeGetter)
                ).apply(in, displayConstructor)),
                PacketCodec.tuple(NEXORecipe.Serializer.PACKET_CODEC,
                        recipeGetter,
                        displayConstructor)
        );

    }
}
