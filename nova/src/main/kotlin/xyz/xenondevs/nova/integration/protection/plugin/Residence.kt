package xyz.xenondevs.nova.integration.protection.plugin

import com.bekvon.bukkit.residence.Residence
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.integration.InternalIntegration
import xyz.xenondevs.nova.integration.protection.FakeOnlinePlayer

internal object Residence : ProtectionIntegration, InternalIntegration {
    
    private val RESIDENCE = if (Bukkit.getPluginManager().getPlugin("Residence") != null) Residence.getInstance() else null
    override val isInstalled = RESIDENCE != null
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        return getResidencePlayer(player, location).canBreakBlock(location.block, true)
    }
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        return getResidencePlayer(player, location).canPlaceBlock(location.block, true)
    }
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        return canBreak(player, item, location)
    }
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        return canBreak(player, item, location)
    }
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return canHurtEntity(player, entity, item)
    }
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return getResidencePlayer(player, entity.location).canDamageEntity(entity, true)
    }
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        return getResidencePlayer(tileEntity.owner, location).canBreakBlock(location.block, false)
    }
    
    override fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        return getResidencePlayer(tileEntity.owner, location).canPlaceBlock(location.block, false)
    }
    
    override fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        return canBreak(tileEntity, item, location)
    }
    
    override fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        return canBreak(tileEntity, item, location)
    }
    
    override fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean {
        return canHurtEntity(tileEntity, entity, item)
    }
    
    override fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean {
        return getResidencePlayer(tileEntity.owner, entity.location).canDamageEntity(entity, false)
    }
    
    private fun getResidencePlayer(player: OfflinePlayer, location: Location) =
        RESIDENCE!!.playerManager.getResidencePlayer(FakeOnlinePlayer(player, location))
    
}