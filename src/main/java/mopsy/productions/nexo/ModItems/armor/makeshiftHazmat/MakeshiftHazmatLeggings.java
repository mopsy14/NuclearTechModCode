package mopsy.productions.nexo.ModItems.armor.makeshiftHazmat;

import mopsy.productions.nexo.ModItems.armor.ModdedArmorMaterials;
import mopsy.productions.nexo.interfaces.IArmorRadiationProtection;
import mopsy.productions.nexo.interfaces.IModID;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;

import static mopsy.productions.nexo.Main.CREATIVE_TAB_KEY;

public class MakeshiftHazmatLeggings extends ArmorItem implements IModID, IArmorRadiationProtection {
    @Override public String getID() {return "makeshift_hazmat_leggings";}

    public MakeshiftHazmatLeggings() {
        super(ModdedArmorMaterials.MAKESHIFT_HAZMAT, Type.LEGGINGS, new Settings());
        ItemGroupEvents.modifyEntriesEvent(CREATIVE_TAB_KEY).register(entries -> entries.add(this));
    }

    @Override
    public float getRadiationProtection() {
        return 0.1f;
    }
}
