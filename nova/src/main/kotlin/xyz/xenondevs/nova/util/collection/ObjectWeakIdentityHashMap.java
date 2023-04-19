package xyz.xenondevs.nova.util.collection;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An {@link Object2ObjectMap} implementation that uses {@code System.identityHashCode} and {@code ==} to compare keys
 * instead of {@code hashCode} and {@code equals}.
 */
public class ObjectWeakIdentityHashMap<K, V> implements Object2ObjectMap<K, V>, WeakIdentityMap {
    
    private final Object2ObjectMap<WeakKey<K>, V> map = new Object2ObjectOpenHashMap<>();
    private final transient ReferenceQueue<K> queue = new ReferenceQueue<>();
    
    private final Consumer<V> onRemove;
    
    public ObjectWeakIdentityHashMap(Consumer<V> onRemove) {
        this.onRemove = onRemove;
    }
    
    public ObjectWeakIdentityHashMap() {
        this.onRemove = null;
    }
    
    private Object2ObjectMap<WeakKey<K>, V> getMap() {
        checkQueue();
        return map;
    }
    
    @SuppressWarnings({"ReassignedVariable", "SuspiciousMethodCalls"})
    @Override
    public void checkQueue() {
        synchronized (this) {
            for (Reference<? extends K> ref; (ref = this.queue.poll()) != null; ) {
                var val = map.remove(ref);
                if (val != null && onRemove != null)
                    onRemove.accept(val);
            }
        }
    }
    
    @Override
    public int size() {
        return getMap().size();
    }
    
    @Override
    public boolean isEmpty() {
        return getMap().isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return getMap().containsKey(new WeakKey<>(key, null));
    }
    
    @Override
    public boolean containsValue(Object value) {
        return getMap().containsValue(value);
    }
    
    
    @Override
    public V get(Object key) {
        return getMap().get(new WeakKey<>(key, null));
    }
    
    @Nullable
    @Override
    public V put(K key, V value) {
        return getMap().put(new WeakKey<>(key, queue), value);
    }
    
    @Override
    public V remove(Object key) {
        return getMap().remove(new WeakKey<>(key, null));
    }
    
    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public void clear() {
        getMap().clear();
    }
    
    @Override
    public void defaultReturnValue(V rv) {
        map.defaultReturnValue(rv);
    }
    
    @Override
    public V defaultReturnValue() {
        return map.defaultReturnValue();
    }
    
    @NotNull
    @Override
    public ObjectSet<K> keySet() {
        return new WrappedWeakKeySet<>(this::getMap);
    }
    
    @NotNull
    @Override
    public ObjectCollection<V> values() {
        return getMap().values();
    }
    
    @Override
    public ObjectSet<Entry<K, V>> object2ObjectEntrySet() {
        return new AbstractObjectSet<>() {
            
            @Override
            public ObjectIterator<Entry<K, V>> iterator() {
                final ObjectIterator<Entry<WeakKey<K>, V>> iterator = getMap().object2ObjectEntrySet().iterator();
                return new ObjectIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
                    
                    @Override
                    public Entry<K, V> next() {
                        return new Entry<>() {
                            private final Entry<WeakKey<K>, V> entry = iterator.next();
                            
                            @Override
                            public K getKey() {
                                return entry.getKey().get();
                            }
                            
                            @Override
                            public V getValue() {
                                return entry.getValue();
                            }
                            
                            @Override
                            public V setValue(V value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
            
            @Override
            public int size() {
                return getMap().entrySet().size();
            }
            
        };
    }
    
}