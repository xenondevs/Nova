@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.util.item

import com.mojang.brigadier.StringReader
import net.kyori.adventure.text.Component
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.component.CustomData
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.addon.Addon
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
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.Wearable
import xyz.xenondevs.nova.world.item.recipe.ComplexTest
import xyz.xenondevs.nova.world.item.recipe.CustomRecipeChoice
import xyz.xenondevs.nova.world.item.recipe.ModelDataTest
import xyz.xenondevs.nova.world.item.recipe.NovaIdTest
import xyz.xenondevs.nova.world.item.recipe.NovaNameTest
import xyz.xenondevs.nova.world.item.recipe.TagTest
import java.util.concurrent.atomic.AtomicReference
import kotlin.jvm.optionals.getOrNull
import net.minecraft.world.item.ItemStack as MojangStack

val ItemStack.novaItem: NovaItem?
    get() = unwrap().novaItem

var ItemStack.novaModel: String?
    get() = unwrap().novaModel
    set(model) {
        unwrap().novaModel = model
    }

val MojangStack.novaItem: NovaItem?
    get() = unsafeNovaTag
        ?.getString("id")
        ?.let(NovaRegistries.ITEM::get)

var MojangStack.novaModel: String?
    get() = unsafeNovaTag?.getString("modelId")
    set(model) {
        update(DataComponents.CUSTOM_DATA) { customData ->
            customData.update { tag ->
                val novaCompound = tag.getCompoundOrNull("nova")
                if (novaCompound != null) {
                    if (model != null) {
                        novaCompound.putString("modelId", model)
                    } else {
                        novaCompound.remove("modelId")
                    }
                }
            }
        }
    }

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

val ItemStack.equipSound: String?
    get() {
        val novaItem = novaItem
        if (novaItem != null)
            return novaItem.getBehaviorOrNull<Wearable>()?.equipSound
        
        val armorMaterial = (CraftMagicNumbers.getItem(type) as? ArmorItem)?.material?.value()
        return armorMaterial?.equipSound()?.value()?.location?.toString()
    }

fun ItemStack.takeUnlessEmpty(): ItemStack? =
    if (type.isAir || amount <= 0) null else this

fun ItemStack?.isEmpty(): Boolean =
    this == null || type.isAir || amount <= 0

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
inline fun <reified T : Any> ItemStack.retrieveData(addon: Addon, key: String): T? = retrieveData(addon.description.id, key)
inline fun <reified T : Any> ItemStack.retrieveData(namespace: String, key: String): T? = novaCompound?.get(namespace, key)

inline fun <reified T : Any> ItemStack.storeData(key: NamespacedKey, data: T?) = storeData(key.namespace, key.key, data)
inline fun <reified T : Any> ItemStack.storeData(id: ResourceLocation, data: T?) = storeData(id.namespace, id.path, data)
inline fun <reified T : Any> ItemStack.storeData(addon: Addon, key: String, data: T?) = storeData(addon.description.id, key, data)
inline fun <reified T : Any> ItemStack.storeData(namespace: String, key: String, data: T?) {
    val novaCompound = this.novaCompound ?: NamespacedCompound()
    novaCompound[namespace, key] = data
    this.novaCompound = novaCompound
}

inline fun <reified T : Any> MojangStack.retrieveData(key: NamespacedKey): T? = retrieveData(key.namespace, key.key)
inline fun <reified T : Any> MojangStack.retrieveData(id: ResourceLocation): T? = retrieveData(id.namespace, id.path)
inline fun <reified T : Any> MojangStack.retrieveData(addon: Addon, key: String): T? = retrieveData(addon.description.id, key)
inline fun <reified T : Any> MojangStack.retrieveData(namespace: String, key: String): T? = novaCompound?.get(namespace, key)

inline fun <reified T : Any> MojangStack.storeData(key: NamespacedKey, data: T?) = storeData(key.namespace, key.key, data)
inline fun <reified T : Any> MojangStack.storeData(id: ResourceLocation, data: T?) = storeData(id.namespace, id.path, data)
inline fun <reified T : Any> MojangStack.storeData(addon: Addon, key: String, data: T?) = storeData(addon.description.id, key, data)
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
                        val novaItems = NovaRegistries.ITEM[id]
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
            "minecraft" -> ItemStack(BuiltInRegistries.ITEM.get(id).bukkitMaterial)
            "nova" -> NovaRegistries.ITEM.getByName(id.name).firstOrNull()?.createItemStack()
            else -> NovaRegistries.ITEM[id]?.createItemStack()
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
    
    internal fun mergeDataComponentPatches(vararg dataComponentPatches: DataComponentPatch): DataComponentPatch =
        mergeDataComponentPatches(dataComponentPatches.toList())
    
    @Suppress("DEPRECATION")
    internal fun mergeDataComponentPatches(dataComponentPatches: List<DataComponentPatch>): DataComponentPatch {
        val builder = DataComponentPatch.builder()
        val customTag = CompoundTag()
        
        for (dataComponentPatch in dataComponentPatches) {
            for ((type, valueOpt) in dataComponentPatch.entrySet()) {
                if (valueOpt.isPresent) {
                    builder.set(TypedDataComponent.createUnchecked(type, valueOpt.get()))
                } else {
                    builder.remove(type)
                }
            }
            
            val customData = dataComponentPatch.get(DataComponents.CUSTOM_DATA)?.getOrNull()
            if (customData != null)
                customTag.merge(customData.unsafe)
        }
        
        if (!customTag.isEmpty)
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag))
        
        return builder.build()
    }
    
}