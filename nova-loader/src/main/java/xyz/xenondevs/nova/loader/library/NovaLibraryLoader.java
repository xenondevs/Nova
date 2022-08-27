package xyz.xenondevs.nova.loader.library;

import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

public class NovaLibraryLoader {
    
    public static List<URL> loadLibraries(Logger logger) throws IOException, DependencyResolutionException {
        YamlConfiguration cfg;
        try(var stream = NovaLibraryLoader.class.getResourceAsStream("/libraries.yml")) {
            assert stream != null;
            cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        
        return LibraryLoader.downloadLibraries(
            cfg.getStringList("repositories"),
            readRequestedLibraries(cfg),
            logger
        );
    }
    
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static List<Dependency> readRequestedLibraries(YamlConfiguration cfg) {
        var libraries = new ArrayList<Dependency>();
        
        if (!cfg.contains("libraries"))
            return libraries;
        
        for (Object obj : cfg.getList("libraries")) {
            if (obj instanceof String lib) {
                libraries.add(new Dependency.DefaultDependency(lib));
            } else if (obj instanceof HashMap<?, ?> map) {
                var lib = map.get("library");
                if (lib == null)
                    continue;
                
                var exclusions = map.get("exclusions");
                
                libraries.add(
                    new Dependency.ExclusionDependency(
                        (String) lib,
                        exclusions != null ? (List<String>) exclusions : emptyList()
                    )
                );
            }
        }
        
        return libraries;
    }
    
}
