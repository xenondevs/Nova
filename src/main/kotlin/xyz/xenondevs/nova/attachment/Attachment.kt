package xyz.xenondevs.nova.attachment

import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.ReflectionUtils
import java.util.*

/**
 * A model with or without functionality that attaches to a [Player]
 * by using an [ArmorStand] as a passenger.
 *
 * @param key Specifies a key for this Attachment type. This key helps to remove
 * the [Attachment] later without having to save to reference. If a [Player] already
 * carries an [Attachment] of this key, an exception is thrown.
 * @param playerUUID Specifies the [UUID] of the [Player] that carries
 * this [Attachment].
 * @param itemStack The [ItemStack] to be used for the [Attachment].
 * @param hideOnDown If the [Attachment] should be hidden clientside when
 * the [Player] is looking down.
 */
class Attachment(
    val key: String,
    val playerUUID: UUID,
    val itemStack: ItemStack,
    val hideOnDown: Boolean
) {
    
    val player: Player?
        get() = Bukkit.getPlayer(playerUUID)
    lateinit var armorStand: ArmorStand
    private lateinit var nmsEntity: Any
    private var hidden = false
    
    init {
        AttachmentManager.registerAttachment(this)
        spawn()
    }
    
    fun spawn() {
        val player = player
        if (player != null) {
            armorStand = EntityUtils.spawnArmorStandSilently(player.location, itemStack, false)
            nmsEntity = ReflectionUtils.getNMSEntity(armorStand)
        }
    }
    
    fun despawn() {
        val player = player
        if (player != null) {
            player.removePassenger(armorStand)
            armorStand.remove()
        }
    }
    
    fun remove() {
        despawn()
        AttachmentManager.unregisterAttachment(this)
    }
    
    fun handleTick(tick: Int) {
        val player = player
        if (player != null) {
            val armorStandId = ReflectionUtils.getEntityId(armorStand)
            
            if (hideOnDown) {
                val pitch = player.location.pitch
                if (pitch >= 40 && !hidden) {
                    // hide armor stand for attachment carrier
                    val packet = ReflectionUtils.createPacketPlayOutEntityEquipment(armorStandId, listOf("head" to null))
                    ReflectionUtils.sendPacket(player, packet)
                    
                    hidden = true
                } else if (hidden && pitch < 40) {
                    // show armor stand again
                    val packet = ReflectionUtils.createPacketPlayOutEntityEquipment(armorStandId, listOf("head" to itemStack))
                    ReflectionUtils.sendPacket(player, packet)
                    
                    hidden = false
                }
            }
            
            if (tick % 3 == 0) {
                // teleport the armor stand near the player because it's not actually a passenger
                armorStand.teleport(player.location)
                
                // send the mount packet to everyone because we don't know who knows that the armor stand is a passenger of the player
                // (this could theoretically be optimized by listening to outgoing packets and checking for a spawn packet of the
                // armor stand and then send a mount packet immediately afterwards)
                val packet = ReflectionUtils.createPacketPlayOutMount(
                    ReflectionUtils.getEntityId(player),
                    player.passengers
                        .toMutableList()
                        .mapTo(ArrayList(), ReflectionUtils::getEntityId)
                        .also { it.add(armorStandId) }
                        .toIntArray()
                )
                ReflectionUtils.sendPacketToEveryone(packet)
            }
        }
    }
    
}