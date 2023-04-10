package xyz.xenondevs.nova.data.world.block.state

import org.bukkit.Location
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.property.BlockProperty
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

@Suppress("CanBePrimaryConstructorProperty", "UNCHECKED_CAST")
open class NovaBlockState internal constructor(override val pos: BlockPos, block: NovaBlock) : BlockState() {
    
    override val id = block.id
    open val block = block
    val modelProvider by lazy { block.model.modelProviderType.create(this) }
    internal val properties = block.properties.associateWithTo(LinkedHashMap(), BlockPropertyType<*>::create)
    
    val location: Location
        get() = pos.location
    
    @Volatile
    final override var isLoaded = false
        private set
    
    internal constructor(material: NovaBlock, ctx: BlockPlaceContext) : this(ctx.pos, material) {
        properties.values.forEach { it.init(ctx) }
    }
    
    fun <T : BlockProperty> getProperty(type: BlockPropertyType<T>): T? =
        properties[type] as? T
    
    fun <T : BlockProperty> getProperty(clazz: KClass<T>): T? =
        properties.values.firstOrNull { it::class == clazz || clazz in it::class.superclasses } as T?
    
    override fun handleInitialized(placed: Boolean) {
        modelProvider.load(placed)
        
        block.multiBlockLoader?.invoke(pos)?.forEach {
            WorldDataManager.setBlockState(it, LinkedBlockState(it, this))
        }
        
        isLoaded = true
    }
    
    override fun handleRemoved(broken: Boolean) {
        isLoaded = false
        
        if (broken) {
            block.multiBlockLoader?.invoke(pos)?.forEach { BlockManager.removeLinkedBlockState(BlockBreakContext(it), breakEffects = true) }
        }
        
        modelProvider.remove(broken)
    }
    
    override fun read(buf: ByteBuffer) {
        val compound = CBF.read<Compound>(buf)!!
        properties.values.forEach { it.read(compound) }
    }
    
    override fun write(buf: ByteBuffer) {
        val compound = Compound()
        properties.values.forEach { it.write(compound) }
        CBF.write(compound, buf)
    }
    
}