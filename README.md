# quarkus-infinispan-cache-reset

Project to reproduce a "reset" of the Infinispan cache and the way Quarkus handles it.

See https://github.com/quarkusio/quarkus/discussions/38877

## Build

```sh
./mvnw package
```

## Run

#### 1. Start the Infinispan cache

```sh
docker run --name infinispan-reset-test -d --rm -it -p 11222:11222 -e USER="user" -e PASS="hunter2" quay.io/infinispan/server:14.0
```

*Console: http://localhost:11222/console/*

#### 2. Run the app

```sh
java -jar target/quarkus-app/quarkus-run.jar
```

#### 3. Send requests to the API every seconds

```sh
watch -n 1 curl --silent http://localhost:8080/math/info?n=16
```

The cache was created by Quarkus and is used by the app.

#### 4. In another console, kill and run a new Infinispan cache

*This is a simulation of what happens when my Infinispan server pod is terminated on my k8s cluster and another one is starting.*

```sh
docker rm --force infinispan-reset-test && \
    docker run --name infinispan-reset-test -d --rm -it -p 11222:11222 -e USER="user" -e PASS="hunter2" quay.io/infinispan/server:14.0
```

After this command, there are some error logs because the connection to the cache is lost (like "Connection to localhost/127.0.0.1:11222 was closed while waiting for response"), this is expected.
But after the new cache server is started, we have other errors:

```
Failed to read in cache: org.infinispan.server.hotrod.CacheNotFoundException: Cache with name 'math-infos' not found amongst the configured caches
Failed to write in cache: org.infinispan.server.hotrod.CacheNotFoundException: Cache with name 'math-infos' not found amongst the configured caches
```

The cache/schema wasn't created in Infinispan by Quarkus so the app can't use the cache. If I stop my app and restart it then the caches are created.

**Here's the question: what is the best/recommanded way to create the caches/schemas without restarting the Quarkus app? Is there a way to run what the `use-schema-registration` option does but on demand in a running app?**

- **Edit 1: I managed to get a working recovery from the app code itself. See branch [`code-recovery`](https://github.com/jdussouillez/quarkus-infinispan-cache-reset/tree/code-recovery). I don't really know why but I had to duplicate the `RemoteCacheManager` instance (the one injected by Quarkus) to get it working ([code](https://github.com/jdussouillez/quarkus-infinispan-cache-reset/blob/code-recovery/src/main/java/com/github/jdussouillez/CacheRecoveryService.java#L45)).**

## Solutions

See https://github.com/quarkusio/quarkus/discussions/38877#discussion-6243587

## Cleanup

```sh
docker rm --force infinispan-reset-test
```
