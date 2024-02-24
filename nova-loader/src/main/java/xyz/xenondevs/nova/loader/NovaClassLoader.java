package xyz.xenondevs.nova.loader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;

public class NovaClassLoader extends URLClassLoader {
    
    // This URLClassLoader is only used because URLClassPath is not accessible.
    // Under no circumstances should it be used to load classes, only to find resources.
    private final URLClassLoader prioritizedLibraries;
    
    public NovaClassLoader(@NotNull URL nova, @NotNull URL @NotNull [] libraries, @NotNull URL @NotNull [] prioritizedLibraries, @Nullable ClassLoader parent) {
        super(concatUrls(nova, libraries), parent);
        this.prioritizedLibraries = new URLClassLoader(prioritizedLibraries, null);
    }
    
    private static URL[] concatUrls(URL url, URL[] arr) {
        URL[] result = new URL[arr.length + 1];
        result[0] = url;
        System.arraycopy(arr, 0, result, 1, arr.length);
        return result;
    }
    
    @Override
    public @NotNull Class<?> loadClass(@NotNull String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, true, true);
    }
    
    public @NotNull Class<?> loadClass(@NotNull String name, boolean resolve, boolean checkParents, boolean checkPrioritizedLibraries) throws ClassNotFoundException {
        // TODO: evaluate the possibility of potential deadlocks considering PatchedClassLoader
        
        // check if class is already loaded
        Class<?> c = findLoadedClass(name);
        
        // try load class from prioritized libraries
        synchronized (getClassLoadingLock(name)) {
            if (c == null && checkPrioritizedLibraries)
                c = tryFindClass(prioritizedLibraries, name);
        }
        
        // try load class from parent (PaperPluginClassLoader)
        if (c == null && checkParents)
            c = tryLoadClass(getParent(), name);
        
        // load class from nova and libraries or throw ClassNotFoundException
        synchronized (getClassLoadingLock(name)) {
            if (c == null)
                c = findClass(name);
        }
        
        if (resolve)
            resolveClass(c);
        
        return c;
    }
    
    private @Nullable Class<?> tryLoadClass(@NotNull ClassLoader loader, @NotNull String name) {
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
    
    private @Nullable Class<?> tryFindClass(@NotNull URLClassLoader loader, @NotNull String name) {
        try {
            return findClass(loader, name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
    
    private @NotNull Class<?> findClass(@NotNull URLClassLoader loader, @NotNull String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        var url = loader.findResource(path);
        if (url != null) {
            return defineClass(name, url);
        }
        
        throw new ClassNotFoundException(name);
    }
    
    private @NotNull Class<?> defineClass(String name, URL url) throws ClassNotFoundException {
        // define package if not already defined
        int i = name.lastIndexOf('.');
        if (i != -1) {
            String packageName = name.substring(0, i);
            if (getDefinedPackage(packageName) == null)
                definePackage(packageName, null, null, null, null, null, null, null);
        }
        
        // define class
        try (var in = url.openStream()) {
            var bin = in.readAllBytes();
            return defineClass(name, bin, 0, bin.length);
        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }
    }
    
}
