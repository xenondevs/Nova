package xyz.xenondevs.nova.util

import com.mojang.brigadier.StringReader
import de.studiocode.invui.item.builder.ItemBuilder
import net.minecraft.commands.arguments.item.ItemParser
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundGroup
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.nova.data.recipe.ComplexChoice
import xyz.xenondevs.nova.data.recipe.CustomRecipeChoice
import xyz.xenondevs.nova.data.recipe.SingleCustomRecipeChoice
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import kotlin.math.absoluteValue
import kotlin.random.Random
import net.minecraft.world.item.ItemStack as NMSItemStack

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
    get() {
        val customModelData = customModelData
        val material = NovaMaterialRegistry.getOrNull(customModelData)
        if (material != null && material.item.material == type) return material
        return null
    }

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

fun ItemStack.isSimilarIgnoringName(other: ItemStack?): Boolean {
    val first = this.namelessCopyOrSelf
    val second = other?.namelessCopyOrSelf
    
    return first.isSimilar(second)
}

object ItemUtils {
    
    fun getRecipeChoice(nameList: List<String>): RecipeChoice {
        if (nameList.any { it.contains("{") }) return ComplexChoice(nameList.map(ItemUtils::toItemStack))
        
        val choices = ArrayList<Pair<Material, IntArray>>()
        val examples = ArrayList<ItemStack>()
        
        nameList.forEach { name ->
            try {
                val namespace = name.substringBefore(':')
                
                if (namespace == "nova") {
                    val material = NovaMaterialRegistry.get(name.drop(5).uppercase())
                    choices += material.item.material to intArrayOf(material.item.data).let { if (material.legacyItemIds != null) it + material.legacyItemIds else it }
                    examples += material.createItemStack()
                } else if (namespace == "minecraft") {
                    val material = Material.valueOf(name.drop(10).uppercase())
                    choices += material to intArrayOf(0)
                    examples += ItemStack(material)
                } else if (CustomItemServiceManager.hasNamespace(namespace)) {
                    val stack = CustomItemServiceManager.getItemByName(name)!!
                    choices += stack.type to intArrayOf(stack.customModelData)
                    examples += stack
                } else throw IllegalArgumentException("Invalid item name: $name (Unknown or missing prefix)")
            } catch (ex: Exception) {
                throw IllegalArgumentException("Unknown item $name", ex)
            }
        }
        
        return if (choices.size == 1 && choices[0].second.size == 1)
            SingleCustomRecipeChoice(choices[0].first, choices[0].second[0], examples[0])
        else CustomRecipeChoice(choices, examples)
    }
    
    @Suppress("LiftReturnOrAssignment")
    fun getItemBuilder(name: String, basic: Boolean = false): ItemBuilder {
        try {
            val namespace = name.substringBefore(':')
            
            if (namespace == "nova") {
                val novaMaterial = NovaMaterialRegistry.get(name.substringAfter(':').uppercase())
                return if (basic) novaMaterial.createBasicItemBuilder() else novaMaterial.createItemBuilder()
            } else if (namespace == "minecraft") {
                return ItemBuilder(toItemStack(name))
            } else if (CustomItemServiceManager.hasNamespace(namespace)) {
                val stack = CustomItemServiceManager.getItemByName(name)!!
                return ItemBuilder(stack)
            } else throw IllegalArgumentException("Unknown prefix")
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $name", ex)
        }
    }
    
    fun getItemAndLocalizedName(name: String, basic: Boolean = false): Pair<ItemStack, String> {
        val itemStack: ItemStack
        val localizedName: String
        
        try {
            val namespace = name.substringBefore(':')
            
            if (namespace == "nova") {
                val novaMaterial = NovaMaterialRegistry.get(name.substringAfter(':').uppercase())
                localizedName = novaMaterial.localizedName
                itemStack = if (basic) novaMaterial.createBasicItemBuilder().get() else novaMaterial.createItemStack()
            } else if (namespace == "minecraft") {
                itemStack = toItemStack(name)
                localizedName = itemStack.type.localizedName!!
            } else if (CustomItemServiceManager.hasNamespace(namespace)) {
                itemStack = CustomItemServiceManager.getItemByName(name)!!
                localizedName = itemStack.displayName ?: ""
            } else throw IllegalArgumentException("Unknown prefix")
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid item name: $name", ex)
        }
        
        return itemStack to localizedName
    }
    
    fun toItemStack(s: String): ItemStack {
        val parser = ItemParser(StringReader(s), false).parse()
        val nmsStack = NMSItemStack(parser.item, 1).apply { tag = parser.nbt }
        return CraftItemStack.asBukkitCopy(nmsStack)
    }
    
    fun getNameKey(itemStack: ItemStack): String {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null) return "nova:${novaMaterial.typeName.lowercase()}"
        
        val customNameKey = CustomItemServiceManager.getNameKey(itemStack)
        if (customNameKey != null) return customNameKey
        
        return "minecraft:${itemStack.type.name.lowercase()}"
    }
    
}