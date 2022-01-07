package xyz.xenondevs.nova.tileentity.impl.processing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.item.impl.CycleItem
import de.studiocode.invui.item.impl.SimpleItem
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.FluidInfuserRecipe
import xyz.xenondevs.nova.data.recipe.FluidInfuserRecipe.InfuserMode
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.FLUID_INFUSER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.roundToInt

private val ENERGY_PER_TICK = NovaConfig[FLUID_INFUSER].getLong("energy_per_tick")!!
private val ENERGY_CAPACITY = NovaConfig[FLUID_INFUSER].getLong("energy_capacity")!!
private val FLUID_CAPACITY = NovaConfig[FLUID_INFUSER].getLong("fluid_capacity")!!

class FluidInfuser(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    override val gui = lazy(::FluidInfuserGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.FLUID)
    private val input = getInventory("input", 1, ::handleInputInventoryUpdate)
    private val output = getInventory("output", 1, ::handleOutputInventoryUpdate)
    private val tank = getFluidContainer("tank", hashSetOf(FluidType.WATER, FluidType.LAVA), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    override val itemHolder = NovaItemHolder(this, input to NetworkConnectionType.BUFFER, output to NetworkConnectionType.EXTRACT)
    override val energyHolder = ConsumerEnergyHolder(this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val fluidHolder = NovaFluidHolder(this, tank to NetworkConnectionType.BUFFER)
    
    private var mode = retrieveEnum("mode") { InfuserMode.INSERT }
    
    private var recipe: FluidInfuserRecipe? = null
    private val recipeTime: Int
        get() = (recipe!!.time.toDouble() / upgradeHolder.getSpeedModifier()).roundToInt()
    private var timePassed = 0
    
    override fun saveData() {
        super.saveData()
        storeData("mode", mode)
    }
    
    private fun handleInputInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = !event.isRemove && RecipeManager.getConversionRecipeFor(RecipeType.FLUID_INFUSER, event.newItemStack) == null
        if (!event.isAdd) reset()
    }
    
    private fun handleOutputInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && !event.isRemove
    }
    
    private fun reset() {
        this.recipe = null
        this.timePassed = 0
        if (gui.isInitialized()) gui.value.updateProgress()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (recipe == null && !input.isEmpty) {
                val item = input.getItemStack(0)
                
                if (mode == InfuserMode.INSERT && tank.hasFluid()) {
                    recipe = RecipeManager.getFluidInfuserInsertRecipeFor(tank.type!!, item)
                } else if (mode == InfuserMode.EXTRACT) {
                    recipe = RecipeManager.getFluidInfuserExtractRecipeFor(item)
                }
            }
            
            val recipe = recipe
            if (recipe != null) {
                if (((mode == InfuserMode.INSERT && tank.amount >= recipe.fluidAmount)
                        || (mode == InfuserMode.EXTRACT && tank.accepts(recipe.fluidType, recipe.fluidAmount)))
                    && output.canHold(recipe.result)) {
                    
                    energyHolder.energy -= energyHolder.energyConsumption
                    if (++timePassed >= recipeTime) {
                        input.addItemAmount(SELF_UPDATE_REASON, 0, -1)
                        output.addItem(SELF_UPDATE_REASON, recipe.result)
                        
                        if (mode == InfuserMode.INSERT) tank.takeFluid(recipe.fluidAmount)
                        else tank.addFluid(recipe.fluidType, recipe.fluidAmount)
                        
                        reset()
                    } else if (gui.isInitialized()) gui.value.updateProgress()
                } else timePassed = 0
            }
        }
    }
    
    inner class FluidInfuserGUI : TileEntityGUI() {
        
        private val progressItem = InfuserProgressItem()
        private val changeModeItem = CycleItem.withStateChangeHandler(
            ::changeMode,
            mode.ordinal,
            NovaMaterialRegistry.FLUID_LEFT_RIGHT_BUTTON.itemProvider,
            NovaMaterialRegistry.FLUID_RIGHT_LEFT_BUTTON.itemProvider
        )
        
        private val sideConfigGUI = SideConfigGUI(
            this@FluidInfuser,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(input) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(output) to "inventory.nova.output"
            ),
            listOf(tank to "container.nova.fluid_tank"),
            ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| f # m u s # e |" +
                "| f p i > o # e |" +
                "| f # # # # # e |" +
                "3 - - - - - - - 4")
            .addIngredient('i', input)
            .addIngredient('o', output)
            .addIngredient('p', progressItem)
            .addIngredient('m', changeModeItem)
            .addIngredient('>', SimpleItem(NovaMaterialRegistry.PROGRESS_ARROW.itemProvider))
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('f', FluidBar(3, fluidHolder, tank))
            .addIngredient('e', EnergyBar(3, energyHolder))
            .build()
        
        fun updateProgress() {
            progressItem.percentage = if (recipe != null) timePassed.toDouble() / recipeTime.toDouble() else 0.0
        }
        
        private fun changeMode(player: Player?, modeOrdinal: Int) {
            mode = InfuserMode.values()[modeOrdinal]
            reset()
            player!!.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        }
        
        private inner class InfuserProgressItem : BaseItem() {
            
            var percentage: Double = 0.0
                set(value) {
                    field = value.coerceIn(0.0, 1.0)
                    notifyWindows()
                }
            
            override fun getItemProvider(): ItemProvider {
                val material = if (mode == InfuserMode.INSERT)
                    NovaMaterialRegistry.FLUID_PROGRESS_LEFT_RIGHT
                else NovaMaterialRegistry.FLUID_PROGRESS_RIGHT_LEFT
                
                return material.item.createItemBuilder("", (percentage * 16).roundToInt())
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
        }
        
    }
    
}