package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.infinispan.client.hotrod.RemoteCache;

import io.quarkus.infinispan.client.Remote;

import static org.acme.cache.CacheConstants.LOAD_STATUS_CACHE_KEY;
import org.acme.cache.CacheConstants.LoadStatus;

@Path("/health")
public class HealthResource {

    @Inject
    @Remote("preload-control-cache")
    RemoteCache<String, String> preloadControlCache;

    @GET
    @Path("/readiness")
    public Response readiness() {
        return isCacheReady() ? Response.ok().build() : Response.serverError().build();
    }

    @GET
    @Path("/liveness")
    public Response liveness() {
        return isCacheReady() ? Response.ok().build() : Response.serverError().build();
    }

    boolean isCacheReady() {
        return preloadControlCache.get(LOAD_STATUS_CACHE_KEY).equals(LoadStatus.LOADED.value);
    }
}