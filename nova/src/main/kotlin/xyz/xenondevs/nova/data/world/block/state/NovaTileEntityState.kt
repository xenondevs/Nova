package xyz.xenondevs.nova.data.world.block.state

import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteBuffer
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.util.UUIDUtils
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.*

class NovaTileEntityState : NovaBlockState {
    
    override val material: NovaTileEntityBlock
    
    @Volatile
    lateinit var uuid: UUID
    
    @Volatile
    var ownerUUID: UUID? = null
    
    @Volatile
    lateinit var data: Compound
    
    @Volatile
    private var _tileEntity: TileEntity? = null
    var tileEntity: TileEntity
        get() = _tileEntity ?: throw IllegalStateException("TileEntity is not initialized")
        internal set(value) {
            _tileEntity = value
        }
    
    internal constructor(pos: BlockPos, material: NovaTileEntityBlock) : super(pos, material) {
        this.material = material
    }
    
    internal constructor(material: NovaTileEntityBlock, ctx: BlockPlaceContext) : super(material, ctx) {
        this.material = material
        this.uuid = UUID.randomUUID()
        this.ownerUUID = ctx.ownerUUID
        this.data = Compound()
        
        val item = ctx.item
        val globalData = item.novaCompoundOrNull?.get<Compound>(TileEntity.TILE_ENTITY_DATA_KEY)
        if (globalData != null) {
            data["global"] = globalData
        }
    }
    
    override fun handleInitialized(placed: Boolean) {
        _tileEntity = material.tileEntityConstructor(this)
        tileEntity.handleInitialized(placed)
        
        TileEntityManager.registerTileEntity(this)
        
        super.handleInitialized(placed)
    }
    
    override fun handleRemoved(broken: Boolean) {
        super.handleRemoved(broken)
        
        if (_tileEntity != null) {
            tileEntity.saveData()
            tileEntity.handleRemoved(!broken)
            TileEntityManager.unregisterTileEntity(this)
            _tileEntity = null
        }
    }
    
    override fun read(buf: ByteBuffer) {
        super.read(buf)
        uuid = buf.readUUID()
        ownerUUID = buf.readUUID().takeUnless(UUIDUtils.ZERO::equals)
        data = CBF.read(buf)!!
    }
    
    override fun write(buf: ByteBuffer) {
        super.write(buf)
        buf.writeUUID(uuid)
        buf.writeUUID(ownerUUID ?: UUIDUtils.ZERO)
        
        if (_tileEntity != null)
            tileEntity.saveData()
        
        CBF.write(data, buf)
    }
    
    override fun toString(): String {
        return "NovaTileEntityState(pos=$pos, id=$id, uuid=$uuid, ownerUUID=$ownerUUID, data=$data)"
    }
    
}