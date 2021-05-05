package xyz.xenondevs.nova.attachment

import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.ReflectionRegistry
import xyz.xenondevs.nova.util.ReflectionUtils
import xyz.xenondevs.nova.util.toPackedByte
import java.util.*
import kotlin.properties.Delegates

/**
 * A model with or without functionality that attaches to a [Player]
 * by using an [ArmorStand] as a passenger.
 *
 * @param uuid Specifies the [UUID] of the [Player] that carries
 * this [Attachment].
 * @param itemStack The [ItemStack] to be used for the [Attachment].
 * @param hideOnDown If the [Attachment] should be hidden clientside when
 * the [Player] is looking down.
 */
class Attachment(
    val uuid: UUID,
    val itemStack: ItemStack,
    val hideOnDown: Boolean
) {
    
    val player: Player?
        get() = Bukkit.getPlayer(uuid)
    lateinit var armorStand: ArmorStand
    private lateinit var nmsEntity: Any
    private var entityId by Delegates.notNull<Int>()
    private var currentYaw = -1f
    private var hidden = false
    
    init {
        AttachmentManager.registerAttachment(this)
        spawn()
    }
    
    fun spawn() {
        val player = player
        if (player != null) {
            armorStand = EntityUtils.spawnArmorStandSilently(player.location, itemStack, false) { player.addPassenger(this) }
            nmsEntity = ReflectionUtils.getNMSEntity(armorStand)
            entityId = ReflectionRegistry.NMS_ENTITY_GET_ID_METHOD.invoke(nmsEntity) as Int
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
    
    fun handleTick() {
        val player = player
        if (player != null) {
            
            if (hideOnDown) {
                val pitch = player.location.pitch
                if (pitch >= 40 && !hidden) {
                    // hide armor stand for attachment carrier
                    val packet = ReflectionUtils.createPacketPlayOutEntityEquipment(entityId, listOf("head" to null))
                    ReflectionUtils.sendPacket(player, packet)
                    
                    hidden = true
                } else if (hidden && pitch < 40) {
                    // show armor stand again
                    val packet = ReflectionUtils.createPacketPlayOutEntityEquipment(entityId, listOf("head" to itemStack))
                    ReflectionUtils.sendPacket(player, packet)
                    
                    hidden = false
                }
            }
            
            val yaw = player.location.yaw
            if (currentYaw != yaw) {
                val packet = ReflectionRegistry.NMS_PACKET_PLAY_OUT_ENTITY_HEAD_ROTATION_CONSTRUCTOR.newInstance(
                    nmsEntity,
                    yaw.toPackedByte()
                )
                ReflectionUtils.sendPacketToEveryone(packet)
            }
        }
    }
    
}