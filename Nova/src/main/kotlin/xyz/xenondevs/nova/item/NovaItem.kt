package xyz.xenondevs.nova.item

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.material.ItemNovaMaterial
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Handles actions performed on [ItemStack]s of a [ItemNovaMaterial]
 */
class NovaItem(val behaviors: List<ItemBehavior>) {
    
    constructor(vararg behaviors: ItemBehavior) : this(behaviors.toList())
    
    fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        behaviors.forEach {
            builder = it.modifyItemBuilder(builder)
        }
        return builder
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : ItemBehavior> getBehavior(type: KClass<T>): T? {
        return behaviors.firstOrNull { type == it::class || type in it::class.superclasses } as T?
    }
    
    fun hasBehavior(type: KClass<out ItemBehavior>): Boolean {
        return behaviors.any { it::class == type }
    }
    
}