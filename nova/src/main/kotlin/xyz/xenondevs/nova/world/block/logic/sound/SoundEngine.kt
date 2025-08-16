package xyz.xenondevs.nova.world.block.logic.sound

import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
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
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.take
import xyz.xenondevs.nova.world.format.WorldDataManager
import kotlin.random.Random

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [WorldDataManager::class]
)
internal object SoundEngine : Listener, PacketListener {
    
    private val SOUND_OVERRIDES: HashSet<String> = PermanentStorage.retrieve("soundOverrides") ?: HashSet()
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    fun overridesSound(sound: String): Boolean {
        return sound.removePrefix("minecraft:") in SOUND_OVERRIDES
    }
    
    fun overridesSound(sound: Sound): Boolean {
        return overridesSound(sound.key.toString())
    }
    
    @JvmStatic
    fun broadcast(entity: Entity, oldSound: String, newSound: String, volume: Float, pitch: Float) {
        val level = entity.level()
        val player = if (overridesSound(oldSound)) null else entity as? Player

        MINECRAFT_SERVER.playerList.broadcast(
            player,
            entity.x, entity.y, entity.z, 
            16.0, 
            level.dimension(),
            ClientboundSoundPacket(
                Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.parse(newSound))),
                entity.soundSource,
                entity.x, entity.y, entity.z,
                volume, pitch,
                level.random.nextLong()
            )
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
                Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.parse(newSound))),
                source,
                x, y, z,
                volume, pitch,
                Random.nextLong()
            )
        )
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
        Holder.direct(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("nova", path)))
    
}