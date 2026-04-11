package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundMerchantOffersPacketEvent(
    player: Player,
    packet: ClientboundMerchantOffersPacket
) : PlayerPacketEvent<ClientboundMerchantOffersPacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    
    var offers = packet.offers
        set(value) {
            field = value
            changed = true
        }
    
    var villagerLevel = packet.villagerLevel
        set(value) {
            field = value
            changed = true
        }
    
    var villagerXp = packet.villagerXp
        set(value) {
            field = value
            changed = true
        }
    
    var showProgressBar = packet.showProgress()
        set(value) {
            field = value
            changed = true
        }
    
    var canRestock = packet.canRestock()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundMerchantOffersPacket {
        return ClientboundMerchantOffersPacket(containerId, offers, villagerLevel, villagerXp, showProgressBar, canRestock)
    }
    
}