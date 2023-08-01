package xyz.xenondevs.nova.loader;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

public class NovaBootstrapper implements PluginBootstrap {
    
    private static final String NOVA_JAR_PATH = "/nova.jar";
    private static final String LIBRARIES_PATH = "/libraries.json";
    private static final String PRIORITIZED_LIBRARIES_PATH = "/prioritized_libraries.json";
    
    private static final String BUNDLER_DIR_PATH = ".internal_data/bundler";
    private static final String NOVA_EXTRACTED_JAR_FORMAT = "Nova-%s.jar";
    
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
    }
    
    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        var logger = Logger.getLogger("Nova");
        try {
            var novaJar = extractNovaJar(context);
            var classLoader = new NovaClassLoader(
                novaJar.toURI().toURL(),
                loadLibraries(logger, LIBRARIES_PATH),
                loadLibraries(logger, PRIORITIZED_LIBRARIES_PATH),
                getClass().getClassLoader() // PaperPluginClassLoader
            );
            return new NovaJavaPlugin(novaJar, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private File extractNovaJar(@NotNull PluginProviderContext context) throws IOException {
        // create bundler dir
        var bundlerDir = context.getDataDirectory().resolve(BUNDLER_DIR_PATH).toFile();
        bundlerDir.mkdirs();
        
        // clear bundler dir
        var files = bundlerDir.listFiles();
        if (files != null) {
            for (var file : files) {
                file.delete();
            }
        }
        
        // extract nova jar
        var novaJar = new File(bundlerDir, String.format(NOVA_EXTRACTED_JAR_FORMAT, context.getConfiguration().getVersion()));
        try (var in = NovaBootstrapper.class.getResourceAsStream(NOVA_JAR_PATH)) {
            assert in != null;
            try (var out = new FileOutputStream(novaJar)) {
                in.transferTo(out);
            }
        }
        
        return novaJar;
    }
    
    private URL[] loadLibraries(Logger logger, String path) throws DependencyResolutionException, IOException {
        return toUrlArray(NovaLibraryLoader.loadLibraries(logger, path));
    }
    
    private URL[] toUrlArray(List<File> files) throws MalformedURLException {
        var array = new URL[files.size()];
        for (int i = 0; i < files.size(); i++) {
            array[i] = files.get(i).toURI().toURL();
        }
        return array;
    }
    
}
