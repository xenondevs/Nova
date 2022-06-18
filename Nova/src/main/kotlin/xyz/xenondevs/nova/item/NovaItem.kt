package xyz.xenondevs.nova.item

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Handles actions performed on [ItemStack]s of a [ItemNovaMaterial]
 */
class NovaItem(val behaviors: List<ItemBehavior>) {
    
    private lateinit var material: ItemNovaMaterial
    private lateinit var name: Array<BaseComponent>
    
    constructor(vararg behaviors: ItemBehavior) : this(behaviors.toList())
    
    internal fun setMaterial(material: ItemNovaMaterial) {
        if (::material.isInitialized)
            throw IllegalStateException("NovaItems cannot be used for multiple materials")
        
        this.material = material
        this.name = TranslatableComponent(material.localizedName).withoutPreFormatting()
    }
    
    fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        behaviors.forEach { builder = it.modifyItemBuilder(builder) }
        return builder
    }
    
    fun getName(itemStack: ItemStack): Array<BaseComponent> {
        return behaviors.firstNotNullOfOrNull { it.getName(itemStack) } ?: name
    }
    
    fun getLore(itemStack: ItemStack): List<Array<BaseComponent>> {
        val lore = ArrayList<Array<BaseComponent>>()
        behaviors.forEach { it.getLore(itemStack)?.also(lore::addAll) }
        return lore
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ItemBehavior> getBehavior(type: KClass<T>): T? {
        return behaviors.firstOrNull { type == it::class || type in it::class.superclasses } as T?
    }
    
    fun hasBehavior(type: KClass<out ItemBehavior>): Boolean {
        return behaviors.any { it::class == type }
    }
    
}