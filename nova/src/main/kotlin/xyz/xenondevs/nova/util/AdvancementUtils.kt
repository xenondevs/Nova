package xyz.xenondevs.nova.util

import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.advancement.Advancement
import xyz.xenondevs.nmsutils.advancement.CriteriaBuilder
import xyz.xenondevs.nmsutils.advancement.Criterion
import xyz.xenondevs.nmsutils.advancement.Display
import xyz.xenondevs.nmsutils.advancement.FrameType
import xyz.xenondevs.nmsutils.advancement.advancement
import xyz.xenondevs.nmsutils.advancement.predicate.ItemPredicate
import xyz.xenondevs.nmsutils.advancement.predicate.NbtPredicate
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.material.ItemNovaMaterial

fun ItemPredicate.Builder.nbt(compound: CompoundTag) {
    nbt(NbtPredicate(compound))
}

fun ItemPredicate.Builder.nbt(init: CompoundTag.() -> Unit) {
    val tag = CompoundTag()
    tag.init()
    nbt(tag)
}

fun Display.Builder.icon(icon: ItemNovaMaterial) {
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

fun advancement(addon: Addon, name: String, init: Advancement.Builder.() -> Unit): Advancement =
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

fun Player.awardAdvancement(key: NamespacedKey) {
    val advancement = Bukkit.getAdvancement(key)
    if (advancement != null) {
        val progress = getAdvancementProgress(advancement)
        advancement.criteria.forEach { progress.awardCriteria(it) }
    }
}