package mopsy.productions.nexo.ModBlocks.entities.machines;

import mopsy.productions.nexo.enums.SlotIO;
import mopsy.productions.nexo.interfaces.IBlockEntityRecipeCompat;
import mopsy.productions.nexo.interfaces.IEnergyStorage;
import mopsy.productions.nexo.networking.payloads.EnergyChangePayload;
import mopsy.productions.nexo.recipes.CrusherRecipe;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.screen.DefaultSHPayload;
import mopsy.productions.nexo.screen.crusher.CrusherScreenHandler;
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


public class CrusherEntity extends BlockEntity implements ExtendedScreenHandlerFactory, SidedInventory, IEnergyStorage, IBlockEntityRecipeCompat {

    private final Inventory inventory = new SimpleInventory(2);
    protected final PropertyDelegate propertyDelegate;
    private int progress;
    private int maxProgress = 200;
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

    public CrusherEntity(BlockPos pos, BlockState state) {
        super(ModdedBlockEntities.CRUSHER, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                switch (index){
                    case 0: return CrusherEntity.this.progress;
                    case 1: return CrusherEntity.this.maxProgress;
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0: CrusherEntity.this.progress = value; break;
                    case 1: CrusherEntity.this.maxProgress = value; break;
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
        return Text.literal("Crusher");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ServerPlayNetworking.send((ServerPlayerEntity) player, new EnergyChangePayload(pos,getPower()));
        return new CrusherScreenHandler(syncId, inv, this, this.propertyDelegate, pos);
    }

    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        return new DefaultSHPayload(pos);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.writeNbt(nbt,registries);
        InvUtils.writeInv(registries, inventory,nbt);
        nbt.putInt("crusher.progress", progress);
        nbt.putLong("crusher.power", energyStorage.amount);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries){
        super.readNbt(nbt,registries);
        InvUtils.readInv(registries, inventory,nbt);
        progress = nbt.getInt("crusher.progress");
        energyStorage.amount = nbt.getLong("crusher.power");
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, CrusherEntity entity) {
        if(world.isClient)return;

        CrusherRecipe recipe = getRecipe(entity);
        if(recipe != null && recipe.canOutput(entity) && entity.energyStorage.amount >= 5){
            entity.progress++;
            entity.energyStorage.amount -= 5;
            if(entity.progress >= entity.maxProgress){
                recipe.craft(entity,true,true);
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
    }

    private static CrusherRecipe getRecipe(CrusherEntity entity){
        for(RecipeEntry<?> recipeEntry : entity.getWorld().getRecipeManager().values()){
            if(recipeEntry.value() instanceof CrusherRecipe recipe) {
                if (recipe.hasRecipe(entity)) {
                    return recipe;
                }
            }
        }
        return null;
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
