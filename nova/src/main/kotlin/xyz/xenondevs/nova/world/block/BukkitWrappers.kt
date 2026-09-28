package xyz.xenondevs.nova.world.block

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.block.CraftBlockEntityState
import org.bukkit.craftbukkit.block.data.CraftBlockData
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty

internal val NovaBlockState.novaBlock: NovaBlock
    get() = blockType.novaBlock!!

sealed interface NovaBlockState : BlockData {
    
    operator fun <T : Comparable<T>> set(property: BlockStateProperty<T>, value: T)
    
    operator fun <T : Comparable<T>> get(property: BlockStateProperty<T>): T?
    
    fun <T : Comparable<T>> getOrThrow(property: BlockStateProperty<T>): T
    
    override fun clone(): NovaBlockState
    
}

internal class NovaBlockStateImpl internal constructor(state: BlockState) : CraftBlockData(state), NovaBlockState {
    
    override fun <T : Comparable<T>> set(property: BlockStateProperty<T>, value: T) {
        property.set(this, value)
    }
    
    override fun <T : Comparable<T>> get(property: BlockStateProperty<T>): T? =
        property.get(state)
    
    override fun <T : Comparable<T>> getOrThrow(property: BlockStateProperty<T>): T =
        property.get(state) ?: throw NoSuchElementException("Property $property not present")
    
    override fun clone() = NovaBlockStateImpl(state)
    
}

internal class NovaCapturedBlockEntityState : CraftBlockEntityState<NovaTileEntityProxy> {
    constructor(world: World?, blockEntity: NovaTileEntityProxy) : super(world, blockEntity)
    private constructor(state: NovaCapturedBlockEntityState, location: Location?) : super(state, location)
    
    override fun copy() = NovaCapturedBlockEntityState(this, null)
    override fun copy(location: Location) = NovaCapturedBlockEntityState(this, location)
}