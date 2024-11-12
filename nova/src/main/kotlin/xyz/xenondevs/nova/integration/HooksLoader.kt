@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.integration

import org.bukkit.Bukkit
import org.objectweb.asm.Type
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_JAR
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
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.resources.upload.UploadService
import xyz.xenondevs.nova.util.data.JarUtils
import kotlin.reflect.KClass

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC
)
internal object HooksLoader {
    
    @InitFun
    private fun loadHooks() {
        JarUtils.findAnnotatedClasses(
            NOVA_JAR,
            listOf(Hook::class), emptyList(),
            "xyz/xenondevs/nova/hook/impl/"
        ).classes[Hook::class]?.forEach { (className, annotations) ->
            val annotation = annotations.first()
            try {
                val plugins = annotation["plugins"] as? List<String> ?: emptyList()
                val unless = annotation["unless"] as? List<String> ?: emptyList()
                val requireAll = annotation["requireAll"] as? Boolean ?: false
                val loadListener = annotation["loadListener"] as? Type
                
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
    
    private fun loadHook(className: String, loadListener: Type?) {
        if (loadListener != null) {
            val obj = (Class.forName(loadListener.className).kotlin as KClass<out LoadListener>).objectInstance
                ?: throw IllegalStateException("LoadListener $loadListener is not an object")
            
            if (!obj.loaded.get())
                return
        }
        
        val hookClass = Class.forName(className).kotlin
        val hookInstance = hookClass.objectInstance
            ?: throw IllegalStateException("Hook $hookClass is not an object")
        
        useHook(hookInstance)
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
        
        if (hook is UploadService) {
            AutoUploadManager.services += hook
        }
    }
    
}