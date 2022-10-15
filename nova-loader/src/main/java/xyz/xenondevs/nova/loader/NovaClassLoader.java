package xyz.xenondevs.nova.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class NovaClassLoader extends URLClassLoader {
    
    public NovaClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        
        // check if the class is already loaded
        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);
            
            // check nova classes and libraries
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        
        // check parent loader
        if (c == null) {
            c = getParent().loadClass(name);
        }
        
        if (resolve) {
            synchronized (getClassLoadingLock(name)) {
                resolveClass(c);
            }
        }
        
        return c;
    }
    
    public Class<?> loadClassNoParent(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // check if the class is already loaded
            Class<?> c = findLoadedClass(name);
            
            // check nova classes and libraries
            if (c == null) {
                c = findClass(name);
            }
            
            return c;
        }
    }
    
}
