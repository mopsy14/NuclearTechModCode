package mopsy.productions.nexo.mixin;

import mopsy.productions.nexo.ModItems.blocks.BatteryMK1Item;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.reborn.energy.api.base.SimpleEnergyItem;

@Mixin(DrawContext.class)
public abstract class RenderGuiItemOverlayMixin {
    //@Shadow public abstract void fill(RenderLayer layer, int x1, int y1, int x2, int y2, int color);

    @Shadow public abstract void fill(RenderLayer layer, int x1, int y1, int x2, int y2, int z, int color);

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V", at = @At("RETURN"))
    protected void injectIntoRenderItemOverlay(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci){
        if (stack.getItem() instanceof BatteryMK1Item batteryItem) {
            int coloredBarPixels = getScaledPower(batteryItem, stack);
            //fill(RenderLayer.getGui(),x + 2, y + 13, x + 14, y + 15, 0x191919FF);
            //fill(RenderLayer.getGui(),x + 2, y + 13, x+2+coloredBarPixels, y+14, 0xFFF000FF);
            int i = x + 2;
            int j = y + 13;
            this.fill(RenderLayer.getGuiOverlay(), i, j, i + 13, j + 2, 200, 0x191919FF);
            this.fill(RenderLayer.getGuiOverlay(), i, j, i + coloredBarPixels, j + 1, 200, 0xFFF000FF);
        }
    }
    @Unique
    private static int getScaledPower(SimpleEnergyItem energyItem, ItemStack itemStack){
        return (int)(energyItem.getEnergyCapacity(itemStack)!=0 && energyItem.getStoredEnergy(itemStack)!=0 ? energyItem.getStoredEnergy(itemStack)*12/energyItem.getEnergyCapacity(itemStack) : 0);
    }
}
