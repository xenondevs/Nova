package xyz.xenondevs.nova.tileentity.vanilla

import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.NetworkedShulkerBoxInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.SingleMojangStackContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos

internal class VanillaContainerTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(pos, data) {
    
    override val type = Type.CONTAINER
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        val container = pos.nmsBlockEntity as BaseContainerBlockEntity
        val inventory = getInventory(container)
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(storedValue("itemHolder", ::Compound), inventories) // TODO: legacy support
    }
    
    private fun getInventory(blockEntity: BaseContainerBlockEntity): NetworkedInventory {
        if (blockEntity is ShulkerBoxBlockEntity)
            return NetworkedShulkerBoxInventory(SingleMojangStackContainer(blockEntity.contents))
        
        val contents = when (blockEntity) {
            is BarrelBlockEntity -> blockEntity.contents
            is BrewingStandBlockEntity -> blockEntity.contents // TODO: should have its own vte
            is DispenserBlockEntity -> blockEntity.contents
            is HopperBlockEntity -> blockEntity.contents
            else -> throw IllegalArgumentException("Unsupported container block entity: $blockEntity")
        }
        
        return NetworkedNMSInventory(SingleMojangStackContainer(contents))
    }
    
}