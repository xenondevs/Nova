package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.FilterItem
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.item.impl.MobCatcherItem
import xyz.xenondevs.nova.tileentity.impl.agriculture.*
import xyz.xenondevs.nova.tileentity.impl.energy.*
import xyz.xenondevs.nova.tileentity.impl.mob.Breeder
import xyz.xenondevs.nova.tileentity.impl.mob.MobDuplicator
import xyz.xenondevs.nova.tileentity.impl.mob.MobKiller
import xyz.xenondevs.nova.tileentity.impl.processing.*
import xyz.xenondevs.nova.tileentity.impl.processing.brewing.ElectricBrewingStand
import xyz.xenondevs.nova.tileentity.impl.storage.*
import xyz.xenondevs.nova.tileentity.impl.world.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, NovaMaterial>()
    
    val values: Collection<NovaMaterial>
        get() = materialsById.values
    
    val sortedValues: Set<NovaMaterial> by lazy { materialsById.values.toSortedSet() }
    
    // Blocks
    val MECHANICAL_PRESS = registerEnergyTileEntity("MECHANICAL_PRESS", ::MechanicalPress, COBBLESTONE)
    val BASIC_POWER_CELL = registerEnergyTileEntity("BASIC_POWER_CELL", ::BasicPowerCell, IRON_BLOCK)
    val ADVANCED_POWER_CELL = registerEnergyTileEntity("ADVANCED_POWER_CELL", ::AdvancedPowerCell, IRON_BLOCK)
    val ELITE_POWER_CELL = registerEnergyTileEntity("ELITE_POWER_CELL", ::ElitePowerCell, IRON_BLOCK)
    val ULTIMATE_POWER_CELL = registerEnergyTileEntity("ULTIMATE_POWER_CELL", ::UltimatePowerCell, IRON_BLOCK)
    val CREATIVE_POWER_CELL = registerEnergyTileEntity("CREATIVE_POWER_CELL", ::CreativePowerCell, IRON_BLOCK)
    val PULVERIZER = registerEnergyTileEntity("PULVERIZER", ::Pulverizer, COBBLESTONE)
    val SOLAR_PANEL = registerEnergyTileEntity("SOLAR_PANEL", ::SolarPanel, BARRIER)
    val QUARRY = registerEnergyTileEntity("QUARRY", ::Quarry, COBBLESTONE, Quarry::canPlace)
    val CHUNK_LOADER = registerEnergyTileEntity("CHUNK_LOADER", ::ChunkLoader, COBBLESTONE)
    val BLOCK_BREAKER = registerEnergyTileEntity("BLOCK_BREAKER", ::BlockBreaker, COBBLESTONE)
    val BLOCK_PLACER = registerEnergyTileEntity("BLOCK_PLACER", ::BlockPlacer, COBBLESTONE)
    val STORAGE_UNIT = registerDefaultTileEntity("STORAGE_UNIT", ::StorageUnit, BARRIER)
    val CHARGER = registerEnergyTileEntity("CHARGER", ::Charger, COBBLESTONE)
    val MOB_KILLER = registerEnergyTileEntity("MOB_KILLER", ::MobKiller, COBBLESTONE)
    val VACUUM_CHEST = registerDefaultTileEntity("VACUUM_CHEST", ::VacuumChest, BARRIER)
    val BREEDER = registerEnergyTileEntity("BREEDER", ::Breeder, COBBLESTONE)
    val MOB_DUPLICATOR = registerEnergyTileEntity("MOB_DUPLICATOR", ::MobDuplicator, COBBLESTONE)
    val PLANTER = registerEnergyTileEntity("PLANTER", ::Planter, COBBLESTONE)
    val HARVESTER = registerEnergyTileEntity("HARVESTER", ::Harvester, COBBLESTONE)
    val FERTILIZER = registerEnergyTileEntity("FERTILIZER", ::Fertilizer, COBBLESTONE)
    val WIRELESS_CHARGER = registerEnergyTileEntity("WIRELESS_CHARGER", ::WirelessCharger, COBBLESTONE)
    val AUTO_FISHER = registerEnergyTileEntity("AUTO_FISHER", ::AutoFisher, COBBLESTONE)
    val LIGHTNING_EXCHANGER = registerEnergyTileEntity("LIGHTNING_EXCHANGER", ::LightningExchanger, BARRIER)
    val TREE_FACTORY = registerEnergyTileEntity("TREE_FACTORY", ::TreeFactory, BARRIER)
    val TRASH_CAN = registerDefaultTileEntity("TRASH_CAN", ::TrashCan, BARRIER)
    val BASIC_FLUID_TANK = registerDefaultTileEntity("BASIC_FLUID_TANK", ::BasicFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ADVANCED_FLUID_TANK = registerDefaultTileEntity("ADVANCED_FLUID_TANK", ::AdvancedFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ELITE_FLUID_TANK = registerDefaultTileEntity("ELITE_FLUID_TANK", ::EliteFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ULTIMATE_FLUID_TANK = registerDefaultTileEntity("ULTIMATE_FLUID_TANK", ::UltimateFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val CREATIVE_FLUID_TANK = registerDefaultTileEntity("CREATIVE_FLUID_TANK", ::CreativeFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val INFINITE_WATER_SOURCE = registerDefaultTileEntity("INFINITE_WATER_SOURCE", ::InfiniteWaterSource, SANDSTONE)
    val PUMP = registerDefaultTileEntity("PUMP", ::Pump, BARRIER)
    val COBBLESTONE_GENERATOR = registerDefaultTileEntity("COBBLESTONE_GENERATOR", ::CobblestoneGenerator, BARRIER, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val FLUID_INFUSER = registerDefaultTileEntity("FLUID_INFUSER", ::FluidInfuser, COBBLESTONE, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val FREEZER = registerDefaultTileEntity("FREEZER", ::Freezer, COBBLESTONE, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    val SPRINKLER = registerDefaultTileEntity("SPRINKLER", ::Sprinkler, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val FLUID_STORAGE_UNIT = registerDefaultTileEntity("FLUID_STORAGE_UNIT", ::FluidStorageUnit, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ELECTRIC_BREWING_STAND = registerDefaultTileEntity("ELECTRIC_BREWING_STAND", ::ElectricBrewingStand, BARRIER, listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder))
    
    // Crafting Items
    // Plates
    val IRON_PLATE = registerDefaultItem("IRON_PLATE")
    val GOLD_PLATE = registerDefaultItem("GOLD_PLATE")
    val DIAMOND_PLATE = registerDefaultItem("DIAMOND_PLATE")
    val NETHERITE_PLATE = registerDefaultItem("NETHERITE_PLATE")
    val EMERALD_PLATE = registerDefaultItem("EMERALD_PLATE")
    val REDSTONE_PLATE = registerDefaultItem("REDSTONE_PLATE")
    val LAPIS_PLATE = registerDefaultItem("LAPIS_PLATE")
    val COPPER_PLATE = registerDefaultItem("COPPER_PLATE")
    
    // Gears
    val IRON_GEAR = registerDefaultItem("IRON_GEAR")
    val GOLD_GEAR = registerDefaultItem("GOLD_GEAR")
    val DIAMOND_GEAR = registerDefaultItem("DIAMOND_GEAR")
    val NETHERITE_GEAR = registerDefaultItem("NETHERITE_GEAR")
    val EMERALD_GEAR = registerDefaultItem("EMERALD_GEAR")
    val REDSTONE_GEAR = registerDefaultItem("REDSTONE_GEAR")
    val LAPIS_GEAR = registerDefaultItem("LAPIS_GEAR")
    val COPPER_GEAR = registerDefaultItem("COPPER_GEAR")
    
    // Dust
    val IRON_DUST = registerDefaultItem("IRON_DUST")
    val GOLD_DUST = registerDefaultItem("GOLD_DUST")
    val DIAMOND_DUST = registerDefaultItem("DIAMOND_DUST")
    val NETHERITE_DUST = registerDefaultItem("NETHERITE_DUST")
    val EMERALD_DUST = registerDefaultItem("EMERALD_DUST")
    val LAPIS_DUST = registerDefaultItem("LAPIS_DUST")
    val COAL_DUST = registerDefaultItem("COAL_DUST")
    val COPPER_DUST = registerDefaultItem("COPPER_DUST")
    val STAR_DUST = registerDefaultItem("STAR_DUST")
    
    // Other
    val NETHERITE_DRILL = registerDefaultItem("NETHERITE_DRILL")
    val SOLAR_CELL = registerDefaultItem("SOLAR_CELL")
    val MOB_CATCHER = registerDefaultItem("MOB_CATCHER", MobCatcherItem)
    val STAR_SHARDS = registerDefaultItem("STAR_SHARDS")
    val BASIC_MACHINE_FRAME = registerDefaultItem("BASIC_MACHINE_FRAME")
    val ADVANCED_MACHINE_FRAME = registerDefaultItem("ADVANCED_MACHINE_FRAME")
    val ELITE_MACHINE_FRAME = registerDefaultItem("ELITE_MACHINE_FRAME")
    val ULTIMATE_MACHINE_FRAME = registerDefaultItem("ULTIMATE_MACHINE_FRAME")
    val CREATIVE_MACHINE_FRAME = registerDefaultItem("CREATIVE_MACHINE_FRAME")
    
    // Upgrades and similar
    val WRENCH = registerDefaultItem("WRENCH")
    val ITEM_FILTER = registerDefaultItem("ITEM_FILTER", FilterItem)
    val SPEED_UPGRADE = registerDefaultItem("SPEED_UPGRADE")
    val EFFICIENCY_UPGRADE = registerDefaultItem("EFFICIENCY_UPGRADE")
    val ENERGY_UPGRADE = registerDefaultItem("ENERGY_UPGRADE")
    val RANGE_UPGRADE = registerDefaultItem("RANGE_UPGRADE")
    val FLUID_UPGRADE = registerDefaultItem("FLUID_UPGRADE")
    
    // Equipment, Attachments
    val JETPACK = registerItem("JETPACK", "item.nova.jetpack", JetpackItem)
    
    // MultiModel Blocks
    // Reserved for legacy cables
    val BASIC_CABLE = registerTileEntity("BASIC_CABLE", "block.nova.basic_cable", null, STRUCTURE_VOID, ::BasicCable, isDirectional = false)
    val ADVANCED_CABLE = registerTileEntity("ADVANCED_CABLE", "block.nova.advanced_cable", null, STRUCTURE_VOID, ::AdvancedCable, isDirectional = false)
    val ELITE_CABLE = registerTileEntity("ELITE_CABLE", "block.nova.elite_cable", null, STRUCTURE_VOID, ::EliteCable, isDirectional = false)
    val ULTIMATE_CABLE = registerTileEntity("ULTIMATE_CABLE", "block.nova.ultimate_cable", null, STRUCTURE_VOID, ::UltimateCable, isDirectional = false)
    val CREATIVE_CABLE = registerTileEntity("CREATIVE_CABLE", "block.nova.creative_cable", null, STRUCTURE_VOID, ::CreativeCable, isDirectional = false)
    val SCAFFOLDING = register(NovaMaterial("SCAFFOLDING", "item.nova.scaffolding", null, null))
    val WIND_TURBINE = registerTileEntity("WIND_TURBINE", "block.nova.wind_turbine", listOf(EnergyHolder::modifyItemBuilder), BARRIER, ::WindTurbine, WindTurbine::canPlace)
    val FURNACE_GENERATOR = registerTileEntity("FURNACE_GENERATOR", "block.nova.furnace_generator", listOf(EnergyHolder::modifyItemBuilder), COBBLESTONE, ::FurnaceGenerator)
    val ELECTRICAL_FURNACE = registerTileEntity("ELECTRICAL_FURNACE", "block.nova.electrical_furnace", listOf(EnergyHolder::modifyItemBuilder), COBBLESTONE, ::ElectricFurnace)
    val STAR_COLLECTOR = registerTileEntity("STAR_COLLECTOR", "block.nova.star_collector", listOf(EnergyHolder::modifyItemBuilder), BARRIER, ::StarCollector)
    val LAVA_GENERATOR = registerTileEntity("LAVA_GENERATOR", "block.nova.lava_generator", listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder), COBBLESTONE, ::LavaGenerator)
    
    // UI Elements
    val GRAY_BUTTON = registerItem("GRAY_BUTTON", "")
    val ORANGE_BUTTON = registerItem("ORANGE_BUTTON", "")
    val BLUE_BUTTON = registerItem("BLUE_BUTTON", "")
    val GREEN_BUTTON = registerItem("GREEN_BUTTON", "")
    val WHITE_BUTTON = registerItem("WHITE_BUTTON", "")
    val RED_BUTTON = registerItem("RED_BUTTON", "")
    val YELLOW_BUTTON = registerItem("YELLOW_BUTTON", "")
    val PINK_BUTTON = registerItem("PINK_BUTTON", "")
    val SIDE_CONFIG_BUTTON = registerItem("SIDE_CONFIG_BUTTON", "")
    val PLATE_ON_BUTTON = registerItem("PLATE_ON_BUTTON", "")
    val PLATE_OFF_BUTTON = registerItem("PLATE_OFF_BUTTON", "")
    val GEAR_ON_BUTTON = registerItem("GEAR_ON_BUTTON", "")
    val GEAR_OFF_BUTTON = registerItem("GEAR_OFF_BUTTON", "")
    val WHITELIST_BUTTON = registerItem("WHITELIST_BUTTON", "")
    val BLACKLIST_BUTTON = registerItem("BLACKLIST_BUTTON", "")
    val PLUS_ON_BUTTON = registerItem("PLUS_ON_BUTTON", "")
    val PLUS_OFF_BUTTON = registerItem("PLUS_OFF_BUTTON", "")
    val MINUS_ON_BUTTON = registerItem("MINUS_ON_BUTTON", "")
    val MINUS_OFF_BUTTON = registerItem("MINUS_OFF_BUTTON", "")
    val AREA_ON_BUTTON = registerItem("AREA_ON_BUTTON", "")
    val AREA_OFF_BUTTON = registerItem("AREA_OFF_BUTTON", "")
    val NBT_ON_BUTTON = registerItem("NBT_ON_BUTTON", "")
    val NBT_OFF_BUTTON = registerItem("NBT_OFF_BUTTON", "")
    val HOE_ON_BUTTON = registerItem("HOE_ON_BUTTON", "")
    val HOE_OFF_BUTTON = registerItem("HOE_OFF_BUTTON", "")
    val UPGRADES_BUTTON = registerItem("UPGRADES_BUTTON", "menu.nova.upgrades")
    val ARROW_LEFT_ON_BUTTON = registerItem("ARROW_LEFT_ON_BUTTON", "")
    val ARROW_LEFT_OFF_BUTTON = registerItem("ARROW_LEFT_OFF_BUTTON", "")
    val ARROW_RIGHT_ON_BUTTON = registerItem("ARROW_RIGHT_ON_BUTTON", "")
    val ARROW_RIGHT_OFF_BUTTON = registerItem("ARROW_RIGHT_OFF_BUTTON", "")
    val ENERGY_ON_BUTTON = registerItem("ENERGY_ON_BUTTON", "menu.nova.side_config.energy")
    val ENERGY_OFF_BUTTON = registerItem("ENERGY_OFF_BUTTON", "")
    val ENERGY_SELECTED_BUTTON = registerItem("ENERGY_SELECTED_BUTTON", "menu.nova.side_config.energy")
    val ITEM_ON_BUTTON = registerItem("ITEM_ON_BUTTON", "menu.nova.side_config.items")
    val ITEM_OFF_BUTTON = registerItem("ITEM_OFF_BUTTON", "")
    val ITEM_SELECTED_BUTTON = registerItem("ITEM_SELECTED_BUTTON", "menu.nova.side_config.items")
    val FLUID_ON_BUTTON = registerItem("FLUID_ON_BUTTON", "menu.nova.side_config.fluids")
    val FLUID_OFF_BUTTON = registerItem("FLUID_OFF_BUTTON", "")
    val FLUID_SELECTED_BUTTON = registerItem("FLUID_SELECTED_BUTTON", "menu.nova.side_config.fluids")
    val COBBLESTONE_MODE_BUTTON = registerItem("COBBLESTONE_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.cobblestone")
    val STONE_MODE_BUTTON = registerItem("STONE_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.stone")
    val OBSIDIAN_MODE_BUTTON = registerItem("OBSIDIAN_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.obsidian")
    val FLUID_LEFT_RIGHT_BUTTON = registerItem("FLUID_LEFT_RIGHT_BUTTON", "menu.nova.fluid_infuser.mode.insert")
    val FLUID_RIGHT_LEFT_BUTTON = registerItem("FLUID_RIGHT_LEFT_BUTTON", "menu.nova.fluid_infuser.mode.extract")
    val ICE_MODE_BUTTON = registerItem("ICE_MODE_BUTTON", "menu.nova.freezer.mode.ice")
    val PACKED_ICE_MODE_BUTTON = registerItem("PACKED_ICE_MODE_BUTTON", "menu.nova.freezer.mode.packed_ice")
    val BLUE_ICE_MODE_BUTTON = registerItem("BLUE_ICE_MODE_BUTTON", "menu.nova.freezer.mode.blue_ice")
    val PUMP_PUMP_BUTTON = registerItem("PUMP_PUMP_ICON", "")
    val PUMP_REPLACE_BUTTON = registerItem("PUMP_REPLACE_ICON", "")
    val INVISIBLE_ITEM = registerItem("INVISIBLE", "")
    val STOPWATCH_ICON = registerItem("STOPWATCH_ICON", "")
    val SEARCH_ICON = registerItem("SEARCH_ICON", "")
    val NO_NUMBER = registerItem("NO_NUMBER", "")
    val PLUS_ICON = registerItem("PLUS_ICON", "")
    val MINUS_ICON = registerItem("MINUS_ICON", "")
    val COLOR_PICKER_ICON = registerItem("COLOR_PICKER_ICON", "menu.nova.color_picker")
    val SPEED_UPGRADE_ICON = registerItem("SPEED_UPGRADE_ICON", "")
    val TRANSLUCENT_SPEED_UPGRADE_ICON = registerItem("TRANSLUCENT_SPEED_UPGRADE_ICON", "")
    val EFFICIENCY_UPGRADE_ICON = registerItem("EFFICIENCY_UPGRADE_ICON", "")
    val TRANSLUCENT_EFFICIENCY_UPGRADE_ICON = registerItem("TRANSLUCENT_EFFICIENCY_UPGRADE_ICON", "")
    val ENERGY_UPGRADE_ICON = registerItem("ENERGY_UPGRADE_ICON", "")
    val TRANSLUCENT_ENERGY_UPGRADE_ICON = registerItem("TRANSLUCENT_ENERGY_UPGRADE_ICON", "")
    val RANGE_UPGRADE_ICON = registerItem("RANGE_UPGRADE_ICON", "")
    val TRANSLUCENT_RANGE_UPGRADE_ICON = registerItem("TRANSLUCENT_RANGE_UPGRADE_ICON", "")
    val FLUID_UPGRADE_ICON = registerItem("FLUID_UPGRADE_ICON", "")
    val TRANSLUCENT_FLUID_UPGRADE_ICON = registerItem("TRANSLUCENT_FLUID_UPGRADE_ICON", "")
    val HOE_PLACEHOLDER = registerItem("HOE_PLACEHOLDER", "")
    val AXE_PLACEHOLDER = registerItem("AXE_PLACEHOLDER", "")
    val SHEARS_PLACEHOLDER = registerItem("SHEARS_PLACEHOLDER", "")
    val ITEM_FILTER_PLACEHOLDER = registerItem("ITEM_FILTER_PLACEHOLDER", "")
    val MOB_CATCHER_PLACEHOLDER = registerItem("MOB_CATCHER_PLACEHOLDER", "")
    val FISHING_ROD_PLACEHOLDER = registerItem("FISHING_ROD_PLACEHOLDER", "")
    val SAPLING_PLACEHOLDER = registerItem("SAPLING_PLACEHOLDER", "")
    val TRASH_CAN_PLACEHOLDER = registerItem("TRASH_CAN_PLACEHOLDER", "")
    val BOTTLE_PLACEHOLDER = registerItem("BOTTLE_PLACEHOLDER", "")
    
    // Multi-Texture UI Elements
    val PROGRESS_ARROW = registerItem("PROGRESS_ARROW", "")
    val ENERGY_PROGRESS = registerItem("ENERGY_PROGRESS", "")
    val RED_BAR = registerItem("RED_BAR", "")
    val GREEN_BAR = registerItem("GREEN_BAR", "")
    val BLUE_BAR = registerItem("BLUE_BAR", "")
    val PRESS_PROGRESS = registerItem("PRESS_PROGRESS", "")
    val PULVERIZER_PROGRESS = registerItem("PULVERIZER_PROGRESS", "")
    val ORANGE_BAR = registerItem("ORANGE_BAR", "")
    val FLUID_PROGRESS_LEFT_RIGHT = registerItem("FLUID_PROGRESS_LEFT_RIGHT", "")
    val FLUID_PROGRESS_RIGHT_LEFT = registerItem("FLUID_PROGRESS_RIGHT_LEFT", "")
    val FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT = registerItem("FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT", "")
    val FLUID_PROGRESS_RIGHT_LEFT_TRANSPARENT = registerItem("FLUID_PROGRESS_RIGHT_LEFT_TRANSPARENT", "")
    val BLUE_BAR_TRANSPARENT = registerItem("BLUE_BAR_TRANSPARENT", "")
    val ORANGE_BAR_TRANSPARENT = registerItem("ORANGE_BAR_TRANSPARENT", "")
    val BREW_PROGRESS_TRANSPARENT = registerItem("BREW_PROGRESS_TRANSPARENT", "")
    
    // Numbers
    val NUMBER = registerItem("NUMBER", "")
    
    // Fluid Levels
    val TANK_LAVA_LEVELS = registerItem("TANK_LAVA_LEVELS", "")
    val TANK_WATER_LEVELS = registerItem("TANK_WATER_LEVELS", "")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem("COBBLESTONE_GENERATOR_LAVA_LEVELS", "")
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem("COBBLESTONE_GENERATOR_WATER_LEVELS", "")
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem("OAK_TREE_MINIATURE", "")
    val SPRUCE_TREE_MINIATURE = registerItem("SPRUCE_TREE_MINIATURE", "")
    val BIRCH_TREE_MINIATURE = registerItem("BIRCH_TREE_MINIATURE", "")
    val JUNGLE_TREE_MINIATURE = registerItem("JUNGLE_TREE_MINIATURE", "")
    val ACACIA_TREE_MINIATURE = registerItem("ACACIA_TREE_MINIATURE", "")
    val DARK_OAK_TREE_MINIATURE = registerItem("DARK_OAK_TREE_MINIATURE", "")
    val CRIMSON_TREE_MINIATURE = registerItem("CRIMSON_TREE_MINIATURE", "")
    val WARPED_TREE_MINIATURE = registerItem("WARPED_TREE_MINIATURE", "")
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem("GIANT_RED_MUSHROOM_MINIATURE", "")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem("GIANT_BROWN_MUSHROOM_MINIATURE", "")
    
    override fun getOrNull(id: String): NovaMaterial? = materialsById[id.uppercase().removePrefix("NOVA:")]
    override fun getOrNull(item: ItemStack): NovaMaterial? = item.novaMaterial
    override fun get(id: String): NovaMaterial = getOrNull(id)!!
    override fun get(item: ItemStack): NovaMaterial = getOrNull(item)!!
    
    fun registerDefaultTileEntity(
        id: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        val namespacedId = id.split(':')
        return registerTileEntity(
            id,
            if (namespacedId.size == 2) "block.${namespacedId[0]}.${namespacedId[1]}" else "block.nova.$id",
            itemBuilderModifiers,
            hitboxType,
            tileEntityConstructor,
            placeCheck,
            isDirectional
        )
    }
    
    fun registerEnergyTileEntity(
        id: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        return registerDefaultTileEntity(
            id,
            tileEntityConstructor,
            hitboxType,
            listOf(EnergyHolder::modifyItemBuilder),
            placeCheck,
            isDirectional,
            legacyItemIds
        )
    }
    
    fun registerTileEntity(
        id: String,
        name: String,
        itemBuilderModifiers: List<ItemBuilderModifierFun>?,
        hitboxType: Material,
        tileEntityConstructor: TileEntityConstructor?,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
    ): NovaMaterial {
        val material = NovaMaterial(id, name, null, itemBuilderModifiers,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional)
        
        return register(material)
    }
    
    fun registerDefaultItem(id: String, novaItem: NovaItem? = null) =
        registerItem(id, "item.nova.${id.lowercase()}", novaItem)
    
    fun registerItem(id: String, name: String, novaItem: NovaItem? = null) =
        register(NovaMaterial(id, name, novaItem))
    
    fun registerItem(id: String) =
        register(NovaMaterial(id, ""))
    
    private fun register(material: NovaMaterial): NovaMaterial {
        val id = material.id
        require(id !in materialsById) { "Duplicate NovaMaterial id: $id" }
        
        materialsById[id] = material
        
        return material
    }
    
}