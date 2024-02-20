package com.github.jdussouillez;

import io.quarkus.infinispan.client.Remote;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import org.infinispan.client.hotrod.RemoteCache;

@ApplicationScoped
public class MathService {

    @Inject
    protected CacheService cacheService;

    @Inject
    @Remote("math-infos")
    protected RemoteCache<Integer, MathInfo> mathInfosCache;

    public Uni<MathInfo> info(final int n) {
        return cacheService.getOrFetch(mathInfosCache, n, this::generateInfo);
    }

    private Uni<MathInfo> generateInfo(final int n) {
        System.out.println("Generating math infos for n=" + n);
        var info = new MathInfo(
            n % 2 == 0,
            Math.sqrt((double) n),
            (int) (Math.log(Math.abs(n)) + 1)
        );
        return Uni.createFrom().item(info)
            .onItem()
            .delayIt()
            .by(Duration.ofSeconds(1L));
    }
}
