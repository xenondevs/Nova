package xyz.xenondevs.nova.addon.library;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@ApiStatus.Internal
@Deprecated
public class NovaLibraryLoader {
    
    private static final HashSet<String> allExclusions = new HashSet<>();
    
    public static List<File> loadLibraries(Logger logger, String path) throws IOException, DependencyResolutionException {
        JsonObject librariesJson;
        try (var stream = NovaLibraryLoader.class.getResourceAsStream(path)) {
            assert stream != null;
            librariesJson = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        }
        
        var repositories = LibraryFileParser.readRepositories(librariesJson);
        var dependencies = LibraryFileParser.readLibraries(librariesJson);
        var exclusions = LibraryFileParser.readExclusions(librariesJson);
        
        allExclusions.addAll(exclusions);
        
        return LibraryLoader.downloadLibraries(repositories, dependencies, exclusions, logger);
    }
    
    public static Set<String> getAllExclusions() {
        return allExclusions;
    }
    
}
