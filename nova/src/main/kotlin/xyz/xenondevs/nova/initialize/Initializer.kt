package xyz.xenondevs.nova.initialize

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.papermc.paper.configuration.GlobalConfiguration
import kotlinx.coroutines.debug.DebugProbes
import net.kyori.adventure.text.Component
import org.bstats.bukkit.Metrics
import org.bstats.charts.DrilldownPie
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.collections.poll
import xyz.xenondevs.invui.InvUI
import xyz.xenondevs.invui.inventory.StackSizeProvider
import xyz.xenondevs.invui.util.InventoryUtils
import xyz.xenondevs.nmsutils.NMSUtilities
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.NOVA_PLUGIN
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.cbf.CBFAdapters
import xyz.xenondevs.nova.registry.NovaRegistryAccess
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistryAccess
import xyz.xenondevs.nova.ui.menu.setGlobalIngredients
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.data.JarUtils
import xyz.xenondevs.nova.util.item.novaMaxStackSize
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Level
import xyz.xenondevs.inventoryaccess.component.i18n.Languages as InvUILanguages

internal object Initializer : Listener {
    
    /**
     * The thread pool used for post-world initialization.
     */
    private val execService = Executors.newCachedThreadPool(ThreadFactoryBuilder().setNameFormat("Nova Initializer - %d").build())
    
    /**
     * A list containing all [InitializableClasses][InitializableClass] at all times.
     */
    private val initClasses = ArrayList<InitializableClass>()
    
    /**
     * A list containing all not yet initialized [InitializableClasses][InitializableClass] for pre-world initialization
     * in the order that they should be initialized in.
     */
    private var toInitPreWorld: MutableList<InitializableClass> = ArrayList()
    
    /**
     * A list containing all [InitializableClasses][InitializableClass] for post-world initialization (sync and async)
     * in the order that they should be initialized in.
     *
     * Unlike the pre-world variant, this list is not updated after the initialization of its elements.
     */
    private var toInitPostWorld: List<InitializableClass> = ArrayList()
    
    /**
     * A list of successfully initialized [InitializableClasses][InitializableClass].
     */
    val initialized: MutableList<InitializableClass> = Collections.synchronizedList(ArrayList())
    
    /**
     * Whether the pre-world initialization step has been completed successfully.
     */
    private var preWorldInitialized = false
    
    /**
     * Whether the initialization process has been completed successfully.
     */
    var isDone = false
        private set
    
    /**
     * Stats the initialization process.
     */
    fun start() {
        searchClasses()
        initPreWorld()
    }
    
    /**
     * Searches for classes annotated with [InternalInit] and adds stores them to be initialized.
     */
    private fun searchClasses() {
        val classes = JarUtils.findAnnotatedClasses(NOVA.novaJar, InternalInit::class)
            .map { (clazz, annotation) -> InitializableClass.fromInternalAnnotation(clazz, annotation) }
        addInitClasses(classes)
    }
    
    /**
     * Adds the given [InitializableClasses][InitializableClass] to the [initClasses] list and sorts them.
     *
     * This method can only be invoked during the pre-world initialization phase or before the [start] method is called.
     */
    fun addInitClasses(classes: List<InitializableClass>) {
        check(!preWorldInitialized) { "Cannot add additional init classes after pre-world initialization!" }
        
        initClasses.addAll(classes)
        initClasses.forEach { it.loadDependencies(initClasses) }
        
        fun combineAndSort(a: List<InitializableClass>, b: List<InitializableClass>) =
            ArrayList(CollectionUtils.sortDependencies(a + b, InitializableClass::dependsOn))
        
        toInitPreWorld = combineAndSort(toInitPreWorld, classes.filter { it.stage == InternalInitStage.PRE_WORLD })
        toInitPostWorld = combineAndSort(toInitPostWorld, classes.filter { it.stage != InternalInitStage.PRE_WORLD })
    }
    
    /**
     * Stats the pre-world initialization process.
     */
    private fun initPreWorld() {
        if (IS_DEV_SERVER) {
            DebugProbes.install()
            DebugProbes.enableCreationStackTraces = true
        }
        
        Configs.extractDefaultConfig()
        VanillaRegistryAccess.unfreezeAll()
        registerEvents()
        NMSUtilities.init(NOVA_PLUGIN)
        InvUI.getInstance().plugin = NOVA_PLUGIN
        InvUILanguages.getInstance().enableServerSideTranslations(false)
        CBFAdapters.register()
        InventoryUtils.stackSizeProvider = StackSizeProvider(ItemStack::novaMaxStackSize)
        
        val cfg = GlobalConfiguration.get().blockUpdates
        cfg.disableNoteblockUpdates = true
        cfg.disableMushroomBlockUpdates = true
        
        // pre-world initialization polls from the list because additional elements may be added by addons
        var failed = false
        while (toInitPreWorld.isNotEmpty()) {
            val initClass = toInitPreWorld.poll()!!
            if (!initClass.initialize()) {
                failed = true
                break
            }
        }
        
        if (!failed) {
            NovaRegistryAccess.freezeAll()
            VanillaRegistryAccess.freezeAll()
            preWorldInitialized = true
        } else {
            shutdown()
        }
    }
    
    /**
     * Starts the post-world initialization process.
     */
    private fun initPostWorld() {
        execService.submit {
            toInitPostWorld.forEach { initializable ->
                // each InitializableClass gets its own thread in which it waits for its dependencies to be initialized
                execService.submit initializableThread@{
                    if (!waitForDependencies(initializable))
                        return@initializableThread
                    
                    // dependencies have been initialized, now initialize this class in the preferred thread
                    when (initializable.stage) {
                        // for post-world, jump to the server thread
                        InternalInitStage.POST_WORLD -> runTask { initializable.initialize() }
                        // for async, we can stay in the current thread
                        InternalInitStage.POST_WORLD_ASYNC -> initializable.initialize()
                        
                        // should not happen
                        else -> throw UnsupportedOperationException()
                    }
                }
            }
            
            // block thread until everything is initialized
            var failed = false
            for (initClass in toInitPostWorld) {
                if (!initClass.initialization.get()) {
                    failed = true
                }
            }
            
            if (!failed) {
                isDone = true
                callEvent(NovaLoadDataEvent())
                
                runTask {
                    PermanentStorage.store("last_version", NOVA_PLUGIN.pluginMeta.version)
                    setGlobalIngredients()
                    AddonManager.enableAddons()
                    setupMetrics()
                    LOGGER.info("Done loading")
                }
            } else {
                shutdown()
            }
        }
    }
    
    /**
     * Disables all [InitializableClasses][InitializableClass] in the reverse order that they were initialized in.
     */
    fun disable() {
        CollectionUtils.sortDependencies(initialized, InitializableClass::dependsOn).reversed().forEach {
            try {
                it.disable()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to disable $it", e)
            }
        }
        
        NMSUtilities.disable()
    }
    
    /**
     * Blocks the thread until all dependencies of the given [InitializableClass] have been initialized and
     * returns true if all dependencies were successfully initialized.
     */
    private fun waitForDependencies(initializable: InitializableClass): Boolean {
        initializable.dependsOn.forEach { dependency ->
            // wait for all dependencies to load and skip own initialization if one of them failed
            if (!dependency.initialization.get()) {
                LOGGER.warning("Skipping initialization: $initializable")
                initializable.initialization.complete(false)
                return false
            }
        }
        return true
    }
    
    private fun shutdown() {
        LOGGER.warning("Shutting down the server...")
        Bukkit.shutdown()
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleLogin(event: PlayerLoginEvent) {
        if (!isDone && !IS_DEV_SERVER) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("[Nova] Initialization not complete. Please wait."))
        }
    }
    
    @EventHandler
    private fun handleServerStarted(event: ServerLoadEvent) {
        if (preWorldInitialized) {
            initPostWorld()
        } else LOGGER.warning("Skipping post world initialization")
    }
    
    private fun setupMetrics() {
        val metrics = Metrics(NOVA_PLUGIN, 11927)
        metrics.addCustomChart(DrilldownPie("addons") {
            val map = HashMap<String, Map<String, Int>>()
            
            AddonManager.addons.values.forEach {
                map[it.description.name] = mapOf(it.description.version to 1)
            }
            
            return@DrilldownPie map
        })
    }
    
}