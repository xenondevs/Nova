package xyz.xenondevs.nova.item.behavior

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.ChargeableOptions
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.item.retrieveDataOrNull
import xyz.xenondevs.nova.util.item.storeData

private val ENERGY_KEY = NamespacedKey(NOVA, "item_energy")

@Suppress("FunctionName")
fun Chargeable(affectsItemDurability: Boolean): ItemBehaviorFactory<Chargeable> =
    object : ItemBehaviorFactory<Chargeable>() {
        override fun create(material: ItemNovaMaterial): Chargeable =
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
    
    fun getEnergy(itemStack: ItemStack): Long {
        val currentEnergy = itemStack.retrieveDataOrNull<Long>(ENERGY_KEY) ?: 0L
        if (currentEnergy > options.maxEnergy) {
            setEnergy(itemStack, options.maxEnergy)
            return options.maxEnergy
        }
        return currentEnergy
    }
    
    fun setEnergy(itemStack: ItemStack, energy: Long) {
        val coercedEnergy = energy.coerceIn(0, options.maxEnergy)
        itemStack.storeData(ENERGY_KEY, coercedEnergy)
    }
    
    fun addEnergy(itemStack: ItemStack, energy: Long) {
        setEnergy(itemStack, getEnergy(itemStack) + energy)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        itemBuilder.addModifier { setEnergy(it, 0); it }
        return itemBuilder
    }
    
    override fun updatePacketItemData(itemStack: ItemStack, itemData: PacketItemData) {
        val energy = getEnergy(itemStack)
        
        itemData.addLore(TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy, options.maxEnergy)))
        
        if (affectsItemDurability)
            itemData.durabilityBar = energy.toDouble() / options.maxEnergy.toDouble()
    }
    
    companion object : ItemBehaviorFactory<Chargeable>() {
        override fun create(material: ItemNovaMaterial): Chargeable =
            Chargeable(ChargeableOptions.configurable(material), true)
    }
    
}