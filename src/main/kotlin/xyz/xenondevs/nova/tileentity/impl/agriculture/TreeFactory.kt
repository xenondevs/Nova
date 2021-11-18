package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TREE_FACTORY
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.particleBuilder
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import java.util.*

private class PlantConfiguration(val miniature: NovaMaterial, val loot: ItemStack, val color: Color)

private val PLANTS = mapOf(
    Material.OAK_SAPLING to PlantConfiguration(NovaMaterialRegistry.OAK_TREE_MINIATURE, ItemStack(Material.OAK_LOG), Color(43, 82, 39)),
    Material.SPRUCE_SAPLING to PlantConfiguration(NovaMaterialRegistry.SPRUCE_TREE_MINIATURE, ItemStack(Material.SPRUCE_LOG), Color(43, 87, 60)),
    Material.BIRCH_SAPLING to PlantConfiguration(NovaMaterialRegistry.BIRCH_TREE_MINIATURE, ItemStack(Material.BIRCH_LOG), Color(49, 63, 35)),
    Material.JUNGLE_SAPLING to PlantConfiguration(NovaMaterialRegistry.JUNGLE_TREE_MINIATURE, ItemStack(Material.JUNGLE_LOG), Color(51, 127, 43)),
    Material.ACACIA_SAPLING to PlantConfiguration(NovaMaterialRegistry.ACACIA_TREE_MINIATURE, ItemStack(Material.ACACIA_LOG), Color(113, 125, 75)),
    Material.DARK_OAK_SAPLING to PlantConfiguration(NovaMaterialRegistry.DARK_OAK_TREE_MINIATURE, ItemStack(Material.DARK_OAK_LOG), Color(26, 65, 17)),
    Material.CRIMSON_FUNGUS to PlantConfiguration(NovaMaterialRegistry.CRIMSON_TREE_MINIATURE, ItemStack(Material.CRIMSON_STEM), Color(121, 0, 0)),
    Material.WARPED_FUNGUS to PlantConfiguration(NovaMaterialRegistry.WARPED_TREE_MINIATURE, ItemStack(Material.WARPED_STEM), Color(22, 124, 132)),
    Material.RED_MUSHROOM to PlantConfiguration(NovaMaterialRegistry.GIANT_RED_MUSHROOM_MINIATURE, ItemStack(Material.RED_MUSHROOM, 3), Color(192, 39, 37)),
    Material.BROWN_MUSHROOM to PlantConfiguration(NovaMaterialRegistry.GIANT_BROWN_MUSHROOM_MINIATURE, ItemStack(Material.BROWN_MUSHROOM, 3), Color(149, 112, 80))
)

private val MAX_ENERGY = NovaConfig[TREE_FACTORY].getLong("capacity")!!
private val ENERGY_PER_TICK = NovaConfig[TREE_FACTORY].getLong("energy_per_tick")!!
private val PROGRESS_PER_TICK = NovaConfig[TREE_FACTORY].getDouble("progress_per_tick")!!
private val IDLE_TIME = NovaConfig[TREE_FACTORY].getInt("idle_time")!!

private const val MAX_GROWTH_STAGE = 450

class TreeFactory(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inputInventory = getInventory("input", 1, intArrayOf(1), ::handleInputInventoryUpdate)
    private val outputInventory = getInventory("output", 9, ::handleOutputInventoryUpdate)
    
    override val gui: Lazy<TileEntityGUI> = lazy(::TreeFactoryGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradesUpdate, allowed = UpgradeType.ALL_ENERGY)
    override val itemHolder = NovaItemHolder(this, outputInventory to NetworkConnectionType.EXTRACT, inputInventory to NetworkConnectionType.BUFFER)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) {
        createExclusiveEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.BOTTOM, BlockSide.BACK)
    }
    
    private var plantType = inputInventory.getItemStack(0)?.type
    private val plant: FakeArmorStand
    
    private var progressPerTick = 0.0
    private var maxIdleTime = 0
    
    private var growthProgress = 0.0
    private var idleTimeLeft = 0
    
    init {
        val plantLocation = location.clone().center().apply { y += 1 / 16.0 }
        plant = FakeArmorStand(plantLocation, true) {
            it.isInvisible = true
            it.isMarker = true
        }
        handleUpgradesUpdate()
    }
    
    private fun handleUpgradesUpdate() {
        progressPerTick = PROGRESS_PER_TICK * upgradeHolder.getSpeedModifier()
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && plantType != null) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            if (idleTimeLeft == 0) {
                if (plantType != null) {
                    growthProgress += progressPerTick
                    if (growthProgress >= 1.0)
                        idleTimeLeft = maxIdleTime
                    
                    updatePlantArmorStand()
                }
            } else {
                idleTimeLeft--
                
                particleBuilder(ParticleEffect.REDSTONE) {
                    color(PLANTS[plantType]!!.color)
                    location(location.clone().center().apply { y += 0.5 })
                    offset(0.15, 0.15, 0.15)
                    speed(0.1f)
                    amount(5)
                }.display(getViewers())
                
                if (idleTimeLeft == 0) {
                    growthProgress = 0.0
                    
                    val loot = PLANTS[plantType]!!.loot
                    val leftover = outputInventory.addItem(SELF_UPDATE_REASON, loot)
                    if (leftover > 0)
                        armorStand.location.dropItem(loot.clone().apply { amount = leftover })
                }
            }
        }
    }
    
    private fun updatePlantArmorStand() {
        val growthStage = (MAX_GROWTH_STAGE * growthProgress).toInt().coerceAtMost(MAX_GROWTH_STAGE)
        plant.setEquipment(EquipmentSlot.HEAD, plantType?.let { PLANTS[it]!!.miniature.item.createItemStack(growthStage) })
        plant.updateEquipment()
    }
    
    private fun handleInputInventoryUpdate(event: ItemUpdateEvent) {
        if (event.newItemStack != null && event.newItemStack.type !in PLANTS.keys) {
            event.isCancelled = true
        } else {
            plantType = event.newItemStack?.type
            growthProgress = 0.0
            updatePlantArmorStand()
        }
    }
    
    private fun handleOutputInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && !event.isRemove
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        plant.remove()
    }
    
    private inner class TreeFactoryGUI : TileEntityGUI("menu.nova.tree_factory") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@TreeFactory,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inputInventory) to "inventory.nova.input",
                itemHolder.getNetworkedInventory(outputInventory) to "inventory.nova.output"
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 6)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # # # . |" +
                "| # # # o o o . |" +
                "| # i # o o o . |" +
                "| # # # o o o . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', VISlotElement(inputInventory, 0, NovaMaterialRegistry.SAPLING_PLACEHOLDER.itemProvider))
            .addIngredient('o', outputInventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        init {
            EnergyBar(gui = gui, x = 7, y = 1, height = 4, energyHolder)
        }
        
    }
    
}