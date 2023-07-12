package mopsy.productions.nucleartech.ModBlocks.blocks.ores;

import mopsy.productions.nucleartech.interfaces.IModID;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.block.OreBlock;
import net.minecraft.sound.BlockSoundGroup;

public class VanadiumOreBlock extends OreBlock implements IModID {
    @Override
    public String getID(){return "vanadium_ore";}
    public VanadiumOreBlock() {
        super(FabricBlockSettings
                        .of(Material.STONE, MapColor.GRAY)
                        .strength(5.0F, 5.0F)
                        .sounds(BlockSoundGroup.STONE)
                        .requiresTool()
        );
    }
}
