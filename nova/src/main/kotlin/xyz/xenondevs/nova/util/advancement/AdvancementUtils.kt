package xyz.xenondevs.nova.util.advancement

import net.kyori.adventure.text.Component
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.DisplayInfo
import net.minecraft.advancements.critereon.DataComponentMatchers
import net.minecraft.advancements.critereon.InventoryChangeTrigger
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.advancements.critereon.NbtPredicate
import net.minecraft.core.component.predicates.CustomDataPredicate
import net.minecraft.core.component.predicates.DataComponentPredicates
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.*

fun advancement(addon: Addon, name: String, init: Advancement.Builder.() -> Unit): AdvancementHolder {
    val builder = Advancement.Builder()
    builder.init()
    return builder.build(ResourceLocation.fromNamespaceAndPath(addon.id, name))
}

fun obtainNovaItemAdvancement(
    addon: Addon,
    parent: AdvancementHolder?,
    item: NovaItem,
    frameType: AdvancementType = AdvancementType.TASK
): AdvancementHolder {
    require(addon.id == item.id.namespace()) { "The specified item is from a different addon" }
    val id = item.id
    return advancement(addon, "obtain_${id.value()}") {
        if (parent != null)
            parent(parent)
        
        display(DisplayInfo(
            item.clientsideProvider.get().unwrap().copy(),
            Component.translatable("advancement.${id.namespace()}.${id.value()}.title").toNMSComponent(),
            Component.translatable("advancement.${id.namespace()}.${id.value()}.description").toNMSComponent(),
            Optional.empty(),
            frameType,
            true, true, false
        ))
        
        addCriterion("obtain_${id.value()}", createObtainNovaItemCriterion(item))
    }
}

fun obtainNovaItemsAdvancement(
    addon: Addon,
    name: String,
    parent: AdvancementHolder?,
    items: List<NovaItem>, requireAll: Boolean,
    frameType: AdvancementType = AdvancementType.TASK
): AdvancementHolder {
    require(items.all { it.id.namespace() == addon.id }) { "At least one of the specified items is from a different addon" }
    val namespace = addon.id
    return advancement(addon, name) {
        if (parent != null)
            parent(parent)
        
        display(DisplayInfo(
            items[0].clientsideProvider.get().unwrap().copy(),
            Component.translatable("advancement.$namespace.$name.title").toNMSComponent(),
            Component.translatable("advancement.$namespace.$name.description").toNMSComponent(),
            Optional.empty(),
            frameType,
            true, true, false
        ))
        
        val criteriaNames = ArrayList<String>()
        
        for (item in items) {
            val criterionName = "obtain_${item.id.value()}"
            addCriterion(criterionName, createObtainNovaItemCriterion(item))
            criteriaNames += criterionName
        }
        
        if (requireAll) {
            requirements(AdvancementRequirements.allOf(criteriaNames))
        } else {
            requirements(AdvancementRequirements.anyOf(criteriaNames))
        }
    }
}

private fun createObtainNovaItemCriterion(item: NovaItem): Criterion<InventoryChangeTrigger.TriggerInstance> {
    val expectedCustomData = CompoundTag().apply {
        put("nova", CompoundTag().apply {
            putString("id", item.id.toString())
        })
    }
    return InventoryChangeTrigger.TriggerInstance.hasItems(
        ItemPredicate.Builder.item().withComponents(
            DataComponentMatchers.Builder.components()
                .partial(DataComponentPredicates.CUSTOM_DATA, CustomDataPredicate(NbtPredicate(expectedCustomData)))
                .build()
        )
    )
}

fun Player.awardAdvancement(key: NamespacedKey) {
    val advancement = Bukkit.getAdvancement(key)
    if (advancement != null) {
        val progress = getAdvancementProgress(advancement)
        advancement.criteria.forEach { progress.awardCriteria(it) }
    }
}