package xyz.xenondevs.nova.world.block.logic.sound

import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.bukkit.Sound
import org.bukkit.craftbukkit.CraftSound
import org.bukkit.event.Listener
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundEntityPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSoundPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.take
import xyz.xenondevs.nova.world.format.WorldDataManager
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    runAfter = [WorldDataManager::class]
)
internal object SoundEngine : Listener, PacketListener {
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    fun overridesSound(sound: String): Boolean {
        return sound.removePrefix("minecraft:") in ResourceLookups.soundOverrides
    }
    
    fun overridesSound(sound: Sound): Boolean {
        return overridesSound(CraftSound.bukkitToMinecraft(sound))
    }
    
    fun overridesSound(sound: SoundEvent): Boolean {
        return overridesSound(sound.location.toString())
    }
    
    @JvmStatic
    fun broadcast(entity: Entity, oldSound: SoundEvent, newSound: SoundEvent, volume: Float, pitch: Float) {
        val level = entity.level()
        val player = if (overridesSound(oldSound)) null else entity as? Player
        
        level.playSound(
            player,
            entity.x, entity.y, entity.z,
            newSound,
            entity.soundSource,
            volume, pitch
        )
    }
    
    @JvmStatic
    fun broadcastIfOverridden(
        level: Level, x: Double, y: Double, z: Double, radius: Double,
        oldSound: String, newSound: String, volume: Float, pitch: Float, source: SoundSource
    ) {
        if (!overridesSound(oldSound))
            return
        
        MINECRAFT_SERVER.playerList.broadcast(
            null,
            x, y, z,
            radius,
            level.dimension(),
            ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(newSound))),
                source,
                x, y, z,
                volume, pitch,
                Random.nextLong()
            )
        )
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.identifier() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in ResourceLookups.soundOverrides) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    @PacketHandler
    private fun handleSoundPacket(event: ClientboundSoundEntityPacketEvent) {
        val location = event.sound.unwrap().mapBoth({ it.identifier() }, { it.location }).take()
        if (location.namespace == "minecraft" && location.path in ResourceLookups.soundOverrides) {
            event.sound = getNovaSound(location.path)
        }
    }
    
    private fun getNovaSound(path: String): Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("nova", path)))
    
}