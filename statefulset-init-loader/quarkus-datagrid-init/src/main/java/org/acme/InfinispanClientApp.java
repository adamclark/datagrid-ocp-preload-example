package org.acme;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.logging.Log;
import org.infinispan.client.hotrod.RemoteCache;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class InfinispanClientApp {

    @Inject
    @Remote("preload-control-cache")
    RemoteCache<String, Boolean> preloadControlCache;

    @Inject
    @Remote("greetings-cache")
    RemoteCache<String, String> greetingsCache;

    void onStart(@Observes StartupEvent ev) {

        Log.info("Checking if cache is already preloaded...");
        Boolean loaded = preloadControlCache.get("loaded");

        if(loaded == null || !loaded) {
            Log.info("Preloading cache...");
            greetingsCache.put("hello1", "Hello1 World, Infinispan is up!");
            greetingsCache.put("hello2", "Hello2 World, Infinispan is up!");
            greetingsCache.put("hello3", "Hello3 World, Infinispan is up!");

            Log.info("Sleeping to simulate cache preloading...");
            try {
                Thread.sleep(30000);
            } catch(InterruptedException ex) {}

            Log.info("Setting preloaded to true...");
            preloadControlCache.put("loaded", true);
        }

        Quarkus.asyncExit();
    }
}
