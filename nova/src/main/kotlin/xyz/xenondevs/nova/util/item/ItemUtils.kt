@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.util.item

import com.mojang.brigadier.StringReader
import net.kyori.adventure.text.Component
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.AdventureModePredicate
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.ItemEnchantments
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.api.NamespacedId
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.bukkitMaterial
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.data.getByteArrayOrNull
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.item.recipe.ComplexTest
import xyz.xenondevs.nova.world.item.recipe.CustomRecipeChoice
import xyz.xenondevs.nova.world.item.recipe.ModelDataTest
import xyz.xenondevs.nova.world.item.recipe.NovaIdTest
import xyz.xenondevs.nova.world.item.recipe.NovaNameTest
import xyz.xenondevs.nova.world.item.recipe.TagTest
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import net.minecraft.world.item.ItemStack as MojangStack

val ItemStack.novaItem: NovaItem?
    get() = unwrap().novaItem

val MojangStack.novaItem: NovaItem?
    get() = unsafeNovaTag
        ?.getString("id")
        ?.let(NovaRegistries.ITEM::getValue)

@Suppress("DEPRECATION")
internal val MojangStack.unsafeCustomData: CompoundTag?
    get() = components.get(DataComponents.CUSTOM_DATA)?.unsafe

internal val MojangStack.unsafeNovaTag: CompoundTag?
    get() = unsafeCustomData?.getCompoundOrNull("nova")

val ItemStack.customModelData: Int
    get() {
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            if (itemMeta.hasCustomModelData())
                return itemMeta.customModelData
        }
        
        return 0
    }

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

val ItemStack.craftingRemainingItem: ItemStack?
    get() {
        val novaItem = novaItem
        if (novaItem != null)
            return novaItem.craftingRemainingItem
        
        return type.craftingRemainingItem?.let(::ItemStack)
    }

fun ItemStack.takeUnlessEmpty(): ItemStack? =
    if (type.isAir || amount <= 0) null else this

fun ItemStack?.isNullOrEmpty(): Boolean =
    this == null || type.isAir || amount <= 0

fun ItemStack?.isNotNullOrEmpty(): Boolean =
    this != null && !type.isAir && amount > 0

internal fun <T> MojangStack.update(type: DataComponentType<T>, action: (T) -> T): T? =
    get(type)?.let { set(type, action(it)) }

fun ItemStack.damage(amount: Int, world: World): ItemStack? {
    val nms = unwrap()
    val ref = AtomicReference(nms)
    nms.hurtAndBreak(amount, world.serverLevel, null) {
        ref.set(MojangStack.EMPTY)
    }
    return ref.get().asBukkitMirror().takeUnlessEmpty()
}

internal fun ItemStack.clientsideCopy(): ItemStack =
    PacketItems.getClientSideStack(null, unwrap(), true).asBukkitMirror()

//<editor-fold desc="Nova Compound", defaultstate="collapsed">
var ItemStack.novaCompound: NamespacedCompound?
    get() = unwrap().novaCompound
    set(novaCompound) {
        CraftItemStack.unwrap(this).novaCompound = novaCompound
    }

var MojangStack.novaCompound: NamespacedCompound?
    get() = unsafeCustomData
        ?.getByteArrayOrNull("nova_cbf")
        ?.let(CBF::read)
    set(novaCompound) {
        if (novaCompound != null) {
            update(DataComponents.CUSTOM_DATA, CustomData.EMPTY) { customData ->
                customData.update { compoundTag ->
                    compoundTag.putByteArray("nova_cbf", CBF.write(novaCompound))
                }
            }
        } else {
            var customData = get(DataComponents.CUSTOM_DATA) ?: return
            customData = customData.update { it.remove("nova_cbf") }
            set(DataComponents.CUSTOM_DATA, customData)
        }
    }

inline fun <reified T : Any> ItemStack.retrieveData(key: NamespacedKey): T? = retrieveData(key.namespace, key.key)
inline fun <reified T : Any> ItemStack.retrieveData(id: ResourceLocation): T? = retrieveData(id.namespace, id.path)
inline fun <reified T : Any> ItemStack.retrieveData(addon: Addon, key: String): T? = retrieveData(addon.id, key)
inline fun <reified T : Any> ItemStack.retrieveData(namespace: String, key: String): T? = novaCompound?.get(namespace, key)

inline fun <reified T : Any> ItemStack.storeData(key: NamespacedKey, data: T?) = storeData(key.namespace, key.key, data)
inline fun <reified T : Any> ItemStack.storeData(id: ResourceLocation, data: T?) = storeData(id.namespace, id.path, data)
inline fun <reified T : Any> ItemStack.storeData(addon: Addon, key: String, data: T?) = storeData(addon.id, key, data)
inline fun <reified T : Any> ItemStack.storeData(namespace: String, key: String, data: T?) {
    val novaCompound = this.novaCompound ?: NamespacedCompound()
    novaCompound[namespace, key] = data
    this.novaCompound = novaCompound
}

inline fun <reified T : Any> MojangStack.retrieveData(key: NamespacedKey): T? = retrieveData(key.namespace, key.key)
inline fun <reified T : Any> MojangStack.retrieveData(id: ResourceLocation): T? = retrieveData(id.namespace, id.path)
inline fun <reified T : Any> MojangStack.retrieveData(addon: Addon, key: String): T? = retrieveData(addon.id, key)
inline fun <reified T : Any> MojangStack.retrieveData(namespace: String, key: String): T? = novaCompound?.get(namespace, key)

inline fun <reified T : Any> MojangStack.storeData(key: NamespacedKey, data: T?) = storeData(key.namespace, key.key, data)
inline fun <reified T : Any> MojangStack.storeData(id: ResourceLocation, data: T?) = storeData(id.namespace, id.path, data)
inline fun <reified T : Any> MojangStack.storeData(addon: Addon, key: String, data: T?) = storeData(addon.id, key, data)
inline fun <reified T : Any> MojangStack.storeData(namespace: String, key: String, data: T?) {
    val novaCompound = this.novaCompound ?: NamespacedCompound()
    novaCompound[namespace, key] = data
    this.novaCompound = novaCompound
}
//</editor-fold>

object ItemUtils {
    
    fun isIdRegistered(id: String): Boolean {
        try {
            val nid = NamespacedId.of(id, "minecraft")
            return when (nid.namespace) {
                "minecraft" -> runCatching { Material.valueOf(nid.name.uppercase()) }.isSuccess
                "nova" -> NovaRegistries.ITEM.getByName(nid.name).isNotEmpty()
                else -> NovaRegistries.ITEM[id] != null || CustomItemServiceManager.getItemByName(id) != null
            }
        } catch (ignored: Exception) {
        }
        
        return false
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
                    "minecraft" -> {
                        val material = Material.valueOf(id.drop(10).uppercase())
                        return@map ModelDataTest(material, intArrayOf(0), ItemStack(material))
                    }
                    
                    "nova" -> {
                        val name = id.substringAfter(':')
                        val novaItems = NovaRegistries.ITEM.getByName(name)
                        if (novaItems.isNotEmpty()) {
                            return@map NovaNameTest(name, novaItems.map { it.createItemStack() })
                        } else throw IllegalArgumentException("Not an item name in Nova: $name")
                    }
                    
                    else -> {
                        val novaItems = NovaRegistries.ITEM.getValue(id)
                        if (novaItems != null) {
                            return@map NovaIdTest(id, novaItems.createItemStack())
                        } else {
                            return@map CustomItemServiceManager.getItemTest(id)!!
                        }
                    }
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
        return when (s.substringBefore(':')) {
            "minecraft" -> toItemStack(s)
            else -> getItemStack(ResourceLocation.parse(s))
        }
    }
    
    /**
     * Creates an [ItemStack] from the given [id]. Resolves ids from vanilla, nova and custom item services.
     */
    fun getItemStack(id: ResourceLocation): ItemStack {
        return when (id.namespace) {
            "minecraft" -> ItemStack(BuiltInRegistries.ITEM.getValue(id).bukkitMaterial)
            "nova" -> NovaRegistries.ITEM.getByName(id.path).firstOrNull()?.createItemStack()
            else -> NovaRegistries.ITEM.getValue(id)?.createItemStack()
                ?: CustomItemServiceManager.getItemByName(id.toString())
        } ?: throw IllegalArgumentException("Could not find item with id $id")
    }
    
    /**
     * Gets the actually displayed name of the given [itemStack].
     * If the [itemStack] has a custom display name, that will be returned. Otherwise, the localized name will be returned.
     */
    fun getName(itemStack: ItemStack): Component =
        getName(itemStack.unwrap())
    
    /**
     * Gets the actually displayed name of the given [itemStack].
     * If the [itemStack] has a custom display name, that will be returned. Otherwise, the localized name will be returned.
     */
    internal fun getName(itemStack: MojangStack): Component {
        val displayName = itemStack.get(DataComponents.CUSTOM_NAME)?.toAdventureComponent()
        
        if (displayName != null)
            return displayName
        
        val novaItem = itemStack.novaItem
        if (novaItem != null)
            return novaItem.name ?: Component.empty()
        
        return itemStack.item.getName(itemStack).toAdventureComponent()
    }
    
    /**
     * Converts the given string to an [ItemStack].
     * Does not understand custom item ids.
     */
    fun toItemStack(s: String): ItemStack {
        val parser = ItemParser(REGISTRY_ACCESS)
        val result = parser.parse(StringReader(s))
        return MojangStack(result.item, 1, result.components).asBukkitMirror()
    }
    
    /**
     * Gets the id of the given [itemStack].
     */
    fun getId(itemStack: ItemStack): String {
        val novaItem = itemStack.novaItem
        if (novaItem != null) return novaItem.id.toString()
        
        val customNameKey = CustomItemServiceManager.getId(itemStack)
        if (customNameKey != null) return customNameKey
        
        return "minecraft:${itemStack.type.name.lowercase()}"
    }
    
    @Suppress("UNCHECKED_CAST")
    internal fun mergeDataComponentPatches(dataComponentPatches: List<DataComponentPatch>): DataComponentPatch {
        val components = HashMap<DataComponentType<Any>, ArrayList<Optional<Any>>>()
        
        for (dataComponentPatch in dataComponentPatches) {
            for ((type, newValueOpt) in dataComponentPatch.entrySet()) {
                type as DataComponentType<Any>
                newValueOpt as Optional<Any>
                components.getOrPut(type, ::ArrayList) += newValueOpt
            }
        }
        
        val builder = DataComponentPatch.builder()
        for ((type, valueOpts) in components) {
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
        for ((type, values) in components) {
            builder.set(type, mergeDataComponents(type, values))
        }
        return builder.build()
    }
    
    @Suppress("UNCHECKED_CAST") // not sure why Kotlin cannot infer the types here
    internal fun <T> mergeDataComponents(type: DataComponentType<T>, values: List<T>): T {
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
            DataComponents.RECIPES -> mergeResourceLocations(values as List<List<ResourceLocation>>)
            DataComponents.STORED_ENCHANTMENTS -> mergeEnchantments(values as List<ItemEnchantments>)
            else -> values.last()
        } as T
    }
    
    internal fun mergeAttributeModifiers(values: List<ItemAttributeModifiers>): ItemAttributeModifiers {
        val builder = ItemAttributeModifiers.builder()
        for (itemAttributeModifiers in values) {
            for (modifier in itemAttributeModifiers.modifiers) {
                builder.add(modifier.attribute, modifier.modifier, modifier.slot)
            }
        }
        return builder.build()
    }
    
    internal fun mergeAdventureModePredicate(values: List<AdventureModePredicate>): AdventureModePredicate {
        return AdventureModePredicate(values.flatMap { it.predicates }, values.any { it.showInTooltip() })
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
            for ((enchantment, level) in itemEnchantments.entrySet()) {
                enchantments.set(enchantment, max(enchantments.getLevel(enchantment), level))
            }
        }
        return enchantments.toImmutable()
    }
    
    internal fun mergeLore(values: List<ItemLore>): ItemLore {
        return ItemLore(values.flatMap {it.lines }, values.flatMap { it.styledLines })
    }
    
    internal fun mergePotionContents(values: List<PotionContents>): PotionContents {
        return PotionContents(
            Optional.empty(),
            Optional.empty(),
            values.flatMap { it.allEffects },
            values.asSequence().map { it.customName }.lastOrNull { it.isPresent } ?: Optional.empty()
        )
    }
    
    internal fun mergeResourceLocations(values: List<List<ResourceLocation>>): List<ResourceLocation> {
        val set = LinkedHashSet<ResourceLocation>()
        for (value in values) {
            set.addAll(value)
        }
        return ArrayList(set)
    }
    
}