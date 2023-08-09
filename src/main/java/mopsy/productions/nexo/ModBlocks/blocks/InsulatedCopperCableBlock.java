package mopsy.productions.nexo.ModBlocks.blocks;

import mopsy.productions.nexo.CableNetworks.CNetwork;
import mopsy.productions.nexo.CableNetworks.CNetworkID;
import mopsy.productions.nexo.CableNetworks.CNetworkManager;
import mopsy.productions.nexo.ModBlocks.entities.InsulatedCopperCableEntity;
import mopsy.productions.nexo.interfaces.IModID;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

@SuppressWarnings("deprecation")
public class InsulatedCopperCableBlock extends BlockWithEntity implements IModID {
    @Override
    public String getID(){return "insulated_copper_cable";}

    public InsulatedCopperCableBlock() {
        super(FabricBlockSettings
                        .of(Material.METAL, MapColor.GRAY)
                        .strength(5.0F, 5.0F)
                        .sounds(BlockSoundGroup.COPPER)
                        .requiresTool()
        );
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InsulatedCopperCableEntity(pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if(!state.isOf(newState.getBlock())){
            if(getConnectedCables(world,pos).size()==0)
                CNetworkManager.networks.get(world).remove(CNetworkManager.getNetworkIndexFromID(((InsulatedCopperCableEntity)world.getBlockEntity(pos)).networkID));
            else
                createCNetworksFromCables(world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if(world.isClient())return;

        List<InsulatedCopperCableEntity> connectedCables = getConnectedCables(world, pos);
        if(connectedCables.size()==0)
            ((InsulatedCopperCableEntity)world.getBlockEntity(pos)).networkID = CNetworkManager.createNetworkFromCable((InsulatedCopperCableEntity)world.getBlockEntity(pos)).id;
        else if(connectedCables.size()==1) {
            ((InsulatedCopperCableEntity) world.getBlockEntity(pos)).networkID = connectedCables.get(0).networkID;
            CNetworkManager.getNetworkFromID(connectedCables.get(0).networkID).cableEntities.put(pos,((InsulatedCopperCableEntity) world.getBlockEntity(pos)));
        } else {
            Set<CNetworkID> connectedIDs = getAllConnectedNetworkIDs(world, connectedCables);
            if(connectedIDs.size()==1)
                ((InsulatedCopperCableEntity)world.getBlockEntity(pos)).networkID = connectedCables.get(0).networkID;
            else{
                CNetworkManager.mergeNetworks(networkIDsToNetworks(connectedIDs),true);
            }
        }
        //Probable best way of checking block type   world.getBlockState(pos).getBlock() instanceof InsulatedCopperCableBlock;
    }

    private Set<CNetworkID> getAllConnectedNetworkIDs(World world, List<InsulatedCopperCableEntity> connectedCables){
        Set<CNetworkID> res = new HashSet<>();
        for (InsulatedCopperCableEntity cable : connectedCables)
            res.add(cable.networkID);
        return res;
    }
    private List<CNetwork> networkIDsToNetworks(Set<CNetworkID> IDs){
        List<CNetwork> res = new ArrayList<>(IDs.size());
        for(CNetworkID id : IDs)
            res.add(CNetworkManager.getNetworkFromID(id));
        return res;
    }

    private List<InsulatedCopperCableEntity> getConnectedCables(World world, BlockPos pos){
        List<InsulatedCopperCableEntity> res = new ArrayList<>(6);
        if(world.getBlockEntity(pos.up())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.up()));
        if(world.getBlockEntity(pos.down())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.down()));
        if(world.getBlockEntity(pos.north())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.north()));
        if(world.getBlockEntity(pos.east())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.east()));
        if(world.getBlockEntity(pos.south())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.south()));
        if(world.getBlockEntity(pos.west())instanceof InsulatedCopperCableEntity)
            res.add((InsulatedCopperCableEntity) world.getBlockEntity(pos.west()));
        return res;
    }

    public void createCNetworksFromCables(World world, BlockPos pos){
        InsulatedCopperCableEntity entity = (InsulatedCopperCableEntity)world.getBlockEntity(pos);
        if(entity!=null && entity.networkID==null){
            //world.getBlock
        }
    }


    private Set<BlockPos> getSurroundingBlocks(World world, BlockPos pos, BiPredicate<World,BlockPos> condition){
        Set<BlockPos> blocks = new HashSet<>();
        if(condition.test(world,pos.up()))
            blocks.add(pos.up());
        if(condition.test(world,pos.down()))
            blocks.add(pos.down());
        if(condition.test(world,pos.north()))
            blocks.add(pos.north());
        if(condition.test(world,pos.east()))
            blocks.add(pos.east());
        if(condition.test(world,pos.south()))
            blocks.add(pos.south());
        if(condition.test(world,pos.west()))
            blocks.add(pos.west());

        return blocks;
    }
}
