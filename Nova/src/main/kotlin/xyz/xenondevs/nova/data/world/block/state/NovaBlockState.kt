package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.property.BlockProperty
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

@Suppress("CanBePrimaryConstructorProperty")
open class NovaBlockState(override val pos: BlockPos, material: BlockNovaMaterial) : BlockState {
    
    override val id = material.id
    open val material = material
    val modelProvider by lazy { material.modelProvider.create(this) }
    private val properties = material.properties.associateWithTo(LinkedHashMap(), BlockPropertyType<*>::create)
    
    constructor(material: BlockNovaMaterial, ctx: BlockPlaceContext) : this(ctx.pos, material) {
        properties.values.forEach { it.init(ctx) }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : BlockProperty> getProperty(type: BlockPropertyType<T>): T? = properties[type] as? T
    
    override fun handleInitialized(placed: Boolean) {
        modelProvider.load(placed)
        
        material.multiBlockLoader?.invoke(pos)?.forEach {
            WorldDataManager.setBlockState(it, LinkedBlockState(it, this))
        }
    }
    
    override fun handleRemoved(broken: Boolean) {
        if (broken) {
            material.multiBlockLoader?.invoke(pos)?.forEach { BlockManager.removeLinkedBlock(BlockBreakContext(it)) }
        }
        
        modelProvider.remove(broken)
    }
    
    override fun read(buf: ByteBuf) {
        properties.values.forEach { it.read(buf) }
    }
    
    override fun write(buf: ByteBuf) {
        properties.values.forEach { it.write(buf) }
    }
    
}