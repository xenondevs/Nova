package xyz.xenondevs.nova.loader;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("resource")
public class NovaLoader extends JavaPlugin {
    
    private Plugin nova;
    private final Logger logger = getLogger();
    
    @Override
    public void onEnable() {
        try {
            var novaJarFile = extractNovaJar(this, "/nova.jar");
            
            var classpath = NovaLibraryLoader.loadLibraries(logger);
            classpath.add(novaJarFile.toURI().toURL());
            
            var loader = new NovaClassLoader(classpath.toArray(URL[]::new), getClassLoader());
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
    
    public static File extractNovaJar(NovaLoader loader, String pathInJar) throws IOException {
        var file = new File(loader.getDataFolder(), ".internal_data/bundler/Nova-" + loader.getDescription().getVersion() + ".jar");
        var parentFile = file.getParentFile();
        parentFile.mkdirs();
        Arrays.stream(parentFile.listFiles()).forEach(File::delete);
        
        try (var in = NovaLoader.class.getResourceAsStream(pathInJar)) {
            assert in != null;
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        return file;
    }
    
}
