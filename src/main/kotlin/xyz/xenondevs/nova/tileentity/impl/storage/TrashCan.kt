package xyz.xenondevs.nova.tileentity.impl.storage

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.VirtualInventory
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.associateWithToEnumMap
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

class TrashCan(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    private val inventory = VirtualInventory(null, 1)
    
    override val gui: Lazy<TileEntityGUI> = lazy(::TrashCanGUI)
    override val itemHolder = NovaItemHolder(this, inventory, lazyDefaultTypeConfig = { CUBE_FACES.associateWithToEnumMap { ItemConnectionType.INSERT } })
    
    override fun handleTick() = inventory.setItemStackSilently(0, null)
    
    private inner class TrashCanGUI : TileEntityGUI("menu.nova.trash_can") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@TrashCan,
            null,
            listOf(Triple(itemHolder.getNetworkedInventory(inventory), "inventory.nova.input", ItemConnectionType.INSERT_TYPES)),
            ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # i # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
    }
    
}