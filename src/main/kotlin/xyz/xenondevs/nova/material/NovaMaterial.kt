package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.impl.*
import xyz.xenondevs.nova.util.toIntArray
import java.util.*

private fun blockOf(data: IntArray) = ModelData(BARRIER, data)

private fun blockOf(data: Int) = ModelData(BARRIER, intArrayOf(data))

private fun structureBlockOf(data: IntArray) = ModelData(STRUCTURE_VOID, data)

private fun structureBlockOf(data: Int) = ModelData(STRUCTURE_VOID, intArrayOf(data))

private fun itemOf(data: IntArray) = ModelData(SHULKER_SHELL, data)

private fun itemOf(data: Int) = ModelData(SHULKER_SHELL, intArrayOf(data))

enum class NovaMaterial(
    val itemName: String,
    val item: ModelData,
    createItemBuilderFunction: ((NovaMaterial, TileEntity?) -> ItemBuilder)?,
    val block: ModelData?,
    val hitbox: Material?,
    val createTileEntity: ((UUID?, NovaMaterial, ArmorStand) -> TileEntity)?
) {
    
    // 1 - 1000: Blocks
    FURNACE_GENERATOR("Furnace Generator", blockOf(1), EnergyTileEntity::createItemBuilder, blockOf(1), COBBLESTONE, ::FurnaceGenerator),
    MECHANICAL_PRESS("Mechanical Press", blockOf(2), EnergyTileEntity::createItemBuilder, blockOf(2), IRON_BLOCK, ::MechanicalPress),
    BASIC_POWER_CELL("Basic Power Cell", blockOf(3), EnergyTileEntity::createItemBuilder, blockOf(3), IRON_BLOCK, ::BasicPowerCell),
    ADVANCED_POWER_CELL("Advanced Power Cell", blockOf(4), EnergyTileEntity::createItemBuilder, blockOf(4), IRON_BLOCK, ::AdvancedPowerCell),
    ELITE_POWER_CELL("Elite Power Cell", blockOf(5), EnergyTileEntity::createItemBuilder, blockOf(5), IRON_BLOCK, ::ElitePowerCell),
    ULTIMATE_POWER_CELL("Ultimate Power Cell", blockOf(6), EnergyTileEntity::createItemBuilder, blockOf(6), IRON_BLOCK, ::UltimatePowerCell),
    CREATIVE_POWER_CELL("Creative Power Cell", blockOf(7), EnergyTileEntity::createItemBuilder, blockOf(7), IRON_BLOCK, ::CreativePowerCell),
    PULVERIZER("Pulverizer", blockOf(8), EnergyTileEntity::createItemBuilder, blockOf(8), COBBLESTONE, ::Pulverizer),
    SOLAR_PANEL("Solar Panel", blockOf(9), EnergyTileEntity::createItemBuilder, blockOf(9), BARRIER, ::SolarPanel),
    QUARRY("Quarry", blockOf(10), EnergyTileEntity::createItemBuilder, blockOf(10), COBBLESTONE, ::Quarry),
    ELECTRICAL_FURNACE("Electrical Furnace", blockOf(11), EnergyTileEntity::createItemBuilder, blockOf(11), COBBLESTONE, ::ElectricalFurnace),
    CHUNK_LOADER("Chunk Loader", blockOf(12), EnergyTileEntity::createItemBuilder, blockOf(12), COBBLESTONE, ::ChunkLoader),
    
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
    IRON_DUST("Iron Dust", itemOf(1020)),
    GOLD_DUST("Gold Dust", itemOf(1021)),
    DIAMOND_DUST("Diamond Dust", itemOf(1022)),
    NETHERITE_DUST("Netherite Dust", itemOf(1023)),
    EMERALD_DUST("Emerald Dust", itemOf(1024)),
    LAPIS_DUST("Lapis Dust", itemOf(1025)),
    COAL_DUST("Coal Dust", itemOf(1026)),
    NETHERITE_DRILL("Netherite Drill", itemOf(1030)),
    
    // 2000 - 3000: Upgrades and similar
    WRENCH("Wrench", itemOf(2000)),
    ITEM_FILTER("Item Filter", itemOf(2001)),
    
    // 5000 - 10.000 MultiModel Blocks
    BASIC_CABLE("Basic Cable", structureBlockOf(5004), null, structureBlockOf(intArrayOf(-1) + (5000..5003).toIntArray() + (5025..5033).toIntArray()), null, ::BasicCable),
    ADVANCED_CABLE("Advanced Cable", structureBlockOf(5009), null, structureBlockOf(intArrayOf(-1) + (5005..5008).toIntArray() + (5025..5033).toIntArray()), null, ::AdvancedCable),
    ELITE_CABLE("Elite Cable", structureBlockOf(5014), null, structureBlockOf(intArrayOf(-1) + (5010..5013).toIntArray() + (5025..5033).toIntArray()), null, ::EliteCable),
    ULTIMATE_CABLE("Ultimate Cable", structureBlockOf(5019), null, structureBlockOf(intArrayOf(-1) + (5015..5018).toIntArray() + (5025..5033).toIntArray()), null, ::UltimateCable),
    CREATIVE_CABLE("Creative Cable", structureBlockOf(5024), null, structureBlockOf(intArrayOf(-1) + (5020..5023).toIntArray() + (5025..5033).toIntArray()), null, ::CreativeCable),
    SCAFFOLDING("Quarry Scaffolding", itemOf(5040), null, itemOf((5041..5046).toIntArray()), null, null),
    
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
    WHITELIST_BUTTON("", itemOf(9013)),
    BLACKLIST_BUTTON("", itemOf(9014)),
    PLUS_ON_BUTTON("", itemOf(9015)),
    PLUS_OFF_BUTTON("", itemOf(9016)),
    MINUS_ON_BUTTON("", itemOf(9017)),
    MINUS_OFF_BUTTON("", itemOf(9018)),
    
    // 10.000 - ? Multi-Texture UI Elements
    PROGRESS_ARROW("", itemOf((10_000..10_016).toIntArray())),
    ENERGY_PROGRESS("", itemOf((10_100..10_116).toIntArray())),
    RED_BAR("", itemOf((10_200..10_216).toIntArray())),
    GREEN_BAR("", itemOf((10_300..10_316).toIntArray())),
    BLUE_BAR("", itemOf((10_400..10_416).toIntArray())),
    PRESS_PROGRESS("", itemOf((10_500..10_508).toIntArray())),
    PULVERIZER_PROGRESS("", itemOf((10_600..10_614).toIntArray())),
    
    // 100.000 - ? Numbers
    NUMBER("", itemOf((100_000..100_999).toIntArray()));
    
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
    
    /**
     * Creates an [ItemStack] with the specified amount for this [NovaMaterial].
     *
     * This is the same as calling `createItemBuilder.build()`
     */
    fun createItemStack(amount: Int): ItemStack = createItemBuilder().setAmount(amount).build()
    
}