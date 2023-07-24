package xyz.xenondevs.nova.item.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.item.novaCompound
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

@Suppress("FunctionName")
fun Chargeable(affectsItemDurability: Boolean): ItemBehaviorFactory<Chargeable.Default> =
    object : ItemBehaviorFactory<Chargeable.Default> {
        override fun create(item: NovaItem): Chargeable.Default {
            return Chargeable.Default(item.config.entry("max_energy"), affectsItemDurability)
        }
    }

/**
 * Allows items to store energy and be charged.
 */
interface Chargeable {
    
    /**
     * The maximum amount of energy this item can store.
     */
    val maxEnergy: Long
    
    /**
     * Gets the current amount of energy stored in the given [itemStack].
     */
    fun getEnergy(itemStack: BukkitStack): Long
    
    /**
     * Sets the current amount of energy stored in the given [itemStack] to [energy].
     */
    fun setEnergy(itemStack: BukkitStack, energy: Long)
    
    /**
     * Adds the given [energy] to the current amount of energy stored in the given [itemStack], capped at [maxEnergy].
     */
    fun addEnergy(itemStack: BukkitStack, energy: Long)
    
    /**
     * Gets the current amount of energy stored in the given [itemStack].
     */
    fun getEnergy(itemStack: MojangStack): Long
    
    /**
     * Sets the current amount of energy stored in the given [itemStack] to [energy].
     */
    fun setEnergy(itemStack: MojangStack, energy: Long)
    
    /**
     * Adds the given [energy] to the current amount of energy stored in the given [itemStack], capped at [maxEnergy].
     */
    fun addEnergy(itemStack: MojangStack, energy: Long)
    
    companion object : ItemBehaviorFactory<Default> {
        
        override fun create(item: NovaItem): Default {
            return Default(item.config.entry<Long>("max_energy"), true)
        }
        
    }
    
    class Default(
        maxEnergy: Provider<Long>,
        private val affectsItemDurability: Boolean
    ) : ItemBehavior, Chargeable {
        
        override val maxEnergy by maxEnergy
        
        override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
            return if (affectsItemDurability)
                listOf(VanillaMaterialProperty.DAMAGEABLE)
            else emptyList()
        }
        
        override fun getDefaultCompound(): NamespacedCompound {
            val compound = NamespacedCompound()
            compound["nova", "energy"] = 0L
            return compound
        }
        
        override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
            val energy = getEnergy(data)
            itemData.addLore(Component.text(NumberFormatUtils.getEnergyString(energy, maxEnergy), NamedTextColor.GRAY))
            if (affectsItemDurability)
                itemData.durabilityBar = energy.toDouble() / maxEnergy.toDouble()
        }
        
        override fun getEnergy(itemStack: BukkitStack) = getEnergy(itemStack.novaCompound)
        override fun setEnergy(itemStack: BukkitStack, energy: Long) = setEnergy(itemStack.novaCompound, energy)
        override fun addEnergy(itemStack: BukkitStack, energy: Long) = addEnergy(itemStack.novaCompound, energy)
        override fun getEnergy(itemStack: MojangStack) = getEnergy(itemStack.novaCompound)
        override fun setEnergy(itemStack: MojangStack, energy: Long) = setEnergy(itemStack.novaCompound, energy)
        override fun addEnergy(itemStack: MojangStack, energy: Long) = addEnergy(itemStack.novaCompound, energy)
        
        private fun getEnergy(data: NamespacedCompound): Long {
            val currentEnergy = data["nova", "energy"] ?: 0L
            if (currentEnergy > maxEnergy) {
                setEnergy(data, maxEnergy)
                return maxEnergy
            }
            return currentEnergy
        }
        
        private fun setEnergy(data: NamespacedCompound, energy: Long) {
            data["nova", "energy"] = energy.coerceIn(0, maxEnergy)
        }
        
        private fun addEnergy(data: NamespacedCompound, energy: Long) {
            setEnergy(data, getEnergy(data) + energy)
        }
        
    }
    
}