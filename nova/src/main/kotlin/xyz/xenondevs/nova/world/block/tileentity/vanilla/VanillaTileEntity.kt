package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.serialization.DataHolder
import xyz.xenondevs.nova.world.BlockPos

private typealias VanillaTileEntityConstructor = (VanillaTileEntity.Type, BlockPos, Compound) -> VanillaTileEntity

internal abstract class VanillaTileEntity internal constructor(
    val type: Type,
    val pos: BlockPos,
    override val data: Compound
) : DataHolder(false) {
    
    open fun handleEnable() = Unit
    open fun handleDisable() = Unit
    open fun handlePlace() = Unit
    open fun handleBreak() = Unit
    open fun handleBlockStateChange(blockState: BlockState) = Unit
    
    open fun saveData() {
        storeData("type", type)
    }
    
    enum class Type(
        private val constructor: VanillaTileEntityConstructor,
        val materials: Set<Material>,
        val hasBlockEntity: Boolean = true,
    ) {
        
        CHEST(::VanillaChestTileEntity, setOf(Material.CHEST)),
        TRAPPED_CHEST(::VanillaChestTileEntity, setOf(Material.TRAPPED_CHEST)),
        FURNACE(::VanillaFurnaceTileEntity, setOf(Material.FURNACE)),
        BLAST_FURNACE(::VanillaFurnaceTileEntity, setOf(Material.BLAST_FURNACE)),
        SMOKER(::VanillaFurnaceTileEntity, setOf(Material.SMOKER)),
        BARREL(::VanillaContainerTileEntity, setOf(Material.BARREL)),
        DISPENSER(::VanillaContainerTileEntity, setOf(Material.DISPENSER)),
        DROPPER(::VanillaContainerTileEntity, setOf(Material.DROPPER)),
        HOPPER(::VanillaHopperTileEntity, setOf(Material.HOPPER)),
        SHULKER_BOX(::VanillaContainerTileEntity, setOf(Material.SHULKER_BOX)),
        CAULDRON(::VanillaCauldronTileEntity, setOf(Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON), false),
        CRAFTER(::VanillaCrafterTileEntity, setOf(Material.CRAFTER)),
        BREWING_STAND(::VanillaBrewingStandTileEntity, setOf(Material.BREWING_STAND));
        
        fun create(pos: BlockPos, data: Compound = Compound()): VanillaTileEntity =
            constructor(this, pos, data)
        
        companion object {
            
            private val map: Map<Material, Type> = run {
                val map = HashMap<Material, Type>()
                for (entry in entries) {
                    for (material in entry.materials) {
                        map[material] = entry
                    }
                }
                map
            }
            
            fun of(block: Material): Type? =
                map[block]
            
        }
        
    }
    
}