package mopsy.productions.nexo.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IFluidStorage;
import mopsy.productions.nexo.util.NEXOInventory;
import mopsy.productions.nexo.util.NFluidStack;
import mopsy.productions.nexo.util.NTFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static mopsy.productions.nexo.Main.LOGGER;


public class NEXORecipe implements Recipe<RecipeInput> {
    public final Identifier id;
    public final List<Ingredient> inputs;
    public final List<ItemStack> outputs;
    public final List<NFluidStack> inputFluids;
    public final List<NFluidStack> outputFluids;
    public final List<String> additionalInfo;

    public static HashMap<Identifier, Function<NEXORecipe, ? extends NEXORecipe>> recipeConverters = new HashMap<>();
    public NEXORecipe(Identifier id, List<Ingredient> inputs, List<ItemStack> outputs, List<NFluidStack> inputFluids, List<NFluidStack> outputFluids, List<String> additionalInfo){
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.inputFluids = inputFluids;
        this.outputFluids = outputFluids;
        this.additionalInfo = additionalInfo;
    }
    private static NEXORecipe fromProperties(Identifier id, List<Ingredient> inputs, List<ItemStack> outputs, List<NFluidStack> inputFluids, List<NFluidStack> outputFluids, List<String> additionalInfo){
        NEXORecipe nexoRecipe = new NEXORecipe(id, inputs, outputs, inputFluids, outputFluids, additionalInfo);
        if (recipeConverters.containsKey(id))
            return recipeConverters.get(id).apply(nexoRecipe);
        else {
            LOGGER.error("Cannot find recipe converter!");
            LOGGER.error("Report this and the following text to a dev of NEXO!");
            throw new RuntimeException();
        }
    }

    //hasRecipe Code:
    public boolean hasRecipe(BlockEntity blockEntity){
        if(inputs.isEmpty() && inputFluids.isEmpty()) {
            LOGGER.error("Empty fluid and item inputs found in recipe");
            return false;
        }
        if(!(blockEntity instanceof Inventory)){
            LOGGER.error(blockEntity+" does not implement Inventory");
            return false;
        }
        if(blockEntity instanceof IFluidStorage)
            return hasItems((Inventory)blockEntity,((IBlockEntityRecipeCompat)blockEntity).getItemSlotIOs()) && hasFluids(((IFluidStorage)blockEntity).getFluidStorages(),((IBlockEntityRecipeCompat)blockEntity));
        else
            return hasItems((Inventory)blockEntity,((IBlockEntityRecipeCompat)blockEntity).getItemSlotIOs());
    }
    private boolean hasFluids(List<SingleVariantStorage<FluidVariant>> fluidStorages, IBlockEntityRecipeCompat config){
        for (NFluidStack inputFluid : inputFluids) {
            if (!hasFluid(fluidStorages, config.getFluidSlotIOs(), inputFluid)) return false;
        }
        return true;
    }
    private boolean hasFluid(List<SingleVariantStorage<FluidVariant>> fluidStorages, SlotIO[] fluidConfig, NFluidStack checkStack){
        for(int i = 0; i < fluidStorages.size(); i++ ){
            if(fluidConfig[i]== SlotIO.INPUT||fluidConfig[i]== SlotIO.BOTH) {
                if (fluidStorages.get(i).variant.equals(checkStack.fluidVariant)) {
                    if (fluidStorages.get(i).amount >= checkStack.fluidAmount)return true;
                }
            }
        }
        return false;
    }
    private boolean hasItems(Inventory inv, SlotIO[] itemConfig){
        for (Ingredient input : inputs) {
            if (!hasItem(inv, input, itemConfig)) return false;
        }
        return true;
    }
    private boolean hasItem(Inventory inv, Ingredient ingredient, SlotIO[] slotIO){
        for (int i = 0; i < inv.size(); i++) {
            if(slotIO[i]==SlotIO.INPUT||slotIO[i]==SlotIO.BOTH)
                if(ingredient.test(inv.getStack(i)))return true;
        }
        return false;
    }

    //canOutput Code:
    public boolean canOutput(BlockEntity blockEntity){
        if(!(blockEntity instanceof Inventory)){
            LOGGER.error(blockEntity+" does not implement Inventory");
            return false;
        }
        if(blockEntity instanceof IFluidStorage)
            return canOutputItems((Inventory)blockEntity,((IBlockEntityRecipeCompat)blockEntity).getItemSlotIOs())&&
                    canOutputFluids(((IFluidStorage)blockEntity).getFluidStorages(),((IBlockEntityRecipeCompat)blockEntity).getFluidSlotIOs());
        else
            return canOutputItems((Inventory)blockEntity,((IBlockEntityRecipeCompat)blockEntity).getItemSlotIOs());
    }
    private boolean canOutputItems(Inventory blockInventory,SlotIO[] slotIOs) {
        //creating a copy inv with all slots that can accept output
        Inventory outputInv = new NEXOInventory(blockInventory.size());
        int invCounter=0;
        for (int i = 0; i < blockInventory.size(); i++) {
            if(slotIOs[i] == SlotIO.OUTPUT || slotIOs[i] == SlotIO.BOTH) {
                outputInv.setStack(invCounter,blockInventory.getStack(i).copy());
                invCounter++;
            }
        }
        outputInv = ((NEXOInventory)outputInv).withSize(invCounter);
        //creating an inv with all outputs
        Inventory toBeOutputted = new SimpleInventory(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            if(!outputs.get(i).isEmpty()) {
                toBeOutputted.setStack(i, outputs.get(i).copy());
            }
        }
        //checking if every output can be put into outputInv
        for (int i = 0; i < toBeOutputted.size();i++) {
            tryOutputItemStack(toBeOutputted.getStack(i), outputInv);
            if(!toBeOutputted.getStack(i).isEmpty())return false;
        }
        return true;
    }
    private void tryOutputItemStackWithChecks(ItemStack outputStack, Inventory outputInv, SlotIO[] slotIOs){
        int outputted = 0;
        for (int i = 0; i < outputInv.size() && !outputStack.isEmpty(); i++) {
            if(slotIOs[i]==SlotIO.OUTPUT||slotIOs[i]==SlotIO.BOTH) {
                if (outputInv.getStack(i).isEmpty()) {
                    outputInv.setStack(i, outputStack.copy());
                    outputted = outputted + outputStack.getCount();
                    outputStack.setCount(0);
                } else if (outputInv.getStack(i).getItem() == outputStack.getItem()) {
                    int toOutput = Math.min(outputStack.getCount(), 64 - outputInv.getStack(i).getCount());
                    outputInv.getStack(i).setCount(outputInv.getStack(i).getCount() + toOutput);
                    outputted = outputted + toOutput;
                    outputStack.setCount(outputStack.getCount() - toOutput);
                }
            }
        }
    }
    private void tryOutputItemStack(ItemStack outputStack, Inventory outputInv){
        int outputted = 0;
        for (int i = 0; i < outputInv.size() && !outputStack.isEmpty(); i++) {
            if(outputInv.getStack(i).isEmpty()){
                outputInv.setStack(i, outputStack.copy());
                outputted = outputted + outputStack.getCount();
                outputStack.setCount(0);
            } else if (outputInv.getStack(i).getItem()==outputStack.getItem()) {
                int toOutput = Math.min(outputStack.getCount(),outputInv.getStack(i).getMaxCount()-outputInv.getStack(i).getCount());
                outputInv.getStack(i).setCount(outputInv.getStack(i).getCount()+toOutput);
                outputted = outputted + toOutput;
                outputStack.setCount(outputStack.getCount()-toOutput);
            }
        }
    }
    private boolean canOutputFluids(List<SingleVariantStorage<FluidVariant>> fluidStorages, SlotIO[] fluidSlotIOs){
        //creating a copy inv with all storages that can accept output
        List<NTFluidStorage> outputStorages = new ArrayList<>();
        for (int i = 0; i < fluidStorages.size(); i++) {
            if(fluidSlotIOs[i] == SlotIO.OUTPUT || fluidSlotIOs[i] == SlotIO.BOTH) {
                outputStorages.add(((NTFluidStorage)fluidStorages.get(i)).copy());
            }
        }
        //creating a copy of all outputs
        List<NFluidStack> toBeOutputted = new ArrayList<>();
        for (NFluidStack outputFluid : outputFluids) {
            if (!outputFluid.isEmpty()) {
                toBeOutputted.add(outputFluid.copy());
            }
        }
        //checking if every output can be put into outputInv
        for (NFluidStack fluidStack : toBeOutputted) {
            tryOutputFluidStack(fluidStack, outputStorages);
            if (!fluidStack.isEmpty()) return false;
        }
        return true;
    }
    private void tryOutputFluidStack(NFluidStack outputStack, List<NTFluidStorage> outputStorages){
        long outputted = 0;
        for (int i = 0; i < outputStorages.size() && !outputStack.isEmpty(); i++) {
            if(outputStorages.get(i).isResourceBlank()){
                outputStorages.get(i).variant = outputStack.fluidVariant;
                long toOutput = Math.min(outputStack.fluidAmount, outputStorages.get(i).getCapacity());
                outputStorages.get(i).amount = outputStorages.get(i).amount + toOutput;
                outputted = outputted + toOutput;
                outputStack.fluidAmount = outputStack.fluidAmount-toOutput;
            } else if (outputStorages.get(i).getResource().equals(outputStack.fluidVariant)) {
                long toOutput = Math.min(outputStack.fluidAmount,outputStorages.get(i).getCapacity()-outputStorages.get(i).amount);
                outputStorages.get(i).amount = outputStorages.get(i).amount + toOutput;
                outputted = outputted + toOutput;
                outputStack.fluidAmount = outputStack.fluidAmount-toOutput;
            }
        }
    }
    public boolean craft(BlockEntity entity, boolean doFitCheck, boolean doRemoveInputs){
        if(!(entity instanceof Inventory)){
            LOGGER.error(entity+" does not implement Inventory");
            return false;
        }
        if(!doFitCheck || canOutput(entity)){
            if(doRemoveInputs)
                removeInputs(entity);
            craftItems((Inventory)entity,((IBlockEntityRecipeCompat)entity).getItemSlotIOs());
            if(entity instanceof IFluidStorage)
                craftFluids(((IFluidStorage)entity).getFluidStorages(),((IBlockEntityRecipeCompat)entity).getFluidSlotIOs());
            return true;
        }
        return true;
    }
    public void removeInputs(BlockEntity entity){
        if(entity instanceof Inventory)
            removeInputItems((Inventory)entity,((IBlockEntityRecipeCompat)entity).getItemSlotIOs());
        if(entity instanceof IFluidStorage)
            removeInputFluids(((IFluidStorage)entity).getFluidStorages(),((IBlockEntityRecipeCompat)entity).getFluidSlotIOs());
    }
    private void removeInputItems(Inventory inv, SlotIO[] itemSlotIOs){
        for (Ingredient ingredient : inputs) {
            for (int i = 0; i < inv.size(); i++) {
                if(itemSlotIOs[i]==SlotIO.INPUT||itemSlotIOs[i]==SlotIO.BOTH){
                    if(ingredient.test(inv.getStack(i)))
                        inv.getStack(i).setCount(inv.getStack(i).getCount()-1);
                }
            }
        }
    }
    private void removeInputFluids(List<SingleVariantStorage<FluidVariant>> fluidStorages,SlotIO[] fluidSlotIOs){
        for(NFluidStack stack : inputFluids){
            long toExtract = stack.fluidAmount;
            for (int i = 0; i < fluidStorages.size(); i++) {
                if(fluidSlotIOs[i]==SlotIO.INPUT||fluidSlotIOs[i]==SlotIO.BOTH){
                    if(fluidStorages.get(i).variant.equals(stack.fluidVariant)){
                        long extractAmount = Math.min(toExtract,fluidStorages.get(i).amount);
                        fluidStorages.get(i).amount -= extractAmount;
                        toExtract-=extractAmount;
                    }
                }
            }
            if(toExtract > 0)
                LOGGER.error("Couldn't extract all "+ stack.fluidVariant +" fluid for recipe");
        }
    }
    private void craftItems(Inventory blockInventory, SlotIO[] slotIOs){
        //creating an inv with all outputs
        Inventory toBeOutputted = new SimpleInventory(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            if(!outputs.get(i).isEmpty()) {
                toBeOutputted.setStack(i, outputs.get(i).copy());
            }
        }
        //Running tryOutputItemStack for every output stack
        for (int i = 0; i < toBeOutputted.size();i++) {
            tryOutputItemStackWithChecks(toBeOutputted.getStack(i), blockInventory, slotIOs);
        }
    }
    private void craftFluids(List<SingleVariantStorage<FluidVariant>> fluidStorages, SlotIO[] fluidSlotIOs){
        //creating a copy inv with all storages that can accept output
        List<NTFluidStorage> outputStorages = new ArrayList<>();
        for (int i = 0; i < fluidStorages.size(); i++) {
            if(fluidSlotIOs[i] == SlotIO.OUTPUT || fluidSlotIOs[i] == SlotIO.BOTH) {
                outputStorages.add(((NTFluidStorage)fluidStorages.get(i)));
            }
        }
        //creating a copy of all outputs
        List<NFluidStack> toBeOutputted = new ArrayList<>();
        for (NFluidStack outputFluid : outputFluids) {
            if (!outputFluid.isEmpty()) {
                toBeOutputted.add(outputFluid.copy());
            }
        }
        //checking if every output can be put into outputInv
        for (NFluidStack fluidStack : toBeOutputted) {
            tryOutputFluidStack(fluidStack, outputStorages);
        }
    }


    @Override
    public RecipeSerializer<? extends NEXORecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<NEXORecipe>{
        public static final Serializer INSTANCE = new Serializer();
        public static final MapCodec<NEXORecipe> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
                Identifier.CODEC.fieldOf("type").forGetter(r -> r.id),
                Ingredient.ALLOW_EMPTY_CODEC.listOf().fieldOf("item_inputs").orElse(Collections.emptyList()).forGetter(r -> r.inputs),
                ItemStack.CODEC.listOf().fieldOf("item_outputs").orElse(Collections.emptyList()).forGetter(r -> r.outputs),
                NFluidStack.CODEC.listOf().fieldOf("fluid_inputs").orElse(Collections.emptyList()).forGetter(r -> r.inputFluids),
                NFluidStack.CODEC.listOf().fieldOf("fluid_outputs").orElse(Collections.emptyList()).forGetter(r -> r.outputFluids),
                Codec.STRING.listOf().fieldOf("additional_info").orElse(Collections.emptyList()).forGetter(r -> r.additionalInfo)
        ).apply(in, NEXORecipe::fromProperties));
        public static final PacketCodec<RegistryByteBuf, NEXORecipe> PACKET_CODEC = PacketCodec.tuple(
                Identifier.PACKET_CODEC,
                recipe->recipe.id,
                PacketCodecs.codec(Ingredient.ALLOW_EMPTY_CODEC.listOf()),
                recipe->recipe.inputs,
                PacketCodecs.codec(ItemStack.CODEC.listOf()),
                recipe->recipe.outputs,
                PacketCodecs.codec(NFluidStack.CODEC.listOf()),
                recipe->recipe.inputFluids,
                PacketCodecs.codec(NFluidStack.CODEC.listOf()),
                recipe->recipe.outputFluids,
                PacketCodecs.codec(Codec.STRING.listOf()),
                recipe->recipe.additionalInfo,
                NEXORecipe::fromProperties
        );

        @Override
        public MapCodec<NEXORecipe> codec() {
            return CODEC;
        }
        @Override
        public PacketCodec<RegistryByteBuf, NEXORecipe> packetCodec() {
            return PACKET_CODEC;
        }

        /*
        @Override
        public NEXORecipe read(Identifier id, JsonObject json) {
            List<Ingredient> inputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            List<NFluidStack> fluidInputs = new ArrayList<>();
            List<NFluidStack> fluidOutputs = new ArrayList<>();
            List<String> additionalInfo = new ArrayList<>();

            JsonArray jsonArray = json.getAsJsonArray("item_inputs");
            if(jsonArray!=null) {
                for (JsonElement element : jsonArray){
                    inputs.add(Ingredient.fromJson(element));
                }
            }

            jsonArray = json.getAsJsonArray("item_outputs");
            if(jsonArray!=null) {
                for (JsonElement element : jsonArray){
                    outputs.add(new ItemStack(
                            Registry.ITEM.get(Identifier.tryParse(element.getAsJsonObject().get("item").getAsString())),
                            element.getAsJsonObject().get("count").getAsInt()
                    ));

                }
            }

            jsonArray = json.getAsJsonArray("fluid_inputs");
            if(jsonArray!=null) {
                for (JsonElement element : jsonArray){
                    fluidInputs.add(NFluidStack.fromJson(element));
                }
            }

            jsonArray = json.getAsJsonArray("fluid_outputs");
            if(jsonArray!=null) {
                for (JsonElement element : jsonArray){
                    fluidOutputs.add(NFluidStack.fromJson(element));
                }
            }
            jsonArray = json.getAsJsonArray("additional_info");
            if(jsonArray!=null){
                for(JsonElement element : jsonArray){
                    additionalInfo.add(element.getAsString());
                }
            }

            return new NEXORecipe(id,inputs,outputs,fluidInputs,fluidOutputs,additionalInfo);
        }

        @Override
        public NEXORecipe read(Identifier id, PacketByteBuf buf) {
            Identifier type = Identifier.of(modid,buf.readString());
            List<Ingredient> inputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            List<NFluidStack> fluidInputs = new ArrayList<>();
            List<NFluidStack> fluidOutputs = new ArrayList<>();
            List<String> additionalInfo = new ArrayList<>();

            int length = buf.readInt();
            for (int i = 0; i < length; i++) {
                inputs.add(Ingredient.fromPacket(buf));
            }
            length = buf.readInt();
            for (int i = 0; i < length; i++) {
                outputs.add(buf.readItemStack());
            }
            length = buf.readInt();
            for (int i = 0; i < length; i++) {
                fluidInputs.add(NFluidStack.fromPacket(buf));
            }
            length = buf.readInt();
            for (int i = 0; i < length; i++) {
                fluidOutputs.add(NFluidStack.fromPacket(buf));
            }
            length = buf.readInt();
            for (int i = 0; i < length; i++) {
                additionalInfo.add(buf.readString());
            }

            NEXORecipe recipe = new NEXORecipe(id,inputs,outputs,fluidInputs,fluidOutputs,additionalInfo);

            if (recipeConverters.containsKey(type))
                return recipeConverters.get(type).apply(recipe);
            else {
                LOGGER.error("Cannot find recipe converter!");
                LOGGER.error("Report this and the following text to a dev of NEXO!");
                throw new RuntimeException();
            }
        }

        @Override
        public void write(PacketByteBuf buf, NEXORecipe recipe) {
            if(recipe.getTypeID()==null){
                LOGGER.error("Recipe does not override getTypeID!");
                LOGGER.error("Report this and the following text to a dev of NEXO!");
                throw new RuntimeException();
            }
            buf.writeString(recipe.getTypeID());
            buf.writeInt(recipe.inputs.size());
            for(Ingredient ingredient : recipe.inputs){
                ingredient.write(buf);
            }
            buf.writeInt(recipe.outputs.size());
            for(ItemStack itemStack : recipe.outputs){
                buf.writeItemStack(itemStack);
            }
            buf.writeInt(recipe.inputFluids.size());
            for(NFluidStack fluidStack : recipe.inputFluids){
                NFluidStack.toBuf(fluidStack,buf);
            }
            buf.writeInt(recipe.outputFluids.size());
            for(NFluidStack fluidStack : recipe.outputFluids){
                NFluidStack.toBuf(fluidStack,buf);
            }
            buf.writeInt(recipe.additionalInfo.size());
            for(String info : recipe.additionalInfo){
                buf.writeString(info);
            }
        }
         */
    }

    /**
     * OVERWRITE THIS METHOD!
     */
    @Override
    public RecipeType<? extends NEXORecipe> getType() {
        return null;
    }


    /**
     * OVERWRITE THIS METHOD!
     */
    public String getTypeID(){
        return null;
    }


    /**
     * UNUSED METHOD OF RECIPE!!!
     */
    @Override
    public boolean matches(RecipeInput input, World world) {
        return false;
    }

    /**
     * UNUSED METHOD OF RECIPE!!!
     */
    @Override
    public ItemStack craft(RecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return null;
    }

    /**
     * UNUSED METHOD OF RECIPE!!!
     */
    @Override
    public boolean fits(int width, int height) {
        return false;
    }

    /**
     * UNUSED METHOD OF RECIPE!!!
     */
    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return null;
    }


    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }
}
