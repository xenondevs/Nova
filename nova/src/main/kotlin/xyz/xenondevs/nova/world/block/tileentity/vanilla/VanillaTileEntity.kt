package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.CrafterBlockEntity
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.level.block.entity.DropperBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle

private val VANILLA_TILE_ENTITY: VarHandle = MethodHandles
    .privateLookupIn(BlockEntity::class.java, MethodHandles.lookup())
    .findVarHandle(BlockEntity::class.java, $$"nova$vanillaTileEntity", Any::class.java)

internal val BlockEntity.vanillaTileEntity: VanillaTileEntity?
    get() = VANILLA_TILE_ENTITY.get(this) as VanillaTileEntity?

internal abstract class VanillaTileEntity(
    protected val blockEntity: BlockEntity
) {
    
    protected open fun handleCreated() = Unit
    
    companion object {
        
        fun of(blockEntity: BlockEntity): VanillaTileEntity? {
            checkServerThread()
            blockEntity.vanillaTileEntity?.let { return it }
            
            val tileEntity = when (blockEntity) {
                is VanillaCauldronBlockEntity -> VanillaCauldronTileEntity(blockEntity)
                is ChestBlockEntity -> VanillaChestTileEntity(blockEntity)
                is AbstractFurnaceBlockEntity -> VanillaFurnaceTileEntity(blockEntity)
                is BarrelBlockEntity -> VanillaContainerTileEntity(blockEntity)
                is DropperBlockEntity -> VanillaContainerTileEntity(blockEntity)
                is DispenserBlockEntity -> VanillaContainerTileEntity(blockEntity)
                is ShulkerBoxBlockEntity -> VanillaContainerTileEntity(blockEntity)
                is HopperBlockEntity -> VanillaHopperTileEntity(blockEntity)
                is CrafterBlockEntity -> VanillaCrafterTileEntity(blockEntity)
                is BrewingStandBlockEntity -> VanillaBrewingStandTileEntity(blockEntity)
                else -> null
            } ?: return null
            
            VANILLA_TILE_ENTITY.set(blockEntity, tileEntity)
            tileEntity.handleCreated()
            return tileEntity
        }
        
    }
    
}