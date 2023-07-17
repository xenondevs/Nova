package xyz.xenondevs.nova.hook.impl.residence

import com.bekvon.bukkit.residence.Residence
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.util.FakeOnlinePlayer

@Hook(plugins = ["Residence"])
internal object ResidenceHook : ProtectionIntegration {
    
    private val RESIDENCE: Residence = Residence.getInstance()
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        getResidencePlayer(player, location).canBreakBlock(location.block, true)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        getResidencePlayer(player, location).canPlaceBlock(location.block, true)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        canBreak(player, item, location)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        canBreak(player, item, location)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntity(player, entity, item)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        getResidencePlayer(player, entity.location).canDamageEntity(entity, true)
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        getResidencePlayer(tileEntity.owner!!, location).canBreakBlock(location.block, false)
    
    override fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        getResidencePlayer(tileEntity.owner!!, location).canPlaceBlock(location.block, false)
    
    override fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        canBreak(tileEntity, item, location)
    
    override fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        canBreak(tileEntity, item, location)
    
    override fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntity(tileEntity, entity, item)
    
    override fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        getResidencePlayer(tileEntity.owner!!, entity.location).canDamageEntity(entity, false)
    
    private fun getResidencePlayer(player: OfflinePlayer, location: Location) =
        RESIDENCE.playerManager.getResidencePlayer(FakeOnlinePlayer.create(player, location))
    
}