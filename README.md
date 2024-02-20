# quarkus-infinispan-cache-reset

Project to reproduce a "reset" of the Infinispan cache and the way Quarkus handles it.

See XXXX

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

#### 4. In another console, restart the Infinispan cache

*This is a simulation of what happens when my Infinispan server pod is terminated on my k8s cluster and another one is starting.*

```sh
docker stop infinispan-reset-test && \
    docker run --name infinispan-reset-test -d --rm -it -p 11222:11222 -e USER="user" -e PASS="hunter2" quay.io/infinispan/server:14.0
```

After this command, there are some error logs because the connection to the cache is lost (like "Connection to localhost/127.0.0.1:11222 was closed while waiting for response"), this is expected.
But after the new cache server is started, we have other errors:

```
Failed to read in cache: org.infinispan.server.hotrod.CacheNotFoundException: Cache with name 'math-infos' not found amongst the configured caches
Failed to write in cache: org.infinispan.server.hotrod.CacheNotFoundException: Cache with name 'math-infos' not found amongst the configured caches
```

The cache/schema wasn't created in Infinispan by Quarkus so the app can't use the cache. If I stop my app and restart it then the caches are created.

**Here's the question: what is the best way to create the caches/schemas without restarting the Quarkus app?**

## Cleanup

```sh
docker stop infinispan-reset-test
```
