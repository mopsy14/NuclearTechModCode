package mopsy.productions.nucleartech.registry;

import mopsy.productions.nucleartech.ModBlocks.entities.machines.CrusherEntity;
import mopsy.productions.nucleartech.ModBlocks.entities.machines.PressEntity;
import mopsy.productions.nucleartech.ModBlocks.entities.machines.TankEntity_MK1;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import team.reborn.energy.api.EnergyStorage;

import static mopsy.productions.nucleartech.Main.modid;

public class ModdedBlockEntities {

    public static BlockEntityType<CrusherEntity> CRUSHER;
    public static BlockEntityType<TankEntity_MK1> TANK_MK1;
    public static BlockEntityType<PressEntity> PRESS;

    public static void regBlockEntities() {
        CRUSHER = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(modid, "crusher"),
                FabricBlockEntityTypeBuilder.create(CrusherEntity::new, ModdedBlocks.Blocks.get("crusher")).build(null));

        TANK_MK1 = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(modid, "tank_mk1"),
                FabricBlockEntityTypeBuilder.create(TankEntity_MK1::new, ModdedBlocks.Blocks.get("tank_mk1")).build(null));

        PRESS = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(modid, "press"),
                FabricBlockEntityTypeBuilder.create(PressEntity::new, ModdedBlocks.Blocks.get("press")).build(null));


        //Power
        EnergyStorage.SIDED.registerForBlockEntity((entity, direction) -> entity.energyStorage, CRUSHER);
        EnergyStorage.SIDED.registerForBlockEntity((entity, direction) -> entity.energyStorage, PRESS);
        //Fluids
        FluidStorage.SIDED.registerForBlockEntity(((entity, direction) -> entity.fluidStorage), TANK_MK1);
    }
}
