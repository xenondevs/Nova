package xyz.xenondevs.nova.tileentity.impl.storage

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.impl.BaseItem
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
    private val fluidTank = getFluidContainer("fluid", setOf(FluidType.LAVA, FluidType.WATER), MAX_CAPACITY, 0, ::handleFluidUpdate)
    private val fluidLevel = FakeArmorStand(armorStand.location) { it.isInvisible = true; it.isMarker = true }
    override val fluidHolder = NovaFluidHolder(this, fluidTank to NetworkConnectionType.BUFFER) { createSideConfig(NetworkConnectionType.BUFFER) }
    
    init {
        handleFluidUpdate()
    }
    
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
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # f |" +
                "| # # # d # # f |" +
                "| # # # # # # f |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('d', FluidStorageUnitDisplay())
            .addIngredient('f', FluidBar(3, fluidHolder, fluidTank))
            .build()
        
        private inner class FluidStorageUnitDisplay : BaseItem() {
            
            init {
                fluidTank.updateHandlers += { notifyWindows() }
            }
            
            override fun getItemProvider(): ItemProvider {
                val type = fluidTank.type?.bucket
                    ?: return ItemBuilder(Material.BARRIER).setDisplayName("§r")
                val amount = fluidTank.amount
                return ItemBuilder(type).setDisplayName("§a$amount §7mB").setAmount(1)
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
            
        }
    }
    
}