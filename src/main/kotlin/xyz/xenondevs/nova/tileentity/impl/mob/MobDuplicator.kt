package xyz.xenondevs.nova.tileentity.impl.mob

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
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
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("mob_duplicator.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("mob_duplicator.energy_per_tick")!!
private val ENERGY_PER_TICK_NBT = NovaConfig.getInt("mob_duplicator.energy_per_tick_nbt")!!
private val IDLE_TIME = NovaConfig.getInt("mob_duplicator.idle_time")!!
private val IDLE_TIME_NBT = NovaConfig.getInt("mob_duplicator.idle_time_nbt")!!

class MobDuplicator(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    private val inventory = getInventory("inventory", 1, true, ::handleInventoryUpdate)
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.TOP) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    private val energyPerTick: Int
        get() = if (keepNbt) ENERGY_PER_TICK_NBT else ENERGY_PER_TICK
    private val totalIdleTime: Int
        get() = if (keepNbt) IDLE_TIME_NBT else IDLE_TIME
    
    private val spawnLocation = location.clone().center().add(0.0, 1.0, 0.0)
    private var idleTime = 0
    private var entityType: EntityType? = null
    private var entityData: ByteArray? = null
    private var keepNbt = retrieveData("keepNbt") { false }
    
    override val gui by lazy { MobDuplicatorGUI() }
    
    init {
        setDefaultInventory(inventory)
        
        updateEntityData(inventory.getItemStack(0))
    }
    
    override fun handleTick() {
        if (entityData != null && entityType != null && energy >= energyPerTick) {
            energy -= energyPerTick
            
            if (idleTime++ == totalIdleTime) {
                idleTime = 0
                
                spawnEntity()
            }
            
            gui.updateIdleBar()
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
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
        idleTime = 0
        gui.updateIdleBar()
    }
    
    private fun spawnEntity() {
        if (keepNbt) EntityUtils.deserializeAndSpawn(entityData!!, spawnLocation, NBTUtils::removeItemData)
        else spawnLocation.world!!.spawnEntity(spawnLocation, entityType!!)
    }
    
    inner class MobDuplicatorGUI : TileEntityGUI("menu.nova.mob_duplicator") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MobDuplicator,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.ALL_TYPES))
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
            .addIngredient('u', UpgradesTeaserItem)
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -energyPerTick) }
        
        private val idleBar = object : VerticalBar(gui, x = 6, y = 1, height = 3, NovaMaterialRegistry.GREEN_BAR) {
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.mob_duplicator.idle", totalIdleTime - idleTime))
            
        }
        
        fun updateIdleBar() {
            idleBar.percentage = idleTime.toDouble() / totalIdleTime.toDouble()
        }
        
        private inner class ToggleNBTModeItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                return (if (keepNbt) NovaMaterialRegistry.NBT_ON_BUTTON else NovaMaterialRegistry.NBT_OFF_BUTTON)
                    .createBasicItemBuilder().setLocalizedName("menu.nova.mob_duplicator.nbt")
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                keepNbt = !keepNbt
                notifyWindows()
                
                idleTime = 0
                updateIdleBar()
                
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            
        }
        
    }
    
}