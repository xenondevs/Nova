package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.util.data.writeUUID
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.*

class NovaTileEntityState : NovaBlockState {
    
    override val material: TileEntityNovaMaterial
    lateinit var uuid: UUID
    lateinit var ownerUUID: UUID
    lateinit var data: CompoundElement
    
    lateinit var tileEntity: TileEntity
    
    val isInitialized: Boolean
        get() = ::tileEntity.isInitialized
    
    constructor(pos: BlockPos, material: TileEntityNovaMaterial) : super(pos, material) {
        this.material = material
    }
    
    constructor(material: TileEntityNovaMaterial, ctx: BlockPlaceContext) : super(material, ctx) {
        this.material = material
        this.uuid = UUID.randomUUID()
        this.ownerUUID = ctx.ownerUUID
        this.data = CompoundElement()
        
        val globalData = ctx.item?.itemMeta?.persistentDataContainer?.get(TILE_ENTITY_KEY, CompoundElementDataType)
        if (globalData != null) data.putElement("global", globalData)
    }
    
    override fun handleInitialized(placed: Boolean) {
        super.handleInitialized(placed)
        
        tileEntity = material.tileEntityConstructor(this)
        tileEntity.handleInitialized(placed)
        
        TileEntityManager.registerTileEntity(this)
    }
    
    override fun handleRemoved(broken: Boolean) {
        super.handleRemoved(broken)
        tileEntity.handleRemoved(!broken)
        TileEntityManager.unregisterTileEntity(this)
    }
    
    override fun read(buf: ByteBuf) {
        super.read(buf)
        uuid = buf.readUUID()
        ownerUUID = buf.readUUID()
        data = CompoundDeserializer.read(buf)
    }
    
    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeUUID(uuid)
        buf.writeUUID(ownerUUID)
        data.write(buf)
    }
    
}