@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.provider.combinedLazyProvider
import xyz.xenondevs.nova.data.provider.flatten
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.resources.builder.content.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import xyz.xenondevs.nova.util.enumMapOf
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Handles actions performed on [ItemStack]s of a [ItemNovaMaterial]
 */
class NovaItem(holders: List<ItemBehaviorHolder<*>>) {
    
    val behaviors by lazy { holders.map { it.get(material) } }
    private lateinit var material: ItemNovaMaterial
    private lateinit var name: Array<BaseComponent>
    
    internal val vanillaMaterialProvider = combinedLazyProvider { behaviors.map(ItemBehavior::vanillaMaterialProperties) }
        .flatten()
        .map { VanillaMaterialTypes.getMaterial(it.toHashSet()) }
    internal val attributeModifiersProvider = combinedLazyProvider { behaviors.map(ItemBehavior::attributeModifiers) }
        .flatten()
        .map { modifiers ->
            val map = enumMapOf<EquipmentSlot, ArrayList<AttributeModifier>>()
            modifiers.forEach { modifier -> modifier.slots.forEach { slot -> map.getOrPut(slot, ::ArrayList) += modifier } }
            return@map map
        }
    
    internal val vanillaMaterial: Material by vanillaMaterialProvider
    internal val attributeModifiers: Map<EquipmentSlot, List<AttributeModifier>> by attributeModifiersProvider
    
    constructor(vararg holders: ItemBehaviorHolder<*>) : this(holders.toList())
    
    internal fun setMaterial(material: ItemNovaMaterial) {
        if (::material.isInitialized)
            throw IllegalStateException("NovaItems cannot be used for multiple materials")
        
        this.material = material
        this.name = TranslatableComponent(material.localizedName).withoutPreFormatting()
    }
    
    internal fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    internal fun getPacketItemData(itemStack: ItemStack, nbt: CompoundTag): PacketItemData {
        val itemData = PacketItemData(nbt)
        behaviors.forEach { it.updatePacketItemData(itemStack, itemData) }
        return itemData.also { if (it.name == null) it.name = this.name }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ItemBehavior> getBehavior(type: KClass<T>): T? {
        return behaviors.firstOrNull { type == it::class || type in it::class.superclasses } as T?
    }
    
    fun hasBehavior(type: KClass<out ItemBehavior>): Boolean {
        return behaviors.any { it::class == type }
    }
    
}