@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.patch.impl.misc

import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader
import org.bukkit.plugin.Plugin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.TypeInsnNode
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.isClass
import xyz.xenondevs.bytebase.util.replaceEvery
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

private val PAPER_PLUGIN_INSTANCE_MANAGER = ReflectionUtils.getClass("io.papermc.paper.plugin.manager.PaperPluginInstanceManager").kotlin
private val DISABLE_PLUGIN = ReflectionUtils.getMethod(PAPER_PLUGIN_INSTANCE_MANAGER, "disablePlugin", Plugin::class)

/**
 * Prevents the plugin class loaders from addons or Nova to be closed.
 * This is necessary as Nova may call into addons during its disable (which is after the addon's class
 * loader has been closed), which may require class loading.
 */
internal object DontCloseAddonClassLoadersPatch : MultiTransformer(PAPER_PLUGIN_INSTANCE_MANAGER) {
    
    override fun transform() {
        VirtualClassPath[DISABLE_PLUGIN].replaceEvery(
            0, 0,
            { invokeStatic(::shouldClosePluginClassLoader) }
        ) { it.opcode == Opcodes.INSTANCEOF && (it as TypeInsnNode).isClass(ConfiguredPluginClassLoader::class) }
    }
    
    @JvmStatic
    fun shouldClosePluginClassLoader(loader: ClassLoader): Boolean {
        if (loader !is ConfiguredPluginClassLoader)
            return false
        
        val plugin = loader.plugin
        return plugin == null || (plugin.name != "Nova" && !AddonBootstrapper.addons.any { it.plugin == plugin })
    }
    
}