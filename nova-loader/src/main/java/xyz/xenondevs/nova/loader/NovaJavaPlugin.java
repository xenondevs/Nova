package xyz.xenondevs.nova.loader;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class NovaJavaPlugin extends JavaPlugin {
    
    private final File novaJar;
    private final NovaClassLoader classLoader;
    private PluginDelegate nova;
    
    public NovaJavaPlugin(File novaJar, NovaClassLoader classLoader) {
        this.novaJar = novaJar;
        this.classLoader = classLoader;
    }
    
    @Override
    public void onEnable() {
        try {
            var novaClass = classLoader.loadClass("xyz.xenondevs.nova.Nova", true);
            var constructor = novaClass.getConstructor(NovaJavaPlugin.class, File.class);
            nova = (PluginDelegate) constructor.newInstance(this, novaJar);
            nova.onEnable();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void onDisable() {
        if (nova != null)
            nova.onDisable();
    }
    
    public PluginDelegate getNova() {
        return nova;
    }
    
}
