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

@ApplicationScoped
public class InfinispanClientApp {

    private static final Logger log = LoggerFactory.getLogger(InfinispanClientApp.class);

    @Inject
    @Remote("preload-control-cache")
    RemoteCache<String, String> preloadControlCache;

    @ConfigProperty(name="hostname")
    String hostname;

    enum LoadStatus {
        LOADING("LOADING"), LOADED("LOADED");
        
        public final String value;

        private LoadStatus(String value) {
            this.value = value;
        }
    }

    final String LOAD_STATUS_CACHE_KEY = "LOAD_STATUS";
    final String LOADER_HOSTNAME_CACHE_KEY = "LOADER_HOSTNAME";

    void onStart(@Observes StartupEvent ev) {

        log.info("Checking cache load status");
        String loadStatus = preloadControlCache.get(LOAD_STATUS_CACHE_KEY);
        String loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

        if(loadStatus == null) {
            // assign this instance to loading
            log.info("Assigning loading to this instance ({})", hostname);
            preloadControlCache.put(LOAD_STATUS_CACHE_KEY, LoadStatus.LOADING.value);
            preloadControlCache.put(LOADER_HOSTNAME_CACHE_KEY, hostname);

            loadStatus = LoadStatus.LOADING.value;
        }

        int exitCode = 1; // default to error

        if(loadStatus.equals(LoadStatus.LOADED.value)) {
            // exit cleanly so app can start
            log.info("Cache already loaded so proceeding with normal app startup");
            exitCode = 0;
        } else {
            // Reload the hostname to check this instance got the lock
            loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

            if(loaderHostname.equals(hostname)) {
                // exit cleanly so loading can continue in the app
                log.info("Loading will proceed with this instance ({})", hostname);
                exitCode = 0;
            } else {
                // exit with error so pod restarts (another instance is loading cache)
                log.info("Loading already started in a different instance ({})", loaderHostname);
                exitCode = 1;
            }
        }

        Quarkus.asyncExit(exitCode);
    }
}