@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.util.item

import com.mojang.brigadier.StringReader
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Namespaced
import net.kyori.adventure.text.Component
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.item.AdventureModePredicate
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.Weapon
import net.minecraft.world.item.enchantment.ItemEnchantments
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.asBukkitMirror
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.isNova
import xyz.xenondevs.nova.world.item.itemType
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.item.name
import xyz.xenondevs.nova.world.item.recipe.ComplexTest
import xyz.xenondevs.nova.world.item.recipe.CustomRecipeChoice
import xyz.xenondevs.nova.world.item.recipe.ItemTypeTest
import xyz.xenondevs.nova.world.item.recipe.NovaNameTest
import xyz.xenondevs.nova.world.item.recipe.TagTest
import java.util.*
import kotlin.contracts.contract
import kotlin.math.max
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("DEPRECATION")
internal val MojangStack.unsafeCustomData: CompoundTag?
    get() = components.get(DataComponents.CUSTOM_DATA)?.unsafe

internal val ItemStack.namelessCopyOrSelf: ItemStack
    get() {
        var itemStack = this
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            if (itemMeta.hasDisplayName()) {
                itemMeta.displayName(null)
                itemStack = clone().apply { setItemMeta(itemMeta) }
            }
        }
        
        return itemStack
    }

/**
 * Returns `null` if [this][ItemStack] [is empty][isEmpty], otherwise returns [this][ItemStack].
 */
fun ItemStack.takeUnlessEmpty(): ItemStack? =
    if (isEmpty) null else this

/**
 * Returns `true` if [this][ItemStack] is either null or an empty item stack,
 * otherwise returns `false`.
 */
fun ItemStack?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || isEmpty
}

/**
 * Returns `true` if [this][ItemStack] is neither null, nor an empty item stack,
 * otherwise returns `false`.
 */
fun ItemStack?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && !isEmpty
}

internal fun <T : Any> MojangStack.update(type: DataComponentType<T>, action: (T) -> T): T? =
    get(type)?.let { set(type, action(it)) }

internal fun ItemStack.clientsideCopy(): ItemStack =
    PacketItems.getClientSideStack(null, unwrap(), true).asBukkitMirror()

//<editor-fold desc="Nova item data", defaultstate="collapsed">
inline fun <reified T : Any> ItemStack.retrieveData(key: Key): T? = persistentDataContainer[key]
inline fun <reified T : Any> ItemStack.retrieveData(addon: Namespaced, key: String): T? = retrieveData(addon.namespace(), key)
inline fun <reified T : Any> ItemStack.retrieveData(namespace: String, key: String): T? = retrieveData(Key.key(namespace, key))

inline fun <reified T : Any> ItemStack.storeData(key: Key, data: T?) {
    editPersistentDataContainer { it[key] = data }
}

inline fun <reified T : Any> ItemStack.storeData(addon: Namespaced, key: String, data: T?) = storeData(addon.namespace(), key, data)
inline fun <reified T : Any> ItemStack.storeData(namespace: String, key: String, data: T?) = storeData(Key.key(namespace, key), data)

inline fun <reified T : Any> MojangStack.retrieveData(key: Key): T? = asBukkitMirror().retrieveData(key)
inline fun <reified T : Any> MojangStack.retrieveData(addon: Namespaced, key: String): T? = retrieveData(addon.namespace(), key)
inline fun <reified T : Any> MojangStack.retrieveData(namespace: String, key: String): T? = retrieveData(Key.key(namespace, key))

inline fun <reified T : Any> MojangStack.storeData(key: Key, data: T?) = asBukkitMirror().storeData(key, data)
inline fun <reified T : Any> MojangStack.storeData(addon: Namespaced, key: String, data: T?) = storeData(addon.namespace(), key, data)
inline fun <reified T : Any> MojangStack.storeData(namespace: String, key: String, data: T?) = storeData(Key.key(namespace, key), data)
//</editor-fold>

object ItemUtils {
    
    fun isIdRegistered(id: String): Boolean {
        if (CustomItemServiceManager.getItemByName(id) != null)
            return true
        val namespace = id.substringBefore(':')
        val name = id.substringAfter(':')
        return when (namespace) {
            "nova" -> Registry.ITEM.any { it.isNova && it.key.value() == name }
            else -> Key.parseable(id) && Registry.ITEM.get(Key.key(id)) != null
        }
    }
    
    fun getRecipeChoice(nameList: List<String>): RecipeChoice {
        val tests = nameList.map { id ->
            try {
                if (id.startsWith("#")) {
                    val tagName = NamespacedKey.fromString(id.substringAfter('#'))
                        ?: throw IllegalArgumentException("Malformed tag: $id")
                    val tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagName, Material::class.java)
                        ?: throw IllegalArgumentException("Invalid tag: $id")
                    return@map TagTest(tag)
                }
                
                if (id.contains("{"))
                    return@map ComplexTest(toItemStack(id))
                
                when (id.substringBefore(':')) {
                    "nova" -> {
                        val name = id.substringAfter(':')
                        val novaItems = Registry.ITEM.filter { it.isNova && it.key.value() == name }
                        if (novaItems.isNotEmpty()) {
                            return@map NovaNameTest(name, novaItems.map { it.createItemStack() })
                        } else throw IllegalArgumentException("Not an item name in Nova: $name")
                    }
                    
                    else -> ItemTypeTest(Registry.ITEM.getOrThrow(Key.key(id)))
                }
            } catch (ex: Exception) {
                throw IllegalArgumentException("Unknown item $id", ex)
            }
        }
        
        return CustomRecipeChoice(tests)
    }
    
    /**
     * Creates an [ItemStack] from the given [String].
     * Resolves ids from vanilla, nova and custom item services. Can also parse snbt.
     */
    fun getItemStack(s: String): ItemStack {
        val namespace = s.substringBefore(':')
        val name = s.substringAfter(':')
        return when (namespace) {
            "minecraft" -> toItemStack(s)
            "nova" -> Registry.ITEM.firstOrNull { it.isNova && it.key.value() == name }
                ?.createItemStack()
                ?: throw IllegalArgumentException("Unknown Nova item: $name")
            else -> CustomItemServiceManager.getItemByName(s) ?: getItemStack(Key.key(s))
        }
    }
    
    /**
     * Creates an [ItemStack] from the given [id]. Resolves ids from vanilla, nova and custom item services.
     */
    @Deprecated("Use registry instead", ReplaceWith("Registry.ITEM.get(id).createItemStack()", imports = ["org.bukkit.Registry"]))
    fun getItemStack(id: Key): ItemStack =
        Registry.ITEM.get(id)?.createItemStack() ?: throw IllegalArgumentException("Unknown item $id")
    
    /**
     * Gets the actually displayed name of the given [itemStack].
     * If the [itemStack] has a custom display name, that will be returned. Otherwise, the localized name will be returned.
     */
    fun getName(itemStack: ItemStack): Component =
        itemStack.getData(DataComponentTypes.CUSTOM_NAME) ?: itemStack.itemType.name
    
    /**
     * Converts the given string to an [ItemStack].
     */
    fun toItemStack(s: String): ItemStack {
        val parser = ItemParser(REGISTRY_ACCESS)
        val result = parser.parse(StringReader(s))
        return MojangStack(result.item, 1, result.components).asBukkitMirror()
    }
    
    /**
     * Gets the id of the given [itemStack].
     */
    @Deprecated("Use ItemType instead", ReplaceWith("itemStack.itemType.key", imports = ["xyz.xenondevs.nova.world.item.itemType"]))
    fun getId(itemStack: ItemStack): Key = itemStack.itemType.key
    
    @Suppress("UNCHECKED_CAST")
    internal fun mergeDataComponentPatches(dataComponentPatches: List<DataComponentPatch>): DataComponentPatch {
        val components = HashMap<DataComponentType<Any>, ArrayList<Optional<Any>>>()
        
        for (dataComponentPatch in dataComponentPatches) {
            val (added, removed) = dataComponentPatch.split()
            for (component in added) {
                val type = component.type as DataComponentType<Any>
                components.getOrPut(type, ::ArrayList) += Optional.of(component.value)
            }
            for (type in removed) {
                type as DataComponentType<Any>
                components.getOrPut(type, ::ArrayList) += Optional.empty()
            }
        }
        
        val builder = DataComponentPatch.builder()
        for ([type, valueOpts] in components) {
            // only merge in data components that were added after the last unset (empty optional)
            val valuesAfterUnset = ArrayList<Any>()
            for (valueOpt in valueOpts) {
                if (valueOpt.isPresent) {
                    valuesAfterUnset += valueOpt.get()
                } else {
                    valuesAfterUnset.clear()
                }
            }
            
            if (valuesAfterUnset.isNotEmpty()) {
                builder.set(TypedDataComponent.createUnchecked(type, mergeDataComponents(type, valuesAfterUnset)))
            } else {
                builder.remove(type)
            }
        }
        return builder.build()
    }
    
    @Suppress("UNCHECKED_CAST")
    internal fun mergeDataComponentMaps(dataComponentMaps: List<DataComponentMap>): DataComponentMap {
        val components = HashMap<DataComponentType<Any>, ArrayList<Any>>()
        
        for (dataComponentMap in dataComponentMaps) {
            for (typedComponent in dataComponentMap) {
                val type = typedComponent.type as DataComponentType<Any>
                val value = typedComponent.value
                components.getOrPut(type, ::ArrayList) += value
            }
        }
        
        val builder = DataComponentMap.builder()
        for ([type, values] in components) {
            builder.set(type, mergeDataComponents(type, values))
        }
        return builder.build()
    }
    
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> mergeDataComponents(type: DataComponentType<T>, values: List<T>): T {
        require(values.isNotEmpty())
        if (values.size == 1)
            return values.first()
        
        return when (type) {
            DataComponents.ATTRIBUTE_MODIFIERS -> mergeAttributeModifiers(values as List<ItemAttributeModifiers>)
            DataComponents.BLOCK_ENTITY_DATA -> mergeCustomData(values as List<CustomData>)
            DataComponents.CAN_BREAK -> mergeAdventureModePredicate(values as List<AdventureModePredicate>)
            DataComponents.CAN_PLACE_ON -> mergeAdventureModePredicate(values as List<AdventureModePredicate>)
            DataComponents.CUSTOM_DATA -> mergeCustomData(values as List<CustomData>)
            DataComponents.ENCHANTMENTS -> mergeEnchantments(values as List<ItemEnchantments>)
            DataComponents.ENTITY_DATA -> mergeCustomData(values as List<CustomData>)
            DataComponents.LORE -> mergeLore(values as List<ItemLore>)
            DataComponents.POTION_CONTENTS -> mergePotionContents(values as List<PotionContents>)
            DataComponents.RECIPES -> mergeIdentifiers(values as List<List<Identifier>>)
            DataComponents.STORED_ENCHANTMENTS -> mergeEnchantments(values as List<ItemEnchantments>)
            DataComponents.TOOLTIP_DISPLAY -> mergeTooltipDisplay(values as List<TooltipDisplay>)
            DataComponents.WEAPON -> mergeWeapon(values as List<Weapon>)
            else -> values.last()
        } as T
    }
    
    internal fun mergeAttributeModifiers(values: List<ItemAttributeModifiers>): ItemAttributeModifiers {
        val builder = ItemAttributeModifiers.builder()
        for (itemAttributeModifiers in values) {
            for (modifier in itemAttributeModifiers.modifiers) {
                builder.add(modifier.attribute, modifier.modifier, modifier.slot, modifier.display)
            }
        }
        return builder.build()
    }
    
    internal fun mergeAdventureModePredicate(values: List<AdventureModePredicate>): AdventureModePredicate {
        return AdventureModePredicate(values.flatMap { it.predicates })
    }
    
    @Suppress("DEPRECATION")
    internal fun mergeCustomData(values: List<CustomData>): CustomData {
        val nbt = CompoundTag()
        for (customData in values) {
            nbt.merge(customData.unsafe)
        }
        return CustomData.of(nbt)
    }
    
    internal fun mergeEnchantments(values: List<ItemEnchantments>): ItemEnchantments {
        val enchantments = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
        for (itemEnchantments in values) {
            for ([enchantment, level] in itemEnchantments.entrySet()) {
                enchantments.set(enchantment, max(enchantments.getLevel(enchantment), level))
            }
        }
        return enchantments.toImmutable()
    }
    
    internal fun mergeLore(values: List<ItemLore>): ItemLore {
        return ItemLore(values.flatMap { it.lines }, values.flatMap { it.styledLines })
    }
    
    internal fun mergePotionContents(values: List<PotionContents>): PotionContents {
        return PotionContents(
            Optional.empty(),
            Optional.empty(),
            values.flatMap { it.allEffects },
            values.asSequence().map { it.customName }.lastOrNull { it.isPresent } ?: Optional.empty()
        )
    }
    
    internal fun mergeTooltipDisplay(values: List<TooltipDisplay>): TooltipDisplay {
        return TooltipDisplay(
            values.any { it.hideTooltip },
            values.flatMapTo(LinkedHashSet()) { it.hiddenComponents }
        )
    }
    
    internal fun mergeWeapon(values: List<Weapon>): Weapon {
        // This is not ideal, but currently required to prevent conflict between Damageable and Tool behavior's
        // usage of the Weapon component.
        return Weapon(
            values.maxOf { it.itemDamagePerAttack },
            values.maxOf { it.disableBlockingForSeconds }
        )
    }
    
    internal fun mergeIdentifiers(values: List<List<Identifier>>): List<Identifier> {
        val set = LinkedHashSet<Identifier>()
        for (value in values) {
            set.addAll(value)
        }
        return ArrayList(set)
    }
    
}
