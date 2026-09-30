package xyz.xenondevs.nova.integration

import org.bukkit.Bukkit
import org.objectweb.asm.Type
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
import xyz.xenondevs.nova.annotations.AnnotationValue
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.integration.permission.PermissionIntegration
import xyz.xenondevs.nova.integration.permission.PermissionManager
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.data.JarUtils
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC
)
internal object HooksLoader {
    
    @InitFun
    private fun loadHooks() {
        val hook = Type.getDescriptor(Hook::class.java)
        for ([className, metadata] in JarUtils.getAnnotationIndex(NOVA_JAR).classes) {
            val annotation = metadata.annotations.firstOrNull { it.descriptor == hook }
                ?: continue
            val arguments = annotation.arguments
            try {
                val plugins = (arguments["plugins"] as? AnnotationValue.ArrayValue)?.values?.map { (it as AnnotationValue.String).value } ?: emptyList()
                val unless = (arguments["unless"] as? AnnotationValue.ArrayValue)?.values?.map { (it as AnnotationValue.String).value } ?: emptyList()
                val requireAll = (arguments["requireAll"] as? AnnotationValue.Boolean)?.value ?: false
                val loadListener = (arguments["loadListener"] as? AnnotationValue.ClassLiteral)?.descriptor
                
                if (plugins.isEmpty())
                    throw IllegalStateException("Hook annotation on $className does not specify any plugins")
                
                if (shouldLoadHook(plugins, unless, requireAll))
                    loadHook(className.replace('/', '.'), loadListener)
            } catch (t: Throwable) {
                LOGGER.error("Failed to load hook $className", t)
            }
        }
    }
    
    private fun shouldLoadHook(plugins: List<String>, unless: List<String>, requireAll: Boolean): Boolean {
        if (plugins.isEmpty())
            throw IllegalStateException("No plugins specified")
        
        val pluginManager = Bukkit.getPluginManager()
        
        return if (requireAll) {
            plugins.all { pluginManager.getPlugin(it) != null } && unless.none { pluginManager.getPlugin(it) != null }
        } else {
            plugins.any { pluginManager.getPlugin(it) != null } && unless.none { pluginManager.getPlugin(it) != null }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun loadHook(className: String, loadListener: String?) {
        val loaded: CompletableFuture<Boolean>
        if (loadListener != null) {
            val obj = (Class.forName(Type.getType(loadListener).className).kotlin as KClass<out LoadListener>).objectInstance
                ?: throw IllegalStateException("LoadListener $loadListener is not an object")
            
            loaded = obj.loaded
        } else {
            loaded = CompletableFuture.completedFuture(true)
        }
        
        loaded.thenAccept {
            val hookClass = Class.forName(className).kotlin
            val hookInstance = hookClass.objectInstance
                ?: throw IllegalStateException("Hook $hookClass is not an object")
            
            useHook(hookInstance)
        }
    }
    
    private fun useHook(hook: Any) {
        if (hook is PermissionIntegration) {
            PermissionManager.integrations += hook
        }
        
        if (hook is ProtectionIntegration) {
            ProtectionManager.integrations += hook
        }
        
        if (hook is CustomItemService) {
            CustomItemServiceManager.services += hook
        }
    }
    
}