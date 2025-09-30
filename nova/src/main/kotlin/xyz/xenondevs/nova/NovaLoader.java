package xyz.xenondevs.nova;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@ApiStatus.Internal
public class NovaLoader implements PluginLoader {
    
    private static final Path LIBRARIES_DIR = Path.of("libraries/");
    
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        try (var zfs = FileSystems.newFileSystem(Path.of(NovaLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            var lines = Files.readAllLines(zfs.getPath("nova-libraries"));
            for (var line : lines) {
                var src = zfs.getPath(line);
                var dst = LIBRARIES_DIR.resolve(line.substring("lib/".length()));
                if (!Files.exists(dst)) {
                    Files.createDirectories(dst.getParent());
                    Files.copy(src, dst);
                }
                classpathBuilder.addLibrary(new JarLibrary(dst));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Nova libraries", e);
        }
    }
    
}
