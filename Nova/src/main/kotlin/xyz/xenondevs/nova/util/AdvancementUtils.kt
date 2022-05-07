package xyz.xenondevs.nova.util

import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.nbt.CompoundTag
import xyz.xenondevs.kadvancements.Advancement
import xyz.xenondevs.kadvancements.Criterion
import xyz.xenondevs.kadvancements.FrameType
import xyz.xenondevs.kadvancements.builder.AdvancementBuilder
import xyz.xenondevs.kadvancements.builder.CriteriaBuilder
import xyz.xenondevs.kadvancements.builder.DisplayBuilder
import xyz.xenondevs.kadvancements.builder.advancement
import xyz.xenondevs.kadvancements.builder.predicate.ItemPredicateBuilder
import xyz.xenondevs.kadvancements.predicate.NbtPredicate
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.material.ItemNovaMaterial

fun ItemPredicateBuilder.nbt(compound: CompoundTag) {
    nbt(NbtPredicate(compound))
}

fun ItemPredicateBuilder.nbt(init: CompoundTag.() -> Unit) {
    val tag = CompoundTag()
    tag.init()
    nbt(tag)
}

fun DisplayBuilder.icon(icon: ItemNovaMaterial) {
    icon(icon.clientsideProvider.get())
}

fun CriteriaBuilder.obtainNovaItem(item: ItemNovaMaterial): Criterion {
    return inventoryChanged("obtain_${item.id}") {
        item {
            nbt {
                val nova = CompoundTag()
                nova.putString("id", item.id.toString())
                put("nova", nova)
            }
        }
    }
}

fun advancement(addon: Addon, name: String, init: AdvancementBuilder.() -> Unit): Advancement =
    advancement("${addon.description.id}:$name", init)

fun obtainNovaItemAdvancement(
    addon: Addon,
    parent: Advancement?,
    item: ItemNovaMaterial,
    frameType: FrameType = FrameType.TASK
): Advancement {
    require(addon.description.id == item.id.namespace) { "The specified item is from a different addon" }
    
    val id = item.id
    return advancement("${id.namespace}:obtain_${id.name}") {
        if (parent != null) parent(parent)
        
        display {
            icon(item.clientsideProvider.get())
            frame(frameType)
            title(TranslatableComponent("advancement.${id.namespace}.${id.name}.title"))
            description(TranslatableComponent("advancement.${id.namespace}.${id.name}.description"))
        }
        
        criteria { obtainNovaItem(item) }
    }
}

fun obtainNovaItemsAdvancement(
    addon: Addon,
    name: String,
    parent: Advancement?,
    items: List<ItemNovaMaterial>, requireAll: Boolean,
    frameType: FrameType = FrameType.TASK
): Advancement {
    require(items.all { it.id.namespace == addon.description.id }) { "At least one of the specified items is from a different addon" }
    
    val namespace = addon.description.id
    return advancement("$namespace:$name") {
        if (parent != null) parent(parent)
        
        display {
            icon(items[0].clientsideProvider.get())
            frame(frameType)
            title(TranslatableComponent("advancement.$namespace.$name.title"))
            description(TranslatableComponent("advancement.$namespace.$name.description"))
        }
        
        val criteria = ArrayList<Criterion>()
        
        criteria { items.forEach { obtainNovaItem(it).also(criteria::add) } }
        
        if (!requireAll) {
            requirements {
                requirement {
                    criteria(criteria)
                }
            }
        }
        
    }
}