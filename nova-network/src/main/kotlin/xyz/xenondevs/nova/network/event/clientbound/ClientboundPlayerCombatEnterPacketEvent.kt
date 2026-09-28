package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundPlayerCombatEnterPacketEvent(
    player: Player,
    packet: ClientboundPlayerCombatEnterPacket
) : PlayerPacketEvent<ClientboundPlayerCombatEnterPacket>(player, packet)
