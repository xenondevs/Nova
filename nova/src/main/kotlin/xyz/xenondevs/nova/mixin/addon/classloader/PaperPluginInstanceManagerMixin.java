package xyz.xenondevs.nova.mixin.addon.classloader;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import io.papermc.paper.plugin.manager.PaperPluginInstanceManager;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.addon.AddonBootstrapper;

@Mixin(PaperPluginInstanceManager.class)
abstract class PaperPluginInstanceManagerMixin {
    
    /**
     * Prevents the plugin class loaders from addons or Nova to be closed.
     * This is necessary as Nova may call into addons during its disable (which is after the addon's class
     * loader has been closed), which may require class loading.
     */
    @Definition(id = "ConfiguredPluginClassLoader", type = ConfiguredPluginClassLoader.class)
    @Expression("? instanceof ConfiguredPluginClassLoader")
    @Redirect(method = "disablePlugin", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean shouldCloseClassLoader(Object obj, Class<?> type) {
        if (!(obj instanceof ConfiguredPluginClassLoader loader))
            return false;
        
        var plugin = loader.getPlugin();
        if (plugin == null)
            return true;
        
        // don't close the classloader for Nova or any addon
        return !plugin.getName().equals("Nova") 
               && AddonBootstrapper.getAddons().stream()
                   .noneMatch(addon -> addon.getPlugin() == plugin);
    }
    
}
