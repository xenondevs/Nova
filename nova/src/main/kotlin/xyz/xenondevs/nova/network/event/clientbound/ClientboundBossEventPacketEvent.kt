package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.ClientboundBossEventPacket
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.MutableLazy
import xyz.xenondevs.nova.util.bossbar.operation.BossBarOperation
import java.lang.invoke.MethodHandles
import java.util.*

private val CLIENTBOUND_BOSS_EVENT_PACKET_ID_GETTER = MethodHandles
    .privateLookupIn(ClientboundBossEventPacket::class.java, MethodHandles.lookup())
    .findGetter(ClientboundBossEventPacket::class.java, "id", UUID::class.java)

class ClientboundBossEventPacketEvent(
    player: Player,
    packet: ClientboundBossEventPacket
) : PlayerPacketEvent<ClientboundBossEventPacket>(player, packet) {
    
    var id: UUID by MutableLazy({ changed = true }) { CLIENTBOUND_BOSS_EVENT_PACKET_ID_GETTER.invoke(packet) as UUID }
    var operation: BossBarOperation by MutableLazy({ changed = true }) { BossBarOperation.fromPacket(packet) }
    
    override fun buildChangedPacket(): ClientboundBossEventPacket {
        return ClientboundBossEventPacket(id, operation)
    }
    
}