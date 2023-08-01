package xyz.xenondevs.nova.loader.library;

import com.google.gson.JsonObject;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryFileParser {
    
    public static List<String> readRepositories(JsonObject json) {
        var repositories = new ArrayList<String>();
        if (!json.has("repositories"))
            return repositories;
        
        for (var repo : json.get("repositories").getAsJsonArray()) {
            repositories.add(repo.getAsString());
        }
        
        return repositories;
    }
    
    public static List<Dependency> readLibraries(JsonObject json) {
        var libraries = new ArrayList<Dependency>();
        
        if (!json.has("libraries"))
            return libraries;
        
        for (var library : json.get("libraries").getAsJsonArray()) {
            var libraryObj = library.getAsJsonObject();
            var coords = libraryObj.get("coords").getAsString();
            
            var exclusions = new ArrayList<Exclusion>();
            for (var excludedCoords : libraryObj.get("excludes").getAsJsonArray()) {
                var parts = excludedCoords.getAsString().split(":");
                var exclusion = new Exclusion(
                    parts.length > 0 ? parts[0] : null,
                    parts.length > 1 ? parts[1] : null,
                    parts.length > 2 ? parts[2] : null,
                    parts.length > 3 ? parts[3] : null
                );
                exclusions.add(exclusion);
            }
            
            var dependency = new Dependency(
                new DefaultArtifact(coords),
                null,
                false,
                exclusions
            );
            
            libraries.add(dependency);
        }
        
        return libraries;
    }
    
    public static Set<String> readExclusions(JsonObject json) {
        var repositories = new HashSet<String>();
        if (!json.has("excludes"))
            return repositories;
        
        for (var repo : json.get("excludes").getAsJsonArray()) {
            repositories.add(repo.getAsString());
        }
        
        return repositories;
    }
    
}
