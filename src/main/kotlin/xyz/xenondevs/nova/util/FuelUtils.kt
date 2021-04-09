package xyz.xenondevs.nova.util

import org.bukkit.Material
import org.bukkit.Tag

val Material.burnTime: Int
    get() = FuelUtils.getBurnTime(this)

val Material.fuel: Fuel?
    get() = FuelUtils.getFuel(this)

class Fuel(val material: Material, val burnTime: Int, val remains: Material? = null)

private object FuelUtils {
    
    private val FUELS: Map<Material, Fuel>
    
    init {
        
        val fuels = mutableListOf(
            Fuel(Material.LAVA_BUCKET, 20000, Material.BUCKET),
            Fuel(Material.COAL_BLOCK, 16000),
            Fuel(Material.BLAZE_ROD, 2400),
            Fuel(Material.COAL, 1600),
            Fuel(Material.CHARCOAL, 1600),
            Fuel(Material.OAK_FENCE, 300),
            Fuel(Material.BIRCH_FENCE, 300),
            Fuel(Material.SPRUCE_FENCE, 300),
            Fuel(Material.JUNGLE_FENCE, 300),
            Fuel(Material.DARK_OAK_FENCE, 300),
            Fuel(Material.ACACIA_FENCE, 300),
            Fuel(Material.OAK_FENCE_GATE, 300),
            Fuel(Material.BIRCH_FENCE_GATE, 300),
            Fuel(Material.SPRUCE_FENCE_GATE, 300),
            Fuel(Material.JUNGLE_FENCE_GATE, 300),
            Fuel(Material.DARK_OAK_FENCE_GATE, 300),
            Fuel(Material.ACACIA_FENCE_GATE, 300),
            Fuel(Material.NOTE_BLOCK, 300),
            Fuel(Material.BOOKSHELF, 300),
            Fuel(Material.LECTERN, 300),
            Fuel(Material.JUKEBOX, 300),
            Fuel(Material.CHEST, 300),
            Fuel(Material.TRAPPED_CHEST, 300),
            Fuel(Material.CRAFTING_TABLE, 300),
            Fuel(Material.DAYLIGHT_DETECTOR, 300),
            Fuel(Material.BOW, 300),
            Fuel(Material.FISHING_ROD, 300),
            Fuel(Material.LADDER, 300),
            Fuel(Material.WOODEN_SHOVEL, 200),
            Fuel(Material.WOODEN_SWORD, 200),
            Fuel(Material.WOODEN_HOE, 200),
            Fuel(Material.WOODEN_AXE, 200),
            Fuel(Material.WOODEN_PICKAXE, 200),
            Fuel(Material.STICK, 100),
            Fuel(Material.BOWL, 100),
            Fuel(Material.DRIED_KELP_BLOCK, 4001),
            Fuel(Material.CROSSBOW, 300),
            Fuel(Material.BAMBOO, 50),
            Fuel(Material.DEAD_BUSH, 100),
            Fuel(Material.SCAFFOLDING, 400),
            Fuel(Material.LOOM, 300),
            Fuel(Material.BARREL, 300),
            Fuel(Material.CARTOGRAPHY_TABLE, 300),
            Fuel(Material.FLETCHING_TABLE, 300),
            Fuel(Material.SMITHING_TABLE, 300),
            Fuel(Material.COMPOSTER, 300),
        )
        
        fun addToList(tag: Tag<Material>, burnTime: Int) = tag.values.forEach { fuels += Fuel(it, burnTime) }
        
        addToList(Tag.LOGS, 300)
        addToList(Tag.PLANKS, 300)
        addToList(Tag.WOODEN_STAIRS, 300)
        addToList(Tag.WOODEN_SLABS, 150)
        addToList(Tag.WOODEN_TRAPDOORS, 300)
        addToList(Tag.WOODEN_PRESSURE_PLATES, 300)
        addToList(Tag.BANNERS, 300)
        addToList(Tag.SIGNS, 200)
        addToList(Tag.WOODEN_DOORS, 200)
        addToList(Tag.ITEMS_BOATS, 1200)
        addToList(Tag.WOOL, 100)
        addToList(Tag.WOODEN_BUTTONS, 100)
        addToList(Tag.SAPLINGS, 100)
        addToList(Tag.CARPETS, 67)
        
        FUELS = fuels.associateBy { it.material }
    }
    
    fun getBurnTime(material: Material) = FUELS[material]?.burnTime ?: 0
    
    fun getFuel(material: Material): Fuel? = FUELS[material]
    
}