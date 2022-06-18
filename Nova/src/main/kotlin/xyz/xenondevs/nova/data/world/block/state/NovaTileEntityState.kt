package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.CBF
import xyz.xenondevs.nova.data.serialization.cbf.Compound
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntity.Companion.TILE_ENTITY_KEY
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
    lateinit var data: Compound
    
    private var _tileEntity: TileEntity? = null
    
    val tileEntity: TileEntity
        get() = _tileEntity ?: throw IllegalStateException("TileEntity is not initialized")
    
    val isInitialized: Boolean
        get() = _tileEntity != null
    
    constructor(pos: BlockPos, material: TileEntityNovaMaterial) : super(pos, material) {
        this.material = material
    }
    
    constructor(material: TileEntityNovaMaterial, ctx: BlockPlaceContext) : super(material, ctx) {
        this.material = material
        this.uuid = UUID.randomUUID()
        this.ownerUUID = ctx.ownerUUID
        this.data = Compound()
        
        val globalData = ctx.item.itemMeta?.persistentDataContainer?.get<Compound>(TILE_ENTITY_KEY)
        if (globalData != null) data["global"] = globalData
    }
    
    override fun handleInitialized(placed: Boolean) {
        super.handleInitialized(placed)
        
        _tileEntity = material.tileEntityConstructor(this)
        tileEntity.handleInitialized(placed)
        
        TileEntityManager.registerTileEntity(this)
    }
    
    override fun handleRemoved(broken: Boolean) {
        super.handleRemoved(broken)
        tileEntity.saveData()
        tileEntity.handleRemoved(!broken)
        TileEntityManager.unregisterTileEntity(this)
        _tileEntity = null
    }
    
    override fun read(buf: ByteBuf) {
        super.read(buf)
        uuid = buf.readUUID()
        ownerUUID = buf.readUUID()
        data = CBF.read(buf)!!
    }
    
    override fun write(buf: ByteBuf) {
        super.write(buf)
        buf.writeUUID(uuid)
        buf.writeUUID(ownerUUID)
        CBF.write(data, buf)
    }
    
    override fun toString(): String {
        return "NovaTileEntityState(pos=$pos, id=$id, uuid=$uuid, ownerUUID=$ownerUUID, data=$data)"
    }
    
}