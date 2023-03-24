package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.infinispan.client.hotrod.RemoteCache;

import io.quarkus.infinispan.client.Remote;

@Path("/helloFromCache")
public class CacheGreetingResource {

    @Inject
    @Remote("greetings-cache")
    RemoteCache<String, String> cache;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String helloFromCache(@QueryParam("greetingKey") String cacheKey) {
        return cache.get(cacheKey);
    }
}