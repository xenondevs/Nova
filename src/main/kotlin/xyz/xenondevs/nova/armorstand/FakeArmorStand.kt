package xyz.xenondevs.nova.armorstand

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
    
    private var registered = false
    
    private var _location: Location = location.clone()
    private var chunk = location.chunk.pos
    private val equipment = HashMap<EquipmentSlot, MItemStack>()
    
    private val viewers: List<Player>
        get() = FakeArmorStandManager.getChunkViewers(chunk)
    
    val location: Location
        get() = _location.clone()
    
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
        val entityPacket = NMSUtils.createAddMobPacket(id, uuid, EntityType.ARMOR_STAND, _location)
        val entityDataPacket = ClientboundSetEntityDataPacket(id, entityData, true)
        val entityEquipmentPacket = ClientboundSetEquipmentPacket(id, equipment.map { (slot, stack) -> Pair(slot, stack) })
        
        player.send(entityPacket, entityDataPacket, entityEquipmentPacket)
    }
    
    /**
     * Despawns the [FakeArmorStand] for a specific [Player].
     */
    fun despawn(player: Player) {
        val removePacket = ClientboundRemoveEntitiesPacket(id)
        player.send(removePacket)
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
        if (newLocation.world != _location.world) throw UnsupportedOperationException("Teleporting to different worlds is not supported.")
        
        val viewers = viewers
        
        if (viewers.isNotEmpty()) {
            var packet: Packet<ClientGamePacketListener>? = null
            
            // get the correct packet for this kind of movement
            if (_location.positionEquals(newLocation)) {
                if (newLocation.yaw != bukkitYaw || newLocation.pitch != xRot) {
                    packet = ClientboundMoveEntityPacket.Rot(id, newLocation.yaw.toPackedByte(), newLocation.pitch.toPackedByte(), true)
                    _location = newLocation.clone() // position won't be changed, exact rotation is not necessary
                }
            } else if (_location.distance(newLocation) > 8) {
                packet = NMSUtils.createTeleportPacket(id, newLocation)
                _location = newLocation.clone() // exact position will be displayed to user
            } else {
                val deltaX = (newLocation.x - _location.x).toFixedPoint()
                val deltaY = (newLocation.y - _location.y).toFixedPoint()
                val deltaZ = (newLocation.z - _location.z).toFixedPoint()
                
                // removes precision that cannot be displayed to players to prevent desyncing
                _location.add(deltaX.fromFixedPoint(), deltaY.fromFixedPoint(), deltaZ.fromFixedPoint())
                
                if (newLocation.yaw != bukkitYaw || newLocation.pitch != xRot) {
                    // rotation also loses precision (a lot actually) but it isn't necessary to reflect that in the
                    // armor stand location as no rotation deltas are sent
                    _location.yaw = newLocation.yaw
                    _location.pitch = newLocation.pitch
                    
                    packet = ClientboundMoveEntityPacket.PosRot(id, deltaX, deltaY, deltaZ, newLocation.yaw.toPackedByte(), newLocation.pitch.toPackedByte(), true)
                } else {
                    packet = ClientboundMoveEntityPacket.Pos(id, deltaX, deltaY, deltaZ, true)
                }
            }
            
            if (packet != null) viewers.forEach { it.send(packet) }
        } else {
            _location = newLocation
        }
        
        val previousChunk = chunk
        val newChunk = _location.chunk.pos
        chunk = newChunk
        
        if (previousChunk != newChunk) FakeArmorStandManager.changeArmorStandChunk(this, previousChunk, newChunk)
    }
    
    /**
     * Sends a teleport packet to the current position.
     */
    fun syncPosition() {
        val teleportPacket = NMSUtils.createTeleportPacket(id, _location)
        viewers.forEach { it.send(teleportPacket) }
    }
    
    /**
     * Sends a packet updating the equipment to all viewers.
     */
    fun updateEquipment() {
        val entityEquipmentPacket = ClientboundSetEquipmentPacket(id, equipment.map { (slot, stack) -> Pair(slot, stack) })
        viewers.forEach { it.send(entityEquipmentPacket) }
    }
    
    /**
     * Sends a packet updating the entity data to all viewers.
     */
    fun updateEntityData() {
        val entityDataPacket = ClientboundSetEntityDataPacket(id, entityData, true)
        viewers.forEach { it.send(entityDataPacket) }
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