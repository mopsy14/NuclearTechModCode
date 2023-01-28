package mopsy.productions.nucleartech.ModBlocks.entities.machines;

import mopsy.productions.nucleartech.interfaces.ImplementedInventory;
import mopsy.productions.nucleartech.registry.ModdedBlockEntities;
import mopsy.productions.nucleartech.registry.ModdedItems;
import mopsy.productions.nucleartech.screen.crusher.CrusherScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CrusherEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    protected final PropertyDelegate propertyDelegate;
    private int progress;
    private int maxProgress = 200;


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
        return new CrusherScreenHandler(syncId, inv, this, this.propertyDelegate);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public void writeNbt(NbtCompound nbt){
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("crusher.progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt){
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt("crusher.progress");
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, CrusherEntity crusherEntity) {
        if(world.isClient)return;

        if(hasRecipe(crusherEntity)){
            crusherEntity.progress++;
            markDirty(world, blockPos, blockState);
            if(crusherEntity.progress >= crusherEntity.maxProgress){
                craft(crusherEntity);
                markDirty(world, blockPos, blockState);
            }
        }else{
            crusherEntity.progress = 0;
            markDirty(world,blockPos,blockState);
        }
    }

    private static void craft(CrusherEntity entity) {
        SimpleInventory inventory = new SimpleInventory(entity.size());
        for(int i = 0; i< entity.size(); i++){
            inventory.setStack(i, entity.getStack(i));
        }

        if(hasRecipe(entity)){
            entity.removeStack(0, 1);
            entity.setStack(1, new ItemStack(ModdedItems.Items.get("coal_dust"), entity.getStack(1).getCount()+1));
            entity.progress = 0;
        }
    }

    private static boolean hasRecipe(CrusherEntity entity) {
        SimpleInventory inventory = new SimpleInventory(entity.size());
        for(int i = 0; i< entity.size(); i++){
            inventory.setStack(i, entity.getStack(i));
        }

        boolean hasInput = entity.getStack(0).getItem() == Items.COAL;

        return hasInput&&canOutput(inventory, ModdedItems.Items.get("coal_dust"),1);
    }

    private static boolean canOutput(SimpleInventory inventory, Item outputType, int count){
        return (inventory.getStack(1).getItem()==outputType || inventory.getStack(1).isEmpty())
                &&inventory.getStack(1).getMaxCount() > inventory.getStack(1).getCount() + count;
    }


}
