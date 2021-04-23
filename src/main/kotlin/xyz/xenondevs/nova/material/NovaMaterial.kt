package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.impl.*
import xyz.xenondevs.nova.util.toIntArray

private fun blockOf(data: IntArray) = ModelData(STRUCTURE_VOID, data)

private fun blockOf(data: Int) = ModelData(STRUCTURE_VOID, intArrayOf(data))

private fun itemOf(data: IntArray) = ModelData(SHULKER_SHELL, data)

private fun itemOf(data: Int) = ModelData(SHULKER_SHELL, intArrayOf(data))

enum class NovaMaterial(
    val itemName: String,
    val item: ModelData,
    createItemBuilderFunction: ((NovaMaterial, TileEntity?) -> ItemBuilder)?,
    val block: ModelData?,
    val hitbox: Material?,
    val createTileEntity: ((NovaMaterial, ArmorStand) -> TileEntity)?
) {
    
    // 1 - 1000: Blocks
    FURNACE_GENERATOR("Furnace Generator", blockOf(1), FurnaceGenerator::createItemBuilder, blockOf(1), COBBLESTONE, ::FurnaceGenerator),
    MECHANICAL_PRESS("Mechanical Press", blockOf(2), null, blockOf(2), IRON_BLOCK, ::MechanicalPress),
    BASIC_POWER_CELL("Basic Power Cell", blockOf(3), PowerCell::createItemBuilder, blockOf(3), IRON_BLOCK, ::BasicPowerCell),
    ADVANCED_POWER_CELL("Advanced Power Cell", blockOf(4), PowerCell::createItemBuilder, blockOf(4), IRON_BLOCK, ::AdvancedPowerCell),
    ELITE_POWER_CELL("Elite Power Cell", blockOf(5), PowerCell::createItemBuilder, blockOf(5), IRON_BLOCK, ::ElitePowerCell),
    ULTIMATE_POWER_CELL("Ultimate Power Cell", blockOf(6), PowerCell::createItemBuilder, blockOf(6), IRON_BLOCK, ::UltimatePowerCell),
    CREATIVE_POWER_CELL("Creative Power Cell", blockOf(7), PowerCell::createItemBuilder, blockOf(7), IRON_BLOCK, ::CreativePowerCell),
    
    // 1000 - 2000: Crafting Items
    IRON_PLATE("Iron Plate", itemOf(1000)),
    GOLD_PLATE("Gold Plate", itemOf(1001)),
    DIAMOND_PLATE("Diamond Plate", itemOf(1002)),
    NETHERITE_PLATE("Netherite Plate", itemOf(1003)),
    EMERALD_PLATE("Emerald Plate", itemOf(1004)),
    REDSTONE_PLATE("Redstone Plate", itemOf(1005)),
    LAPIS_PLATE("Lapis Plate", itemOf(1006)),
    IRON_GEAR("Iron Gear", itemOf(1010)),
    GOLD_GEAR("Gold Gear", itemOf(1011)),
    DIAMOND_GEAR("Diamond Gear", itemOf(1012)),
    NETHERITE_GEAR("Netherite Gear", itemOf(1013)),
    EMERALD_GEAR("Emerald Gear", itemOf(1014)),
    REDSTONE_GEAR("Redstone Gear", itemOf(1015)),
    LAPIS_GEAR("Lapis Gear", itemOf(1016)),
    IRON_DUST("Iron Dust", itemOf(1017)),
    GOLD_DUST("Gold Dust", itemOf(1018)),
    DIAMOND_DUST("Diamond Dust", itemOf(1019)),
    NETHERITE_DUST("Netherite Dust", itemOf(1020)),
    EMERALD_DUST("Emerald Dust", itemOf(1021)),
    LAPIS_DUST("Lapis Dust", itemOf(1022)),
    
    // 2000 - 3000: Upgrades and similar
    
    // 5000 - 10.000 MultiModel Blocks
    BASIC_CABLE("Basic Cable", blockOf(5004), null, blockOf(intArrayOf(-1) + (5000..5003).toIntArray()), null, ::BasicCable),
    ADVANCED_CABLE("Advanced Cable", blockOf(5009), null, blockOf(intArrayOf(-1) + (5005..5008).toIntArray()), null, ::AdvancedCable),
    ELITE_CABLE("Elite Cable", blockOf(5014), null, blockOf(intArrayOf(-1) + (5010..5013).toIntArray()), null, ::EliteCable),
    ULTIMATE_CABLE("Ultimate Cable", blockOf(5019), null, blockOf(intArrayOf(-1) + (5015..5018).toIntArray()), null, ::UltimateCable),
    CREATIVE_CABLE("Creative Cable", blockOf(5024), null, blockOf(intArrayOf(-1) + (5020..5023).toIntArray()), null, ::CreativeCable),
    
    // 9.000 - 10.000 UI Elements
    SIDE_CONFIG_BUTTON("", itemOf(9000)),
    GRAY_BUTTON("", itemOf(9001)),
    ORANGE_BUTTON("", itemOf(9002)),
    BLUE_BUTTON("", itemOf(9003)),
    GREEN_BUTTON("", itemOf(9004)),
    PLATE_ON_BUTTON("", itemOf(9005)),
    PLATE_OFF_BUTTON("", itemOf(9006)),
    GEAR_ON_BUTTON("", itemOf(9007)),
    GEAR_OFF_BUTTON("", itemOf(9008)),
    ENERGY_ON_BUTTON("", itemOf(9009)),
    ENERGY_OFF_BUTTON("", itemOf(9010)),
    ITEM_ON_BUTTON("", itemOf(9011)),
    ITEM_OFF_BUTTON("", itemOf(9012)),
    
    // 10.000 - ? Multi-Texture UI Elements
    PROGRESS_ARROW("", itemOf((10_000..10_016).toIntArray())),
    ENERGY_PROGRESS("", itemOf((10_100..10_116).toIntArray())),
    RED_BAR("", itemOf((10_200..10_216).toIntArray())),
    GREEN_BAR("", itemOf((10_300..10_316).toIntArray())),
    BLUE_BAR("", itemOf((10_400..10_416).toIntArray())),
    PRESS_PROGRESS("", itemOf((10_500..10_508).toIntArray()));
    
    
    val isBlock = block != null && createTileEntity != null
    private val createItemBuilderFunction: ((TileEntity?) -> ItemBuilder)? = if (createItemBuilderFunction != null) {
        { createItemBuilderFunction(this, it) }
    } else null
    
    constructor(itemName: String, item: ModelData) : this(itemName, item, null, null, null, null)
    
    /**
     * Creates a basic [ItemBuilder] without any additional information
     * like an energy bar added to the [ItemStack].
     *
     * Can be used for just previewing the item type or as a base in
     * a `createItemBuilder` function for a [TileEntity].
     */
    fun createBasicItemBuilder(): ItemBuilder = item.getItemBuilder(itemName)
    
    /**
     * Creates an [ItemBuilder] for this [NovaMaterial].
     *
     * The [TileEntity] provided must be of the same type as the [TileEntity]
     * returned in the [createTileEntity] function.
     *
     * If there is no custom [createItemBuilderFunction] for this [NovaMaterial],
     * it will return the result of [createBasicItemBuilder].
     */
    fun createItemBuilder(tileEntity: TileEntity? = null): ItemBuilder {
        return if (createItemBuilderFunction != null) {
            createItemBuilderFunction!!(tileEntity)
        } else createBasicItemBuilder()
    }
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial].
     *
     * This is the same as calling `createItemBuilder.build()`
     */
    fun createItemStack(): ItemStack = createItemBuilder().build()
    
    companion object {
        
        fun toNovaMaterial(itemStack: ItemStack): NovaMaterial? =
            values().find {
                it.item.material == itemStack.type
                    && it.item.dataArray.contains(itemStack.itemMeta?.customModelData ?: -1)
            }
        
    }
    
}