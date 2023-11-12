package mopsy.productions.nexo.ModBlocks.blocks.transport;

import mopsy.productions.nexo.ModBlocks.entities.transport.FluidPipe_MK1Entity;
import mopsy.productions.nexo.interfaces.IModID;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import static mopsy.productions.nexo.util.NEXOStateProperties.*;
import static net.minecraft.state.property.Properties.*;

@SuppressWarnings("deprecation")
public class FluidPipe_MK1Block extends BlockWithEntity implements IModID, BlockEntityProvider, Waterloggable {
    private static final VoxelShape MID_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);
    private static final VoxelShape UP_SHAPE = VoxelShapes.cuboid(0.375, 0.625, 0.375, 0.625, 1, 0.625);
    private static final VoxelShape DOWN_SHAPE = VoxelShapes.cuboid(0.375, 0, 0.375, 0.625, 0.375, 0.625);
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0, 0.625, 0.625, 0.375);
    private static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(0.625, 0.375, 0.375, 1, 0.625, 0.625);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0.625, 0.625, 0.625, 1);
    private static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(0, 0.375, 0.375, 0.375, 0.625, 0.625);
    @Override
    public String getID(){return "fluid_pipe_mk1";}

    public FluidPipe_MK1Block() {
        super(FabricBlockSettings
                        .of(Material.METAL, MapColor.GRAY)
                        .strength(5.0F, 5.0F)
                        .sounds(BlockSoundGroup.COPPER)
                        .requiresTool()
                        .nonOpaque()
        );
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(UP, false).with(UP_IS_IN, false)
                .with(DOWN, false).with(DOWN_IS_IN, false)
                .with(NORTH, false).with(NORTH_IS_IN, false)
                .with(EAST, false).with(EAST_IS_IN, false)
                .with(SOUTH, false).with(SOUTH_IS_IN, false)
                .with(WEST, false).with(WEST_IS_IN, false)
                .with(WATERLOGGED,false)
        );
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape res = MID_SHAPE;
        if(state.get(UP))
            res = VoxelShapes.union(res, UP_SHAPE);
        if(state.get(DOWN))
            res = VoxelShapes.union(res, DOWN_SHAPE);
        if(state.get(NORTH))
            res = VoxelShapes.union(res, NORTH_SHAPE);
        if(state.get(EAST))
            res = VoxelShapes.union(res, EAST_SHAPE);
        if(state.get(SOUTH))
            res = VoxelShapes.union(res, SOUTH_SHAPE);
        if(state.get(WEST))
            res = VoxelShapes.union(res, WEST_SHAPE);

        return res;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModdedBlockEntities.FLUID_PIPE_MK1, FluidPipe_MK1Entity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1f;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState res = this.getDefaultState().with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().up(),Direction.UP))
            res = res.with(UP,true);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().down(),Direction.DOWN))
            res = res.with(DOWN,true);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().north(),Direction.NORTH))
            res = res.with(NORTH,true);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().east(),Direction.EAST))
            res = res.with(EAST,true);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().south(),Direction.SOUTH))
            res = res.with(SOUTH,true);
        if(isEnergyBlock(ctx.getWorld(),ctx.getBlockPos().west(),Direction.WEST))
            res = res.with(WEST,true);
        return res;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState originalState, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (originalState.get(WATERLOGGED)) {
            world.createAndScheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        switch (direction){
            case UP -> {return originalState.with(UP, isEnergyBlock(world, neighborPos, Direction.UP));}
            case DOWN -> {return originalState.with(DOWN, isEnergyBlock(world, neighborPos, Direction.DOWN));}
            case NORTH -> {return originalState.with(NORTH, isEnergyBlock(world, neighborPos, Direction.NORTH));}
            case EAST -> {return originalState.with(EAST, isEnergyBlock(world, neighborPos, Direction.EAST));}
            case SOUTH -> {return originalState.with(SOUTH, isEnergyBlock(world, neighborPos, Direction.SOUTH));}
            case WEST -> {return originalState.with(WEST, isEnergyBlock(world, neighborPos, Direction.WEST));}
        }
        return originalState;
    }

    private boolean isEnergyBlock(World world, BlockPos pos, Direction direction){
        return EnergyStorage.SIDED.find(world, pos, direction) != null;
    }
    private boolean isEnergyBlock(WorldAccess world, BlockPos pos, Direction direction){
        return EnergyStorage.SIDED.find((World)world, pos, direction) != null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipe_MK1Entity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(UP, UP_IS_IN, DOWN, DOWN_IS_IN, NORTH, NORTH_IS_IN, EAST, EAST_IS_IN, SOUTH, SOUTH_IS_IN, WEST, WEST_IS_IN, WATERLOGGED, BooleanProperty.of("test"), BooleanProperty.of("test_2"));
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
}
