package xyz.xenondevs.nova.network.event.clientbound

import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
import net.minecraft.core.SectionPos
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSectionBlocksUpdatePacketEvent(
    player: Player,
    packet: ClientboundSectionBlocksUpdatePacket
) : PlayerPacketEvent<ClientboundSectionBlocksUpdatePacket>(player, packet) {
    
    var sectionPos: SectionPos = packet.sectionPos
        set(value) {
            field = value
            changed = true
        }
    
    var positions: ShortArray = packet.positions
        set(value) {
            field = value
            changed = true
        }
    
    var states: Array<BlockState> = packet.states
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSectionBlocksUpdatePacket {
        require(positions.size == states.size)
        val changes = Short2ObjectOpenHashMap<BlockState>(positions.size)
        positions.indices.forEach { changes[positions[it]] = states[it] }
        return ClientboundSectionBlocksUpdatePacket(sectionPos, changes)
    }
}
