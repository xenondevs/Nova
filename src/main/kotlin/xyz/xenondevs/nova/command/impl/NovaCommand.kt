package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import xyz.xenondevs.nova.command.PlayerCommand
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.getPlayer
import xyz.xenondevs.nova.material.NovaMaterial

class NovaCommand(name: String, permission: String) : PlayerCommand(name, permission) {
    
    init {
        builder = builder
            .then(literal("give")
                .apply {
                    NovaMaterial.values().forEach { material ->
                        then(literal(material.name)
                            .executesCatching { context -> handleGive(material, context) }
                        )
                    }
                })
    }
    
    private fun handleGive(material: NovaMaterial, context: CommandContext<Any>) {
        val player = context.getPlayer()
        player.inventory.addItem(material.createItemStack())
        val itemName = material.itemName.ifBlank { material.name }
        player.sendMessage("§7The item §b$itemName§7 has been added to your inventory.")
    }
    
}