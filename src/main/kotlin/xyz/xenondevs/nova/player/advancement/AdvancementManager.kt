package xyz.xenondevs.nova.player.advancement

import net.md_5.bungee.api.chat.TranslatableComponent
import net.roxeez.advancement.Advancement
import net.roxeez.advancement.Criteria
import net.roxeez.advancement.display.Display
import net.roxeez.advancement.display.Icon
import net.roxeez.advancement.trigger.TriggerType
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.IS_VERSION_CHANGE
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.advancement.agriculture.*
import xyz.xenondevs.nova.player.advancement.cable.AdvancedCableAdvancement
import xyz.xenondevs.nova.player.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.player.advancement.cable.EliteCableAdvancement
import xyz.xenondevs.nova.player.advancement.cable.UltimateCableAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.CobblestoneGeneratorAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.InfiniteWaterSourceAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.LavaGeneratorAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.PumpAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.tank.AdvancedFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.tank.BasicFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.tank.EliteFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.fluid.tank.UltimateFluidTankAdvancement
import xyz.xenondevs.nova.player.advancement.mob.BreederAdvancement
import xyz.xenondevs.nova.player.advancement.mob.MobCatcherAdvancement
import xyz.xenondevs.nova.player.advancement.mob.MobDuplicatorAdvancement
import xyz.xenondevs.nova.player.advancement.mob.MobKillerAdvancement
import xyz.xenondevs.nova.player.advancement.power.FurnaceGeneratorAdvancement
import xyz.xenondevs.nova.player.advancement.power.LightningExchangerAdvancement
import xyz.xenondevs.nova.player.advancement.power.SolarPanelAdvancement
import xyz.xenondevs.nova.player.advancement.power.WindTurbineAdvancement
import xyz.xenondevs.nova.player.advancement.powercell.AdvancedPowerCellAdvancement
import xyz.xenondevs.nova.player.advancement.powercell.BasicPowerCellAdvancement
import xyz.xenondevs.nova.player.advancement.powercell.ElitePowerCellAdvancement
import xyz.xenondevs.nova.player.advancement.powercell.UltimatePowerCellAdvancement
import xyz.xenondevs.nova.player.advancement.press.*
import xyz.xenondevs.nova.player.advancement.pulverizer.AllDustsAdvancement
import xyz.xenondevs.nova.player.advancement.pulverizer.DustAdvancement
import xyz.xenondevs.nova.player.advancement.pulverizer.PulverizerAdvancement
import xyz.xenondevs.nova.player.advancement.stardust.StarCollectorAdvancement
import xyz.xenondevs.nova.player.advancement.stardust.StarShardsAdvancement
import xyz.xenondevs.nova.util.awardAdvancement
import net.roxeez.advancement.AdvancementManager as RoxeezAdvancementManager

fun NovaMaterial.toIcon(): Icon {
    val itemStack = createItemStack()
    val material = itemStack.type
    val customModelData = itemStack.itemMeta?.customModelData ?: 0
    return Icon(material, "{CustomModelData:$customModelData}")
}

fun Advancement.addObtainCriteria(novaMaterial: NovaMaterial): Criteria {
    val itemStack = novaMaterial.createItemStack()
    val material = itemStack.type
    val customModelData = itemStack.itemMeta?.customModelData ?: 0
    
    return addCriteria("obtain_${novaMaterial.typeName.lowercase()}", TriggerType.INVENTORY_CHANGED) {
        it.hasItemMatching { data ->
            data.setType(material)
            data.setNbt("{CustomModelData:$customModelData}")
        }
    }
}

fun Display.setTitleLocalized(translate: String, vararg with: Any) {
    setTitle(TranslatableComponent(translate, *with))
}

fun Display.setDescriptionLocalized(translate: String, vararg with: Any) {
    setDescription(TranslatableComponent(translate, *with))
}

fun Advancement.setDisplayLocalized(run: (Display) -> Unit) {
    setDisplay {
        it.setTitleLocalized("advancement.nova.${key.key}.title")
        it.setDescriptionLocalized("advancement.nova.${key.key}.description")
        run(it)
    }
}

object AdvancementManager : RoxeezAdvancementManager(NOVA), Listener {
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    fun loadAdvancements() {
        LOGGER.info("Loading advancements")
        registerAll(
            RootAdvancement,
            BasicCableAdvancement, AdvancedCableAdvancement, EliteCableAdvancement, UltimateCableAdvancement,
            ItemFilterAdvancement, VacuumChestAdvancement, StorageUnitAdvancement,
            TrashCanAdvancement, ChargerAdvancement, WirelessChargerAdvancement, JetpackAdvancement,
            FurnaceGeneratorAdvancement, SolarPanelAdvancement, WindTurbineAdvancement, LightningExchangerAdvancement,
            BasicPowerCellAdvancement, AdvancedPowerCellAdvancement, ElitePowerCellAdvancement, UltimatePowerCellAdvancement,
            MechanicalPressAdvancement, GearsAdvancement, PlatesAdvancement, AllPlatesAdvancement, AllGearsAdvancement,
            PulverizerAdvancement, DustAdvancement, AllDustsAdvancement,
            BlockPlacerAdvancement, BlockBreakerAdvancement, QuarryAdvancement,
            MobCatcherAdvancement, BreederAdvancement, MobKillerAdvancement, MobDuplicatorAdvancement,
            PlanterAdvancement, FertilizerAdvancement, HarvesterAdvancement, TreeFactoryAdvancement,
            AutoFisherAdvancement,
            StarShardsAdvancement, StarCollectorAdvancement,
            BasicFluidTankAdvancement, AdvancedFluidTankAdvancement, EliteFluidTankAdvancement, UltimateFluidTankAdvancement,
            InfiniteWaterSourceAdvancement, PumpAdvancement, CobblestoneGeneratorAdvancement, LavaGeneratorAdvancement
        )
        
        createAll(IS_VERSION_CHANGE)
    }
    
    private fun registerAll(vararg advancements: Advancement) {
        advancements.forEach { register(it) }
    }
    
    @EventHandler
    fun handlePlayerJoin(event: PlayerJoinEvent) {
        event.player.awardAdvancement(RootAdvancement.key)
    }
    
}