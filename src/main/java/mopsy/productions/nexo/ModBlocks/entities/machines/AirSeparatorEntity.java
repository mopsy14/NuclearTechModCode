package mopsy.productions.nexo.ModBlocks.entities.machines;

import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IEnergyStorage;
import mopsy.productions.nexo.interfaces.IFluidStorage;
import mopsy.productions.nexo.interfaces.IMultiBlockController;
import mopsy.productions.nexo.multiblock.AirSeparatorMultiBlock;
import mopsy.productions.nexo.networking.payloads.AdvancedFluidChangePayload;
import mopsy.productions.nexo.networking.payloads.EnergyChangePayload;
import mopsy.productions.nexo.recipes.AirSeparatorRecipe;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.screen.DefaultSHPayload;
import mopsy.productions.nexo.screen.airSeparator.AirSeparatorScreenHandler;
import mopsy.productions.nexo.util.FluidTransactionUtils;
import mopsy.productions.nexo.util.NTFluidStorage;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

import static mopsy.productions.nexo.util.InvUtils.readInv;
import static mopsy.productions.nexo.util.InvUtils.writeInv;


public class AirSeparatorEntity extends BlockEntity implements ExtendedScreenHandlerFactory, IFluidStorage, SidedInventory, IEnergyStorage, IMultiBlockController, IBlockEntityRecipeCompat {

    private final Inventory inventory = new SimpleInventory(4);
    protected final PropertyDelegate propertyDelegate;
    private int progress;
    public int pumpAmount = 0;
    public int coolerAmount = 0;
    private int maxProgress = 200;
    public final List<SingleVariantStorage<FluidVariant>> fluidStorages = new ArrayList<>();
    public long previousPower = 0;
    public static final long POWER_CAPACITY = 1000;
    public static final long POWER_MAX_INSERT = 10;
    public static final long POWER_MAX_EXTRACT = 0;
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(POWER_CAPACITY, POWER_MAX_INSERT, POWER_MAX_EXTRACT) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    public AirSeparatorEntity(BlockPos pos, BlockState state) {
        super(ModdedBlockEntities.AIR_SEPARATOR, pos, state);
        fluidStorages.add(new NTFluidStorage(8* FluidConstants.BUCKET ,this, false , 0));
        fluidStorages.add(new NTFluidStorage(8* FluidConstants.BUCKET ,this, false, 1));
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return AirSeparatorEntity.this.progress;
                    case 1: return AirSeparatorEntity.this.maxProgress;
                    case 2: return AirSeparatorEntity.this.pumpAmount;
                    case 3: return AirSeparatorEntity.this.coolerAmount;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: AirSeparatorEntity.this.progress = value; break;
                    case 1: AirSeparatorEntity.this.maxProgress = value; break;
                    case 2: AirSeparatorEntity.this.pumpAmount = value; break;
                    case 3: AirSeparatorEntity.this.coolerAmount = value; break;
                }
            }

            @Override
            public int size() {
                return 4;
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Air Separator");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ServerPlayNetworking.send((ServerPlayerEntity) player, new EnergyChangePayload(pos,getPower()));
        for (int i = 0; i < fluidStorages.size(); i++){
            ServerPlayNetworking.send((ServerPlayerEntity) player, new AdvancedFluidChangePayload(pos,i,fluidStorages.get(i).variant,fluidStorages.get(i).amount));
        }
        return new AirSeparatorScreenHandler(syncId, inv, this, this.propertyDelegate, pos);
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new DefaultSHPayload(pos);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);
        writeInv(registries,inventory,nbt);
        nbt.putInt("air_separator.progress", progress);
        nbt.putLong("air_separator.power", energyStorage.amount);
        nbt.putInt("air_separator.air_pumps", pumpAmount);
        nbt.putInt("air_separator.coolers", coolerAmount);
        for (int i = 0; i < fluidStorages.size(); i++) {
            nbt.putLong("fluid_amount_"+i, fluidStorages.get(i).amount);
            nbt.put("fluid_variant_"+i, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, fluidStorages.get(i).variant).getOrThrow());
        }
    }
    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);
        readInv(registries,inventory,nbt);
        progress = nbt.getInt("air_separator.progress");
        energyStorage.amount = nbt.getLong("air_separator.power");
        pumpAmount = nbt.getInt("air_separator.air_pumps");
        coolerAmount = nbt.getInt("air_separator.coolers");
        for (int i = 0; i < fluidStorages.size(); i++) {
            fluidStorages.get(i).amount = nbt.getLong("fluid_amount_"+i);
            fluidStorages.get(i).variant = FluidVariant.CODEC.parse(NbtOps.INSTANCE,nbt.get("fluid_variant_"+i)).result().orElse(FluidVariant.blank());
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, AirSeparatorEntity entity) {
        if(world.isClient)return;

        AirSeparatorRecipe recipe = getRecipe(entity);

        if(entity.energyStorage.amount >= 5 && recipe.canOutput(entity)){
            entity.progress++;
            entity.energyStorage.amount -= 5;
            if(entity.progress >= entity.maxProgress){
                if (recipe.craft(entity,true,false)) {
                    sendFluidUpdate(entity);
                }
                entity.progress = 0;
            }
        }else{
            entity.progress = 0;
        }

        if(entity.energyStorage.amount!= entity.previousPower){
            entity.previousPower = entity.energyStorage.amount;
            PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, new EnergyChangePayload(blockPos,entity.getPower())));
        }

        if(tryFabricTransactions(entity)){

        }
        if(tryTransactions(entity)){
            sendFluidUpdate(entity);
        }

        markDirty(world,blockPos,blockState);
    }

    private static AirSeparatorRecipe getRecipe(AirSeparatorEntity entity){
        for(RecipeEntry<?> recipeEntry : entity.getWorld().getRecipeManager().values()){
            if(recipeEntry.value() instanceof AirSeparatorRecipe recipe) {
                if (recipe.hasRecipe(entity)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static boolean tryFabricTransactions(AirSeparatorEntity entity) {
        boolean didSomething = FluidTransactionUtils.doFabricImportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;

        return didSomething;
    }
    //Slots: 0=FluidInputInput, 1=FluidInputOutput, 2=FluidOutput1Input, 3=FluidOutput1Output
    private static boolean tryTransactions(AirSeparatorEntity entity){
        boolean didSomething = FluidTransactionUtils.tryImportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;

        return didSomething;
    }

    private static void sendFluidUpdate(AirSeparatorEntity entity){
        for (int i = 0; i < entity.fluidStorages.size(); i++) {
            SingleVariantStorage<FluidVariant> fluidStorage = entity.fluidStorages.get(i);
            int finalI = i;
            PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player,
                    new AdvancedFluidChangePayload(entity.pos,finalI,fluidStorage.variant,fluidStorage.amount)));
        }
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
    public void updateMultiBlock() {
        pumpAmount = AirSeparatorMultiBlock.INSTANCE.getAirPumpAmount(this);
        coolerAmount = AirSeparatorMultiBlock.INSTANCE.getCoolerAmount(this);
    }


    @Override
    public FluidVariant getFluidType() {
        return FluidVariant.blank();
    }

    @Override
    public void setFluidType(FluidVariant fluidType) {

    }

    @Override
    public long getFluidAmount() {
        return 0;
    }

    @Override
    public void setFluidAmount(long amount) {

    }

    @Override
    public List<SingleVariantStorage<FluidVariant>> getFluidStorages() {
        return fluidStorages;
    }

    //Sided Inventory code:
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
        return inventory.removeStack(slot,amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        int[] res = new int[inventory.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = i;
        }

        return res;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public SlotIO[] getFluidSlotIOs() {
        return new SlotIO[]{SlotIO.OUTPUT,SlotIO.OUTPUT};
    }

    @Override
    public SlotIO[] getItemSlotIOs() {
        return new SlotIO[]{SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE};
    }
}
