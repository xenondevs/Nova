package xyz.xenondevs.nova.tileentity.impl.mob

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.item.impl.BottledMobItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.MOB_DUPLICATOR
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY = NovaConfig[MOB_DUPLICATOR].getInt("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[MOB_DUPLICATOR].getInt("energy_per_tick")!!
private val ENERGY_PER_TICK_NBT = NovaConfig[MOB_DUPLICATOR].getInt("energy_per_tick_nbt")!!
private val IDLE_TIME = NovaConfig[MOB_DUPLICATOR].getInt("idle_time")!!
private val IDLE_TIME_NBT = NovaConfig[MOB_DUPLICATOR].getInt("idle_time_nbt")!!

class MobDuplicator(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui = lazy { MobDuplicatorGUI() }
    override val upgradeHolder = UpgradeHolder(data, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, ENERGY_PER_TICK_NBT, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.TOP) }
    override val itemHolder = NovaItemHolder(this, inventory)
    private val energyPerTick: Int
        get() = if (keepNbt) energyHolder.specialEnergyConsumption else energyHolder.energyConsumption
    private val totalIdleTime: Int
        get() = if (keepNbt) idleTimeNBT else idleTime
    
    private var idleTimeNBT = 0
    private var idleTime = 0
    
    private val spawnLocation = location.clone().center().add(0.0, 1.0, 0.0)
    private var timePassed = 0
    private var entityType: EntityType? = null
    private var entityData: ByteArray? = null
    private var keepNbt = retrieveData("keepNbt") { false }
    
    
    init {
        handleUpgradeUpdates()
        updateEntityData(inventory.getItemStack(0))
    }
    
    private fun handleUpgradeUpdates() {
        idleTimeNBT = (IDLE_TIME_NBT / upgradeHolder.getSpeedModifier()).toInt()
        idleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > totalIdleTime) timePassed = totalIdleTime
    }
    
    override fun handleTick() {
        if (entityData != null && entityType != null && energyHolder.energy >= energyPerTick) {
            energyHolder.energy -= energyPerTick
            
            if (timePassed++ == totalIdleTime) {
                timePassed = 0
                
                spawnEntity()
            }
            
            if (gui.isInitialized()) gui.value.updateIdleBar()
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null) {
            event.isCancelled = !updateEntityData(event.newItemStack)
        } else setEntityData(null, null)
    }
    
    private fun updateEntityData(itemStack: ItemStack?): Boolean {
        val novaItem = itemStack?.novaMaterial?.novaItem
        if (novaItem is BottledMobItem) {
            setEntityData(novaItem.getEntityType(itemStack), novaItem.getEntityData(itemStack))
            return true
        }
        return false
    }
    
    private fun setEntityData(type: EntityType?, data: ByteArray?) {
        entityData = data
        entityType = type
        timePassed = 0
        if (gui.isInitialized()) gui.value.updateIdleBar()
    }
    
    private fun spawnEntity() {
        if (keepNbt) EntityUtils.deserializeAndSpawn(entityData!!, spawnLocation, NBTUtils::removeItemData)
        else spawnLocation.world!!.spawnEntity(spawnLocation, entityType!!)
    }
    
    inner class MobDuplicatorGUI : TileEntityGUI("menu.nova.mob_duplicator") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MobDuplicator,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(itemHolder.getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # . . |" +
                "| n # # i # . . |" +
                "| u # # # # . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('i', VISlotElement(inventory, 0, NovaMaterialRegistry.BOTTLED_MOB_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('n', ToggleNBTModeItem())
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3, energyHolder)
        
        private val idleBar = object : VerticalBar(gui, x = 6, y = 1, height = 3, NovaMaterialRegistry.GREEN_BAR) {
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.mob_duplicator.idle", totalIdleTime - timePassed))
            
        }
        
        fun updateIdleBar() {
            idleBar.percentage = timePassed.toDouble() / totalIdleTime.toDouble()
        }
        
        private inner class ToggleNBTModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return (if (keepNbt) NovaMaterialRegistry.NBT_ON_BUTTON else NovaMaterialRegistry.NBT_OFF_BUTTON)
                    .createBasicItemBuilder().setLocalizedName("menu.nova.mob_duplicator.nbt")
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                keepNbt = !keepNbt
                notifyWindows()
                
                timePassed = 0
                updateIdleBar()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
}