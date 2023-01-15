package xyz.xenondevs.nova.item.behavior

import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.DamageableOptions
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.item.novaCompound
import kotlin.math.min
import net.minecraft.world.item.ItemStack as MojangStack

class Damageable(val options: DamageableOptions) : ItemBehavior() {
    
    @Deprecated("Replaced by DamageableOptions", ReplaceWith("options.durability"))
    val durability: Int by options.durabilityProvider
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.DAMAGEABLE))
    
    //<editor-fold desc="Bukkit ItemStack methods", defaultstate="collapsed">
    fun getDamage(itemStack: ItemStack): Int {
        return getDamage(itemStack.novaCompound)
    }
    
    fun setDamage(itemStack: ItemStack, damage: Int) {
        setDamage(itemStack.novaCompound, damage)
    }
    
    fun addDamage(itemStack: ItemStack, damage: Int) {
        addDamage(itemStack.novaCompound, damage)
    }
    
    fun getDurability(itemStack: ItemStack): Int {
        return getDurability(itemStack.novaCompound)
    }
    
    fun setDurability(itemStack: ItemStack, durability: Int) {
        return setDurability(itemStack.novaCompound, durability)
    }
    //</editor-fold>
    
    //<editor-fold desc="Mojang ItemStack methods", defaultstate="collapsed">
    fun getDamage(itemStack: MojangStack): Int {
        return getDamage(itemStack.novaCompound)
    }
    
    fun setDamage(itemStack: MojangStack, damage: Int) {
        setDamage(itemStack.novaCompound, damage)
    }
    
    fun addDamage(itemStack: MojangStack, damage: Int) {
        addDamage(itemStack.novaCompound, damage)
    }
    
    fun getDurability(itemStack: MojangStack): Int {
        return getDurability(itemStack.novaCompound)
    }
    
    fun setDurability(itemStack: MojangStack, durability: Int) {
        return setDurability(itemStack.novaCompound, durability)
    }
    //</editor-fold>
    
    //<editor-fold desc="Compound methods", defaultstate="collapsed">
    fun getDamage(data: Compound): Int {
        return min(options.durability, data["damage"] ?: 0)
    }
    
    fun setDamage(data: Compound, damage: Int) {
        val coercedDamage = damage.coerceIn(0..options.durability)
        data["damage"] = coercedDamage
    }
    
    fun addDamage(data: Compound, damage: Int) {
        setDamage(data, getDamage(data) + damage)
    }
    
    fun getDurability(data: Compound): Int {
        return options.durability - getDamage(data)
    }
    
    fun setDurability(data: Compound, durability: Int) {
        setDamage(data, options.durability - durability)
    }
    //</editor-fold>
    
    override fun updatePacketItemData(data: Compound, itemData: PacketItemData) {
        val damage = getDamage(data)
        val durability = options.durability - damage
        
        itemData.durabilityBar = durability / options.durability.toDouble()
        
        itemData.addAdvancedTooltipsLore(
            arrayOf(localized(ChatColor.WHITE, "item.durability", durability, options.durability))
        )
    }
    
    companion object : ItemBehaviorFactory<Damageable>() {
        override fun create(material: ItemNovaMaterial) =
            Damageable(DamageableOptions.configurable(material))
    }
    
}