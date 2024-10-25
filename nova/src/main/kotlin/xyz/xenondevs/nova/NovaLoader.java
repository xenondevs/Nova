package xyz.xenondevs.nova;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings({"UnstableApiUsage", "unused"})
@ApiStatus.Internal
public class NovaLoader implements PluginLoader {
    
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        try {
            var pluginJar = new File(NovaLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (var in = new ZipInputStream(new FileInputStream(pluginJar))) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    var name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith("lib/"))
                        continue;
                    
                    File lib = new File("libraries/" + name.substring(4));
                    if (!lib.exists()) {
                        lib.getParentFile().mkdirs();
                        try (var out = new FileOutputStream(lib)) {
                            in.transferTo(out);
                        }
                    }
                    classpathBuilder.addLibrary(new JarLibrary(lib.toPath()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
