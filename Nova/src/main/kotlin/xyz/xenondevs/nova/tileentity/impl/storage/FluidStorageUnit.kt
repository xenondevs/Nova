package xyz.xenondevs.nova.tileentity.impl.storage

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FLUID_STORAGE_UNIT
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*


private val MAX_CAPACITY = NovaConfig[FLUID_STORAGE_UNIT].getLong("max_capacity")!!

class FluidStorageUnit(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val gui = lazy(::FluidStorageUnitGUI)
    val fluidTank = getFluidContainer("fluid", setOf(FluidType.LAVA, FluidType.WATER), MAX_CAPACITY, 0, ::handleFluidUpdate)
    val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    private val fluidLevel = FakeArmorStand(armorStand.location) { it.isInvisible = true; it.isMarker = true }
    
    override val fluidHolder = NovaFluidHolder(this, fluidTank to NetworkConnectionType.BUFFER)
    
    private fun handleFluidUpdate() {
        val stack = if (fluidTank.hasFluid()) {
            when (fluidTank.type) {
                FluidType.LAVA -> NovaMaterialRegistry.TANK_LAVA_LEVELS
                FluidType.WATER -> NovaMaterialRegistry.TANK_WATER_LEVELS
                else -> throw IllegalStateException()
            }.item.createItemStack(10)
        } else null
        fluidLevel.setEquipment(EquipmentSlot.HEAD, stack)
        fluidLevel.updateEquipment()
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        if (event.isAdd) {
            if (!(event.newItemStack.type == Material.LAVA_BUCKET || event.newItemStack.type == Material.WATER_BUCKET)) {
                event.isCancelled = true
                return
            }
            val fluidType = FluidType.values().first { it.bucket?.type == event.newItemStack.type }
            if (!fluidTank.accepts(fluidType, 1000)) {
                event.isCancelled = true
                return
            }
            fluidTank.addFluid(fluidType, 1000)
            event.newItemStack.type = Material.BUCKET
            runTaskLater(1) {
                gui.value.update()
            }
            
        }
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        fluidLevel.remove()
    }
    
    inner class FluidStorageUnitGUI : TileEntityGUI() {
        private val sideConfigGUI = SideConfigGUI(
            this@FluidStorageUnit,
            fluidContainers = listOf(fluidTank to "container.nova.fluid_tank"),
            openPrevious = ::openWindow
        )
        private val fluidStorageDisplay = FluidStorageUnitDisplay(this@FluidStorageUnit)
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| # # # # # f # |" +
                "| # i # c # f s |" +
                "| # # # # # f # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('i', inventory)
            .addIngredient('c', fluidStorageDisplay)
            .addIngredient('f', FluidBar(3, fluidHolder, fluidTank))
            .build()
        
        fun update() {
            fluidStorageDisplay.notifyWindows()
        }
        
        private inner class FluidStorageUnitDisplay(val fluidStorageUnit: FluidStorageUnit) : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                
                val type = fluidStorageUnit.fluidTank.type?.bucket
                    ?: return ItemBuilder(Material.BARRIER).setDisplayName("§r")
                val amount = fluidStorageUnit.fluidTank.amount
                val component = TranslatableComponent(
                    "menu.nova.fluid_storage_unit.item_display_" + if (amount > 1) "plural" else "singular",
                    TextComponent(amount.toString()).apply { color = ChatColor.GREEN }
                )
                return ItemBuilder(type).setDisplayName(component).setAmount(1)
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        }
    }
    
}