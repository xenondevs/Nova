package xyz.xenondevs.nova.util

import com.mojang.brigadier.StringReader
import de.studiocode.invui.item.builder.ItemBuilder
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.meta.ItemMeta
import xyz.xenondevs.nova.data.recipe.ComplexTest
import xyz.xenondevs.nova.data.recipe.CustomRecipeChoice
import xyz.xenondevs.nova.data.recipe.ModelDataTest
import xyz.xenondevs.nova.data.recipe.NovaIdTest
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.math.absoluteValue
import kotlin.random.Random
import net.minecraft.world.item.ItemStack as MojangStack

fun Material.isGlass() = name.endsWith("GLASS") || name.endsWith("GLASS_PANE")

fun Material.toItemStack(amount: Int = 1): ItemStack = ItemBuilder(this).setAmount(amount).get()

fun Material.isTraversable() = isAir || this == Material.WATER || this == Material.BUBBLE_COLUMN || this == Material.LAVA

fun Material.isBreakable() = blastResistance < 3600000.0f

fun Material.isFluid() = this == Material.WATER || this == Material.BUBBLE_COLUMN || this == Material.LAVA

val Material.fluidType: FluidType?
    get() {
        val fluidType = when (this) {
            Material.WATER, Material.BUBBLE_COLUMN -> FluidType.WATER
            Material.LAVA -> FluidType.LAVA
            else -> null
        }
        return fluidType
    }

/**
 * The break speed for a specific material, always positive.
 */
val Material.breakSpeed: Double
    get() = 1.0 / hardness.absoluteValue

val Material.localizedName: String?
    get() = CraftMagicNumbers.getItem(this)?.descriptionId

val ItemStack.novaMaterial: NovaMaterial?
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

val MojangStack.novaMaterial: NovaMaterial?
    get() = tag?.getCompound("nova")
        ?.getString("id")
        ?.let(NovaMaterialRegistry::getOrNull)

val Material.soundGroup: SoundGroup
    get() = createBlockData().soundGroup

fun Material.playPlaceSoundEffect(location: Location) {
    location.world!!.playSound(location, soundGroup.placeSound, 1f, Random.nextDouble(0.8, 0.95).toFloat())
}

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


object ItemUtils {
    
    fun getRecipeChoice(nameList: List<String>): RecipeChoice {
        val tests = nameList.map { name ->
            try {
                if (name.contains("{"))
                    return@map ComplexTest(toItemStack(name))
                
                return@map when (name.substringBefore(':')) {
                    "minecraft" -> {
                        val material = Material.valueOf(name.drop(10).uppercase())
                        ModelDataTest(material, intArrayOf(0), ItemStack(material))
                    }
                    else -> {
                        val novaMaterial = NovaMaterialRegistry.getOrNull(name)
                        if (novaMaterial != null)
                            NovaIdTest(name, novaMaterial.clientsideProvider.get())
                        else CustomItemServiceManager.getItemTest(name)!!
                    }
                }
            } catch (ex: Exception) {
                throw IllegalArgumentException("Unknown item $name", ex)
            }
        }
        
        return CustomRecipeChoice(tests)
    }
    
    @Suppress("LiftReturnOrAssignment")
    fun getItemBuilder(name: String, basic: Boolean = false): ItemBuilder {
        try {
            return when (name.substringBefore(':')) {
                "nova" -> {
                    val novaMaterial = NovaMaterialRegistry.get(name)
                    if (basic) novaMaterial.createBasicItemBuilder() else novaMaterial.createItemBuilder()
                }
                "minecraft" -> ItemBuilder(toItemStack(name))
                else -> CustomItemServiceManager.getItemByName(name)!!.let(::ItemBuilder)
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $name", ex)
        }
    }
    
    fun getItemAndLocalizedName(name: String, basic: Boolean = false): Pair<ItemStack, String> {
        val itemStack: ItemStack
        val localizedName: String
        
        try {
            when (name.substringBefore(':')) {
                "minecraft" -> {
                    itemStack = toItemStack(name)
                    localizedName = itemStack.type.localizedName!!
                }
                else -> {
                    val novaMaterial = NovaMaterialRegistry.getOrNull(name)
                    if (novaMaterial != null) {
                        localizedName = novaMaterial.localizedName
                        itemStack = if (basic) novaMaterial.createBasicItemBuilder().get() else novaMaterial.createItemStack()
                    } else {
                        itemStack = CustomItemServiceManager.getItemByName(name)!!
                        localizedName = itemStack.displayName ?: ""
                    }
                }
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $name", ex)
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
        if (novaMaterial != null) return "nova:${novaMaterial.id.lowercase()}"
        
        val customNameKey = CustomItemServiceManager.getNameKey(itemStack)
        if (customNameKey != null) return customNameKey
        
        return "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}