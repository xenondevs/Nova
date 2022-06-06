package xyz.xenondevs.nova.util.item

import com.mojang.brigadier.StringReader
import de.studiocode.invui.item.builder.ItemBuilder
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.recipe.*
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import net.minecraft.world.item.ItemStack as MojangStack

val ItemStack.novaMaterial: ItemNovaMaterial?
    get() = (itemMeta?.unhandledTags?.get("nova") as? CompoundTag)
        ?.getString("id")
        ?.let(NovaMaterialRegistry::getOrNull)

val ItemStack.customModelData: Int
    get() {
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            if (itemMeta.hasCustomModelData()) return itemMeta.customModelData
        }
        
        return 0
    }

val ItemStack.displayName: String?
    get() {
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            return itemMeta.displayName
        }
        
        return null
    }

val ItemStack.localizedName: String?
    get() = novaMaterial?.localizedName ?: type.localizedName

val MojangStack.novaMaterial: ItemNovaMaterial?
    get() = tag?.getCompound("nova")
        ?.getString("id")
        ?.let(NovaMaterialRegistry::getOrNull)

val ItemStack.namelessCopyOrSelf: ItemStack
    get() {
        var itemStack = this
        if (hasItemMeta()) {
            val itemMeta = itemMeta!!
            if (itemMeta.hasDisplayName()) {
                itemMeta.setDisplayName(null)
                itemStack = clone().apply { setItemMeta(itemMeta) }
            }
        }
        
        return itemStack
    }

@Suppress("UNCHECKED_CAST")
val ItemMeta.unhandledTags: MutableMap<String, Tag>
    get() = ReflectionRegistry.CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD.get(this) as MutableMap<String, Tag>

fun ItemStack.isSimilarIgnoringName(other: ItemStack?): Boolean {
    val first = this.namelessCopyOrSelf
    val second = other?.namelessCopyOrSelf
    
    return first.isSimilar(second)
}

fun ItemStack.takeUnlessAir(): ItemStack? =
    if (type.isAir) null else this

inline fun <reified K> ItemStack.retrieveData(key: NamespacedKey): K? {
    return itemMeta?.persistentDataContainer?.get(key)
}

fun <T> ItemStack.retrieveData(key: NamespacedKey, persistentDataType: PersistentDataType<*, T>): T? {
    return itemMeta?.persistentDataContainer?.get(key, persistentDataType)
}

inline fun <reified T> ItemStack.storeData(key: NamespacedKey, data: T?) {
    val itemMeta = itemMeta
    val dataContainer = itemMeta?.persistentDataContainer
    if (dataContainer != null) {
        if (data != null) dataContainer.set(key, data)
        else dataContainer.remove(key)
        
        this.itemMeta = itemMeta
    }
}

fun <T> ItemStack.storeData(key: NamespacedKey, dataType: PersistentDataType<*, T>, data: T?) {
    val itemMeta = itemMeta
    val dataContainer = itemMeta?.persistentDataContainer
    if (dataContainer != null) {
        if (data != null) dataContainer.set(key, dataType, data)
        else dataContainer.remove(key)
        
        this.itemMeta = itemMeta
    }
}

object ItemUtils {
    
    val SHULKER_BOX_ITEMS = setOf(
        Items.SHULKER_BOX,
        Items.BLUE_SHULKER_BOX,
        Items.BLACK_SHULKER_BOX,
        Items.CYAN_SHULKER_BOX,
        Items.BROWN_SHULKER_BOX,
        Items.GREEN_SHULKER_BOX,
        Items.GRAY_SHULKER_BOX,
        Items.LIGHT_BLUE_SHULKER_BOX,
        Items.LIGHT_GRAY_SHULKER_BOX,
        Items.LIME_SHULKER_BOX,
        Items.MAGENTA_SHULKER_BOX,
        Items.ORANGE_SHULKER_BOX,
        Items.PINK_SHULKER_BOX,
        Items.PURPLE_SHULKER_BOX,
        Items.RED_SHULKER_BOX,
        Items.WHITE_SHULKER_BOX,
        Items.YELLOW_SHULKER_BOX
    )
    
    fun isIdRegistered(id: String): Boolean {
        try {
            val nid = NamespacedId.of(id, "minecraft")
            return when (nid.namespace) {
                "minecraft" -> runCatching { Material.valueOf(nid.name.uppercase()) }.isSuccess
                "nova" -> NovaMaterialRegistry.getNonNamespaced(nid.name).isNotEmpty()
                else -> NovaMaterialRegistry.getOrNull(id) != null || CustomItemServiceManager.getItemByName(id) != null
            }
        } catch(ignored: Exception) {
        }
        
        return false
    }
    
    fun getRecipeChoice(nameList: List<String>): RecipeChoice {
        val tests = nameList.map { id ->
            try {
                if (id.contains("{"))
                    return@map ComplexTest(toItemStack(id))
                
                when (id.substringBefore(':')) {
                    "minecraft" -> {
                        val material = Material.valueOf(id.drop(10).uppercase())
                        return@map ModelDataTest(material, intArrayOf(0), ItemStack(material))
                    }
                    "nova" -> {
                        val name = id.substringAfter(':')
                        val novaMaterials = NovaMaterialRegistry.getNonNamespaced(name)
                        if (novaMaterials.isNotEmpty()) {
                            return@map NovaNameTest(name, novaMaterials.map { it.itemProvider.get() })
                        } else throw IllegalArgumentException("Not an item name in Nova: $name")
                    }
                    else -> {
                        val novaMaterial = NovaMaterialRegistry.getOrNull(id)
                        if (novaMaterial != null) {
                            return@map NovaIdTest(id, novaMaterial.itemProvider.get())
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
    
    @Suppress("LiftReturnOrAssignment")
    fun getItemBuilder(id: String, basicClientSide: Boolean = false): ItemBuilder {
        try {
            return when (id.substringBefore(':')) {
                "minecraft" -> ItemBuilder(toItemStack(id))
                "nova" -> {
                    val name = id.substringAfter(':')
                    val novaMaterial = NovaMaterialRegistry.getNonNamespaced(name).first()
                    
                    if (basicClientSide) novaMaterial.item.createClientsideItemBuilder()
                    else novaMaterial.createItemBuilder()
                }
                else -> {
                    val novaMaterial = NovaMaterialRegistry.getOrNull(id)
                    if (novaMaterial != null) {
                        if (basicClientSide) novaMaterial.item.createClientsideItemBuilder()
                        else novaMaterial.createItemBuilder()
                    } else CustomItemServiceManager.getItemByName(id)!!.let(::ItemBuilder)
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $id", ex)
        }
    }
    
    fun getItemAndLocalizedName(id: String, basic: Boolean = false): Pair<ItemStack, String> {
        val itemStack: ItemStack
        val localizedName: String
        
        try {
            when (id.substringBefore(':')) {
                "minecraft" -> {
                    itemStack = toItemStack(id)
                    localizedName = itemStack.type.localizedName!!
                }
                "nova" -> {
                    val name = id.substringAfter(':')
                    val novaMaterial = NovaMaterialRegistry.getNonNamespaced(name).first()
                    itemStack = novaMaterial.createItemStack()
                    localizedName = novaMaterial.localizedName
                }
                else -> {
                    val novaMaterial = NovaMaterialRegistry.getOrNull(id)
                    if (novaMaterial != null) {
                        localizedName = novaMaterial.localizedName
                        itemStack = if (basic) novaMaterial.createBasicItemBuilder().get() else novaMaterial.createItemStack()
                    } else {
                        itemStack = CustomItemServiceManager.getItemByName(id)!!
                        localizedName = itemStack.displayName ?: ""
                    }
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $id", ex)
        }
        
        return itemStack to localizedName
    }
    
    fun toItemStack(s: String): ItemStack {
        val parser = ItemParser(StringReader(s), false).parse()
        val nmsStack = MojangStack(parser.item, 1).apply { tag = parser.nbt }
        return CraftItemStack.asBukkitCopy(nmsStack)
    }
    
    fun getId(itemStack: ItemStack): String {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) return novaMaterial.id.toString()
        
        val customNameKey = CustomItemServiceManager.getNameKey(itemStack)
        if (customNameKey != null) return customNameKey
        
        return "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}