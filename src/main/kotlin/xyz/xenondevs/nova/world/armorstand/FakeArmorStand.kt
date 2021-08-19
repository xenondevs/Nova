package xyz.xenondevs.nova.world.armorstand

import com.mojang.datafixers.util.Pair
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import xyz.xenondevs.nova.util.*
import net.minecraft.world.item.ItemStack as MItemStack
import org.bukkit.inventory.ItemStack as BItemStack

/**
 * A fake armor stand that does not exist in the world and does therefore not impact performance
 * as much and can also be used asynchronously.
 */
class FakeArmorStand(
    location: Location,
    autoRegister: Boolean = true,
    beforeSpawn: ((FakeArmorStand) -> Unit)? = null
) : ArmorStand(EntityType.ARMOR_STAND, location.world!!.serverLevel) {
    
    private var spawnPacket: ClientboundAddMobPacket? = null
    private var dataPacket: ClientboundSetEntityDataPacket? = null
    private var equipmentPacket: ClientboundSetEquipmentPacket? = null
    private val despawnPacket = ClientboundRemoveEntitiesPacket(id)
    
    private var registered = false
    private var expectedLocation: Location = location.clone()
    private var actualLocation: Location = location.clone()
    private var chunk = location.chunkPos
    private val equipment = HashMap<EquipmentSlot, MItemStack>()
    
    private val viewers: List<Player>
        get() = FakeArmorStandManager.getChunkViewers(chunk)
    val location: Location
        get() = expectedLocation.clone()
    
    init {
        EquipmentSlot.values().forEach { equipment[it] = MItemStack.EMPTY }
        beforeSpawn?.invoke(this)
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
        if (spawnPacket == null) spawnPacket = NMSUtils.createAddMobPacket(id, uuid, EntityType.ARMOR_STAND, actualLocation)
        if (equipmentPacket == null) equipmentPacket = ClientboundSetEquipmentPacket(id, equipment.map { (slot, stack) -> Pair(slot, stack) })
        if (dataPacket == null) dataPacket = ClientboundSetEntityDataPacket(id, entityData, true)
        player.send(spawnPacket!!, dataPacket!!, equipmentPacket!!)
    }
    
    /**
     * Despawns the [FakeArmorStand] for a specific [Player].
     */
    fun despawn(player: Player) {
        player.send(despawnPacket)
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
     * Teleports the [FakeArmorStand] to a different location. (Different worlds aren't supported)
     *
     * This function automatically chooses which packet (Teleport / Pos / PosRot / Rot) to send.
     */
    fun teleport(newLocation: Location) {
        if (newLocation.world != actualLocation.world) throw UnsupportedOperationException("Teleporting to different worlds is not supported.")
        
        // invalidate the spawn packet as the location has changed
        spawnPacket = null
        
        val viewers = viewers
        if (viewers.isNotEmpty()) {
            var packet: Packet<ClientGamePacketListener>? = null
            
            // get the correct packet for this kind of movement
            if (actualLocation.positionEquals(newLocation)) {
                if (newLocation.yaw != bukkitYaw || newLocation.pitch != xRot) {
                    packet = ClientboundMoveEntityPacket.Rot(id, newLocation.yaw.toPackedByte(), newLocation.pitch.toPackedByte(), true)
                    actualLocation = newLocation.clone() // position won't be changed, exact rotation is not necessary
                }
            } else if (actualLocation.distance(newLocation) > 8) {
                packet = NMSUtils.createTeleportPacket(id, newLocation)
                actualLocation = newLocation.clone() // exact position will be displayed to user
            } else {
                val deltaX = (newLocation.x - actualLocation.x).toFixedPoint()
                val deltaY = (newLocation.y - actualLocation.y).toFixedPoint()
                val deltaZ = (newLocation.z - actualLocation.z).toFixedPoint()
                
                // removes precision that cannot be displayed to players to prevent desyncing
                actualLocation.add(deltaX.fromFixedPoint(), deltaY.fromFixedPoint(), deltaZ.fromFixedPoint())
                
                if (newLocation.yaw != bukkitYaw || newLocation.pitch != xRot) {
                    // rotation also loses precision (a lot actually) but it isn't necessary to reflect that in the
                    // armor stand location as no rotation deltas are sent
                    actualLocation.yaw = newLocation.yaw
                    actualLocation.pitch = newLocation.pitch
                    
                    packet = ClientboundMoveEntityPacket.PosRot(id, deltaX, deltaY, deltaZ, newLocation.yaw.toPackedByte(), newLocation.pitch.toPackedByte(), true)
                } else {
                    packet = ClientboundMoveEntityPacket.Pos(id, deltaX, deltaY, deltaZ, true)
                }
            }
            
            if (packet != null) viewers.forEach { it.send(packet) }
        } else {
            actualLocation = newLocation.clone()
        }
        
        expectedLocation = newLocation.clone()
        
        val previousChunk = chunk
        val newChunk = actualLocation.chunkPos
        chunk = newChunk
        
        if (previousChunk != newChunk) FakeArmorStandManager.changeArmorStandChunk(this, previousChunk, newChunk)
    }
    
    /**
     * Sends a teleport packet to the current position.
     */
    fun syncPosition() {
        val teleportPacket = NMSUtils.createTeleportPacket(id, actualLocation)
        viewers.forEach { it.send(teleportPacket) }
    }
    
    /**
     * Sends a packet updating the equipment to all viewers.
     */
    fun updateEquipment() {
        equipmentPacket = ClientboundSetEquipmentPacket(id, equipment.map { (slot, stack) -> Pair(slot, stack) })
        viewers.forEach { it.send(equipmentPacket!!) }
    }
    
    /**
     * Sends a packet updating the entity data to all viewers.
     */
    fun updateEntityData() {
        dataPacket = ClientboundSetEntityDataPacket(id, entityData, true)
        viewers.forEach { it.send(dataPacket!!) }
    }
    
    /**
     * Sets the equipment for a specific [EquipmentSlot].
     */
    fun setEquipment(slot: EquipmentSlot, bukkitStack: BItemStack) {
        // TODO: cache?
        equipment[slot] = CraftItemStack.asNMSCopy(bukkitStack)
    }
    
    @Deprecated("", ReplaceWith("setEquipment(slot, bukkitStack)"))
    override fun setItemSlot(enumitemslot: EquipmentSlot?, itemstack: net.minecraft.world.item.ItemStack?) {
        throw UnsupportedOperationException()
    }
    
    @Deprecated("", ReplaceWith("setEquipment(slot, bukkitStack)"))
    override fun setSlot(enumitemslot: EquipmentSlot?, itemstack: net.minecraft.world.item.ItemStack?, silent: Boolean) {
        throw UnsupportedOperationException()
    }
    
    override fun tick() {
        // empty
    }
    
    override fun inactiveTick() {
        // empty
    }
    
    override fun aiStep() {
        // empty
    }
    
    override fun serverAiStep() {
        // empty
    }
    
}