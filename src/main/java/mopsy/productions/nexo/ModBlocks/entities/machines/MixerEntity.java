package mopsy.productions.nexo.ModBlocks.entities.machines;

import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IEnergyStorage;
import mopsy.productions.nexo.interfaces.IFluidStorage;
import mopsy.productions.nexo.networking.payloads.AdvancedFluidChangePayload;
import mopsy.productions.nexo.networking.payloads.EnergyChangePayload;
import mopsy.productions.nexo.recipes.MixerRecipe;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.screen.DefaultSHPayload;
import mopsy.productions.nexo.screen.mixer.MixerScreenHandler;
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


public class MixerEntity extends BlockEntity implements ExtendedScreenHandlerFactory, SidedInventory, IEnergyStorage, IFluidStorage, IBlockEntityRecipeCompat {

    private final Inventory inventory = new SimpleInventory(10);
    public final List<SingleVariantStorage<FluidVariant>> fluidStorages = new ArrayList<>();
    protected final PropertyDelegate propertyDelegate;
    public boolean tryStart;
    public int progress;
    private int maxProgress = 500;
    public int heat = 0;
    public long previousPower = 0;
    public static final long POWER_CAPACITY = 100000;
    public static final long POWER_MAX_INSERT = 50;
    public static final long POWER_MAX_EXTRACT = 0;
    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(POWER_CAPACITY, POWER_MAX_INSERT, POWER_MAX_EXTRACT) {
        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    public MixerEntity(BlockPos pos, BlockState state) {
        super(ModdedBlockEntities.MIXER, pos, state);
        fluidStorages.add(new NTFluidStorage(4* FluidConstants.BUCKET ,this, true, 0));
        fluidStorages.add(new NTFluidStorage(4* FluidConstants.BUCKET ,this, true, 1));
        fluidStorages.add(new NTFluidStorage(4* FluidConstants.BUCKET ,this, true, 2));
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return MixerEntity.this.progress;
                    case 1: return MixerEntity.this.maxProgress;
                    case 2: return MixerEntity.this.heat;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: MixerEntity.this.progress = value; break;
                    case 1: MixerEntity.this.maxProgress = value; break;
                    case 2: MixerEntity.this.heat = value; break;
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
        return Text.literal("Mixer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ServerPlayNetworking.send((ServerPlayerEntity) player, new EnergyChangePayload(pos,getPower()));
        for (int i = 0; i < fluidStorages.size(); i++){
            ServerPlayNetworking.send((ServerPlayerEntity) player, new AdvancedFluidChangePayload(pos,i,fluidStorages.get(i).variant,fluidStorages.get(i).amount));
        }
        return new MixerScreenHandler(syncId, inv, this, this.propertyDelegate, pos);
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new DefaultSHPayload(pos);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);
        writeInv(registries, inventory, nbt);
        nbt.putInt("mixer.progress", progress);
        nbt.putLong("mixer.power", energyStorage.amount);
        for (int i = 0; i < fluidStorages.size(); i++) {
            nbt.putLong("fluid_amount_"+i, fluidStorages.get(i).amount);
            nbt.put("fluid_variant_"+i, FluidVariant.CODEC.encodeStart(NbtOps.INSTANCE, fluidStorages.get(i).variant).getOrThrow());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);
        readInv(registries, inventory, nbt);
        progress = nbt.getInt("mixer.progress");
        energyStorage.amount = nbt.getLong("mixer.power");
        for (int i = 0; i < fluidStorages.size(); i++) {
            fluidStorages.get(i).amount = nbt.getLong("fluid_amount_"+i);
            fluidStorages.get(i).variant = FluidVariant.CODEC.parse(NbtOps.INSTANCE,nbt.get("fluid_variant_"+i)).result().orElse(FluidVariant.blank());
        }
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, MixerEntity entity) {
        if(world.isClient)return;

        if(entity.energyStorage.amount >= 25 && (entity.progress > 0|| entity.tryStart)){
            entity.tryStart = false;
            entity.progress++;
            entity.energyStorage.amount -= 25;
            if(entity.progress >= entity.maxProgress){
                entity.progress = 0;
                MixerRecipe recipe = getFirstRecipeMatch(entity);
                if(recipe!=null) {
                    recipe.craft(entity, true, true);

                }else{
                    for (int i = 6; i < 10; i++) {
                        entity.inventory.setStack(i, ItemStack.EMPTY);
                    }
                    for(SingleVariantStorage<FluidVariant> storage : entity.fluidStorages){
                        storage.amount = 0;
                        storage.variant = FluidVariant.blank();
                    }
                }
                sendFluidUpdate(entity);
            }
        }else{
            entity.progress = 0;
        }

        markDirty(world,blockPos,blockState);

        if(entity.progress==0) {
            if (tryFabricTransactions(entity)) {

            }
            if (tryTransactions(entity)) {
                sendFluidUpdate(entity);
            }
        }

        if(entity.energyStorage.amount!=entity.previousPower){
            entity.previousPower = entity.energyStorage.amount;
            PlayerLookup.tracking(entity).forEach(player -> ServerPlayNetworking.send(player, new EnergyChangePayload(blockPos,entity.getPower())));
        }
    }

    private static boolean tryFabricTransactions(MixerEntity entity) {
        boolean didSomething = FluidTransactionUtils.doFabricImportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricImportTransaction(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricImportTransaction(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;
        if(FluidTransactionUtils.doFabricExportTransaction(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;

        return didSomething;
    }
    private static boolean tryTransactions(MixerEntity entity){
        boolean didSomething = FluidTransactionUtils.tryImportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0));
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 0, 1, entity.fluidStorages.get(0)))
            didSomething = true;
        if(FluidTransactionUtils.tryImportFluid(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 2, 3, entity.fluidStorages.get(1)))
            didSomething = true;
        if(FluidTransactionUtils.tryImportFluid(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;
        if(FluidTransactionUtils.tryExportFluid(entity.inventory, 4, 5, entity.fluidStorages.get(2)))
            didSomething = true;

        return didSomething;
    }

    private static MixerRecipe getFirstRecipeMatch(MixerEntity entity){
        for(RecipeEntry<?> recipeEntry : entity.getWorld().getRecipeManager().values()){
            if(recipeEntry.value() instanceof MixerRecipe recipe) {
                if (recipe.hasRecipe(entity)) {
                    return recipe;
                }
            }
        }
        return null;
    }

    private static void sendFluidUpdate(MixerEntity entity){
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
        return slot==0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot==1;
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
        return new SlotIO[]{SlotIO.BOTH, SlotIO.BOTH, SlotIO.BOTH};
    }

    @Override
    public SlotIO[] getItemSlotIOs() {
        return new SlotIO[]{
                SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,SlotIO.NONE,
                SlotIO.BOTH,SlotIO.BOTH,SlotIO.BOTH,SlotIO.BOTH
        };
    }
}
