package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LayeredCauldronBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.set
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.FluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.VanillaFluidHolder
import java.util.*
import kotlin.math.roundToInt

private val ALLOWED_FLUID_TYPES = setOf(FluidType.WATER, FluidType.LAVA)

internal class VanillaCauldronTileEntity internal constructor(
    private val cauldronEntity: VanillaCauldronBlockEntity
) : NetworkedVanillaTileEntity(cauldronEntity) {
    
    private var currentBlockState = cauldronEntity.blockState
    
    private var newBlockState: BlockState? = null
        set(value) {
            field = if (value == currentBlockState) null else value
        }
    
    private var skipContainerUpdate = false
    
    private val container = FluidContainer(
        UUID(0L, 0L),
        ALLOWED_FLUID_TYPES,
        provider(1000L),
        mutableProvider(::getFluidType, ::setFluidType),
        mutableProvider(::getFluidAmount, ::setFluidAmount)
    )
    
    private val fluidHolder = VanillaFluidHolder(
        cauldronEntity,
        mapOf(container to NetworkConnectionType.BUFFER),
        CubeFaceMap(container)
    )
    
    override val holders: Set<EndPointDataHolder> = setOf(fluidHolder)
    
    internal fun handleBlockStateChange(blockState: BlockState) {
        currentBlockState = blockState
        if (skipContainerUpdate)
            return
        
        container.typeProvider.set(getFluidType())
        container.amountProvider.set(getFluidAmount())
        newBlockState = null
    }
    
    internal fun postNetworkTickSync() {
        val newBlockState = newBlockState ?: return
        val level = cauldronEntity.level ?: return
        skipContainerUpdate = true
        try {
            if (level.setBlock(cauldronEntity.blockPos, newBlockState, Block.UPDATE_ALL)) {
                currentBlockState = newBlockState
                this.newBlockState = null
            }
        } finally {
            skipContainerUpdate = false
        }
    }
    
    private fun getFluidType(): FluidType? {
        return when (currentBlockState.block) {
            Blocks.LAVA_CAULDRON -> FluidType.LAVA
            Blocks.WATER_CAULDRON -> FluidType.WATER
            else -> null
        }
    }
    
    private fun setFluidType(fluidType: FluidType?) {
        newBlockState = when (fluidType) {
            FluidType.LAVA -> Blocks.LAVA_CAULDRON
            FluidType.WATER -> Blocks.WATER_CAULDRON
            null -> Blocks.CAULDRON
        }.defaultBlockState()
    }
    
    private fun getFluidAmount(): Long {
        return when (currentBlockState.block) {
            Blocks.WATER_CAULDRON -> currentBlockState.getValue(LayeredCauldronBlock.LEVEL) * 333L + 1
            Blocks.LAVA_CAULDRON -> 1000L
            else -> 0L
        }
    }
    
    private fun setFluidAmount(amount: Long) {
        if ((newBlockState ?: currentBlockState).block != Blocks.WATER_CAULDRON)
            return
        
        newBlockState = when (val level = (amount / 333.0).roundToInt()) {
            0 -> Blocks.CAULDRON.defaultBlockState()
            else -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level)
        }
    }
    
    @InternalInit(stage = InternalInitStage.PRE_WORLD)
    companion object {
        
        @JvmStatic
        lateinit var type: BlockEntityType<VanillaCauldronBlockEntity>
            private set
        
        @InitFun
        private fun register() {
            type = BlockEntityType(
                ::VanillaCauldronBlockEntity,
                setOf(Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON)
            )
            Registries.BLOCK_ENTITY_TYPE[Identifier.fromNamespaceAndPath("nova", "cauldron")] = type
        }
        
    }
    
}

internal class VanillaCauldronBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(VanillaCauldronTileEntity.type, pos, state)
