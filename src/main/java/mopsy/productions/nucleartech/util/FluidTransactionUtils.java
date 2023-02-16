package mopsy.productions.nucleartech.util;

import mopsy.productions.nucleartech.interfaces.IItemFluidData;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class FluidTransactionUtils {
    public static boolean tryExportFluid(Inventory inventory, int inputIndex, int outputIndex, SingleVariantStorage<FluidVariant> fluidStorage) {
        ItemStack inputStack = inventory.getStack(inputIndex);

        if (fluidStorage.amount < fluidStorage.getCapacity() && inputStack.hasNbt() && FluidDataUtils.getFluidAmount(inputStack.getNbt()) > 0) {
            if (fluidStorage.variant.isBlank()) {
                fluidStorage.variant = FluidDataUtils.getFluidType(inputStack.getNbt());
                long insertAmount = Math.min(fluidStorage.getCapacity(), FluidDataUtils.getFluidAmount(inputStack.getNbt()));
                FluidDataUtils.setFluidAmount(inputStack.getNbt(), FluidDataUtils.getFluidAmount(inputStack.getNbt()) - insertAmount);
                fluidStorage.amount += insertAmount;
                moveIToO(inventory, inputIndex, outputIndex);
                return true;
            }
            if (fluidStorage.variant.equals(FluidDataUtils.getFluidType(inputStack.getNbt()))) {
                long insertAmount = Math.min(fluidStorage.getCapacity() - fluidStorage.amount, FluidDataUtils.getFluidAmount(inputStack.getNbt()));
                FluidDataUtils.setFluidAmount(inputStack.getNbt(), FluidDataUtils.getFluidAmount(inputStack.getNbt()) - insertAmount);
                fluidStorage.amount += insertAmount;
                moveIToO(inventory, inputIndex, outputIndex);
                return true;
            }
        }
        return false;
    }
    public static boolean tryImportFluid(Inventory inventory, int inputIndex, int outputIndex, SingleVariantStorage<FluidVariant> fluidStorage) {
        ItemStack inputStack = inventory.getStack(inputIndex);

        if (fluidStorage.amount > 0 && inputStack.hasNbt()) {
            if (FluidDataUtils.getFluidType(inputStack.getNbt()).isBlank()) {
                FluidDataUtils.setFluidType(inputStack.getNbt(), fluidStorage.variant);
                long insertAmount = Math.min((((IItemFluidData) inputStack.getItem()).getMaxCapacity()), fluidStorage.amount);
                fluidStorage.amount -= insertAmount;
                if (fluidStorage.amount == 0)
                    fluidStorage.variant = FluidVariant.blank();
                FluidDataUtils.setFluidAmount(inputStack.getNbt(), insertAmount);
                moveIToO(inventory, inputIndex, outputIndex);
                return true;
            }
            if (FluidDataUtils.getFluidType(inputStack.getNbt()).equals(fluidStorage.variant)) {
                long insertAmount = Math.min((((IItemFluidData) inputStack.getItem()).getMaxCapacity() - FluidDataUtils.getFluidAmount(inputStack.getNbt())), fluidStorage.amount);
                fluidStorage.amount -= insertAmount;
                if (fluidStorage.amount == 0)
                    fluidStorage.variant = FluidVariant.blank();
                FluidDataUtils.setFluidAmount(inputStack.getNbt(), FluidDataUtils.getFluidAmount(inputStack.getNbt()) + insertAmount);
                moveIToO(inventory, inputIndex, outputIndex);
                return true;
            }
        }
        return false;
    }
    private static void moveIToO(Inventory inv, int inputSlot, int outputSlot){
        inv.setStack(outputSlot, inv.getStack(inputSlot).copy());
        inv.getStack(inputSlot).setCount(0);
    }
}
