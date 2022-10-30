package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.buffer.ByteBuffer
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.property.BlockProperty
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.data.world.block.property.LegacyDirectional
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses
import xyz.xenondevs.nova.api.block.NovaBlockState as INovaBlockState

@Suppress("CanBePrimaryConstructorProperty", "UNCHECKED_CAST")
open class NovaBlockState(override val pos: BlockPos, material: BlockNovaMaterial) : BlockState, INovaBlockState {
    
    override val id = material.id
    override val material = material
    val modelProvider by lazy { material.block.modelProviderType.create(this) }
    internal val properties = material.properties.associateWithTo(LinkedHashMap(), BlockPropertyType<*>::create)
    
    override val location: Location
        get() = pos.location
    
    @Volatile
    final override var isLoaded = false
        private set
    
    constructor(material: BlockNovaMaterial, ctx: BlockPlaceContext) : this(ctx.pos, material) {
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
            material.multiBlockLoader?.invoke(pos)?.forEach { BlockManager.removeLinkedBlock(BlockBreakContext(it)) }
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
    
    //<editor-fold desc="Legacy" defaultstate="collapsed">
    
    internal fun readPropertiesLegacy(buf: ByteBuf) {
        properties.forEach { (_, property) ->
            if (property is LegacyDirectional) {
                buf.readByte()
            } else if (property is Directional) {
                property.facing = BlockFace.values()[buf.readByte().toInt()]
            }
        }
    }
    
    //</editor-fold>
}