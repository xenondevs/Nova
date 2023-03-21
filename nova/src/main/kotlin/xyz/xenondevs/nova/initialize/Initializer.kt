package xyz.xenondevs.nova.initialize

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import org.bstats.bukkit.Metrics
import org.bstats.charts.DrilldownPie
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.inventory.ItemStack
import org.objectweb.asm.Type
import xyz.xenondevs.commons.collections.CollectionUtils
import xyz.xenondevs.commons.reflection.hasEmptyArguments
import xyz.xenondevs.invui.InvUI
import xyz.xenondevs.invui.util.InventoryUtils
import xyz.xenondevs.invui.virtualinventory.StackSizeProvider
import xyz.xenondevs.nmsutils.NMSUtilities
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.cbf.CBFAdapters
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.data.JarUtils
import xyz.xenondevs.nova.util.equalsAny
import xyz.xenondevs.nova.util.item.novaMaxStackSize
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmName
import xyz.xenondevs.inventoryaccess.component.i18n.Languages as InvUILanguages

internal object Initializer : Listener {
    
    private val toInit = ObjectArrayList<InitializableClass>()
    
    val initialized: MutableList<InitializableClass> = ObjectLists.synchronize(ObjectArrayList())
    var isDone = false
        private set
    
    private var failedPreWorld = false
    
    // Map structure: https://i.imgur.com/VHLkAtM.png (stage instead of initializationStage)
    @Suppress("UNCHECKED_CAST")
    fun searchClasses() {
        var classes: List<InitializableClass> = JarUtils.searchForAnnotatedClasses(NOVA.pluginFile, InternalInit::class).asSequence()
            .map { (clazz, annotation) ->
                val stageName = (annotation["stage"] as Array<String>?)?.get(1)
                    ?: throw IllegalStateException("InternalInit annotation on $clazz does not contain a stage!")
                val stage = enumValueOf<InitializationStage>(stageName)
                val dependsOn = (annotation["dependsOn"] as List<Type>?)?.mapTo(ObjectOpenHashSet()) { it.internalName } ?: ObjectSets.emptySet()
                
                println("Found class $clazz with stage $stage and dependencies $dependsOn")
                return@map InitializableClass(clazz, stage, dependsOn)
            }.toMutableList()
        
        classes = CollectionUtils.sortDependenciesMapped(classes, InitializableClass::dependsOn, InitializableClass::className)
        toInit.addAll(classes)
    }
    
    fun initPreWorld() {
        registerEvents()
        
        NMSUtilities.init(NOVA)
        InvUI.getInstance().plugin = NOVA
        InvUILanguages.getInstance().enableServerSideTranslations(false)
        
        CBFAdapters.register()
        InventoryUtils.stackSizeProvider = StackSizeProvider(ItemStack::novaMaxStackSize)
        CoreItems.init()
        
        val preWorldInit = toInit.filter { it.stage == InitializationStage.PRE_WORLD }
        val lookup = toInit.associateBy(InitializableClass::className)
        
        preWorldInit.forEach { initializable ->
            if (!waitForDependencies(initializable, lookup))
                return@forEach
            
            initializable.initialize()
        }
        
        preWorldInit.forEach { it.initialization.get() }
        
        if (initialized.size != preWorldInit.size) {
            failedPreWorld = true
            performAppropriateShutdown()
        }
    }
    
    private fun initPostWorld() {
        runAsyncTask {
            val postWorldInit = toInit.filter { it.stage != InitializationStage.PRE_WORLD }
            val lookup = toInit.associateBy(InitializableClass::className)
            
            postWorldInit.forEach { initializable ->
                runAsyncTask initializableTask@{
                    if (!waitForDependencies(initializable, lookup))
                        return@initializableTask
                    
                    if (initializable.stage == InitializationStage.POST_WORLD) {
                        runTask { initializable.initialize() }
                    } else {
                        runAsyncTask { initializable.initialize() }
                    }
                    
                }
            }
            
            postWorldInit.forEach { it.initialization.get() }
            
            if (initialized.size == toInit.size) {
                isDone = true
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
    
    fun disable() {
        CollectionUtils.sortDependenciesMapped(initialized, InitializableClass::dependsOn, InitializableClass::className).reversed().forEach {
            try {
                it.disable()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to disable $it", e)
            }
        }
        
        NMSUtilities.disable()
    }
    
    private fun waitForDependencies(initializable: InitializableClass, lookup: Map<String, InitializableClass>): Boolean {
        val dependencies = initializable.dependsOn.map { lookup[it] ?: throw IllegalStateException("Dependency $it of $initializable not found (Missing @InternalInit annotation?)") }
        if (initializable.stage == InitializationStage.PRE_WORLD && dependencies.any { it.stage != InitializationStage.PRE_WORLD })
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
    
    private fun performAppropriateShutdown() {
        LOGGER.warning("Shutting down the server...")
        Bukkit.shutdown()
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleLogin(event: PlayerLoginEvent) {
        if (!isDone && !IS_DEV_SERVER) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "[Nova] Initialization not complete. Please wait.")
        }
    }
    
    @EventHandler
    private fun handleServerStarted(event: ServerLoadEvent) {
        if (!failedPreWorld) {
            initPostWorld()
        } else LOGGER.warning("Skipping post world initialization")
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

internal class InitializableClass(
    val className: String,
    val stage: InitializationStage,
    val dependsOn: Set<String>
) {
    
    internal val initialization = CompletableFuture<Boolean>()
    internal var isInitialized = false
    
    private val clazz by lazy { Class.forName(className.replace('/', '.')).kotlin }
    
    internal fun initialize() {
        try {
            val initMethod = clazz.declaredFunctions.firstOrNull { it.name.equalsAny("init", "initialize", "onEnable") && it.hasEmptyArguments() }
            if (initMethod != null) {
                val instance = clazz.objectInstance ?: throw InitializationException("Initializable class $className is not a singleton!")
                initMethod.call(instance)
            }
            isInitialized = true
            Initializer.initialized += this
            initialization.complete(true)
        } catch (e: InitializationException) {
            LOGGER.severe(e.message)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "An exception occurred trying to initialize $this", e)
        }
        initialization.complete(false)
    }
    
    internal fun disable() {
        val disableMethod = clazz.declaredFunctions.firstOrNull { it.name.equalsAny("disable", "onDisable") && it.parameters.isEmpty() }
        if (disableMethod != null) {
            val instance = clazz.objectInstance ?: throw InitializationException("Initializable class $className is not a singleton!")
            disableMethod.call(instance)
        }
    }
    
    override fun toString() = "Initializable | $className"
    
}