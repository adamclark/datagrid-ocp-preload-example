package org.acme.cache;

public class CacheConstants {

    public static final String LOAD_STATUS_CACHE_KEY = "LOAD_STATUS";
    public static final String LOADER_HOSTNAME_CACHE_KEY = "LOADER_HOSTNAME";

    public enum LoadStatus {
        LOADING("LOADING"), LOADED("LOADED");
        
        public final String value;

        private LoadStatus(String value) {
            this.value = value;
        }
    }
}