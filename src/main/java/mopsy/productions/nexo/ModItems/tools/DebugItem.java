package mopsy.productions.nexo.ModItems.tools;

import mopsy.productions.nexo.interfaces.IItemRadiation;
import mopsy.productions.nexo.interfaces.IModID;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import static mopsy.productions.nexo.Main.CREATIVE_TOOLS_TAB_KEY;
import static mopsy.productions.nexo.Main.modid;

public class DebugItem extends Item implements IModID, IItemRadiation {
    @Override
    public String getID() {
        return "debug_item";
    }
    public DebugItem() {
        super(new Settings().rarity(Rarity.EPIC)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(modid,"debug_item"))));
        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TOOLS_TAB_KEY).register(entries -> entries.add(this));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public float getRadiation() {
        return -10;
    }

    @Override
    public float getHeat() {
        return 0;
    }

    @Override
    public boolean hasHeat() {
        return false;
    }
}
