@file:Suppress("UnstableApiUsage")

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
import org.bukkit.event.server.ServerLoadEvent
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
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.serialization.cbf.CBFAdapters
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.legacy.LegacyFileConverter
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.integration.utp.UTPIntegration
import xyz.xenondevs.nova.item.ItemListener
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
import xyz.xenondevs.nova.util.CollectionUtils
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
import kotlin.system.exitProcess

internal object Initializer : Listener {
    
    private val INITIALIZABLES = CollectionUtils.sortDependencies(listOf(
        LegacyFileConverter, UpdateReminder, AddonsInitializer, NovaConfig, AutoUploadManager,
        CustomItemServiceManager, PacketItems, LocaleManager, ChunkReloadWatcher, FakeEntityManager,
        RecipeManager, RecipeRegistry, ChunkLoadManager, VanillaTileEntityManager,
        NetworkManager, ItemListener, AttachmentManager, CommandManager, ArmorEquipListener,
        AbilityManager, LootConfigHandler, LootGeneration, AddonsLoader, ItemCategories,
        BlockManager, WorldDataManager, TileEntityManager, BlockBehaviorManager, Patcher, PlayerFreezer,
        BossBarOverlayManager, WailaManager, ResourceGeneration.PreWorld, ResourceGeneration.PostWorld, UTPIntegration
    ), Initializable::dependsOn)
    
    val initialized: MutableList<Initializable> = Collections.synchronizedList(ArrayList())
    var isDone = false
        private set
    
    fun initPreWorld() {
        registerEvents()
        
        ReflectionUtils.setPlugin(NOVA)
        NMSUtilities.init(NOVA)
        InvUI.getInstance().plugin = NOVA
        
        CBFAdapters.register()
        InventoryUtils.stackSizeProvider = StackSizeProvider { it.novaMaxStackSize }
        CoreItems.init()
        
        val toInit = INITIALIZABLES.filter { it.initializationStage == InitializationStage.PRE_WORLD }
        
        toInit.forEach { initializable ->
            if (!waitForDependencies(initializable))
                return@forEach
            
            initializable.initialize()
        }
        
        toInit.forEach { it.initialization.get() }
        
        isDone = true
        
        if (initialized.size != toInit.size)
            performAppropriateShutdown()
    }
    
    fun initPostWorld() {
        runAsyncTask {
            val toInit = INITIALIZABLES.filter { it.initializationStage != InitializationStage.PRE_WORLD }
            
            toInit.forEach { initializable ->
                runAsyncTask initializableTask@{
                    if (!waitForDependencies(initializable))
                        return@initializableTask
                    
                    if (initializable.initializationStage == InitializationStage.POST_WORLD) {
                        runTask { initializable.initialize() }
                    } else {
                        runAsyncTask { initializable.initialize() }
                    }
                    
                }
            }
            
            toInit.forEach { it.initialization.get() }
            
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
                performAppropriateShutdown()
            }
        }
    }
    
    private fun waitForDependencies(initializable: Initializable): Boolean {
        val dependencies = initializable.dependsOn
        if (initializable.initializationStage == InitializationStage.PRE_WORLD && dependencies.any { it.initializationStage != InitializationStage.PRE_WORLD })
            throw IllegalStateException("Initializable ${initializable::class.jvmName} has incompatible dependencies!")
        
        dependencies.forEach { dependency ->
            // wait for all dependencies to load and skip own initialization if one of them failed
            if (!dependency.initialization.get()) {
                LOGGER.warning("Skipping initialization: ${initializable::class.jvmName}")
                initializable.initialization.complete(false)
                return false
            }
        }
        return true
    }
    
    fun disable() {
        CollectionUtils.sortDependencies(initialized, Initializable::dependsOn).reversed().forEach {
            try {
                it.disable()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to disable $it", e)
            }
        }
        
        NMSUtilities.disable()
    }
    
    private fun performAppropriateShutdown() {
        if (Patcher.ENABLED) {
            LOGGER.warning("Shutting down the server...")
            exitProcess(-1)
        } else {
            Bukkit.getPluginManager().disablePlugin(NOVA.loader)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleLogin(event: PlayerLoginEvent) {
        if (!isDone && !IS_DEV_SERVER) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "[Nova] Initialization not complete. Please wait.")
        }
    }
    
    @EventHandler
    private fun handleServerStarted(event: ServerLoadEvent) {
        initPostWorld()
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