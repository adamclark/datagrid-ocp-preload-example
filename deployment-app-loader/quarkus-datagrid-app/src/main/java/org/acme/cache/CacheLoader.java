package org.acme.cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.infinispan.client.Remote;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;

import static org.acme.cache.CacheConstants.LOAD_STATUS_CACHE_KEY;
import static org.acme.cache.CacheConstants.LOADER_HOSTNAME_CACHE_KEY;
import org.acme.cache.CacheConstants.LoadStatus;

@ApplicationScoped
public class CacheLoader {

    private static final Logger log = LoggerFactory.getLogger(CacheLoader.class);

    @Inject
    @Remote("preload-control-cache")
    RemoteCache<String, String> preloadControlCache;

    @Inject
    @Remote("greetings-cache")
    RemoteCache<String, String> greetingsCache;

    @ConfigProperty(name="hostname")
    String hostname;

    void onStart(@Observes StartupEvent ev) {

        log.info("Checking cache load status");
        String loadStatus = preloadControlCache.get(LOAD_STATUS_CACHE_KEY);
    
        if(loadStatus == null || !loadStatus.equals(LoadStatus.LOADED.value)) {

            String loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

            if(!loaderHostname.equals(hostname)) {
                // This instance isn't assigned as the cache loader so terminate and restart
                Quarkus.asyncExit(1);
                return;
            }

            log.info("Preloading cache...");
            greetingsCache.put("hello1", "Hello1 World, Infinispan is up!");
            greetingsCache.put("hello2", "Hello2 World, Infinispan is up!");
            greetingsCache.put("hello3", "Hello3 World, Infinispan is up!");

            log.info("Sleeping to simulate cache loading...");
            try {
                Thread.sleep(30000);
            } catch(InterruptedException ex) {}

            log.info("Setting status to loaded");
            preloadControlCache.put(LOAD_STATUS_CACHE_KEY, LoadStatus.LOADED.value);
        }
    }
}