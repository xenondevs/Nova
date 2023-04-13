package xyz.xenondevs.nova.util.collection;

import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class WrappedWeakKeySet<E> extends AbstractObjectSet<E> {
    
    private final Supplier<Map<WeakKey<E>, ?>> mapSupplier;
    
    public WrappedWeakKeySet(Supplier<Map<WeakKey<E>, ?>> mapSupplier) {
        this.mapSupplier = mapSupplier;
    }
    
    @Override
    public ObjectIterator<E> iterator() {
        return new ObjectIterator<>() {
            private E next;
            private final Iterator<WeakKey<E>> iterator = mapSupplier.get().keySet().iterator();
            
            @Override
            public boolean hasNext() {
                while (iterator.hasNext()) {
                    // skip to current next
                    if ((next = iterator.next().get()) != null) {
                        return true;
                    }
                }
                return false;
            }
            
            @Override
            public E next() {
                if(next == null && !hasNext())
                    throw new NoSuchElementException();
                E ret = next;
                next = null;
                return ret;
            }
        };
    }
    
    @Override
    public int size() {
        return mapSupplier.get().keySet().size();
    }
}