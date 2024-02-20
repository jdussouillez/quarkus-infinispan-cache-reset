package com.github.jdussouillez;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

@ApplicationScoped
public class CacheService {

    private static final int REQUEST_TIMEOUT = 3_000;

    private static final boolean ENABLE_AUTO_RECOVERY = false; // TODO: change this to enable auto recovery

    @Inject
    protected RemoteCacheManager cacheManager;

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
            .invoke(ex -> {
                System.err.println("Failed to read in cache: " + ex.getMessage());
                tryRecovery(ex);
            })
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
            .invoke(ex -> {
                System.err.println("Failed to write in cache: " + ex.getMessage());
                tryRecovery(ex);
            })
            .onFailure()
            .recoverWithItem(value);
    }

    private <K, V> void tryRecovery(final Throwable cause) {
        if (!ENABLE_AUTO_RECOVERY) {
            return;
        }
        // TODO: fix this. Dirty hack because CacheNotFoundException is not public
        if (!cause.getMessage().contains("org.infinispan.server.hotrod.CacheNotFoundException")) {
            return;
        }
        System.out.println("Try to recover caches/schemas...");
        // Register schemas
        List<? extends GeneratedSchema> schemas = List.of(
            new MathCacheSchemaImpl()
        );
        RemoteCache<String, String> protoMetadataCache = cacheManager
            .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        for (var schema : schemas) {
            protoMetadataCache.put(schema.getProtoFileName(), schema.getProtoFile());
        }
        var errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
        if (errors != null) {
            System.err.println("Invalid proto schema: " + errors);
            return;
        }
        System.out.println("Schema registered");
        // Create caches
        cacheManager.getConfiguration().remoteCaches()
            .values()
            .stream()
            .forEach(cache -> cacheManager.administration().createCache(cache.name(), name -> cache.configuration()));
        System.out.println("Caches created. End of recovery");
    }
}
