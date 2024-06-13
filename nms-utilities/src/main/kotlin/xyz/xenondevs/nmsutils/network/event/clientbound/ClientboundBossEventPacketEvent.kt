package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.bossbar.operation.BossBarOperation
import xyz.xenondevs.nmsutils.internal.MutableLazy
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry.CLIENTBOUND_BOSS_EVENT_PACKET_ID_FIELD
import xyz.xenondevs.nmsutils.network.ClientboundBossEventPacket
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent
import java.util.*

class ClientboundBossEventPacketEvent(
    player: Player,
    packet: ClientboundBossEventPacket
) : PlayerPacketEvent<ClientboundBossEventPacket>(player, packet) {
    
    var id: UUID by MutableLazy({changed = true}) { CLIENTBOUND_BOSS_EVENT_PACKET_ID_FIELD.get(packet) as UUID }
    var operation: BossBarOperation by MutableLazy({ changed = true }) { BossBarOperation.fromPacket(packet) }
    
    override fun buildChangedPacket(): ClientboundBossEventPacket {
        return ClientboundBossEventPacket(id, operation)
    }
    
}