package xyz.xenondevs.nova.material

import com.google.gson.JsonObject
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.NovaItemBuilder
import xyz.xenondevs.nova.item.impl.BottledMobItem
import xyz.xenondevs.nova.item.impl.FilterItem
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.impl.agriculture.AutoFisher
import xyz.xenondevs.nova.tileentity.impl.agriculture.Fertilizer
import xyz.xenondevs.nova.tileentity.impl.agriculture.Harvester
import xyz.xenondevs.nova.tileentity.impl.agriculture.Planter
import xyz.xenondevs.nova.tileentity.impl.energy.*
import xyz.xenondevs.nova.tileentity.impl.mob.Breeder
import xyz.xenondevs.nova.tileentity.impl.mob.MobDuplicator
import xyz.xenondevs.nova.tileentity.impl.mob.MobKiller
import xyz.xenondevs.nova.tileentity.impl.processing.ElectricalFurnace
import xyz.xenondevs.nova.tileentity.impl.processing.MechanicalPress
import xyz.xenondevs.nova.tileentity.impl.processing.Pulverizer
import xyz.xenondevs.nova.tileentity.impl.storage.StorageUnit
import xyz.xenondevs.nova.tileentity.impl.storage.VacuumChest
import xyz.xenondevs.nova.tileentity.impl.world.BlockBreaker
import xyz.xenondevs.nova.tileentity.impl.world.BlockPlacer
import xyz.xenondevs.nova.tileentity.impl.world.ChunkLoader
import xyz.xenondevs.nova.tileentity.impl.world.Quarry
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
    val novaItem: NovaItem? = null,
    createItemBuilderFunction: ((NovaMaterial, TileEntity?) -> NovaItemBuilder)? = null,
    val block: ModelData? = null,
    val hitbox: Material? = null,
    val createTileEntity: ((UUID?, NovaMaterial, JsonObject, ArmorStand) -> TileEntity)? = null,
    val canPlace: ((Player, Location) -> Boolean)? = null,
    val isDirectional: Boolean = true,
    val legacyItemIds: IntArray? = null,
) {
    
    // 1 - 1000: Blocks
    FURNACE_GENERATOR(itemName = "block.nova.furnace_generator", item = blockOf(1), createItemBuilderFunction = EnergyTileEntity::createItemBuilder, block = blockOf(1), hitbox = COBBLESTONE, createTileEntity = ::FurnaceGenerator),
    MECHANICAL_PRESS("block.nova.mechanical_press", blockOf(2), null, EnergyTileEntity::createItemBuilder, blockOf(2), COBBLESTONE, ::MechanicalPress),
    BASIC_POWER_CELL("block.nova.basic_power_cell", blockOf(3), null, EnergyTileEntity::createItemBuilder, blockOf(3), IRON_BLOCK, ::BasicPowerCell),
    ADVANCED_POWER_CELL("block.nova.advanced_power_cell", blockOf(4), null, EnergyTileEntity::createItemBuilder, blockOf(4), IRON_BLOCK, ::AdvancedPowerCell),
    ELITE_POWER_CELL("block.nova.elite_power_cell", blockOf(5), null, EnergyTileEntity::createItemBuilder, blockOf(5), IRON_BLOCK, ::ElitePowerCell),
    ULTIMATE_POWER_CELL("block.nova.ultimate_power_cell", blockOf(6), null, EnergyTileEntity::createItemBuilder, blockOf(6), IRON_BLOCK, ::UltimatePowerCell),
    CREATIVE_POWER_CELL("block.nova.creative_power_cell", blockOf(7), null, EnergyTileEntity::createItemBuilder, blockOf(7), IRON_BLOCK, ::CreativePowerCell),
    PULVERIZER("block.nova.pulverizer", blockOf(8), null, EnergyTileEntity::createItemBuilder, blockOf(8), COBBLESTONE, ::Pulverizer),
    SOLAR_PANEL("block.nova.solar_panel", blockOf(9), null, EnergyTileEntity::createItemBuilder, blockOf(9), BARRIER, ::SolarPanel),
    QUARRY("block.nova.quarry", blockOf(10), null, EnergyTileEntity::createItemBuilder, blockOf(10), COBBLESTONE, ::Quarry),
    ELECTRICAL_FURNACE("block.nova.electrical_furnace", blockOf(11), null, EnergyTileEntity::createItemBuilder, blockOf(11), COBBLESTONE, ::ElectricalFurnace),
    CHUNK_LOADER("block.nova.chunk_loader", blockOf(12), null, EnergyTileEntity::createItemBuilder, blockOf(12), COBBLESTONE, ::ChunkLoader),
    BLOCK_BREAKER("block.nova.block_breaker", blockOf(13), null, EnergyTileEntity::createItemBuilder, blockOf(13), COBBLESTONE, ::BlockBreaker),
    BLOCK_PLACER("block.nova.block_placer", blockOf(14), null, EnergyTileEntity::createItemBuilder, blockOf(14), COBBLESTONE, ::BlockPlacer),
    STORAGE_UNIT("block.nova.storage_unit", blockOf(15), null, null, blockOf(15), BARRIER, ::StorageUnit),
    CHARGER("block.nova.charger", blockOf(16), null, EnergyTileEntity::createItemBuilder, blockOf(16), COBBLESTONE, ::Charger),
    MOB_KILLER("block.nova.mob_killer", blockOf(17), null, EnergyTileEntity::createItemBuilder, blockOf(17), COBBLESTONE, ::MobKiller),
    VACUUM_CHEST("block.nova.vacuum_chest", blockOf(18), null, null, blockOf(18), BARRIER, ::VacuumChest),
    BREEDER("block.nova.breeder", blockOf(19), null, EnergyTileEntity::createItemBuilder, blockOf(19), COBBLESTONE, ::Breeder),
    MOB_DUPLICATOR("block.nova.mob_duplicator", blockOf(20), null, EnergyTileEntity::createItemBuilder, blockOf(20), COBBLESTONE, ::MobDuplicator),
    PLANTER("block.nova.planter", blockOf(21), null, EnergyTileEntity::createItemBuilder, blockOf(21), COBBLESTONE, ::Planter),
    HARVESTER("block.nova.harvester", blockOf(22), null, EnergyTileEntity::createItemBuilder, blockOf(22), COBBLESTONE, ::Harvester),
    FERTILIZER("block.nova.fertilizer", blockOf(23), null, EnergyTileEntity::createItemBuilder, blockOf(23), COBBLESTONE, ::Fertilizer),
    WIRELESS_CHARGER("block.nova.wireless_charger", blockOf(24), null, EnergyTileEntity::createItemBuilder, blockOf(24), COBBLESTONE, ::WirelessCharger),
    AUTO_FISHER("block.nova.auto_fisher", blockOf(25), null, EnergyTileEntity::createItemBuilder, blockOf(25), COBBLESTONE, ::AutoFisher),
    LIGHTNING_EXCHANGER("block.nova.lightning_exchanger", blockOf(26), null, EnergyTileEntity::createItemBuilder, blockOf(26), BARRIER, ::LightningExchanger),
    
    // 1000 - 2000: Crafting Items
    // Plates
    IRON_PLATE("item.nova.iron_plate", itemOf(1000)),
    GOLD_PLATE("item.nova.gold_plate", itemOf(1001)),
    DIAMOND_PLATE("item.nova.diamond_plate", itemOf(1002)),
    NETHERITE_PLATE("item.nova.netherite_plate", itemOf(1003)),
    EMERALD_PLATE("item.nova.emerald_plate", itemOf(1004)),
    REDSTONE_PLATE("item.nova.redstone_plate", itemOf(1005)),
    LAPIS_PLATE("item.nova.lapis_plate", itemOf(1006)),
    COPPER_PLATE("item.nova.copper_plate", itemOf(1007)),
    
    // Gears
    IRON_GEAR("item.nova.iron_gear", itemOf(1010)),
    GOLD_GEAR("item.nova.gold_gear", itemOf(1011)),
    DIAMOND_GEAR("item.nova.diamond_gear", itemOf(1012)),
    NETHERITE_GEAR("item.nova.netherite_gear", itemOf(1013)),
    EMERALD_GEAR("item.nova.emerald_gear", itemOf(1014)),
    REDSTONE_GEAR("item.nova.redstone_gear", itemOf(1015)),
    LAPIS_GEAR("item.nova.lapis_gear", itemOf(1016)),
    COPPER_GEAR("item.nova.copper_gear", itemOf(1017)),
    
    // Dust
    IRON_DUST("item.nova.iron_dust", itemOf(1020)),
    GOLD_DUST("item.nova.gold_dust", itemOf(1021)),
    DIAMOND_DUST("item.nova.diamond_dust", itemOf(1022)),
    NETHERITE_DUST("item.nova.netherite_dust", itemOf(1023)),
    EMERALD_DUST("item.nova.emerald_dust", itemOf(1024)),
    LAPIS_DUST("item.nova.lapis_dust", itemOf(1025)),
    COAL_DUST("item.nova.coal_dust", itemOf(1026)),
    COPPER_DUST("item.nova.copper_dust", itemOf(1027)),
    STAR_DUST("item.nova.star_dust", itemOf(1028)),
    
    // Other
    NETHERITE_DRILL("item.nova.netherite_drill", itemOf(1030)),
    SOLAR_CELL("item.nova.solar_cell", itemOf(1031)),
    BOTTLED_MOB("item.nova.bottled_mob", itemOf(1032), BottledMobItem),
    STAR_SHARDS("item.nova.star_shards", itemOf(1033)),
    BASIC_MACHINE_FRAME("block.nova.basic_machine_frame", itemOf(1034)),
    ADVANCED_MACHINE_FRAME("block.nova.advanced_machine_frame", itemOf(1035)),
    ELITE_MACHINE_FRAME("block.nova.elite_machine_frame", itemOf(1036)),
    ULTIMATE_MACHINE_FRAME("block.nova.ultimate_machine_frame", itemOf(1037)),
    CREATIVE_MACHINE_FRAME("block.nova.creative_machine_frame", itemOf(1038)),
    
    // 2000 - 3000: Upgrades and similar
    WRENCH("item.nova.wrench", itemOf(2000)),
    ITEM_FILTER("item.nova.item_filter", itemOf(2001), FilterItem),
    SPEED_UPGRADE("item.nova.speed_upgrade", itemOf(2002)),
    EFFICIENCY_UPGRADE("item.nova.efficiency_upgrade", itemOf(2003)),
    ENERGY_UPGRADE("item.nova.energy_upgrade", itemOf(2004)),
    
    // 3000 - 4000: Equipment, Attachments
    JETPACK("item.nova.jetpack", ModelData(IRON_CHESTPLATE, intArrayOf(3000)), JetpackItem),
    
    // 5000 - 10.000 MultiModel Blocks
    // !!! DO NOT USE 5000 - 5100: LEGACY CABLES !!!
    BASIC_CABLE("block.nova.basic_cable", structureBlockOf(5100), null, null, structureBlockOf((5101..5164).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::BasicCable, isDirectional = false, legacyItemIds = intArrayOf(5004)),
    ADVANCED_CABLE("block.nova.advanced_cable", structureBlockOf(5165), null, null, structureBlockOf((5166..5229).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::AdvancedCable, isDirectional = false, legacyItemIds = intArrayOf(5009)),
    ELITE_CABLE("block.nova.elite_cable", structureBlockOf(5230), null, null, structureBlockOf((5231..5294).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::EliteCable, isDirectional = false, legacyItemIds = intArrayOf(5014)),
    ULTIMATE_CABLE("block.nova.ultimate_cable", structureBlockOf(5295), null, null, structureBlockOf((5296..5359).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::UltimateCable, isDirectional = false, legacyItemIds = intArrayOf(5019)),
    CREATIVE_CABLE("block.nova.creative_cable", structureBlockOf(5360), null, null, structureBlockOf((5361..5424).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::CreativeCable, isDirectional = false, legacyItemIds = intArrayOf(5024)),
    SCAFFOLDING("item.nova.scaffolding", itemOf(5040), null, null, itemOf((5041..5046).toIntArray()), null, null),
    WIND_TURBINE("block.nova.wind_turbine", blockOf(5050), null, EnergyTileEntity::createItemBuilder, blockOf((5051..5054).toIntArray()), BARRIER, ::WindTurbine, WindTurbine::canPlace),
    
    // 9.000 - 10.000 UI Elements
    GRAY_BUTTON("", itemOf(9001)),
    ORANGE_BUTTON("", itemOf(9002)),
    BLUE_BUTTON("", itemOf(9003)),
    GREEN_BUTTON("", itemOf(9004)),
    WHITE_BUTTON("", itemOf(9005)),
    RED_BUTTON("", itemOf(9006)),
    YELLOW_BUTTON("", itemOf(9007)),
    PINK_BUTTON("", itemOf(9008)),
    SIDE_CONFIG_BUTTON("", itemOf(9100)),
    PLATE_ON_BUTTON("", itemOf(9101)),
    PLATE_OFF_BUTTON("", itemOf(9102)),
    GEAR_ON_BUTTON("", itemOf(9103)),
    GEAR_OFF_BUTTON("", itemOf(9104)),
    ENERGY_ON_BUTTON("", itemOf(9105)),
    ENERGY_OFF_BUTTON("", itemOf(9106)),
    ITEM_ON_BUTTON("", itemOf(9107)),
    ITEM_OFF_BUTTON("", itemOf(9108)),
    WHITELIST_BUTTON("", itemOf(9109)),
    BLACKLIST_BUTTON("", itemOf(9110)),
    PLUS_ON_BUTTON("", itemOf(9111)),
    PLUS_OFF_BUTTON("", itemOf(9112)),
    MINUS_ON_BUTTON("", itemOf(9113)),
    MINUS_OFF_BUTTON("", itemOf(9114)),
    AREA_ON_BUTTON("", itemOf(9115)),
    AREA_OFF_BUTTON("", itemOf(9116)),
    NBT_ON_BUTTON("", itemOf(9117)),
    NBT_OFF_BUTTON("", itemOf(9118)),
    HOE_ON_BUTTON("", itemOf(9119)),
    HOE_OFF_BUTTON("", itemOf(9120)),
    UPGRADES_BUTTON("", itemOf(9121)),
    ARROW_LEFT_ON_BUTTON("", itemOf(9122)),
    ARROW_LEFT_OFF_BUTTON("", itemOf(9123)),
    ARROW_RIGHT_ON_BUTTON("", itemOf(9124)),
    ARROW_RIGHT_OFF_BUTTON("", itemOf(9125)),
    STOPWATCH_ICON("", itemOf(9400)),
    HOE_PLACEHOLDER("", itemOf(9500)),
    AXE_PLACEHOLDER("", itemOf(9501)),
    SHEARS_PLACEHOLDER("", itemOf(9502)),
    ITEM_FILTER_PLACEHOLDER("", itemOf(9503)),
    BOTTLED_MOB_PLACEHOLDER("", itemOf(9504)),
    FISHING_ROD_PLACEHOLDER("", itemOf(9505)),
    
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
    private val createItemBuilderFunction: ((TileEntity?) -> NovaItemBuilder)? = if (createItemBuilderFunction != null) {
        { createItemBuilderFunction(this, it) }
    } else null
    
    /**
     * Creates a basic [ItemBuilder][NovaItemBuilder] without any additional information
     * like an energy bar added to the [ItemStack].
     *
     * Can be used for just previewing the item type or as a base in
     * a `createItemBuilder` function for a [TileEntity].
     */
    fun createBasicItemBuilder(): NovaItemBuilder = item.getItemBuilder(itemName)
    
    /**
     * Creates an [ItemBuilder][NovaItemBuilder] for this [NovaMaterial].
     *
     * The [TileEntity] provided must be of the same type as the [TileEntity]
     * returned in the [createTileEntity] function.
     *
     * If there is no custom [createItemBuilderFunction] for this [NovaMaterial],
     * it will return the result of [createBasicItemBuilder].
     */
    fun createItemBuilder(tileEntity: TileEntity? = null): NovaItemBuilder =
        createItemBuilderFunction?.invoke(tileEntity) ?: novaItem?.getDefaultItemBuilder(createBasicItemBuilder())
        ?: createBasicItemBuilder()
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial].
     *
     * This is the same as calling `createItemBuilder.build()`
     */
    fun createItemStack(): ItemStack = createItemBuilder().build()
    
    /**
     * Creates an [ItemStack] with the specified amount for this [NovaMaterial].
     *
     * This is the same as calling `createItemBuilder.setAmount([amount]).build()`
     */
    fun createItemStack(amount: Int): ItemStack = createItemBuilder().setAmount(amount).build()
    
}