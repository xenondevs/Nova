package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundDeserializer
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.TileEntityNovaMaterial
import xyz.xenondevs.nova.util.data.readUUID
import xyz.xenondevs.nova.util.data.writeUUID
import java.util.*

class NovaTileState(override val material: TileEntityNovaMaterial) : NovaBlockState(material) {
    
    lateinit var uuid: UUID
    lateinit var ownerUUID: UUID
    lateinit var data: CompoundElement
    
    constructor(material: TileEntityNovaMaterial, uuid: UUID, ownerUUID: UUID, data: CompoundElement) : this(material) {
        this.uuid = uuid
        this.ownerUUID = ownerUUID
        this.data = data
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