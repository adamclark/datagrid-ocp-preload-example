package org.acme.loader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.infinispan.client.Remote;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

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
    
        if(loadStatus == null || !loadStatus.equals(LoadStatus.LOADED.value)) {

            // TODO: need to check for diff hostname in loaderHostname
            //String loaderHostname = (String) preloadControlCache.get(LOADER_HOSTNAME_CACHE_KEY);

            log.info("Preloading cache...");
            greetingsCache.put("hello1", "Hello1 World, Infinispan is up!");
            greetingsCache.put("hello2", "Hello2 World, Infinispan is up!");
            greetingsCache.put("hello3", "Hello3 World, Infinispan is up!");

            log.info("Sleeping to simulate cache preloading...");
            try {
                Thread.sleep(30000);
            } catch(InterruptedException ex) {}

            log.info("Setting status to loaded");
            preloadControlCache.put(LOAD_STATUS_CACHE_KEY, LoadStatus.LOADED.value);
        }
    }
}
