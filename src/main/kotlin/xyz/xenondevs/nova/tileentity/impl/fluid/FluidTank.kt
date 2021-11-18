package xyz.xenondevs.nova.tileentity.impl.fluid

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.hands
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private const val STATES = 13

class FluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val gui = lazy(::FluidTankGUI)
    
    private val fluidContainer = getFluidContainer("tank", hashSetOf(FluidType.WATER, FluidType.LAVA), 13000, 0, ::handleFluidUpdate)
    override val fluidHolder = NovaFluidHolder(this, fluidContainer to NetworkConnectionType.BUFFER)
    
    private fun handleFluidUpdate() {
        updateHeadStack()
    }
    
    override fun getHeadStack(): ItemStack {
        val model = material.block!!
        
        val data = (fluidContainer.amount.toDouble() / fluidContainer.capacity.toDouble() * STATES.toDouble()).toInt()
            .takeUnless { it == 0 }
            ?.let {
                when (fluidContainer.type) {
                    FluidType.LAVA -> it
                    FluidType.WATER -> STATES + it
                    else -> null
                }
            }
            ?: 0
        
        return model.createItemStack(data)
    }
    
    override fun handleRightClickNoWrench(event: PlayerInteractEvent) {
        val player = event.player
        val success = event.hands.any { (hand, _) -> handleBucketClick(player, hand) }
        if (success) {
            event.isCancelled = true
        } else super.handleRightClickNoWrench(event)
    }
    
    // TODO: Fix issues with filling when the bucket is in the off-hand
    // TODO: clean up
    private fun handleBucketClick(player: Player, hand: EquipmentSlot): Boolean {
        val gameMode = player.gameMode
        val inventory = player.inventory
        
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (inventory.getItem(hand).type) {
            
            Material.BUCKET -> {
                if (fluidContainer.amount >= 1000) {
                    if (gameMode != GameMode.CREATIVE) inventory.setItem(hand, fluidContainer.type!!.bucket)
                    fluidContainer.takeFluid(1000)
                    
                    when (fluidContainer.type) {
                        FluidType.LAVA -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f)
                        FluidType.WATER -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL, 1f, 1f)
                    }
                    
                    player.swingHand(hand)
                    
                    return true
                }
            }
            
            Material.WATER_BUCKET -> {
                if (fluidContainer.accepts(FluidType.WATER, 1000)) {
                    if (gameMode != GameMode.CREATIVE) inventory.setItem(hand, ItemStack(Material.BUCKET))
                    fluidContainer.addFluid(FluidType.WATER, 1000)
                    
                    return true
                }
            }
            
            Material.LAVA_BUCKET -> {
                if (fluidContainer.accepts(FluidType.LAVA, 1000)) {
                    if (gameMode != GameMode.CREATIVE) inventory.setItem(hand, ItemStack(Material.LAVA))
                    fluidContainer.addFluid(FluidType.LAVA, 1000)
                    
                    return true
                }
            }
            
        }
        
        return false
    }
    
    inner class FluidTankGUI : TileEntityGUI("menu.nova.fluid_tank") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@FluidTank,
            fluidContainers = listOf(fluidContainer to "container.nova.fluid_tank"),
            openPrevious = ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # . # # # |" +
                "| # # # . # # # |" +
                "| # # # . # # # |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = FluidBar(gui, x = 4, y = 1, height = 3, fluidContainer)
        
    }
    
}