package forge.view;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import forge.game.IIdentifiable;

/**
 * Implementation of a two-way cache, containing bidirectional mappings from
 * keys of type {@code K} to values of type {@code V}.
 * 
 * Keys and values are checked for equality using the
 * {@link Object#equals(Object)} method.
 * 
 * @author elcnesh
 *
 * @param <K>
 *            the type of this Cache's keys.
 * @param <V>
 *            the type of this Cache's values.
 */
public class Cache<K extends IIdentifiable, V extends IIdentifiable> {

    private final Map<Integer, V> cache;
    private final Map<Integer, K> inverseCache;

    /**
     * Create a new, empty Cache instance.
     */
    public Cache() {
        this.cache = Maps.newTreeMap();
        this.inverseCache = Maps.newTreeMap();
    }

    /**
     * @param key
     *            a key.
     * @return {@code true} if and only if this Cache contains the specified
     *         key.
     */
    public boolean containsKey(final int keyId) {
        return cache.containsKey(keyId);
    }

    /**
     * Get the value associated to a key.
     * 
     * @param key
     *            a key.
     * @return the value associated to key, or {@code null} if no such value
     *         exists.
     */
    public V get(final int keyId) {
        return cache.get(keyId);
    }

    /**
     * Get the key associated to a value.
     * 
     * @param value
     *            a value.
     * @return the key associated to value, or {@code null} if no such key
     *         exists.
     */
    public K getKey(final int valueId) {
        return inverseCache.get(valueId);
    }

    /**
     * Get a value as it is present in this Cache, compared using the equals
     * method.
     * 
     * @param value
     * @return a value equal to value, if such a value is present in the Cache;
     *         {@code null} otherwise.
     */
    public synchronized V getCurrentValue(final V value) {
        for (final V currentValue : cache.values()) {
            if (currentValue.equals(value)) {
                return currentValue;
            }
        }
        return null;
    }

    /**
     * Add a mapping to this Cache.
     * 
     * If either argument is {@code null}, this method silently returns.
     * Otherwise, any existing mapping from the provided key to the provided
     * value is removed, as well as its inverse. After that, the provided
     * mapping and its inverse are added to this Cache.
     * 
     * @param key
     *            the key of the new mapping.
     * @param value
     *            the value to map to.
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public void put(final K key, final V value) {
        if (key == null || value == null) {
            return;
        }

        synchronized (this) {
            final int keyId = key.getId(), valueId = value.getId();

            // remove value mapping if it exists already
            if (inverseCache.containsKey(valueId)) {
                cache.values().remove(value);
                inverseCache.remove(valueId);
            }

            // remove key mapping if it exists already
            if (cache.containsKey(keyId)) {
                inverseCache.values().remove(key);
                cache.remove(keyId);
            }

            // put the new values
            cache.put(keyId, value);
            inverseCache.put(valueId, key);
        }
    }

    /**
     * Update the key of a particular entry. If the supplied key is {@code null}
     * or if the Cache didn't already contain a mapping for the supplied id,
     * this method silently returns.
     * 
     * @param valueId
     *            the id of the value to which the key is associated.
     * @param key
     *            the new key.
     */
    public void updateKey(final int valueId, final K key) {
        if (key == null || !inverseCache.containsKey(valueId)) {
            return;
        }

        synchronized (this) {
            inverseCache.put(valueId, key);
        }
    }

    /**
     * Clear all the mappings in this Cache, except for any key that exists in
     * the argument.
     * 
     * @param keys
     *            the keys to retain.
     */
    public synchronized void retainAllKeys(final Collection<K> keys) {
        cache.keySet().retainAll(Collections2.transform(keys, IIdentifiable.FN_GET_ID));
        inverseCache.values().retainAll(keys);
    }

    public void clear() {
        cache.clear();
        inverseCache.clear();
    }
}
