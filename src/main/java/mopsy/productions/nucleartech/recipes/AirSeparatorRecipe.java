package mopsy.productions.nucleartech.recipes;

import com.google.gson.JsonObject;
import mopsy.productions.nucleartech.util.NFluidStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;

import java.util.List;

public class AirSeparatorRecipe extends NEXORecipe{
    public AirSeparatorRecipe(Identifier id, List<Ingredient> inputs, List<ItemStack> outputs, List<NFluidStack> inputFluids, List<NFluidStack> outputFluids, List<String> additionalInfo) {
        super(id, inputs, outputs, inputFluids, outputFluids, additionalInfo);
    }
    public AirSeparatorRecipe(NEXORecipe recipe){
        super(recipe.id,recipe.inputs,recipe.outputs,recipe.inputFluids,recipe.outputFluids,recipe.additionalInfo);
    }
    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }
    public static class Type implements RecipeType<AirSeparatorRecipe>{
        private Type() {}
        public static final AirSeparatorRecipe.Type INSTANCE = new AirSeparatorRecipe.Type();
        public static final String ID = "air_separator";
    }
    public static class Serializer implements RecipeSerializer<AirSeparatorRecipe> {
        public static final AirSeparatorRecipe.Serializer INSTANCE = new AirSeparatorRecipe.Serializer();

        @Override
        public AirSeparatorRecipe read(Identifier id, JsonObject json) {
            return new AirSeparatorRecipe(NEXORecipe.Serializer.INSTANCE.read(id,json));
        }

        @Override
        public AirSeparatorRecipe read(Identifier id, PacketByteBuf buf) {
            return new AirSeparatorRecipe(NEXORecipe.Serializer.INSTANCE.read(id,buf));
        }

        @Override
        public void write(PacketByteBuf buf, AirSeparatorRecipe recipe) {
            NEXORecipe.Serializer.INSTANCE.write(buf,recipe);
        }
    }
}
