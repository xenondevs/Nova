package xyz.xenondevs.nova.loader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NovaClassLoader extends URLClassLoader {
    
    private final Set<String> injectedClasses = new HashSet<>();
    
    public NovaClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name, resolve, true);
    }
    
    public Class<?> loadClass(String name, boolean resolve, boolean checkParents) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            
            // workaround library conflict for kyori-adventure on paper servers (fixme)
            if (c == null && name.startsWith("net.kyori.adventure") && checkParents) {
                c = loadClassFromParentOrNull(name);
            }
            
            // check Nova classes and libraries before parent to:
            //   - prevent accessing classes of other plugins
            //   - prevent the usage of old patch classes (which stay in memory after reloading)
            if (c == null && !injectedClasses.contains(name)) {
                c = findClassOrNull(name);
            }
            
            if (c == null && checkParents) {
                c = loadClassFromParentOrNull(name);
            }
            
            if (c == null) {
                throw new ClassNotFoundException(name);
            }
            
            if (resolve) {
                resolveClass(c);
            }
            
            return c;
        }
    }
    
    private Class<?> findClassOrNull(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private Class<?> loadClassFromParentOrNull(String name) {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    public void addInjectedClass(String name) {
        injectedClasses.add(name);
    }
    
    public void addInjectedClasses(Collection<String> names) {
        injectedClasses.addAll(names);
    }
    
}
