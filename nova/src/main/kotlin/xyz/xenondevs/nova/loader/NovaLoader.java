package xyz.xenondevs.nova.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.nova.loader.library.NovaLibraryLoader;

import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class NovaLoader implements PluginLoader {
    
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        var logger = getLogger();
        
        try {
            var classpath = NovaLibraryLoader.loadLibraries(logger);
            for (var file : classpath) {
                classpathBuilder.addLibrary(new JarLibrary(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Logger getLogger() {
        var logger = Logger.getLogger("NovaLoader");
        logger.setUseParentHandlers(false);
        logger.addHandler(new StreamHandler(System.out, new SimpleFormatter() {
            
            private static final String format = "[%1$tT %2$s] [Nova] %3$s\n";
            
            @Override
            public String format(LogRecord record) {
                return String.format(
                    format,
                    new Date(record.getMillis()), record.getLevel().getLocalizedName(),
                    record.getMessage()
                );
            }
            
        }));
        
        return logger;
    }
    
}
