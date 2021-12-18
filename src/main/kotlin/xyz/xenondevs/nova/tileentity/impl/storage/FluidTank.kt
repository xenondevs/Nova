package xyz.xenondevs.nova.tileentity.impl.storage

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
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.hands
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.roundToInt
import net.minecraft.world.entity.EquipmentSlot as NMSEquipmentSlot

private const val MAX_STATE = 99

open class FluidTank(
    capacity: Long,
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val gui = lazy(::FluidTankGUI)
    
    private val fluidContainer = getFluidContainer("tank", hashSetOf(FluidType.WATER, FluidType.LAVA), capacity, 0, ::handleFluidUpdate)
    override val fluidHolder = NovaFluidHolder(this, fluidContainer to NetworkConnectionType.BUFFER, defaultConnectionConfig = { createSideConfig(NetworkConnectionType.BUFFER) })
    private val fluidLevel = FakeArmorStand(armorStand.location, true) {
        it.isInvisible = true
        it.isMarker = true
    }
    
    init {
        updateFluidLevel()
    }
    
    private fun handleFluidUpdate() {
        updateFluidLevel()
    }
    
    private fun updateFluidLevel() {
        val stack = if (fluidContainer.hasFluid()) {
            val state = (fluidContainer.amount.toDouble() / fluidContainer.capacity.toDouble() * MAX_STATE.toDouble()).roundToInt()
            when (fluidContainer.type) {
                FluidType.LAVA -> NovaMaterialRegistry.LAVA_LEVELS
                FluidType.WATER -> NovaMaterialRegistry.WATER_LEVELS
                else -> throw IllegalStateException()
            }.item.createItemStack(state)
        } else null
        
        val shouldGlow = fluidContainer.type == FluidType.LAVA
        if (fluidLevel.hasVisualFire != shouldGlow) {
            fluidLevel.hasVisualFire = shouldGlow
            fluidLevel.updateEntityData()
        }
        
        fluidLevel.setEquipment(NMSEquipmentSlot.HEAD, stack)
        fluidLevel.updateEquipment()
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        fluidLevel.remove()
    }
    
    override fun handleRightClickNoWrench(event: PlayerInteractEvent) {
        val player = event.player
        val success = event.hands.any { (hand, _) -> handleBucketClick(player, hand) }
        if (success) {
            event.isCancelled = true
        } else super.handleRightClickNoWrench(event)
    }
    
    // TODO: clean up
    private fun handleBucketClick(player: Player, hand: EquipmentSlot): Boolean {
        val gameMode = player.gameMode
        val inventory = player.inventory
        
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (inventory.getItem(hand).type) {
            
            Material.BUCKET -> {
                if (fluidContainer.amount >= 1000) {
                    if (gameMode != GameMode.CREATIVE) {
                        val bucket = fluidContainer.type!!.bucket!!
                        if (inventory.getItem(hand).amount == 1) {
                            inventory.setItem(hand, bucket)
                        } else {
                            inventory.getItem(hand).amount -= 1
                            inventory.addItem(bucket)
                        }
                    }
                    
                    when (fluidContainer.type) {
                        FluidType.LAVA -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f)
                        FluidType.WATER -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL, 1f, 1f)
                        else -> throw IllegalStateException()
                    }
                    
                    fluidContainer.takeFluid(1000)
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
            
            else -> Unit
            
        }
        
        return false
    }
    
    inner class FluidTankGUI : TileEntityGUI() {
        
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
        
        init {
            FluidBar(gui, x = 4, y = 1, height = 3, fluidContainer)
        }
        
    }
    
}

private val BASIC_CAPACITY = NovaConfig[NovaMaterialRegistry.BASIC_FLUID_TANK].getLong("capacity")!!
private val ADVANCED_CAPACITY = NovaConfig[NovaMaterialRegistry.ADVANCED_FLUID_TANK].getLong("capacity")!!
private val ELITE_CAPACITY = NovaConfig[NovaMaterialRegistry.ELITE_FLUID_TANK].getLong("capacity")!!
private val ULTIMATE_CAPACITY = NovaConfig[NovaMaterialRegistry.ULTIMATE_FLUID_TANK].getLong("capacity")!!

class BasicFluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : FluidTank(BASIC_CAPACITY, uuid, data, material, ownerUUID, armorStand)

class AdvancedFluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : FluidTank(ADVANCED_CAPACITY, uuid, data, material, ownerUUID, armorStand)

class EliteFluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : FluidTank(ELITE_CAPACITY, uuid, data, material, ownerUUID, armorStand)

class UltimateFluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : FluidTank(ULTIMATE_CAPACITY, uuid, data, material, ownerUUID, armorStand)

class CreativeFluidTank(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : FluidTank(Long.MAX_VALUE, uuid, data, material, ownerUUID, armorStand)