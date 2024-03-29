package xyz.xenondevs.nova.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.loader.library.LibraryFileParser;
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader;

import java.io.IOException;
import java.io.InputStreamReader;

public class NovaLoader implements PluginLoader {
    
    private static final String LIBRARIES_PATH = "/libraries.json";
    
    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {
        JsonObject librariesJson;
        try (var stream = NovaLibraryLoader.class.getResourceAsStream(LIBRARIES_PATH)) {
            assert stream != null;
            librariesJson = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        var repositories = LibraryFileParser.readRepositories(librariesJson);
        var dependencies = LibraryFileParser.readLibraries(librariesJson);
        
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        for (var dependency : dependencies)
            resolver.addDependency(dependency);
        for (var repository : repositories)
            resolver.addRepository(new RemoteRepository.Builder(repository, "default", repository).build());
        
        pluginClasspathBuilder.addLibrary(resolver);
    }
    
}
