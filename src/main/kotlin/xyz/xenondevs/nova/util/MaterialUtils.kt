package xyz.xenondevs.nova.util

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial

fun Material.isGlass() = name.endsWith("GLASS") || name.endsWith("GLASS_PANE")

fun Material.toItemStack(amount: Int = 1): ItemStack = ItemBuilder(this).setAmount(amount).build()

val ItemStack.novaMaterial: NovaMaterial?
    get() = NovaMaterial.values().find {
        val itemStack = it.createItemStack()
        
        this.type == itemStack.type
            && hasItemMeta()
            && itemMeta!!.hasCustomModelData()
            && itemMeta!!.customModelData == itemStack.itemMeta!!.customModelData
    }
