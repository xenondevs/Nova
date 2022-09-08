package xyz.xenondevs.nova.loader.library;

import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class NovaLibraryLoader {
    
    public static List<URL> loadLibraries(Logger logger) throws IOException, DependencyResolutionException {
        YamlConfiguration cfg;
        try (var stream = NovaLibraryLoader.class.getResourceAsStream("/libraries.yml")) {
            assert stream != null;
            cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        
        return LibraryLoader.downloadLibraries(
            cfg.getStringList("repositories"),
            readRequestedLibraries(cfg),
            readExclusions(cfg),
            logger
        );
    }
    
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static List<Dependency> readRequestedLibraries(YamlConfiguration cfg) {
        var libraries = new ArrayList<Dependency>();
        
        if (!cfg.contains("libraries"))
            return libraries;
        
        for (Object obj : cfg.getList("libraries")) {
            if (obj instanceof String coords) {
                libraries.add(new Dependency(new DefaultArtifact(coords), null));
            } else if (obj instanceof HashMap<?, ?> map) {
                var coords = map.get("library");
                if (coords == null)
                    continue;
                
                var exclusions = map.get("exclusions");
                
                Dependency dependency;
                if (exclusions != null) {
                    dependency = new Dependency(
                        new DefaultArtifact((String) coords),
                        null,
                        false,
                        ((List<String>) exclusions).stream().map(exCoords -> {
                            var parts = exCoords.split(":");
                            return new Exclusion(
                                parts.length > 0 ? parts[0] : null,
                                parts.length > 1 ? parts[1] : null,
                                parts.length > 2 ? parts[2] : null,
                                parts.length > 3 ? parts[3] : null
                            );
                        }).toList()
                    );
                    
                } else {
                    dependency = new Dependency(new DefaultArtifact((String) coords), null);
                }
                
                libraries.add(dependency);
            }
        }
        
        return libraries;
    }
    
    public static List<String> readExclusions(YamlConfiguration cfg) {
        return cfg.getStringList("exclusions");
    }
    
}
