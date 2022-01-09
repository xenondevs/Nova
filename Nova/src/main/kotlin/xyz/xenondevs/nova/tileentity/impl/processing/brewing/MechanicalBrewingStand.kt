package xyz.xenondevs.nova.tileentity.impl.processing.brewing

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.item.builder.PotionBuilder
import de.studiocode.invui.item.impl.BaseItem
import de.studiocode.invui.virtualinventory.event.InventoryUpdatedEvent
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import de.studiocode.invui.virtualinventory.event.UpdateReason
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.MechanicalBrewingStandRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.MECHANICAL_BREWING_STAND
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
import xyz.xenondevs.nova.ui.item.BrewProgressItem
import xyz.xenondevs.nova.ui.overlay.GUITexture
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.contains
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.localizedName
import xyz.xenondevs.nova.util.removeFirstMatching
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.math.roundToInt

private val ENERGY_CAPACITY = NovaConfig[MECHANICAL_BREWING_STAND].getLong("energy_capacity")!!
private val ENERGY_PER_TICK = NovaConfig[MECHANICAL_BREWING_STAND].getLong("energy_per_tick")!!
private val FLUID_CAPACITY = NovaConfig[MECHANICAL_BREWING_STAND].getLong("fluid_capacity")!!
private val BREW_TIME = NovaConfig[MECHANICAL_BREWING_STAND].getInt("brew_time")!!

private val IGNORE_UPDATE_REASON = object : UpdateReason {}

// TODO: potion color picker, name config button 

class MechanicalBrewingStand(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    // These values need to be accessed from outside the class
    companion object {
        @Suppress("UNCHECKED_CAST")
        val AVAILABLE_POTION_EFFECTS: Map<PotionEffectType, MechanicalBrewingStandRecipe> =
            (RecipeManager.novaRecipes[RecipeType.MECHANICAL_BREWING_STAND]!!.values as Iterable<MechanicalBrewingStandRecipe>)
                .associateBy { it.result }
        
        val ALLOW_DURATION_AMPLIFIER_MIXING = NovaConfig[MECHANICAL_BREWING_STAND].getBoolean("duration_amplifier_mixing")
    }
    
    override val gui = lazy(::MechanicalBrewingStandGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, UpgradeType.SPEED, UpgradeType.EFFICIENCY, UpgradeType.ENERGY, UpgradeType.FLUID)
    private val fluidTank = getFluidContainer("tank", setOf(FluidType.WATER), FLUID_CAPACITY, upgradeHolder = upgradeHolder)
    private val ingredientsInventory = getInventory("ingredients", 27, null, ::handleIngredientsInventoryAfterUpdate)
    private val outputInventory = getInventory("output", 3, ::handleOutputPreUpdate, ::handleOutputInventoryAfterUpdate)
    
    override val energyHolder = ConsumerEnergyHolder(
        this, ENERGY_CAPACITY, ENERGY_PER_TICK, 0, upgradeHolder
    ) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.FRONT) }
    
    override val itemHolder = NovaItemHolder(
        this,
        ingredientsInventory to NetworkConnectionType.BUFFER,
        outputInventory to NetworkConnectionType.BUFFER,
        lazyDefaultTypeConfig = { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    )
    
    override val fluidHolder = NovaFluidHolder(
        this,
        fluidTank to NetworkConnectionType.BUFFER,
        defaultConnectionConfig = { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    )
    
    private var maxBrewTime = 0
    private var timePassed = 0
    
    private var potionType = retrieveEnum<PotionBuilder.PotionType>("potionType") { PotionBuilder.PotionType.NORMAL }
    private lateinit var potionEffects: List<PotionEffectBuilder>
    private var requiredItems: List<ItemStack>? = null
    private var requiredItemsStatus: MutableMap<ItemStack, Boolean>? = null
    private var nextPotion: ItemStack? = null
    
    init {
        val potionEffects = ArrayList<PotionEffectBuilder>()
        retrieveElementOrNull<ListElement>("potionEffects")?.forEach { potionCompound ->
            potionCompound as CompoundElement
            val type = PotionEffectType.getByKey(potionCompound.get<NamespacedKey>("type"))!!
            val duration: Int = potionCompound.getAsserted("duration")
            val amplifier: Int = potionCompound.getAsserted("amplifier")
            
            potionEffects += PotionEffectBuilder(type, duration, amplifier)
        }
        
        updatePotionEffects(potionType, potionEffects)
        handleUpgradeUpdates()
    }
    
    override fun saveData() {
        super.saveData()
        
        val list = ListElement()
        potionEffects.forEach { effect ->
            val potionCompound = CompoundElement()
            potionCompound.put("type", effect.type!!.key)
            potionCompound.put("duration", effect.durationLevel)
            potionCompound.put("amplifier", effect.amplifierLevel)
            
            list += potionCompound
        }
        
        storeData("potionEffects", list)
        storeData("potionType", potionType)
    }
    
    private fun handleUpgradeUpdates() {
        maxBrewTime = (BREW_TIME / upgradeHolder.getSpeedModifier()).roundToInt()
    }
    
    private fun updatePotionEffects(type: PotionBuilder.PotionType, effects: List<PotionEffectBuilder>) {
        this.potionEffects = effects.map(PotionEffectBuilder::clone)
        this.potionType = type
        
        if (effects.isNotEmpty()) {
            val requiredItems = ArrayList<ItemStack>()
            
            // Potion type items
            requiredItems.add(ItemStack(Material.GLASS_BOTTLE, 3))
            if (type == PotionBuilder.PotionType.SPLASH) {
                requiredItems.add(ItemStack(Material.GUNPOWDER))
            } else if (type == PotionBuilder.PotionType.LINGERING) {
                requiredItems.add(ItemStack(Material.GUNPOWDER))
                requiredItems.add(ItemStack(Material.DRAGON_BREATH))
            }
            
            // Potion modifier items
            var redstone = 0
            var glowstone = 0
            
            // Potion ingredients
            potionEffects.forEach { effect ->
                effect.recipe.inputs.forEach { choice ->
                    require(choice is RecipeChoice.ExactChoice && choice.choices.size == 1)
                    val itemStack = choice.choices[0] // There should only ever be one
                    val firstSimilar = requiredItems.firstOrNull { it.isSimilar(itemStack) }
                    
                    if (firstSimilar != null) firstSimilar.amount++
                    else requiredItems += itemStack
                }
                redstone += effect.durationLevel
                glowstone += effect.amplifierLevel
            }
            
            // Potion modifier items
            if (redstone > 0) requiredItems.add(ItemStack(Material.REDSTONE, redstone))
            if (glowstone > 0) requiredItems.add(ItemStack(Material.GLOWSTONE_DUST, glowstone))
            
            // Set required items
            this.requiredItems = requiredItems
        } else {
            requiredItems = null
            requiredItemsStatus = null
        }
        
        // Update
        updateAllRequiredStatus()
        checkBrewingPossibility()
        if (gui.isInitialized()) {
            gui.value.configurePotionItem.notifyWindows()
            gui.value.ingredientsDisplay.notifyWindows()
        }
    }
    
    private fun handleOutputPreUpdate(event: ItemUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    private fun handleIngredientsInventoryAfterUpdate(event: InventoryUpdatedEvent) {
        if (event.updateReason != IGNORE_UPDATE_REASON) {
            if (event.isAdd) updateFalseRequiredStatus()
            else updateAllRequiredStatus()
            
            checkBrewingPossibility()
        }
    }
    
    private fun handleOutputInventoryAfterUpdate(event: InventoryUpdatedEvent) {
        checkBrewingPossibility()
    }
    
    private fun updateFalseRequiredStatus() {
        if (requiredItems == null) return
        requiredItemsStatus!!
            .filter { !it.value }
            .forEach { (item, _) -> requiredItemsStatus!![item] = ingredientsInventory.contains(item) }
        
        if (gui.isInitialized())
            gui.value.ingredientsDisplay.notifyWindows()
    }
    
    private fun updateAllRequiredStatus() {
        if (requiredItems == null) return
        requiredItemsStatus = requiredItems!!.associateWithTo(HashMap()) { ingredientsInventory.contains(it) }
        
        if (gui.isInitialized())
            gui.value.ingredientsDisplay.notifyWindows()
    }
    
    private fun checkBrewingPossibility() {
        if (requiredItems != null && requiredItemsStatus != null && outputInventory.isEmpty && requiredItemsStatus!!.values.all { it }) {
            val builder = PotionBuilder(potionType)
            potionEffects.forEach { builder.addEffect(it.build()) }
            nextPotion = builder.setAmount(3).get()
        } else {
            nextPotion = null
            timePassed = 0
            if (gui.isInitialized()) gui.value.progressItem.percentage = 0.0
        }
    }
    
    override fun handleTick() {
        if (nextPotion != null && energyHolder.energy >= energyHolder.energyConsumption && fluidTank.amount >= 1000) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (++timePassed >= maxBrewTime) {
                outputInventory.addItem(SELF_UPDATE_REASON, nextPotion)
                
                nextPotion = null
                timePassed = 0
                fluidTank.takeFluid(1000)
                if (!requiredItems!!.all { ingredientsInventory.removeFirstMatching(it, IGNORE_UPDATE_REASON) == 0 })
                    throw IllegalStateException("Could not remove all ingredients from the ingredients inventory")
                updateAllRequiredStatus()
            }
            
            if (gui.isInitialized()) gui.value.progressItem.percentage = timePassed.toDouble() / maxBrewTime.toDouble()
        }
    }
    
    inner class MechanicalBrewingStandGUI : TileEntityGUI(GUITexture.MECHANICAL_BREWING_STAND) {
        
        private val sideConfigGUI = SideConfigGUI(
            this@MechanicalBrewingStand,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(ingredientsInventory) to "inventory.nova.ingredients",
                itemHolder.getNetworkedInventory(outputInventory) to "inventory.nova.output",
            ),
            listOf(fluidTank to "container.nova.fluid_tank"),
            openPrevious = ::openWindow
        )
        
        val configurePotionItem = ConfigurePotionItem()
        val progressItem = BrewProgressItem()
        val ingredientsDisplay = IngredientsDisplay()
        
        override val gui: GUI = GUIBuilder(GUIType.SCROLL_INVENTORY, 9, 6)
            .setStructure("" +
                ". x x x u i . U s" +
                ". x x x . p . . ." +
                ". x x x d . . f e" +
                ". ^ . ^ . . . f e" +
                ". o . o . . . f e" +
                ". . o . . . . f e")
            .setInventory(ingredientsInventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('U', OpenUpgradesItem(upgradeHolder))
            .addIngredient('e', EnergyBar(4, energyHolder))
            .addIngredient('f', FluidBar(4, fluidHolder, fluidTank))
            .addIngredient('i', ingredientsDisplay)
            .addIngredient('p', configurePotionItem)
            .addIngredient('o', outputInventory, NovaMaterialRegistry.BOTTLE_PLACEHOLDER.itemProvider)
            .addIngredient('^', progressItem)
            .build()
        
        private val configuratorWindow = PotionConfiguratorWindow(
            potionEffects.map(PotionEffectBuilder::clone),
            potionType,
            ::updatePotionEffects,
            ::openWindow
        )
        
        inner class ConfigurePotionItem : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                val builder = PotionBuilder(potionType)
                    .setBasePotionData(PotionData(PotionType.WATER, false, false))
                    .setDisplayName(TranslatableComponent("menu.nova.mechanical_brewing_stand.configured_potion"))
                potionEffects.forEach { builder.addEffect(it.build()) }
                return builder
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
                configuratorWindow.openConfigurator(player)
            }
            
        }
        
        inner class IngredientsDisplay : BaseItem() {
            
            override fun getItemProvider(): ItemProvider {
                val hasAll = requiredItemsStatus?.all { it.value } ?: false
                val builder = ItemBuilder(Material.KNOWLEDGE_BOOK)
                    .setDisplayName(localized(if (hasAll) ChatColor.GREEN else ChatColor.RED, "menu.nova.mechanical_brewing_stand.ingredients"))
                requiredItems
                    ?.asSequence()
                    ?.sortedByDescending { it.amount }
                    ?.forEach {
                        val hasItem = requiredItemsStatus?.get(it) ?: false
                        val component = ComponentBuilder()
                            .color(if (hasItem) ChatColor.GREEN else ChatColor.RED)
                            .append("${it.amount}x ")
                            .append(TranslatableComponent(it.localizedName))
                            .append(": " + if (hasItem) "✓" else "❌")
                            .create()
                        
                        builder.addLoreLines(component)
                    }
                
                return builder
            }
            
            override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) = Unit
            
        }
        
    }
    
}
