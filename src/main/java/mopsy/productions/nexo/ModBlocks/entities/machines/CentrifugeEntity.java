package mopsy.productions.nexo.ModBlocks.entities.machines;

import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IEnergyStorage;
import mopsy.productions.nexo.interfaces.IFluidStorage;
import mopsy.productions.nexo.networking.payloads.AdvancedFluidChangePayload;
import mopsy.productions.nexo.networking.payloads.EnergyChangePayload;
import mopsy.productions.nexo.recipes.CentrifugeRecipe;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.screen.DefaultSHPayload;
import mopsy.productions.nexo.screen.centrifuge.CentrifugeScreenHandler;
import mopsy.productions.nexo.util.FluidTransactionUtils;
import mopsy.productions.nexo.util.NTFluidStorage;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
import net.minecraft.network.PacketByteBuf;
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


public class CentrifugeEntity extends BlockEntity implements ExtendedScreenHandlerFactory, SidedInventory, IEnergyStorage, IFluidStorage, IBlockEntityRecipeCompat {

    private final Inventory inventory = new SimpleInventory(7);
    public final List<SingleVariantStorage<FluidVariant>> fluidStorages = new ArrayList<>();
    protected final PropertyDelegate propertyDelegate;
    private int progress;
    private int maxProgress = 500;
    public long previousPower = 0;
    public static final long POWER_CAPACITY = 100000;
    public static final long POWER_MAX_INSERT = 100;
    public static final long POWER_MAX_EXTRACT = 0;
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(POWER_CAPACITY, POWER_MAX_INSERT, POWER_MAX_EXTRACT) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    public CentrifugeEntity(BlockPos pos, BlockState state) {
        super(ModdedBlockEntities.CENTRIFUGE, pos, state);
        fluidStorages.add(new NTFluidStorage(8* FluidConstants.BUCKET ,this, true , 0));
        fluidStorages.add(new NTFluidStorage(8* FluidConstants.BUCKET ,this, false, 1));
        fluidStorages.add(new NTFluidStorage(8* FluidConstants.BUCKET ,this, false, 2));
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return CentrifugeEntity.this.progress;
                    case 1: return CentrifugeEntity.this.maxProgress;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: CentrifugeEntity.this.progress = value; break;
                    case 1: CentrifugeEntity.this.maxProgress = value; break;
                }
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Centrifuge");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send((ServerPlayerEntity) player, new EnergyChangePayload(pos,getPower()));
        for (int i = 0; i < fluidStorages.size(); i++){
            ServerPlayNetworking.send((ServerPlayerEntity) player, new AdvancedFluidChangePayload(pos,i,fluidStorages.get(i).variant,fluidStorages.get(i).amount));
        }
        return new CentrifugeScreenHandler(syncId, inv, this, this.propertyDelegate, pos);
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new DefaultSHPayload(pos);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);
        writeInv(registries, inventory, nbt);
        nbt.putInt("centrifuge.progress", progress);
        nbt.putLong("centrifuge.power", energyStorage.amount);
        for (int i = 0; i < fluidStorages.size(); i++) {
            nbt.putLong("fluid_amount_"+i, fluidStorages.get(i).amount);
            nbt.put("fluid_variant_"+i, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, fluidStorages.get(i).variant).getOrThrow());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);
        readInv(registries, inventory, nbt);
        progress = nbt.getInt("centrifuge.progress");
        energyStorage.amount = nbt.getLong("centrifuge.power");
        for (int i = 0; i < fluidStorages.size(); i++) {
            fluidStorages.get(i).amount = nbt.getLong("fluid_amount_"+i);
            fluidStorages.get(i).variant = FluidVariant.CODEC.parse(NbtOps.INSTANCE,nbt.get("fluid_variant_"+i)).result().orElse(FluidVariant.blank());
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, CentrifugeEntity entity) {
        if(world.isClient)return;

        CentrifugeRecipe recipe = getFirstRecipeMatch(entity);

        if(recipe!=null&& entity.energyStorage.amount >= 50){
            entity.progress++;
            entity.energyStorage.amount -= 50;
            if(entity.progress >= entity.maxProgress){
                recipe.craft(entity,true,true);
                sendFluidUpdate(entity);
                entity.progress=0;
            }
        }else{
            entity.progress = 0;
        }

        markDirty(world,blockPos,blockState);

        if(entity.energyStorage.amount!= entity.previousPower){
            entity.previousPower = entity.energyStorage.amount;
            PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, new EnergyChangePayload(blockPos,entity.getPower())));
        }

        if(tryFabricTransactions(entity)){

        }
        if(tryTransactions(entity)){
            sendFluidUpdate(entity);
        }
    }

    private static boolean tryFabricTransactions(CentrifugeEntity entity) {
        boolean didSomething = FluidTransactionUtils.doFabricImportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;

        return didSomething;
    }

    //Slots: 0=FluidInputInput, 1=FluidInputOutput, 2=FluidOutput1Input, 3=FluidOutput1Output, 4=FluidOutput2Input, 5=FluidOutput2Output,
    private static boolean tryTransactions(CentrifugeEntity entity){
        boolean didSomething = FluidTransactionUtils.tryImportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;

        return didSomething;
    }
    private static CentrifugeRecipe getFirstRecipeMatch(CentrifugeEntity entity){
        for(RecipeEntry<?> recipeEntry : entity.getWorld().getRecipeManager().values()){
            if(recipeEntry.value() instanceof CentrifugeRecipe recipe) {
                if (recipe.hasRecipe(entity)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static void sendFluidUpdate(CentrifugeEntity entity){
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

    //Inventory code:
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
        return slot==6;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot==6;
    }

    //IFluidStorage Code:
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

    @Override
    public SlotIO[] getFluidSlotIOs() {
        return new SlotIO[]{SlotIO.INPUT,SlotIO.OUTPUT,SlotIO.OUTPUT};
    }

    @Override
    public SlotIO[] getItemSlotIOs() {
        return new SlotIO[]{SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE};
    }
}
