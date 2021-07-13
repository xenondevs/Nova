package xyz.xenondevs.nova.advancement

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
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.advancement.agriculture.FertilizerAdvancement
import xyz.xenondevs.nova.advancement.agriculture.HarvesterAdvancement
import xyz.xenondevs.nova.advancement.cable.AdvancedCableAdvancement
import xyz.xenondevs.nova.advancement.cable.BasicCableAdvancement
import xyz.xenondevs.nova.advancement.cable.EliteCableAdvancement
import xyz.xenondevs.nova.advancement.cable.UltimateCableAdvancement
import xyz.xenondevs.nova.advancement.agriculture.PlanterAdvancement
import xyz.xenondevs.nova.advancement.mob.BottledMobAdvancement
import xyz.xenondevs.nova.advancement.mob.BreederAdvancement
import xyz.xenondevs.nova.advancement.mob.MobDuplicatorAdvancement
import xyz.xenondevs.nova.advancement.mob.MobKillerAdvancement
import xyz.xenondevs.nova.advancement.powercell.AdvancedPowerCellAdvancement
import xyz.xenondevs.nova.advancement.powercell.BasicPowerCellAdvancement
import xyz.xenondevs.nova.advancement.powercell.ElitePowerCellAdvancement
import xyz.xenondevs.nova.advancement.powercell.UltimatePowerCellAdvancement
import xyz.xenondevs.nova.advancement.press.*
import xyz.xenondevs.nova.advancement.pulverizer.AllDustsAdvancement
import xyz.xenondevs.nova.advancement.pulverizer.DustAdvancement
import xyz.xenondevs.nova.advancement.pulverizer.PulverizerAdvancement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.awardAdvancement
import net.roxeez.advancement.AdvancementManager as RoxeezAdvancementManager

fun NovaMaterial.toIcon(): Icon {
    val itemStack = createBasicItemBuilder().build()
    val material = itemStack.type
    val customModelData = itemStack.itemMeta?.customModelData ?: 0
    return Icon(material, "{CustomModelData:$customModelData}")
}

fun Advancement.addObtainCriteria(novaMaterial: NovaMaterial): Criteria {
    val itemStack = novaMaterial.createBasicItemBuilder().build()
    val material = itemStack.type
    val customModelData = itemStack.itemMeta?.customModelData ?: 0
    
    return addCriteria("obtain_${novaMaterial.name.lowercase()}", TriggerType.INVENTORY_CHANGED) {
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
        registerAll(
            RootAdvancement,
            BasicCableAdvancement, AdvancedCableAdvancement, EliteCableAdvancement, UltimateCableAdvancement,
            ItemFilterAdvancement, VacuumChestAdvancement, StorageUnitAdvancement, ChargerAdvancement, JetpackAdvancement,
            FurnaceGeneratorAdvancement, SolarPanelAdvancement,
            BasicPowerCellAdvancement, AdvancedPowerCellAdvancement, ElitePowerCellAdvancement, UltimatePowerCellAdvancement,
            MechanicalPressAdvancement, GearsAdvancement, PlatesAdvancement, AllPlatesAdvancement, AllGearsAdvancement,
            PulverizerAdvancement, DustAdvancement, AllDustsAdvancement,
            BlockPlacerAdvancement, BlockBreakerAdvancement, QuarryAdvancement,
            BottledMobAdvancement, BreederAdvancement, MobKillerAdvancement, MobDuplicatorAdvancement,
            PlanterAdvancement, HarvesterAdvancement, FertilizerAdvancement
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