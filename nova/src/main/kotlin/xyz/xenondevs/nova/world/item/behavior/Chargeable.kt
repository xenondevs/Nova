package xyz.xenondevs.nova.world.item.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import org.bukkit.inventory.ItemStack as BukkitStack

private val ENERGY_KEY = ResourceLocation.fromNamespaceAndPath("nova", "energy")

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
        
        override val defaultCompound = provider {
            NamespacedCompound().apply { this[ENERGY_KEY] = 0L }
        }
        
        override fun modifyClientSideStack(player: Player?, itemStack: ItemStack, data: NamespacedCompound): ItemStack {
            val energy = data[ENERGY_KEY] ?: 0L
            
            val lore = itemStack.lore() ?: mutableListOf()
            lore += Component.text(
                NumberFormatUtils.getEnergyString(energy, maxEnergy),
                NamedTextColor.GRAY
            ).withoutPreFormatting()
            itemStack.lore(lore)
            
            if (affectsItemDurability) {
                val fraction = (maxEnergy - energy) / maxEnergy.toDouble()
                val damage = (fraction * Int.MAX_VALUE).toInt()
                itemStack.unwrap().set(DataComponents.MAX_DAMAGE, Int.MAX_VALUE)
                itemStack.unwrap().set(DataComponents.DAMAGE, damage)
            }
            
            return itemStack
        }
        
        override fun getEnergy(itemStack: BukkitStack): Long =
            itemStack.retrieveData(ENERGY_KEY) ?: 0L
        
        override fun setEnergy(itemStack: BukkitStack, energy: Long) =
            itemStack.storeData(ENERGY_KEY, energy.coerceIn(0..maxEnergy))
        
        override fun addEnergy(itemStack: BukkitStack, energy: Long) {
            val compound = itemStack.novaCompound ?: NamespacedCompound()
            val currentEnergy = compound[ENERGY_KEY] ?: 0L
            compound[ENERGY_KEY] = (currentEnergy + energy).coerceIn(0..maxEnergy)
            itemStack.novaCompound = compound
        }
        
    }
    
}