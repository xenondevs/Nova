package xyz.xenondevs.nova.world.fakeentity

import io.netty.buffer.Unpooled
import net.minecraft.core.Registry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.send
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.fromFixedPoint
import xyz.xenondevs.nova.util.fromPackedByte
import xyz.xenondevs.nova.util.positionEquals
import xyz.xenondevs.nova.util.toFixedPoint
import xyz.xenondevs.nova.util.toPackedByte
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.fakeentity.metadata.Metadata
import java.util.*
import org.bukkit.inventory.ItemStack as BukkitStack

/**
 * A fake entity that does not exist in the world and can be updated asynchronously.
 */
abstract class FakeEntity<M : Metadata> internal constructor(location: Location) {
    
    protected abstract val metadata: M
    protected abstract val entityType: EntityType<*>
    
    private var registered = false
    val viewers: List<Player>
        get() = FakeEntityManager.getChunkViewers(chunk)
    
    val entityId = NMSUtils.ENTITY_COUNTER.incrementAndGet()
    private val uuid = UUID.randomUUID()
    
    private var spawnBuf: FriendlyByteBuf? = null
    private var dataBuf: FriendlyByteBuf? = null
    private var equipmentBuf: FriendlyByteBuf? = null
    private var despawnBuf: FriendlyByteBuf = createDespawnDataBuf()
    
    private var expectedLocation: Location = location.clone()
    private var actualLocation: Location = location.clone()
    private var chunk = location.chunkPos
    val location: Location
        get() = expectedLocation.clone()
    
    private val equipment = Array<ItemStack>(6) { ItemStack.EMPTY }
    
    var spawnHandler: ((Player) -> Unit)? = null
    var despawnHandler: ((Player) -> Unit)? = null
    
    /**
     * Registers this [FakeEntity] in the [FakeEntityManager].
     * @throws IllegalStateException If this [FakeEntity] is already registered.
     */
    fun register() {
        if (registered) throw IllegalStateException("This FakeEntity is already registered")
        FakeEntityManager.addEntity(chunk, this)
        registered = true
    }
    
    /**
     * Removes the [FakeEntity] from the [chunk entities Map][FakeEntityManager.chunkEntities] and despawns it
     * for all current viewers.
     */
    fun remove() {
        if (registered) {
            registered = false
            FakeEntityManager.removeEntity(chunk, this)
        }
    }
    
    /**
     * Spawns the [FakeEntity] for a specific [Player].
     */
    fun spawn(player: Player) {
        val spawnBuf = this.spawnBuf ?: createSpawnBuf().also { this.spawnBuf = it }
        val dataBuf = this.dataBuf ?: metadata.pack(entityId).also { this.dataBuf = it }
        
        if (equipment.all { it == ItemStack.EMPTY }) {
            player.send(spawnBuf, dataBuf)
        } else {
            val equipmentBuf = this.equipmentBuf ?: createEquipmentBuf().also { this.equipmentBuf = it }
            player.send(spawnBuf, dataBuf, equipmentBuf)
        }
        
        spawnHandler?.invoke(player)
    }
    
    /**
     * Despawns the [FakeEntity] for a specific [Player].
     */
    fun despawn(player: Player) {
        player.send(despawnBuf)
        despawnHandler?.invoke(player)
    }
    
    /**
     * Updates the entity data of this [FakeEntity]
     */
    fun updateEntityData(sendPacket: Boolean, update: M.() -> Unit) {
        // release the dataBuf as it will change
        dataBuf?.release()
        dataBuf = null
        
        // update the entity data
        metadata.update()
        
        // rebuild buf and send packet if requested
        if (sendPacket) {
            val buf = metadata.packDirty(entityId)
            viewers.forEach { it.send(buf) }
            buf.release() // partial buf is no longer needed
        }
    }
    
    /**
     * Sets the equipment for a specific [EquipmentSlot].
     */
    fun setEquipment(slot: EquipmentSlot, bukkitStack: BukkitStack?, sendPacket: Boolean) {
        // release the equipment buf as it will change
        equipmentBuf?.release()
        equipmentBuf = null
        
        // update the equipment array
        equipment[slot.ordinal] = CraftItemStack.asNMSCopy(bukkitStack)
        
        // rebuild buf and send packet if requested
        if (sendPacket) {
            equipmentBuf = createEquipmentBuf()
            viewers.forEach { it.send(equipmentBuf!!) }
        }
    }
    
    /**
     * Teleports the [FakeEntity] to a different location. (Different worlds aren't supported)
     *
     * This function automatically chooses which packet (Teleport / Pos / PosRot / Rot) to send.
     */
    fun teleport(modifyLocation: Location.() -> Unit) {
        val location = location
        modifyLocation(location)
        teleport(location)
    }
    
    /**
     * Teleports the [FakeEntity] to a different location.
     *
     * This function automatically chooses which packet (Teleport / Pos / PosRot / Rot) to send.
     */
    fun teleport(newLocation: Location, forceTeleport: Boolean = false) {
        // release the spawn buf as the location has changed
        spawnBuf?.release()
        spawnBuf = null
        
        val viewers = viewers
        if (newLocation.world == actualLocation.world && viewers.isNotEmpty()) {
            var buf: FriendlyByteBuf? = null
            
            // get the correct packet for this kind of movement
            if (!forceTeleport && actualLocation.positionEquals(newLocation)) {
                if (newLocation.yaw != actualLocation.yaw || newLocation.pitch != actualLocation.pitch) {
                    buf = createRotBuf(newLocation)
                    actualLocation = newLocation.clone() // position won't be changed, exact rotation is not necessary
                }
            } else if (forceTeleport || actualLocation.distance(newLocation) > 8) {
                buf = createTeleportBuf(newLocation)
                actualLocation = newLocation.clone() // exact position will be displayed to user
            } else {
                val deltaX = (newLocation.x - actualLocation.x).toFixedPoint()
                val deltaY = (newLocation.y - actualLocation.y).toFixedPoint()
                val deltaZ = (newLocation.z - actualLocation.z).toFixedPoint()
                
                // removes precision that cannot be displayed to players to prevent desyncing
                actualLocation.add(deltaX.fromFixedPoint(), deltaY.fromFixedPoint(), deltaZ.fromFixedPoint())
                
                if (newLocation.yaw != actualLocation.yaw || newLocation.pitch != actualLocation.pitch) {
                    // rotation also loses precision (a lot actually) but it isn't necessary to reflect that in the
                    // entity location as no rotation deltas are sent
                    actualLocation.yaw = newLocation.yaw
                    actualLocation.pitch = newLocation.pitch
                    
                    buf = createPosRotBuf(deltaX, deltaY, deltaZ, newLocation.yaw, newLocation.pitch)
                } else {
                    buf = createPosBuf(deltaX, deltaY, deltaZ)
                }
            }
            
            if (buf != null) {
                viewers.forEach { it.send(buf) }
                buf.release() // no longer required
            }
        } else {
            actualLocation = newLocation.clone()
        }
        
        expectedLocation = newLocation.clone()
        
        val previousChunk = chunk
        val newChunk = actualLocation.chunkPos
        chunk = newChunk
        
        if (previousChunk != newChunk) FakeEntityManager.changeEntityChunk(this, previousChunk, newChunk)
    }
    
    private fun createSpawnBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        
        val packedYaw = expectedLocation.yaw.toPackedByte().toInt()
        buf.writeVarInt(0x00)
        buf.writeVarInt(entityId)
        buf.writeUUID(uuid)
        buf.writeId(Registry.ENTITY_TYPE, entityType)
        buf.writeDouble(location.x)
        buf.writeDouble(location.y)
        buf.writeDouble(location.z)
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeByte(packedYaw)
        buf.writeByte(packedYaw)
        buf.writeVarInt(0)
        buf.writeShort(0)
        buf.writeShort(0)
        buf.writeShort(0)
        
        return buf
    }
    
    private fun createDespawnDataBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x3B)
        buf.writeVarIntArray(intArrayOf(entityId))
        
        return buf
    }
    
    private fun createEquipmentBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x53)
        buf.writeVarInt(entityId)
        equipment.forEachIndexed { index, item ->
            buf.writeByte(if (index != 5) index or -128 else index)
            buf.writeItem(item)
        }
        
        return buf
    }
    
    private fun createPosBuf(x: Short, y: Short, z: Short): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x28)
        buf.writeVarInt(entityId)
        buf.writeShort(x.toInt())
        buf.writeShort(y.toInt())
        buf.writeShort(z.toInt())
        buf.writeBoolean(true)
        return buf
    }
    
    private fun createPosRotBuf(x: Short, y: Short, z: Short, yaw: Float, pitch: Float): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x29)
        buf.writeVarInt(entityId)
        buf.writeShort(x.toInt())
        buf.writeShort(y.toInt())
        buf.writeShort(z.toInt())
        buf.writeByte(yaw.toPackedByte().toInt())
        buf.writeByte(pitch.toPackedByte().toInt())
        buf.writeBoolean(true)
        return buf
    }
    
    private fun createRotBuf(location: Location): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x2A)
        buf.writeVarInt(entityId)
        buf.writeByte(location.yaw.toPackedByte().toInt())
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeBoolean(true)
        return buf
    }
    
    private fun createTeleportBuf(location: Location): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x66)
        buf.writeVarInt(entityId)
        buf.writeDouble(location.x)
        buf.writeDouble(location.y)
        buf.writeDouble(location.z)
        buf.writeByte((location.yaw % 360).toPackedByte().toInt())
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeBoolean(true)
        return buf
    }
    
}

fun main() {
    
    println("--")
    for (i in -720..720 step 45) {
        println("$i -> " + (i.toFloat().toPackedByte().fromPackedByte()))
    }
    
}