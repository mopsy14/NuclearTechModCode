package mopsy.productions.nucleartech.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import static mopsy.productions.nucleartech.networking.PacketManager.ADVANCED_FLUID_CHANGE_PACKET;
import static mopsy.productions.nucleartech.networking.PacketManager.FLUID_CHANGE_PACKET;

public class NCFluidStorage extends SingleVariantStorage<FluidVariant> {
    long MAX_CAPACITY;
    BlockEntity blockEntity;
    boolean canInsert;
    boolean isAdvanced = false;
    int index;
    public NCFluidStorage (long capacity, BlockEntity blockEntity, boolean canInsert){
        this.MAX_CAPACITY = capacity;
        this.blockEntity = blockEntity;
        this.canInsert = canInsert;
    }
    public NCFluidStorage (long capacity, BlockEntity blockEntity, boolean canInsert, int index){
        this.MAX_CAPACITY = capacity;
        this.blockEntity = blockEntity;
        this.canInsert = canInsert;
        this.isAdvanced = true;
        this.index = index;
    }
    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return MAX_CAPACITY;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        if(!canInsert)
            return false;
        return super.canInsert(variant);
    }

    @Override
    public boolean supportsInsertion() {
        if(!canInsert)
            return false;
        return super.supportsInsertion();
    }

    @Override
    protected void onFinalCommit() {
        blockEntity.markDirty();
        if(isAdvanced)
            advancedFinalCommit();
        else
            simpleFinalCommit();
    }
    private void simpleFinalCommit(){
        if (!blockEntity.getWorld().isClient) {
            var buf = PacketByteBufs.create();
            buf.writeBlockPos(blockEntity.getPos());
            this.variant.toPacket(buf);
            buf.writeLong(this.amount);
            blockEntity.getWorld().getPlayers().forEach(player-> {
                ServerPlayNetworking.send((ServerPlayerEntity) player, FLUID_CHANGE_PACKET, buf);
            });
        }
    }
    private void advancedFinalCommit(){
        if (!blockEntity.getWorld().isClient) {
            var buf = PacketByteBufs.create();
            buf.writeBlockPos(blockEntity.getPos());
            buf.writeInt(index);
            this.variant.toPacket(buf);
            buf.writeLong(this.amount);
            blockEntity.getWorld().getPlayers().forEach(player-> {
                ServerPlayNetworking.send((ServerPlayerEntity) player, ADVANCED_FLUID_CHANGE_PACKET, buf);
            });
        }
    }
}
