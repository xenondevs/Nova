package xyz.xenondevs.nova.world.block.logic.sound

import net.minecraft.core.Holder
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import org.bukkit.Sound
import org.bukkit.event.Listener
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.take
import xyz.xenondevs.nova.world.format.WorldDataManager

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object SoundEngine : Listener, PacketListener {
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    fun overridesSound(sound: String): Boolean {
        return sound.removePrefix("minecraft:") in ResourceLookups.SOUND_OVERRIDES
    }
    
    fun overridesSound(sound: Sound): Boolean {
        return overridesSound(sound.key.toString())
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.location() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in ResourceLookups.SOUND_OVERRIDES) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundEntityPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.location() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in ResourceLookups.SOUND_OVERRIDES) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    private fun getNovaSound(path: String): Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("nova", path)))
    
}