package xyz.xenondevs.nova.initialize

import de.studiocode.invui.InvUI
import de.studiocode.invui.util.InventoryUtils
import de.studiocode.invui.virtualinventory.StackSizeProvider
import org.bstats.bukkit.Metrics
import org.bstats.charts.DrilldownPie
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import xyz.xenondevs.nmsutils.NMSUtilities
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.UpdateReminder
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.serialization.cbf.CBFAdapters
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.legacy.LegacyFileConverter
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.item.ItemManager
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.player.PlayerFreezer
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.player.attachment.AttachmentManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipListener
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.transformer.Patcher
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.item.novaMaxStackSize
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorManager
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.loot.LootConfigHandler
import xyz.xenondevs.nova.world.loot.LootGeneration
import xyz.xenondevs.particle.utils.ReflectionUtils
import java.util.*
import java.util.logging.Level
import kotlin.reflect.jvm.jvmName

internal object Initializer : Listener {
    
    private val INITIALIZABLES = listOf(
        LegacyFileConverter, UpdateReminder, AddonsInitializer, NovaConfig, AutoUploadManager, Resources,
        CustomItemServiceManager, PacketItems, LocaleManager, ChunkReloadWatcher, FakeEntityManager,
        RecipeManager, RecipeRegistry, ChunkLoadManager, VanillaTileEntityManager,
        NetworkManager, ItemManager, AttachmentManager, CommandManager, ArmorEquipListener,
        AbilityManager, LootConfigHandler, LootGeneration, AddonsLoader, ItemCategories,
        BlockManager, WorldDataManager, TileEntityManager, BlockBehaviorManager, Patcher, PlayerFreezer, BossBarOverlayManager, WailaManager
    ).sorted()
    
    val initialized: MutableList<Initializable> = Collections.synchronizedList(ArrayList())
    var isDone = false
        private set
    
    fun init() {
        registerEvents()
        
        ReflectionUtils.setPlugin(NOVA)
        NMSUtilities.init(NOVA)
        InvUI.getInstance().plugin = NOVA
        
        NovaConfig.loadDefaultConfig()
        CBFAdapters.registerExtraAdapters()
        InventoryUtils.stackSizeProvider = StackSizeProvider { it.novaMaxStackSize }
        CoreItems.init()
        
        runAsyncTask {
            INITIALIZABLES.forEach { initializable ->
                runAsyncTask initializableTask@{
                    initializable.dependsOn.forEach { dependency ->
                        // wait for all dependencies to load and skip own initialization if one of them failed
                        if (!dependency.initialization.get()) {
                            LOGGER.warning("Skipping initialization: ${initializable::class.jvmName}")
                            initializable.initialization.complete(false)
                            return@initializableTask
                        }
                    }
                    
                    initializable.initialize()
                }
            }
            
            INITIALIZABLES.forEach { it.initialization.get() }
            isDone = true
            
            if (initialized.size == INITIALIZABLES.size) {
                callEvent(NovaLoadDataEvent())
                
                runTask {
                    PermanentStorage.store("last_version", NOVA.description.version)
                    setGlobalIngredients()
                    AddonManager.enableAddons()
                    setupMetrics()
                    LOGGER.info("Done loading")
                }
            } else {
                Bukkit.getPluginManager().disablePlugin(NOVA.loader)
            }
        }
    }
    
    fun disable() {
        initialized.sortedDescending().forEach {
            try {
                it.disable()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to disable $it", e)
            }
        }
        
        NMSUtilities.disable()
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleLogin(event: PlayerLoginEvent) {
        if (!isDone && !IS_DEV_SERVER) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "[Nova] Initialization not complete. Please wait.")
        }
    }
    
    private fun setupMetrics() {
        val metrics = Metrics(NOVA, 11927)
        metrics.addCustomChart(DrilldownPie("addons") {
            val map = HashMap<String, Map<String, Int>>()
            
            AddonManager.addons.values.forEach {
                map[it.description.name] = mapOf(it.description.version to 1)
            }
            
            return@DrilldownPie map
        })
    }
    
}