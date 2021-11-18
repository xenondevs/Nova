package xyz.xenondevs.nova.tileentity.impl.world

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.ChatColor
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.STAR_COLLECTOR
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
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigGUI
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import java.util.*

private val MAX_ENERGY = NovaConfig[STAR_COLLECTOR].getLong("capacity")!!
private val IDLE_ENERGY_PER_TICK = NovaConfig[STAR_COLLECTOR].getLong("energy_per_tick_idle")!!
private val COLLECTING_ENERGY_PER_TICK = NovaConfig[STAR_COLLECTOR].getLong("energy_per_tick_collecting")!!
private val IDLE_TIME = NovaConfig[STAR_COLLECTOR].getInt("idle_time")!!
private val COLLECTION_TIME = NovaConfig[STAR_COLLECTOR].getInt("collection_time")!!

private const val STAR_PARTICLE_DISTANCE_PER_TICK = 0.75

class StarCollector(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 1, ::handleInventoryUpdate)
    override val gui: Lazy<StarCollectorGUI> = lazy(::StarCollectorGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradesUpdate, allowed = UpgradeType.ALL_ENERGY)
    override val itemHolder = NovaItemHolder(this, inventory to NetworkConnectionType.EXTRACT)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, IDLE_ENERGY_PER_TICK, COLLECTING_ENERGY_PER_TICK, upgradeHolder) {
        createExclusiveEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.BOTTOM)
    }
    
    private var maxIdleTime = 0
    private var maxCollectionTime = 0
    private var timeSpentIdle = 0
    private var timeSpentCollecting = -1
    private lateinit var particleVector: Vector
    
    private val rodLocation = location.clone().center().apply { y += 0.7 }
    private val rod = FakeArmorStand(location.clone().center().apply { y -= 1 }, true) {
        it.isMarker = true
        it.isInvisible = true
        it.setEquipment(EquipmentSlot.HEAD, material.block!!.createItemStack(1))
    }
    
    private val particleTask = createParticleTask(listOf(
        particle(ParticleEffect.DUST_COLOR_TRANSITION) {
            location(location.clone().center().apply { y += 0.2 })
            dustFade(Color(132, 0, 245), Color(196, 128, 217), 1f)
            offset(0.25, 0.1, 0.25)
            amount(3)
        }
    ), 1)
    
    init {
        handleUpgradesUpdate()
    }
    
    private fun handleUpgradesUpdate() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        maxCollectionTime = (COLLECTION_TIME / upgradeHolder.getSpeedModifier()).toInt()
    }
    
    override fun handleTick() {
        if (world.time in 13_000..23_000 || timeSpentCollecting != -1) handleNightTick()
        else handleDayTick()
    }
    
    private fun handleNightTick() {
        if (timeSpentCollecting != -1) {
            if (energyHolder.energy >= energyHolder.specialEnergyConsumption) {
                energyHolder.energy -= energyHolder.specialEnergyConsumption
                handleCollectionTick()
            }
        } else if (energyHolder.energy >= energyHolder.energyConsumption) {
            energyHolder.energy -= energyHolder.energyConsumption
            handleIdleTick()
        }
    }
    
    private fun handleCollectionTick() {
        timeSpentCollecting++
        if (timeSpentCollecting >= maxCollectionTime) {
            timeSpentIdle = 0
            timeSpentCollecting = -1
            
            val item = NovaMaterialRegistry.STAR_DUST.createItemStack()
            if (inventory.addItem(SELF_UPDATE_REASON, item) != 0) location.dropItem(item)
            
            particleTask.stop()
            rod.setEquipment(EquipmentSlot.HEAD, material.block!!.createItemStack(1))
            rod.updateEquipment()
        } else {
            val percentageCollected = (maxCollectionTime - timeSpentCollecting) / maxCollectionTime.toDouble()
            val particleDistance = percentageCollected * (STAR_PARTICLE_DISTANCE_PER_TICK * maxCollectionTime)
            val particleLocation = rodLocation.clone().add(particleVector.clone().multiply(particleDistance))
            
            particleBuilder(ParticleEffect.REDSTONE) {
                location(particleLocation)
                color(Color(255, 255, 255))
            }.display(getViewers())
        }
        
        if (gui.isInitialized()) 
            gui.value.collectionBar.percentage = timeSpentCollecting / maxCollectionTime.toDouble()
    }
    
    private fun handleIdleTick() {
        timeSpentIdle++
        if (timeSpentIdle >= maxIdleTime) {
            timeSpentCollecting = 0
            
            particleTask.start()
            
            rod.setEquipment(EquipmentSlot.HEAD, material.block!!.createItemStack(2))
            rod.updateEquipment()
            
            rodLocation.yaw = rod.location.yaw
            particleVector = Vector(rod.location.yaw, -65F)
        } else rod.teleport { this.yaw += 2F }
        
        if (gui.isInitialized()) 
            gui.value.idleBar.percentage = timeSpentIdle / maxIdleTime.toDouble()
    }
    
    private fun handleDayTick() {
        val player = Bukkit.getOnlinePlayers()
            .asSequence()
            .filter { it.location.world == world }
            .minByOrNull { it.location.distanceSquared(rodLocation) }
        
        if (player != null) {
            val distance = rodLocation.distance(player.location)
            
            if (distance <= 5) {
                val vector = player.location.subtract(rodLocation).toVector()
                val yaw = vector.calculateYaw()
                
                rod.teleport { this.yaw = yaw }
            }
        }
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && !event.isRemove
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        rod.remove()
    }
    
    inner class StarCollectorGUI : TileEntityGUI("menu.nova.star_collector") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@StarCollector,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(itemHolder.getNetworkedInventory(inventory) to "inventory.nova.output"),
            ::openWindow
        )
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s # # # . . . |" +
                "| u # i # . . . |" +
                "| # # # # . . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .addIngredient('i', inventory)
            .build()
        
        val collectionBar = object : VerticalBar(gui, x = 5, y = 1, height = 3) {
            
            override val barMaterial = NovaMaterialRegistry.GREEN_BAR
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
                if (timeSpentCollecting != -1)
                    itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.star_collector.collection"))
                return itemBuilder
            }
            
        }
        
        val idleBar = object : VerticalBar(gui, x = 6, y = 1, height = 3) {
    
            override val barMaterial = NovaMaterialRegistry.GREEN_BAR
            
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(localized(ChatColor.GRAY, "menu.nova.star_collector.idle"))
            
        }
        
        init {
            EnergyBar(gui, 7, 1, 3, energyHolder)
        }
        
    }
    
}