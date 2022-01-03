package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import kotlin.math.roundToInt

open class ProgressItem(val material: NovaMaterial, private val maxState: Int) : BaseItem() {
    
    var percentage: Double = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            notifyWindows()
        }
    
    override fun getItemProvider(): ItemProvider {
        return material.item.createItemBuilder("", (percentage * maxState).roundToInt())
    }
    
    override fun handleClick(clickType: ClickType?, player: Player?, event: InventoryClickEvent?) = Unit
}

class ProgressArrowItem : ProgressItem(NovaMaterialRegistry.PROGRESS_ARROW, 16)

class EnergyProgressItem : ProgressItem(NovaMaterialRegistry.ENERGY_PROGRESS, 16)

class PressProgressItem : ProgressItem(NovaMaterialRegistry.PRESS_PROGRESS, 8)

class PulverizerProgressItem : ProgressItem(NovaMaterialRegistry.PULVERIZER_PROGRESS, 14)

class LeftRightFluidProgressItem : ProgressItem(NovaMaterialRegistry.FLUID_PROGRESS_LEFT_RIGHT, 16)

class RightLeftFluidProgressItem : ProgressItem(NovaMaterialRegistry.FLUID_PROGRESS_RIGHT_LEFT, 16)
