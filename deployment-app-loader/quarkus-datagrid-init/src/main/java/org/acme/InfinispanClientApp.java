package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;

import io.quarkus.infinispan.client.Remote;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

import static org.acme.cache.CacheConstants.LOAD_STATUS_CACHE_KEY;
import static org.acme.cache.CacheConstants.LOADER_HOSTNAME_CACHE_KEY;
import org.acme.cache.CacheConstants.LoadStatus;

@ApplicationScoped
public class InfinispanClientApp {

    private static final Logger log = LoggerFactory.getLogger(InfinispanClientApp.class);

    @Inject
    @Remote("preload-control-cache")
    RemoteCache<String, String> preloadControlCache;

    @ConfigProperty(name="hostname")
    String hostname;

    void onStart(@Observes StartupEvent ev) {

        log.info("Checking cache load status");
        String loadStatus = preloadControlCache.get(LOAD_STATUS_CACHE_KEY);
        String loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

        // If the cache isn't loaded and no other instance is loading, assign loading to this instance
        if(loadStatus == null) {
            // assign this instance to loading
            log.info("Assigning loading to this instance ({})", hostname);
            preloadControlCache.put(LOAD_STATUS_CACHE_KEY, LoadStatus.LOADING.value);
            preloadControlCache.put(LOADER_HOSTNAME_CACHE_KEY, hostname);

            loadStatus = LoadStatus.LOADING.value;
        }

        // Now work out if this instance should start (cache is loaded or this instance will load it)
        // or fail and restart (another instance is loading the cache so restart until it's loaded)
        int exitCode = 1; // default to error

        if(loadStatus.equals(LoadStatus.LOADED.value)) {
            // The cache is loaded, exit cleanly so app can start
            log.info("Cache already loaded so proceeding with normal app startup");
            exitCode = 0;
        } else {
            // Reload the hostname to check this instance got the assignment
            loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

            if(loaderHostname.equals(hostname)) {
                // This instance is assigned loading, exit cleanly so loading can continue in the app
                log.info("Loading will proceed with this instance ({})", hostname);
                exitCode = 0;
            } else {
                // Another instance is loading, exit with error so pod restarts and rechecks the cache is loaded
                // Note: An alternative approch would be to loop in this method until the cache is loaded
                log.info("Loading already started in a different instance ({})", loaderHostname);
                exitCode = 1;
            }
        }

        Quarkus.asyncExit(exitCode);
    }
}