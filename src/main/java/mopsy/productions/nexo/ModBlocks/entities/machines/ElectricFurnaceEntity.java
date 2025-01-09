package mopsy.productions.nexo.ModBlocks.entities.machines;

import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IEnergyStorage;
import mopsy.productions.nexo.networking.payloads.EnergyChangePayload;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.screen.DefaultSHPayload;
import mopsy.productions.nexo.screen.electricFurnace.ElectricFurnaceScreenHandler;
import mopsy.productions.nexo.util.InvUtils;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;



public class ElectricFurnaceEntity extends BlockEntity implements ExtendedScreenHandlerFactory, SidedInventory, IEnergyStorage, IBlockEntityRecipeCompat {

    private final Inventory inventory = new SimpleInventory(2);
    protected final PropertyDelegate propertyDelegate;
    private int wasHeating=0;
    private int isHeating=0;
    private int progress;
    private int maxProgress = 200;
    public long previousPower = 0;
    public long powerUsagePerTick = 2;
    public int normalizedProgressPerTick = 1;
    public SmeltingRecipe lastRecipe = null;
    public static final long POWER_CAPACITY = 1000;
    public static final long POWER_MAX_INSERT = 10;
    public static final long POWER_MAX_EXTRACT = 0;
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(POWER_CAPACITY, POWER_MAX_INSERT, POWER_MAX_EXTRACT) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    public ElectricFurnaceEntity(BlockPos pos, BlockState state) {
        super(ModdedBlockEntities.ELECTRIC_FURNACE, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return ElectricFurnaceEntity.this.progress;
                    case 1: return ElectricFurnaceEntity.this.maxProgress;
                    case 2: return ElectricFurnaceEntity.this.isHeating;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: ElectricFurnaceEntity.this.progress = value; break;
                    case 1: ElectricFurnaceEntity.this.maxProgress = value; break;
                    case 2: ElectricFurnaceEntity.this.isHeating = value; break;
                }
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Electric Furnace");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ServerPlayNetworking.send((ServerPlayerEntity) player, new EnergyChangePayload(pos,getPower()));
        return new ElectricFurnaceScreenHandler(syncId, inv, this, this.propertyDelegate, pos);
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new DefaultSHPayload(pos);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);
        InvUtils.writeInv(registries, inventory,nbt);
        nbt.putInt("progress", progress);
        nbt.putLong("power", energyStorage.amount);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);
        InvUtils.readInv(registries, inventory,nbt);
        progress = nbt.getInt("progress");
        energyStorage.amount = nbt.getLong("power");
    }

    public boolean hasPower(){
        return energyStorage.amount >= powerUsagePerTick;
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, ElectricFurnaceEntity entity) {
        if(world.isClient)return;

        boolean shouldUpdate = false;

        SmeltingRecipe recipe = getFirstRecipeMatch(entity.inventory, world);
        if(recipe != null){

            if(canAcceptRecipeOutput(recipe, entity.inventory, entity.getMaxCountPerStack(),world.getRegistryManager()) && entity.hasPower()){
                entity.isHeating=1;
                entity.energyStorage.amount -= entity.powerUsagePerTick;
                if(recipe==entity.lastRecipe){
                    entity.progress += entity.normalizedProgressPerTick*2;//compensate for the normalized version.
                }else{
                    entity.progress=0;
                    entity.lastRecipe=recipe;
                    entity.maxProgress= recipe.getCookingTime()*2;//compensate for the normalized version.
                }
                if(entity.progress>entity.maxProgress){
                    craftRecipe(recipe, entity.inventory, entity.getMaxCountPerStack(),world.getRegistryManager());
                    entity.progress=0;
                }
                shouldUpdate=true;//Mark dirty when the machine has done something with progress and the recipe.
            }else{
                entity.isHeating=0;
                if(entity.progress>0) {
                    entity.progress = MathHelper.clamp(entity.progress - 1, 0, entity.maxProgress);
                    shouldUpdate=true;//Mark dirty when progress has decreased.
                }
            }
        }else {
            entity.isHeating = 0;
            entity.progress = 0;
        }

        if(entity.wasHeating!=entity.isHeating){
            entity.wasHeating=entity.isHeating;
            shouldUpdate=true;
            world.setBlockState(blockPos,blockState.with(Properties.LIT,entity.isHeating==1),3);
        }

        if(shouldUpdate)
            markDirty(world,blockPos,blockState);

        if(entity.energyStorage.amount!=entity.previousPower) {
            entity.previousPower = entity.energyStorage.amount;
            PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, new EnergyChangePayload(blockPos,entity.getPower())));
        }
    }

    private static SmeltingRecipe getFirstRecipeMatch(Inventory inventory, World world){
        return world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(inventory.getStack(0)),world).orElse(null).value();
    }

    private static boolean canAcceptRecipeOutput(SmeltingRecipe recipe, Inventory inventory, int count, RegistryWrapper.WrapperLookup wrapper) {
        if (!inventory.getStack(0).isEmpty()) {
            ItemStack recipeOutputStack = recipe.craft(new SingleStackRecipeInput(inventory.getStack(0)),wrapper);
            if (recipeOutputStack.isEmpty()) {
                return false;
            } else {
                ItemStack outputSlotStack = inventory.getStack(1);
                if (outputSlotStack.isEmpty()) {
                    return true;
                } else if (!ItemStack.areItemsAndComponentsEqual(recipeOutputStack,outputSlotStack)) {
                    return false;
                } else if (outputSlotStack.getCount() < count && outputSlotStack.getCount() < outputSlotStack.getMaxCount()) {
                    return true;
                } else {
                    return outputSlotStack.getCount() < recipeOutputStack.getMaxCount();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean craftRecipe(SmeltingRecipe recipe, Inventory inventory, int count, RegistryWrapper.WrapperLookup wrapper) {
        if (recipe == null || !canAcceptRecipeOutput(recipe, inventory, count,wrapper)) {
            return false;
        }
        ItemStack inputStack = inventory.getStack(0);
        ItemStack recipeOutputStack = recipe.craft(new SingleStackRecipeInput(inputStack),wrapper);
        ItemStack outputStack = inventory.getStack(1);
        if (outputStack.isEmpty()) {
            inventory.setStack(1, recipeOutputStack.copy());
        } else if (outputStack.isOf(recipeOutputStack.getItem())) {
            outputStack.increment(1);
        }
        inputStack.decrement(1);
        return true;
    }

    @Override
    public long getPower() {
        return energyStorage.amount;
    }

    @Override
    public void setPower(long power) {
        energyStorage.amount = power;
    }
    @Override
    public SlotIO[] getFluidSlotIOs() {
        return new SlotIO[0];
    }

    @Override
    public SlotIO[] getItemSlotIOs() {
        return new SlotIO[]{SlotIO.INPUT,SlotIO.OUTPUT};
    }


    //Inventory Code:
    @Override
    public int[] getAvailableSlots(Direction side) {
        switch(side){
            case UP -> {return new int[]{0};}
            default -> {return new int[]{1};}
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot==0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot==1;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot,stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}
