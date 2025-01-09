package mopsy.productions.nexo.ModBlocks.blocks.transport;

import com.mojang.serialization.MapCodec;
import mopsy.productions.nexo.ModBlocks.entities.transport.FluidPipe_MK1Entity;
import mopsy.productions.nexo.interfaces.IModID;
import mopsy.productions.nexo.registry.ModdedBlockEntities;
import mopsy.productions.nexo.registry.ModdedItems;
import mopsy.productions.nexo.util.NEXORotation;
import mopsy.productions.nexo.util.PipeEndState;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import static net.minecraft.state.property.Properties.WATERLOGGED;

@SuppressWarnings("deprecation")
public class FluidPipe_MK1Block extends BlockWithEntity implements IModID, BlockEntityProvider, Waterloggable {
    private static final VoxelShape MID_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);
    private static final VoxelShape UP_SHAPE = VoxelShapes.cuboid(0.375, 0.625, 0.375, 0.625, 1, 0.625);
    private static final VoxelShape DOWN_SHAPE = VoxelShapes.cuboid(0.375, 0, 0.375, 0.625, 0.375, 0.625);
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0, 0.625, 0.625, 0.375);
    private static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(0.625, 0.375, 0.375, 1, 0.625, 0.625);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(0.375, 0.375, 0.625, 0.625, 0.625, 1);
    private static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(0, 0.375, 0.375, 0.375, 0.625, 0.625);

    private static final VoxelShape UP_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.375, 0.625, 0.375, 0.625, 0.875, 0.625),
            VoxelShapes.cuboid(0.3125, 0.875, 0.3125, 0.6875, 0.9375, 0.6875),
            VoxelShapes.cuboid(0.25, 0.9375, 0.25, 0.75, 1, 0.75));
    private static final VoxelShape DOWN_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.375, 0.125, 0.375, 0.625, 0.375, 0.625),
            VoxelShapes.cuboid(0.3125, 0.0625, 0.3125, 0.6875, 0.125, 0.6875),
            VoxelShapes.cuboid(0.25, 0, 0.25, 0.75, 0.0625, 0.75));
    private static final VoxelShape NORTH_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.375, 0.375, 0.125, 0.625, 0.625, 0.375),
            VoxelShapes.cuboid(0.3125, 0.3125, 0.0625, 0.6875, 0.6875, 0.125),
            VoxelShapes.cuboid(0.25, 0.25, 0, 0.75, 0.75, 0.0625));
    private static final VoxelShape EAST_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.625, 0.375, 0.375, 0.875, 0.625, 0.625),
            VoxelShapes.cuboid(0.875, 0.3125, 0.3125, 0.9375, 0.6875, 0.6875),
            VoxelShapes.cuboid(0.9375, 0.25, 0.25, 1, 0.75, 0.75));
    private static final VoxelShape SOUTH_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.375, 0.375, 0.625, 0.625, 0.625, 0.875),
            VoxelShapes.cuboid(0.3125, 0.3125, 0.875, 0.6875, 0.6875, 0.9375),
            VoxelShapes.cuboid(0.25, 0.25, 0.9375, 0.75, 0.75, 1));
    private static final VoxelShape WEST_END_SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0.125, 0.375, 0.375, 0.375, 0.625, 0.625),
            VoxelShapes.cuboid(0.0625, 0.3125, 0.3125, 0.125, 0.6875, 0.6875),
            VoxelShapes.cuboid(0, 0.25, 0.25, 0.0625, 0.75, 0.75));


    @Override
    public String getID(){return "fluid_pipe_mk1";}

    public FluidPipe_MK1Block() {
        super(Settings.create()
                .strength(5.0F, 5.0F)
                .sounds(BlockSoundGroup.COPPER)
                .requiresTool()
                .nonOpaque()
                .mapColor(MapColor.GRAY)
        );
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED,false));
    }
    public FluidPipe_MK1Block(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED,false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(FluidPipe_MK1Block::new);
    }

    private FluidPipe_MK1Entity getBlockEntity(World world, BlockPos pos){
        BlockEntity entity = world.getBlockEntity(pos);
        return entity instanceof FluidPipe_MK1Entity? (FluidPipe_MK1Entity) entity : null;
    }
    private FluidPipe_MK1Entity getBlockEntity(BlockView world, BlockPos pos){
        BlockEntity entity = world.getBlockEntity(pos);
        return entity instanceof FluidPipe_MK1Entity? (FluidPipe_MK1Entity) entity : null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape res = MID_SHAPE;
        FluidPipe_MK1Entity entity = getBlockEntity(world,pos);
        if(entity!=null) {
            //PIPE
            if (entity.endStates.get(NEXORotation.UP).isPipe())
                res = VoxelShapes.union(res, UP_SHAPE);
            if (entity.endStates.get(NEXORotation.DOWN).isPipe())
                res = VoxelShapes.union(res, DOWN_SHAPE);
            if (entity.endStates.get(NEXORotation.NORTH).isPipe())
                res = VoxelShapes.union(res, NORTH_SHAPE);
            if (entity.endStates.get(NEXORotation.EAST).isPipe())
                res = VoxelShapes.union(res, EAST_SHAPE);
            if (entity.endStates.get(NEXORotation.SOUTH).isPipe())
                res = VoxelShapes.union(res, SOUTH_SHAPE);
            if (entity.endStates.get(NEXORotation.WEST).isPipe())
                res = VoxelShapes.union(res, WEST_SHAPE);
            //END
            if (entity.endStates.get(NEXORotation.UP).isIO())
                res = VoxelShapes.union(res, UP_END_SHAPE);
            if (entity.endStates.get(NEXORotation.DOWN).isIO())
                res = VoxelShapes.union(res, DOWN_END_SHAPE);
            if (entity.endStates.get(NEXORotation.NORTH).isIO())
                res = VoxelShapes.union(res, NORTH_END_SHAPE);
            if (entity.endStates.get(NEXORotation.EAST).isIO())
                res = VoxelShapes.union(res, EAST_END_SHAPE);
            if (entity.endStates.get(NEXORotation.SOUTH).isIO())
                res = VoxelShapes.union(res, SOUTH_END_SHAPE);
            if (entity.endStates.get(NEXORotation.WEST).isIO())
                res = VoxelShapes.union(res, WEST_END_SHAPE);
        }
        return res;
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModdedBlockEntities.FLUID_PIPE_MK1, FluidPipe_MK1Entity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1f;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
    }


    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        if(neighborState.isOf(this))
            getBlockEntity(world,pos).endStates.put(NEXORotation.ofDirection(direction),PipeEndState.PIPE);
        else if(isFluidBlock(world, pos, direction)) {
            if(getBlockEntity(world,pos).endStates.get(NEXORotation.ofDirection(direction)).isNone())
                getBlockEntity(world, pos).endStates.put(NEXORotation.ofDirection(direction), PipeEndState.OUT);
        }else
            getBlockEntity(world,pos).endStates.put(NEXORotation.ofDirection(direction),PipeEndState.NONE);

        return state;
    }
    private boolean isEnergyBlock(WorldAccess world, BlockPos pos, Direction direction){
        return EnergyStorage.SIDED.find((World)world, pos, direction) != null;
    }
    private boolean isFluidBlock(WorldView world, BlockPos pos, Direction direction){
        return FluidStorage.SIDED.find((World) world, pos.add(direction.getVector()), direction)!=null;
    }
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipe_MK1Entity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        for (NEXORotation rotation : NEXORotation.values()){
            if(world.getBlockState(pos.add(rotation.transformToVec3i())).isOf(this))
                getBlockEntity(world, pos).endStates.put(rotation,PipeEndState.PIPE);
            else if(isFluidBlock(world, pos, rotation.direction))
                getBlockEntity(world, pos).endStates.put(rotation,PipeEndState.OUT);
            else
                getBlockEntity(world, pos).endStates.put(rotation,PipeEndState.NONE);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            if (player.getMainHandStack().getItem().equals(ModdedItems.Items.get("pipe_wrench"))
                    ||player.getOffHandStack().getItem().equals(ModdedItems.Items.get("pipe_wrench"))) {
                NamedScreenHandlerFactory screenHandlerFactory = getBlockEntity(world, pos);
                if(screenHandlerFactory != null){
                    player.openHandledScreen(screenHandlerFactory);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
}
