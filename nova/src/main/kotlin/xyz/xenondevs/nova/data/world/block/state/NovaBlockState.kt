package xyz.xenondevs.nova.data.world.block.state

import org.bukkit.Location
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.property.BlockProperty
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.material.NovaBlock
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

@Suppress("CanBePrimaryConstructorProperty", "UNCHECKED_CAST")
open class NovaBlockState(override val pos: BlockPos, material: NovaBlock) : BlockState() {
    
    override val id = material.id
    open val material = material
    val modelProvider by lazy { material.block.modelProviderType.create(this) }
    internal val properties = material.properties.associateWithTo(LinkedHashMap(), BlockPropertyType<*>::create)
    
    val location: Location
        get() = pos.location
    
    @Volatile
    final override var isLoaded = false
        private set
    
    constructor(material: NovaBlock, ctx: BlockPlaceContext) : this(ctx.pos, material) {
        properties.values.forEach { it.init(ctx) }
    }
    
    fun <T : BlockProperty> getProperty(type: BlockPropertyType<T>): T? =
        properties[type] as? T
    
    fun <T : BlockProperty> getProperty(clazz: KClass<T>): T? =
        properties.values.firstOrNull { it::class == clazz || clazz in it::class.superclasses } as T?
    
    override fun handleInitialized(placed: Boolean) {
        modelProvider.load(placed)
        
        material.multiBlockLoader?.invoke(pos)?.forEach {
            WorldDataManager.setBlockState(it, LinkedBlockState(it, this))
        }
        
        isLoaded = true
    }
    
    override fun handleRemoved(broken: Boolean) {
        isLoaded = false
        
        if (broken) {
            material.multiBlockLoader?.invoke(pos)?.forEach { BlockManager.removeLinkedBlock(BlockBreakContext(it), breakEffects = true) }
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