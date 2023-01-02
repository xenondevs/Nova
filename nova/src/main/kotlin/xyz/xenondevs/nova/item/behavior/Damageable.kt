package xyz.xenondevs.nova.item.behavior

import net.md_5.bungee.api.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.DamageableOptions
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.item.retrieveDataOrNull
import xyz.xenondevs.nova.util.item.storeData
import kotlin.math.min

private val DAMAGE_KEY = NamespacedKey(NOVA, "damage")

class Damageable(val options: DamageableOptions) : ItemBehavior() {
    
    @Deprecated("Replaced by DamageableOptions", ReplaceWith("options.maxDurability"))
    val maxDurability: Int by options.durabilityProvider
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.DAMAGEABLE))
    
    fun getDamage(itemStack: ItemStack): Int {
        return min(options.durability, itemStack.retrieveDataOrNull(DAMAGE_KEY) ?: 0)
    }
    
    fun setDamage(itemStack: ItemStack, damage: Int) {
        val coercedDamage = damage.coerceIn(0..options.durability)
        itemStack.storeData(DAMAGE_KEY, coercedDamage)
    }
    
    fun addDamage(itemStack: ItemStack, damage: Int) {
        setDamage(itemStack, getDamage(itemStack) + damage)
    }
    
    fun getDurability(itemStack: ItemStack): Int {
        return options.durability - getDamage(itemStack)
    }
    
    fun setDurability(itemStack: ItemStack, durability: Int) {
        setDamage(itemStack, options.durability - durability)
    }
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        val damage = getDamage(itemStack)
        val durability = options.durability - damage
        
        itemData.durabilityBar = durability / options.durability.toDouble()
        
        if (damage != 0) {
            itemData.addAdvancedTooltipsLore(
                arrayOf(localized(ChatColor.WHITE, "item.durability", durability, options.durability))
            )
        }
    }
    
    companion object : ItemBehaviorFactory<Damageable>() {
        override fun create(material: ItemNovaMaterial) =
            Damageable(DamageableOptions.configurable(material))
    }
    
}