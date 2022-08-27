package xyz.xenondevs.nova.loader;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("resource")
public class NovaLoader extends JavaPlugin {
    
    private Plugin nova;
    private final Logger logger = getLogger();
    
    @Override
    public void onEnable() {
        try {
            var novaJarFile = extractNovaJar("/nova.jar");
            
            var classpath = NovaLibraryLoader.loadLibraries(logger);
            classpath.add(novaJarFile.toURI().toURL());
            
            var loader = new URLClassLoader(classpath.toArray(URL[]::new), getClassLoader());
            var novaClass = loader.loadClass("xyz.xenondevs.nova.Nova");
            nova = (Plugin) novaClass.getConstructor(JavaPlugin.class, File.class).newInstance(this, novaJarFile);
            
            nova.onEnable();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An exception occurred trying to load Nova and its libraries", e);
        }
    }
    
    @Override
    public void onDisable() {
        if (nova != null)
            nova.onDisable();
    }
    
    public Plugin getNova() {
        return nova;
    }
    
    public static File extractNovaJar(String pathInJar) throws IOException {
        var path = Files.createTempFile("nova", ".jar");
        var file = path.toFile();
        file.deleteOnExit();
        
        try (var in = NovaLoader.class.getResourceAsStream(pathInJar)) {
            assert in != null;
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return file;
    }
    
}
