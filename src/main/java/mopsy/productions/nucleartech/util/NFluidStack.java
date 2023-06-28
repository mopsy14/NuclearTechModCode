package mopsy.productions.nucleartech.util;

import com.google.gson.JsonElement;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@SuppressWarnings("UnstableApiUsage")
public class NFluidStack {
    public FluidVariant fluidVariant;
    public long fluidAmount;

    public NFluidStack(FluidVariant variant, long amount){
        fluidVariant = variant;
        fluidAmount = amount;
    }
    public static NFluidStack fromNBT(NbtCompound nbtCompound){
        return new NFluidStack(
                FluidVariant.fromNbt(nbtCompound.getCompound("fluid_type")),
                nbtCompound.getLong("fluid_amount")
        );
    }
    public static NFluidStack fromJson(JsonElement jsonElement){
        return new NFluidStack(
                FluidVariant.of(Registry.FLUID.get(Identifier.tryParse(jsonElement.getAsJsonObject().get("fluid_type").getAsString()))),
                jsonElement.getAsJsonObject().get("fluid_amount").getAsLong()
        );
    }
    public static NFluidStack fromPacket(PacketByteBuf buf){
        return new NFluidStack(
                FluidVariant.fromPacket(buf),
                buf.readLong()
        );
    }
    public static NbtCompound toNBT(NFluidStack fluidStack, NbtCompound nbtCompound){
        nbtCompound.put("fluid_type",  fluidStack.fluidVariant.toNbt());
        nbtCompound.putLong("fluid_amount", fluidStack.fluidAmount);

        return nbtCompound;
    }
    public static JsonElement toJson(NFluidStack fluidStack, JsonElement jsonElement){
        jsonElement.getAsJsonObject().addProperty("fluid_type", Registry.FLUID.getId(fluidStack.fluidVariant.getFluid()).toString());
        jsonElement.getAsJsonObject().addProperty("fluid_amount", fluidStack.fluidAmount);

        return jsonElement;
    }
    public static PacketByteBuf toBuf(NFluidStack fluidStack, PacketByteBuf buf){
        fluidStack.fluidVariant.toPacket(buf);
        buf.writeLong(fluidStack.fluidAmount);

        return buf;
    }
}
