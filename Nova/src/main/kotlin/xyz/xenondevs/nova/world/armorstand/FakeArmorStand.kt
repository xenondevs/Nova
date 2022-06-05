package xyz.xenondevs.nova.world.armorstand

import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.entity.Player
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.world.chunkPos
import java.util.*
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BItemStack

/**
 * A fake armor stand that does not exist in the world and does therefore not impact performance
 * as much and can also be used asynchronously.
 */
class FakeArmorStand(
    location: Location,
    autoRegister: Boolean = true,
    beforeSpawn: ((FakeArmorStand, ArmorStandDataHolder) -> Unit)? = null
) {
    
    private var registered = false
    val viewers: List<Player>
        get() = FakeArmorStandManager.getChunkViewers(chunk)
    
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
    
    private val equipment = Array<MojangStack>(6) { MojangStack.EMPTY }
    private val entityDataHolder = ArmorStandDataHolder(entityId)
    
    var spawnHandler: ((Player) -> Unit)? = null
    var despawnHandler: ((Player) -> Unit)? = null
    
    init {
        beforeSpawn?.invoke(this, entityDataHolder)
        if (autoRegister) register()
    }
    
    /**
     * Registers this [FakeArmorStand] in the [FakeArmorStandManager].
     * @throws IllegalStateException If this [FakeArmorStand] is already registered.
     */
    fun register() {
        if (registered) throw IllegalStateException("This FakeArmorStand is already registered")
        FakeArmorStandManager.addArmorStand(chunk, this)
        registered = true
    }
    
    /**
     * Removes the [FakeArmorStand] from the [chunkArmorStands Map][FakeArmorStandManager.chunkArmorStands] and despawns it
     * for all current viewers.
     */
    fun remove() {
        registered = false
        FakeArmorStandManager.removeArmorStand(chunk, this)
    }
    
    /**
     * Spawns the [FakeArmorStand] for a specific [Player].
     * Also sends entity data and equipment.
     */
    fun spawn(player: Player) {
        val spawnBuf = this.spawnBuf ?: createSpawnBuf().also { this.spawnBuf = it }
        val equipmentBuf = this.equipmentBuf ?: createEquipmentBuf().also { this.equipmentBuf = it }
        val dataBuf = this.dataBuf ?: entityDataHolder.createCompleteDataBuf().also { this.dataBuf = it }
        
        player.send(spawnBuf, equipmentBuf, dataBuf)
        
        spawnHandler?.invoke(player)
    }
    
    /**
     * Despawns the [FakeArmorStand] for a specific [Player].
     */
    fun despawn(player: Player) {
        player.send(despawnBuf)
        despawnHandler?.invoke(player)
    }
    
    /**
     * Updates the entity data of this [FakeArmorStand]
     */
    fun updateEntityData(sendPacket: Boolean, update: ArmorStandDataHolder.() -> Unit) {
        // release the dataBuf as it will change
        dataBuf?.release()
        dataBuf = null
        
        // update the entity data
        entityDataHolder.update()
        
        // rebuild buf and send packet if requested
        if (sendPacket) {
            val buf = entityDataHolder.createPartialDataBuf()
            viewers.forEach { it.send(buf) }
            buf.release() // partial buf is no longer needed
        }
    }
    
    /**
     * Sets the equipment for a specific [EquipmentSlot].
     */
    fun setEquipment(slot: EquipmentSlot, bukkitStack: BItemStack?, sendPacket: Boolean) {
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
     * Teleports the [FakeArmorStand] to a different location. (Different worlds aren't supported)
     *
     * This function automatically chooses which packet (Teleport / Pos / PosRot / Rot) to send.
     */
    fun teleport(modifyLocation: Location.() -> Unit) {
        val location = location
        modifyLocation(location)
        teleport(location)
    }
    
    /**
     * Teleports the [FakeArmorStand] to a different location.
     *
     * This function automatically chooses which packet (Teleport / Pos / PosRot / Rot) to send.
     */
    fun teleport(newLocation: Location) {
        // release the spawn buf as the location has changed
        spawnBuf?.release()
        spawnBuf = null
        
        val viewers = viewers
        if (newLocation.world == actualLocation.world && viewers.isNotEmpty()) {
            var buf: FriendlyByteBuf? = null
            
            // get the correct packet for this kind of movement
            if (actualLocation.positionEquals(newLocation)) {
                if (newLocation.yaw != actualLocation.yaw || newLocation.pitch != actualLocation.pitch) {
                    buf = createRotBuf(newLocation)
                    actualLocation = newLocation.clone() // position won't be changed, exact rotation is not necessary
                }
            } else if (actualLocation.distance(newLocation) > 8) {
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
                    // armor stand location as no rotation deltas are sent
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
        
        if (previousChunk != newChunk) FakeArmorStandManager.changeArmorStandChunk(this, previousChunk, newChunk)
    }
    
    private fun createSpawnBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        
        val packedYaw = expectedLocation.yaw.toPackedByte().toInt()
        buf.writeVarInt(0x02)
        buf.writeVarInt(entityId)
        buf.writeUUID(uuid)
        buf.writeVarInt(1)
        buf.writeDouble(location.x)
        buf.writeDouble(location.y)
        buf.writeDouble(location.z)
        buf.writeByte(packedYaw)
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeByte(packedYaw)
        buf.writeShort(0)
        buf.writeShort(0)
        buf.writeShort(0)
        
        return buf
    }
    
    private fun createDespawnDataBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x3A)
        buf.writeVarIntArray(intArrayOf(entityId))
        
        return buf
    }
    
    private fun createEquipmentBuf(): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x50)
        buf.writeVarInt(entityId)
        equipment.forEachIndexed { index, item ->
            buf.writeByte(if (index != 5) index or -128 else index)
            buf.writeItem(item)
        }
        
        return buf
    }
    
    private fun createPosBuf(x: Short, y: Short, z: Short): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x29)
        buf.writeVarInt(entityId)
        buf.writeShort(x.toInt())
        buf.writeShort(y.toInt())
        buf.writeShort(z.toInt())
        buf.writeBoolean(true)
        return buf
    }
    
    private fun createPosRotBuf(x: Short, y: Short, z: Short, yaw: Float, pitch: Float): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x2A)
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
        buf.writeVarInt(0x2B)
        buf.writeVarInt(entityId)
        buf.writeByte(location.yaw.toPackedByte().toInt())
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeBoolean(true)
        return buf
    }
    
    private fun createTeleportBuf(location: Location): FriendlyByteBuf {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeVarInt(0x62)
        buf.writeVarInt(entityId)
        buf.writeDouble(location.x)
        buf.writeDouble(location.y)
        buf.writeDouble(location.z)
        buf.writeByte(location.yaw.toPackedByte().toInt())
        buf.writeByte(location.pitch.toPackedByte().toInt())
        buf.writeBoolean(true)
        return buf
    }
    
}