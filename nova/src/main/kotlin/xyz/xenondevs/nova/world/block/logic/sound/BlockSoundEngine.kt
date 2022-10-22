package xyz.xenondevs.nova.world.block.logic.sound

import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.bukkit.event.Listener
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundCustomSoundPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.send
import xyz.xenondevs.nova.util.serverPlayer

// TODO: block hit, step and fall sounds
internal object BlockSoundEngine : Listener {
    
    private val SOUND_OVERRIDES: HashSet<String> = PermanentStorage.retrieve("soundOverrides", ::HashSet)
    
    fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    fun overridesSound(sound: String): Boolean {
        return sound.removePrefix("minecraft:") in SOUND_OVERRIDES
    }
    
    //<editor-fold desc="packet interception of non-custom sound packets", defaultstate="collapsed">
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundPacketEvent) {
        val location = event.sound.location
        if (location.namespace == "minecraft" && location.path in SOUND_OVERRIDES) {
            event.isCancelled = true
            event.player.send(
                ClientboundCustomSoundPacket(
                    ResourceLocation("nova", location.path),
                    event.source,
                    Vec3(event.x, event.y, event.z),
                    event.volume,
                    event.pitch,
                    event.seed
                )
            )
        }
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundEntityPacketEvent) {
        val location = event.sound.location
        if (location.namespace == "minecraft" && location.path in SOUND_OVERRIDES) {
            event.isCancelled = true
            runTask {
                val entity = event.player.serverPlayer.level.entities.get(event.entityId)
                if (entity != null) {
                    event.player.send(
                        ClientboundCustomSoundPacket(
                            ResourceLocation("nova", location.path),
                            event.source,
                            entity.position(),
                            event.volume,
                            event.pitch,
                            event.seed
                        )
                    )
                }
            }
        }
    }
    //</editor-fold>
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundCustomSoundPacketEvent) {
        val location = event.name
        if (location.namespace == "minecraft" && location.path in SOUND_OVERRIDES) {
            event.name = ResourceLocation("nova", location.path)
        }
    }
    
}