package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.crafting.RecipePropertySet
import net.minecraft.world.item.crafting.SelectableRecipe
import net.minecraft.world.item.crafting.StonecutterRecipe
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundUpdateRecipesPacketEvent(
    player: Player,
    packet: ClientboundUpdateRecipesPacket
) : PlayerPacketEvent<ClientboundUpdateRecipesPacket>(player, packet) {
    
    var itemSets: Map<ResourceKey<RecipePropertySet>, RecipePropertySet> = packet.itemSets
        set(value) {
            field = value
            changed = true
        }
    
    var stonecutterRecipes: SelectableRecipe.SingleInputSet<StonecutterRecipe> = packet.stonecutterRecipes
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundUpdateRecipesPacket {
        return ClientboundUpdateRecipesPacket(itemSets, stonecutterRecipes)
    }
    
}