package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.SlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.virtualinventory.event.PlayerUpdateReason
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.network.item.inventory.CustomUpdateReason
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.ProgressArrowItem
import xyz.xenondevs.nova.util.BlockSide
import java.util.*

private val RECIPES: List<FurnaceRecipe> by lazy {
    val recipes = ArrayList<FurnaceRecipe>()
    Bukkit.getServer().recipeIterator().forEachRemaining { if (it is FurnaceRecipe) recipes += it }
    return@lazy recipes
}

private fun getRecipe(input: ItemStack) =
    RECIPES.firstOrNull { it.input.isSimilar(input) }

private val MAX_ENERGY = NovaConfig.getInt("electrical_furnace.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("electrical_furnace.energy_per_tick")!!
private val COOK_SPEED = NovaConfig.getInt("electrical_furnace.cook_speed")!!

class ElectricalFurnace(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyItemTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inputInventory = getInventory("input", 1, true, ::handleInputInventoryUpdate)
    private val outputInventory = getInventory("output", 1, true, ::handleOutputInventoryUpdate)
    
    private var currentRecipe: FurnaceRecipe? = retrieveOrNull<NamespacedKey>("currentRecipe")?.let { Bukkit.getRecipe(it) as FurnaceRecipe }
    private var timeCooked = retrieveData("timeCooked") { 0 }
    private var experience = retrieveData("experience") { 0f }
    
    private val gui by lazy { ElectricalFurnaceGUI() }
    
    init {
        addAvailableInventories(inputInventory, outputInventory)
        setDefaultInventory(inputInventory)
    }
    
    override fun saveData() {
        super.saveData()
        storeData("currentRecipe", currentRecipe?.key)
        storeData("timeCooked", timeCooked)
        storeData("experience", experience)
    }
    
    private fun handleInputInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null) {
            val itemStack = event.newItemStack
            if (getRecipe(itemStack) == null) event.isCancelled = true
        }
    }
    
    private fun handleOutputInventoryUpdate(event: ItemUpdateEvent) {
        val updateReason = event.updateReason
        if (updateReason is CustomUpdateReason && updateReason.message == "EF") return
        if (event.isRemove) {
            if (updateReason is PlayerUpdateReason) {
                val player = updateReason.player
                if (event.newItemStack == null) { // took all items
                    spawnExperienceOrb(player.location, experience)
                    experience = 0f
                } else {
                    val amount = event.removedAmount
                    val experiencePerItem = experience / event.previousItemStack.amount
                    val experience = amount * experiencePerItem
                    spawnExperienceOrb(player.location, experience)
                    this.experience -= experience
                }
            }
        } else event.isCancelled = true
    }
    
    private fun spawnExperienceOrb(location: Location, experience: Float) {
        val orb = location.world!!.spawnEntity(location, EntityType.EXPERIENCE_ORB) as ExperienceOrb
        orb.experience += experience.toInt()
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK) {
            if (currentRecipe == null) {
                val item = inputInventory.getItemStack(0)
                if (item != null) {
                    val recipe = getRecipe(item)
                    if (recipe != null && outputInventory.canHold(listOf(recipe.result))) {
                        currentRecipe = recipe
                        inputInventory.removeOne(null, 0)
                    }
                }
            }
            
            val currentRecipe = currentRecipe
            if (currentRecipe != null) {
                energy -= ENERGY_PER_TICK
                timeCooked += COOK_SPEED
                
                if (timeCooked >= currentRecipe.cookingTime) {
                    outputInventory.addItem(CustomUpdateReason("EF"), currentRecipe.result)
                    experience += currentRecipe.experience
                    timeCooked = 0
                    this.currentRecipe = null
                }
                
                gui.updateProgress()
            }
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    inner class ElectricalFurnaceGUI {
        
        private val progressItem = ProgressArrowItem()
        
        private val sideConfigGUI = SideConfigGUI(
            this@ElectricalFurnace,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                Triple(getNetworkedInventory(inputInventory), "Input Inventory", ItemConnectionType.ALL_TYPES),
                Triple(getNetworkedInventory(outputInventory), "Output Inventory", ItemConnectionType.EXTRACT_TYPES)
            )
        ) { openWindow(it) }
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # # # . |" +
                "| i # > # o # . |" +
                "| # # # # # # . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', SlotElement.VISlotElement(inputInventory, 0))
            .addIngredient('o', SlotElement.VISlotElement(outputInventory, 0))
            .addIngredient('>', progressItem)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .build()
        
        val energyBar = EnergyBar(gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, ENERGY_PER_TICK) }
        
        init {
            updateProgress()
        }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Electrical Furnace", gui).show()
        }
        
        fun updateProgress() {
            val cookTime = currentRecipe?.cookingTime ?: 0
            progressItem.percentage = if (timeCooked == 0) 0.0 else timeCooked.toDouble() / cookTime.toDouble()
        }
        
    }
    
}
