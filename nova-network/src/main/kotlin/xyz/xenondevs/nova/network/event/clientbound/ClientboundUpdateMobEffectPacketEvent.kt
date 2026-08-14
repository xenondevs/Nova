package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundUpdateMobEffectPacketEvent(
    player: Player,
    packet: ClientboundUpdateMobEffectPacket
) : PlayerPacketEvent<ClientboundUpdateMobEffectPacket>(player, packet) {
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var effect: Holder<MobEffect> = packet.effect
        set(value) {
            field = value
            changed = true
        }
    
    var effectAmplifier = packet.effectAmplifier
        set(value) {
            field = value
            changed = true
        }
    
    var effectDurationTicks = packet.effectDurationTicks
        set(value) {
            field = value
            changed = true
        }
    
    var ambient = packet.isEffectAmbient
        set(value) {
            field = value
            changed = true
        }
    
    var visible = packet.isEffectVisible
        set(value) {
            field = value
            changed = true
        }
    
    var showIcon = packet.effectShowsIcon()
        set(value) {
            field = value
            changed = true
        }
    
    var blend = packet.shouldBlend()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundUpdateMobEffectPacket {
        val effectInstance = MobEffectInstance(effect, effectDurationTicks, effectAmplifier, ambient, visible, showIcon)
        return ClientboundUpdateMobEffectPacket(entityId, effectInstance, blend)
    }
}
