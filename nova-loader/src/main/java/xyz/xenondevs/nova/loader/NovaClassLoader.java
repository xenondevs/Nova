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
            Class<?> c;
            
            c = findLoadedClass(name);
            
            if (c == null && checkParents) {
                try {
                    c = getParent().loadClass(name);
                } catch (ClassNotFoundException e) {
                    // ignored
                }
            }
            
            if (c == null && !injectedClasses.contains(name)) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // ignored
                }
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
    
    public void addInjectedClass(String name) {
        injectedClasses.add(name);
    }
    
    public void addInjectedClasses(Collection<String> names) {
        injectedClasses.addAll(names);
    }
    
}
