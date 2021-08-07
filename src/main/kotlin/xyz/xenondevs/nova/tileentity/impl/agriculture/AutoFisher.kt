package xyz.xenondevs.nova.tileentity.impl.agriculture

import de.studiocode.invui.gui.GUI
import de.studiocode.invui.gui.SlotElement.VISlotElement
import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import net.md_5.bungee.api.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
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
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.item.NovaItemBuilder
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.tileentity.EnergyItemTileEntity
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.VerticalBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.UpgradesTeaserItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.salt
import xyz.xenondevs.nova.util.serverLevel
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("auto_fisher.capacity")!!
private val ENERGY_PER_TICK = NovaConfig.getInt("auto_fisher.energy_per_tick")!!
private val IDLE_TIME = NovaConfig.getInt("auto_fisher.idle_time")!!

class AutoFisher(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : EnergyItemTileEntity(uuid, data, material, ownerUUID, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME, BlockSide.BOTTOM) }
    override val gui by lazy(::AutoFisherGUI)
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private val inventory = getInventory("inventory", 12, true, ::handleInventoryUpdate)
    private val fishingRodInventory = getInventory("fishingRod", 1, true, ::handleFishingRodInventoryUpdate)
    private var idleTime = 0
    
    private val waterBlock = location.clone().subtract(0.0, 1.0, 0.0).block
    private val random = Random(uuid.mostSignificantBits xor System.currentTimeMillis())
    private val level = world.serverLevel
    private val position = Vec3(location.x, location.y, location.z)
    private val itemDropLocation = location.clone().add(0.0, 1.0, 0.0)
    private lateinit var fakePlayer: ServerPlayer
    
    init {
        setDefaultInventory(inventory)
        addAvailableInventories(fishingRodInventory)
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        
        fakePlayer = EntityUtils.createFakePlayer(location, ownerUUID.salt(uuid.toString()), "AutoFisher")
    }
    
    override fun handleTick() {
        if (energy >= ENERGY_PER_TICK && !fishingRodInventory.isEmpty && waterBlock.type == Material.WATER) {
            energy -= ENERGY_PER_TICK
            
            idleTime++
            
            if (idleTime == IDLE_TIME) {
                idleTime = 0
                fish()
            }
            
            gui.idleBar.percentage = idleTime.toDouble() / IDLE_TIME.toDouble()
        }
        
        if (hasEnergyChanged) {
            hasEnergyChanged = false
            gui.energyBar.update()
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
                Triple(getNetworkedInventory(inventory), "inventory.nova.default", ItemConnectionType.EXTRACT_TYPES),
                Triple(getNetworkedInventory(fishingRodInventory), "inventory.nova.fishing_rod", ItemConnectionType.ALL_TYPES)
            )
        ) { openWindow(it) }
        
        override val gui: GUI = GUIBuilder(GUIType.NORMAL, 9, 5)
            .setStructure("" +
                "1 - - - - - - - 2" +
                "| s u # # f . . |" +
                "| . . . . # . . |" +
                "| . . . . # . . |" +
                "3 - - - - - - - 4")
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('f', VISlotElement(fishingRodInventory, 0, NovaMaterial.FISHING_ROD_PLACEHOLDER.createBasicItemBuilder()))
            .addIngredient('u', UpgradesTeaserItem)
            .build()
            .apply { fillRectangle(1, 2, 6, inventory, true) }
        
        val energyBar = EnergyBar(gui = gui, x = 7, y = 1, height = 3) { Triple(energy, MAX_ENERGY, -ENERGY_PER_TICK) }
        
        val idleBar = object : VerticalBar(gui = gui, x = 6, y = 1, height = 3, NovaMaterial.GREEN_BAR) {
            override fun modifyItemBuilder(itemBuilder: NovaItemBuilder) =
                itemBuilder.setLocalizedName(TranslatableComponent("menu.nova.auto_fisher.idle", IDLE_TIME - idleTime))
        }
        
    }
    
}