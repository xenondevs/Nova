package xyz.xenondevs.nova.item.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.options.ChargeableOptions
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.item.novaCompound
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("FunctionName")
fun Chargeable(affectsItemDurability: Boolean): ItemBehaviorFactory<Chargeable> =
    object : ItemBehaviorFactory<Chargeable>() {
        override fun create(material: NovaItem): Chargeable =
            Chargeable(ChargeableOptions.configurable(material), affectsItemDurability)
    }

class Chargeable(
    val options: ChargeableOptions,
    private val affectsItemDurability: Boolean = true
) : ItemBehavior() {
    
    override val vanillaMaterialProperties = if (affectsItemDurability)
        provider(listOf(VanillaMaterialProperty.DAMAGEABLE))
    else provider(emptyList())
    
    @Deprecated("Replaced by ChargeableOptions", ReplaceWith("options.maxEnergy"))
    val maxEnergy: Long
        get() = options.maxEnergy
    
    //<editor-fold desc="Bukkit ItemStack methods", defaultstate="collapsed">
    fun getEnergy(itemStack: ItemStack): Long {
        return getEnergy(itemStack.novaCompound)
    }
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        return setEnergy(itemStack.novaCompound, energy)
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        return addEnergy(itemStack.novaCompound, energy)
    }
    //</editor-fold>
    
    //<editor-fold desc="Mojang ItemStack methods", defaultstate="collapsed">
    fun getEnergy(itemStack: MojangStack): Long {
        return getEnergy(itemStack.novaCompound)
    }
    
    fun setEnergy(itemStack: MojangStack, energy: Long) {
        return setEnergy(itemStack.novaCompound, energy)
    }
    
    fun addEnergy(itemStack: MojangStack, energy: Long) {
        return addEnergy(itemStack.novaCompound, energy)
    }
    //</editor-fold>
    
    //<editor-fold desc="Compound methods", defaultstate="collapsed">
    fun getEnergy(data: NamespacedCompound): Long {
        val currentEnergy = data["nova", "energy"] ?: 0L
        if (currentEnergy > options.maxEnergy) {
            setEnergy(data, options.maxEnergy)
            return options.maxEnergy
        }
        return currentEnergy
    }
    
    fun setEnergy(data: NamespacedCompound, energy: Long) {
        data["nova", "energy"] = energy.coerceIn(0, options.maxEnergy)
    }
    
    fun addEnergy(data: NamespacedCompound, energy: Long) {
        setEnergy(data, getEnergy(data) + energy)
    }
    //</editor-fold>
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it.novaCompound, 0); it }
        return itemBuilder
    }
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        val energy = getEnergy(data)
        
        itemData.addLore(Component.text(NumberFormatUtils.getEnergyString(energy, options.maxEnergy), NamedTextColor.GRAY))
        
        if (affectsItemDurability)
            itemData.durabilityBar = energy.toDouble() / options.maxEnergy.toDouble()
    }
    
    companion object : ItemBehaviorFactory<Chargeable>() {
        override fun create(material: NovaItem): Chargeable =
            Chargeable(ChargeableOptions.configurable(material), true)
    }
    
}