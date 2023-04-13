package xyz.xenondevs.nova.util.collection;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class WeakKey<K> extends WeakReference<K> {
    private final int hash;
    
    public WeakKey(K referent, ReferenceQueue<K> queue) {
        super(referent, queue);
        this.hash = System.identityHashCode(referent);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof WeakKey<?> other) {
            return other.get() == get();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return hash;
    }
    
}