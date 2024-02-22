package com.github.jdussouillez;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * Cache recovery service
 *
 * <p>
 * If a "cache not found" error is detected it means the Infinispan server died and another instance spawn.
 * This service will create the caches and register the schemas at runtime so {@link CacheService} can still use caches.
 * </p>
 */
@ApplicationScoped
public class CacheRecoveryService {

    private static final List<? extends GeneratedSchema> SCHEMAS = List.of(
        // OPTIMIZE: detect schema classes automatically using reflection?
        new MathCacheSchemaImpl()
    );

    @Inject
    protected RemoteCacheManager cacheManager;

    @ConfigProperty(name = "myapp.infinispan.auto-recovery", defaultValue = "true")
    protected boolean autoRecovery;

    @ConfigProperty(name = "quarkus.infinispan-client.username")
    protected String username;

    @ConfigProperty(name = "quarkus.infinispan-client.password")
    protected String password;

    public void recover(final Throwable thrw) {
        if (!needsRecovery(thrw)) {
            return;
        }
        System.out.println("Cache recovery in progress...");
        try (var recoveryCacheManager = duplicateCacheManager()) {
            // Register schemas
            RemoteCache<String, String> metadataCache = recoveryCacheManager
                .getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
            SCHEMAS.forEach(schema -> metadataCache.put(schema.getProtoFileName(), schema.getProtoFile()));
            System.out.println("Schemas registered");

            // Get the caches once to create them
            recoveryCacheManager.getConfiguration().remoteCaches()
                .keySet()
                .stream()
                .forEach(recoveryCacheManager::getCache);
            System.out.println("Cache recovery completed");
        } catch (RuntimeException ex) {
            System.err.println("Error when recovering Infinispan: " + ex.getMessage());
        }
    }

    private boolean needsRecovery(final Throwable thrw) {
        if (!autoRecovery) {
            return false;
        }
        // Dirty hack because CacheNotFoundException is not public
        return thrw.getMessage().contains("org.infinispan.server.hotrod.CacheNotFoundException");
    }

    private RemoteCacheManager duplicateCacheManager() {
        var originalConf = cacheManager.getConfiguration();
        var confBuilder = new ConfigurationBuilder()
            .clientIntelligence(originalConf.clientIntelligence());
        // Servers
        originalConf.servers().forEach(server ->
            confBuilder.addServer()
                .host(server.host())
                .port(server.port())
        );
        // Security
        confBuilder
            .security()
            .authentication()
            .username(username)
            .password(password);
        // Schemas
        SCHEMAS.forEach(confBuilder::addContextInitializer);
        // Caches
        originalConf.remoteCaches().values().forEach(cache ->
            confBuilder
                .remoteCache(cache.name())
                .configuration(cache.configuration())
        );
        return new RemoteCacheManager(confBuilder.build());
    }
}
