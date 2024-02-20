package com.github.jdussouillez;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import org.infinispan.client.hotrod.RemoteCache;

@ApplicationScoped
public class CacheService {

    private static final int REQUEST_TIMEOUT = 3_000;

    public <K, V> Uni<V> getOrFetch(final RemoteCache<K, V> cache, final K key, final Function<K, Uni<V>> fetch) {
        return get(cache, key)
            .chain(cacheValue -> {
                if (cacheValue.isPresent()) {
                    System.out.println("Cache hit for k=" + key);
                    return Uni.createFrom().item(cacheValue.get());
                }
                System.out.println("Cache missed for k=" + key);
                return fetch.apply(key)
                    .chain(fetchedValue -> put(cache, key, fetchedValue));
            });
    }

    private <K, V> Uni<Optional<V>> get(final RemoteCache<K, V> cache, final K key) {
        return Uni.createFrom().completionStage(cache.getAsync(key))
            .map(Optional::ofNullable)
            .ifNoItem()
            .after(Duration.ofMillis(REQUEST_TIMEOUT))
            .failWith(new TimeoutException())
            .onFailure()
            .invoke(ex -> System.err.println("Failed to read in cache: " + ex.getMessage()))
            .onFailure()
            .recoverWithItem(Optional.empty());
    }

    private <K, V> Uni<V> put(final RemoteCache<K, V> cache, final K key, final V value) {
        return Uni.createFrom().completionStage(cache.putAsync(key, value))
            .replaceWith(value)
            .ifNoItem()
            .after(Duration.ofMillis(REQUEST_TIMEOUT))
            .failWith(new TimeoutException())
            .onFailure()
            .invoke(ex -> System.err.println("Failed to write in cache: " + ex.getMessage()))
            .onFailure()
            .recoverWithItem(value);
    }
}
