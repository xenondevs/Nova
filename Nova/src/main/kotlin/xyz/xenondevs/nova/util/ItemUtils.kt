package xyz.xenondevs.nova.util

import com.mojang.brigadier.StringReader
import de.studiocode.invui.item.builder.ItemBuilder
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.Items
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.meta.ItemMeta
import xyz.xenondevs.nova.data.recipe.*
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

/**
 * More reliable function compared to the Spigot API function [Material.isInteractable].
 * From https://www.spigotmc.org/threads/check-if-a-block-is-interactable.535861/
 * @author LoneDev
 */
fun Material.isActuallyInteractable(): Boolean {
    return if (!isInteractable) false else when (this) {
        Material.ACACIA_STAIRS, Material.ANDESITE_STAIRS, Material.BIRCH_STAIRS, Material.BLACKSTONE_STAIRS,
        Material.BRICK_STAIRS, Material.COBBLESTONE_STAIRS, Material.CRIMSON_STAIRS, Material.DARK_OAK_STAIRS,
        Material.DARK_PRISMARINE_STAIRS, Material.DIORITE_STAIRS, Material.END_STONE_BRICK_STAIRS, Material.GRANITE_STAIRS,
        Material.JUNGLE_STAIRS, Material.MOSSY_COBBLESTONE_STAIRS, Material.MOSSY_STONE_BRICK_STAIRS, Material.NETHER_BRICK_STAIRS,
        Material.OAK_STAIRS, Material.POLISHED_ANDESITE_STAIRS, Material.POLISHED_BLACKSTONE_BRICK_STAIRS, Material.POLISHED_BLACKSTONE_STAIRS,
        Material.POLISHED_DIORITE_STAIRS, Material.POLISHED_GRANITE_STAIRS, Material.PRISMARINE_BRICK_STAIRS, Material.PRISMARINE_STAIRS,
        Material.PURPUR_STAIRS, Material.QUARTZ_STAIRS, Material.RED_NETHER_BRICK_STAIRS, Material.RED_SANDSTONE_STAIRS,
        Material.SANDSTONE_STAIRS, Material.SMOOTH_QUARTZ_STAIRS, Material.SMOOTH_RED_SANDSTONE_STAIRS, Material.SMOOTH_SANDSTONE_STAIRS,
        Material.SPRUCE_STAIRS, Material.STONE_BRICK_STAIRS, Material.STONE_STAIRS, Material.WARPED_STAIRS,
        Material.ACACIA_FENCE, Material.BIRCH_FENCE, Material.CRIMSON_FENCE, Material.DARK_OAK_FENCE,
        Material.JUNGLE_FENCE, Material.MOVING_PISTON, Material.NETHER_BRICK_FENCE, Material.OAK_FENCE,
        Material.PUMPKIN, Material.REDSTONE_ORE, Material.REDSTONE_WIRE, Material.SPRUCE_FENCE,
        Material.WARPED_FENCE -> false
        else -> true
    }
}

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
                            return@map NovaNameTest(name, novaMaterials.map { it.clientsideProvider.get() })
                        } else throw IllegalArgumentException("Not an item name in Nova: $name")
                    }
                    else -> {
                        val novaMaterial = NovaMaterialRegistry.getOrNull(id)
                        if (novaMaterial != null) {
                            return@map NovaIdTest(id, novaMaterial.clientsideProvider.get())
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
    fun getItemBuilder(id: String, basic: Boolean = false): ItemBuilder {
        try {
            return when (id.substringBefore(':')) {
                "minecraft" -> ItemBuilder(toItemStack(id))
                "nova" -> {
                    val name = id.substringAfter(':')
                    val novaMaterial = NovaMaterialRegistry.getNonNamespaced(name).first()
                    
                    if (basic) novaMaterial.createBasicItemBuilder()
                    else novaMaterial.createItemBuilder()
                }
                else -> {
                    val novaMaterial = NovaMaterialRegistry.getOrNull(id)
                    if (novaMaterial != null) {
                        if (basic) novaMaterial.createBasicItemBuilder()
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
        if (novaMaterial != null) return "nova:${novaMaterial.id.lowercase()}"
        
        val customNameKey = CustomItemServiceManager.getNameKey(itemStack)
        if (customNameKey != null) return customNameKey
        
        return "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}