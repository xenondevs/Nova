package xyz.xenondevs.nova.util.advancement

import net.kyori.adventure.text.Component
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.DisplayInfo
import net.minecraft.advancements.criterion.DataComponentMatchers
import net.minecraft.advancements.criterion.InventoryChangeTrigger
import net.minecraft.advancements.criterion.ItemPredicate
import net.minecraft.advancements.criterion.NbtPredicate
import net.minecraft.core.component.predicates.CustomDataPredicate
import net.minecraft.core.component.predicates.DataComponentPredicates
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import xyz.xenondevs.nova.util.toNmsTemplate
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.createItemStack
import java.util.*

fun advancement(addon: Addon, name: String, init: Advancement.Builder.() -> Unit): AdvancementHolder {
    val builder = Advancement.Builder()
    builder.init()
    return builder.build(Identifier.fromNamespaceAndPath(addon.namespace(), name))
}

fun obtainNovaItemAdvancement(
    addon: Addon,
    parent: AdvancementHolder?,
    item: RegistryEntry.Nova<NovaItem>,
    frameType: AdvancementType = AdvancementType.TASK
): AdvancementHolder {
    require(addon.namespace() == item.key.namespace()) { "The specified item is from a different addon" }
    val id = item.key
    return advancement(addon, "obtain_${id.value()}") {
        if (parent != null)
            parent(parent)
        
        display(DisplayInfo(
            item.createItemStack().toNmsTemplate()!!,
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
    items: List<RegistryEntry.Nova<NovaItem>>, requireAll: Boolean,
    frameType: AdvancementType = AdvancementType.TASK
): AdvancementHolder {
    require(items.all { it.key.namespace() == addon.namespace() }) { "At least one of the specified items is from a different addon" }
    val namespace = addon.namespace()
    return advancement(addon, name) {
        if (parent != null)
            parent(parent)
        
        display(DisplayInfo(
            items[0].createItemStack().toNmsTemplate()!!,
            Component.translatable("advancement.$namespace.$name.title").toNMSComponent(),
            Component.translatable("advancement.$namespace.$name.description").toNMSComponent(),
            Optional.empty(),
            frameType,
            true, true, false
        ))
        
        val criteriaNames = ArrayList<String>()
        
        for (item in items) {
            val criterionName = "obtain_${item.key.value()}"
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

private fun createObtainNovaItemCriterion(item: RegistryEntry.Nova<NovaItem>): Criterion<InventoryChangeTrigger.TriggerInstance> {
    val expectedCustomData = CompoundTag().apply {
        put("nova", CompoundTag().apply {
            putString("id", item.key.toString())
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