package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.guitype.GUIType
import de.studiocode.invui.item.ItemBuilder
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_17_R1.CraftServer
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack
import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.material.NovaMaterialRegistry.AUTO_FISHER
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.tileentity.upgrade.Upgradable
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.OpenUpgradesItem
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*

private val MAX_ENERGY =  NovaConfig[AUTO_FISHER].getInt("capacity")!!
private val ENERGY_PER_TICK =  NovaConfig[AUTO_FISHER].getInt("energy_per_tick")!!
private val IDLE_TIME = NovaConfig[AUTO_FISHER].getInt("idle_time")!!

class AutoFisher(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : NetworkedTileEntity(uuid, data, material, ownerUUID, armorStand), Upgradable {
    
    private val inventory = getInventory("inventory", 12, ::handleInventoryUpdate)
    private val fishingRodInventory = getInventory("fishingRod", 1, ::handleFishingRodInventoryUpdate)
    override val gui = lazy(::AutoFisherGUI)
    override val upgradeHolder = UpgradeHolder(this, gui, ::handleUpgradeUpdates, allowed = UpgradeType.ALL_ENERGY)
    override val energyHolder = ConsumerEnergyHolder(this, MAX_ENERGY, ENERGY_PER_TICK, 0, upgradeHolder) { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.BOTTOM) }
    override val itemHolder = NovaItemHolder(this, inventory to ItemConnectionType.EXTRACT, fishingRodInventory to ItemConnectionType.BUFFER)
    
    private var timePassed = 0
    private var maxIdleTime = 0
    
    private val waterBlock = location.clone().subtract(0.0, 1.0, 0.0).block
    private val random = Random(uuid.mostSignificantBits xor System.currentTimeMillis())
    private val level = world.serverLevel
    private val position = Vec3(armorStand.location.x, location.y - 0.5, armorStand.location.z)
    private val itemDropLocation = location.clone().add(0.0, 1.0, 0.0)
    private val fakePlayer = EntityUtils.createFakePlayer(location, ownerUUID.salt(uuid.toString()), "AutoFisher")
    
    init {
        handleUpgradeUpdates()
    }
    
    private fun handleUpgradeUpdates() {
        maxIdleTime = (IDLE_TIME / upgradeHolder.getSpeedModifier()).toInt()
        if (timePassed > maxIdleTime) timePassed = maxIdleTime
    }
    
    override fun handleTick() {
        if (energyHolder.energy >= energyHolder.energyConsumption && !fishingRodInventory.isEmpty && waterBlock.type == Material.WATER) {
            energyHolder.energy -= energyHolder.energyConsumption
            
            timePassed++
            
            if (timePassed >= maxIdleTime) {
                timePassed = 0
                fish()
            }
            
            if (gui.isInitialized()) gui.value.idleBar.percentage = timePassed.toDouble() / maxIdleTime.toDouble()
        }
    }
    
    private fun fish() {
        // Bukkit's LootTable API isn't applicable in this use case
        
        val rodItem = fishingRodInventory.getItemStack(0)
        val luck = rodItem.enchantments[Enchantment.LUCK] ?: 0
        
        // the fake fishing hook is required for the "in_open_water" check as the
        // fishing location affects the loot table
        val fakeFishingHook = FishingHook(fakePlayer, level, luck, 0)
        
        val contextBuilder = LootContext.Builder(level)
            .withParameter(LootContextParams.ORIGIN, position)
            .withParameter(LootContextParams.TOOL, CraftItemStack.asNMSCopy(rodItem))
            .withParameter(LootContextParams.THIS_ENTITY, fakeFishingHook)
            .withRandom(random)
            .withLuck(luck.toFloat())
        
        val server = (Bukkit.getServer() as CraftServer).server
        val lootTable: LootTable = server.lootTables.get(BuiltInLootTables.FISHING)
        
        val list = lootTable.getRandomItems(contextBuilder.create(LootContextParamSets.FISHING))
        
        list.stream()
            .map { CraftItemStack.asCraftMirror(it) }
            .forEach {
                val leftover = inventory.addItem(SELF_UPDATE_REASON, it)
                if (leftover != 0) {
                    it.amount = leftover
                    world.dropItemNaturally(itemDropLocation, it)
                }
            }
        
        // damage the rod item
        useRod()
    }
    
    private fun useRod() {
        val itemStack = fishingRodInventory.getItemStack(0)!!
        fishingRodInventory.setItemStack(SELF_UPDATE_REASON, 0, ToolUtils.damageTool(itemStack))
    }
    
    private fun handleInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.updateReason != SELF_UPDATE_REASON && event.isAdd
    }
    
    private fun handleFishingRodInventoryUpdate(event: ItemUpdateEvent) {
        event.isCancelled = event.isAdd && event.newItemStack.type != Material.FISHING_ROD
    }
    
    inner class AutoFisherGUI : TileEntityGUI("menu.nova.auto_fisher") {
        
        private val sideConfigGUI = SideConfigGUI(
            this@AutoFisher,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            listOf(
                itemHolder.getNetworkedInventory(inventory) to "inventory.nova.default",
                itemHolder.getNetworkedInventory(fishingRodInventory) to "inventory.nova.fishing_rod"
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # f . . |" +
                "| i i i i # . . |" +
                "| i i i i # . . |" +
                "3 - - - - - - - 4")
            .addIngredient('i', inventory)
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', VISlotElement(fishingRodInventory, 0, NovaMaterialRegistry.FISHING_ROD_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('u', OpenUpgradesItem(upgradeHolder))
            .build()
        
        val energyBar = EnergyBar(gui = gui, x = 7, y = 1, height = 3, energyHolder)
        
        val idleBar = object : VerticalBar(gui = gui, x = 6, y = 1, height = 3, NovaMaterialRegistry.GREEN_BAR) {
            override fun modifyItemBuilder(itemBuilder: ItemBuilder) =
                itemBuilder.setDisplayName(TranslatableComponent("menu.nova.auto_fisher.idle", maxIdleTime - timePassed))
        }
        
    }
    
}