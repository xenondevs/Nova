package xyz.xenondevs.nova.world.block.logic.sound

import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import org.bukkit.event.Listener
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.take

internal object BlockSoundEngine : Listener {
    
    private val SOUND_OVERRIDES: HashSet<String> = PermanentStorage.retrieve("soundOverrides", ::HashSet)
    
    fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    fun overridesSound(sound: String): Boolean {
        return sound.removePrefix("minecraft:") in SOUND_OVERRIDES
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.location() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in SOUND_OVERRIDES) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundEntityPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.location() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in SOUND_OVERRIDES) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    private fun getNovaSound(path: String): Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation("nova", path)))
    
}