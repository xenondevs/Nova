package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LayeredCauldronBlock
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.setBlockState
import xyz.xenondevs.nova.util.withoutBlockMigration
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.FluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.DefaultFluidHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import java.util.*
import kotlin.math.roundToInt

private val ALLOWED_FLUID_TYPES = hashSetOf(FluidType.WATER, FluidType.LAVA)

internal class VanillaCauldronTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : NetworkedVanillaTileEntity(type, pos, data) {
    
    private lateinit var container: FluidContainer
    private lateinit var fluidHolder: FluidHolder
    override lateinit var holders: Set<EndPointDataHolder>
    
    @Volatile
    private lateinit var currentBlockState: BlockState
    
    @Volatile
    private var newBlockState: BlockState? = null
        set(value) {
            if (value == currentBlockState)
                return
            field = value
        }
    
    override fun handleEnable() {
        // legacy conversion
        DefaultFluidHolder.tryConvertLegacy(this)?.let {
            it["containerConfig"] = null // previously used tileentity uuid, now 0
            storeData("fluidHolder", it)
        }
        container = FluidContainer(
            UUID(0L, 0L),
            ALLOWED_FLUID_TYPES,
            provider(1000L),
            mutableProvider(::getFluidType, ::setFluidType),
            mutableProvider(::getFluidAmount, ::setFluidAmount)
        )
        fluidHolder = DefaultFluidHolder(
            storedValue("fluidHolder", ::Compound),
            mapOf(container to NetworkConnectionType.BUFFER),
            emptySet(),
            { CUBE_FACES.associateWithTo(enumMap()) { container } },
            { CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.BUFFER } }
        )
        holders = setOf(fluidHolder)
        currentBlockState = pos.nmsBlockState
        
        handleBlockStateChange(currentBlockState)
        
        super.handleEnable()
    }
    
    override fun handleBlockStateChange(blockState: BlockState) {
        currentBlockState = blockState
        container.typeProvider.set(getFluidType())
        container.amountProvider.set(getFluidAmount())
    }
    
    fun postNetworkTickSync() {
        val newBlockState = newBlockState
        if (newBlockState != null) {
            withoutBlockMigration(pos) {
                pos.setBlockState(newBlockState)
            }
            
            this.currentBlockState = newBlockState
            this.newBlockState = null
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
    
    // This will allow players to cheat small amounts of fluids, but that shouldn't be a big issue.
    private fun getFluidAmount(): Long {
        if (currentBlockState.block == Blocks.WATER_CAULDRON) {
            return currentBlockState.getValue(LayeredCauldronBlock.LEVEL) * 333L + 1
        } else if (currentBlockState.block == Blocks.LAVA_CAULDRON) {
            return 1000L
        }
        
        return 0L
    }
    
    private fun setFluidAmount(amount: Long) {
        // (lava cauldron do not have levels, so we just don't to anything for them)
        if (currentBlockState.block == Blocks.WATER_CAULDRON) {
            newBlockState = when (val level = (amount / 333.0).roundToInt()) {
                0 -> Blocks.CAULDRON.defaultBlockState()
                else -> Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level)
            }
        }
    }
    
}