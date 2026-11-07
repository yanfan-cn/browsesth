package com.haleydu.cimoc;

import com.haleydu.cimoc.manager.PreferenceManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Minimal Cimoc App shim for browsesth SPEC-016 adapters.
 */
public final class App {
    private static OkHttpClient httpClient;
    private static final PreferenceManager PREFS = new PreferenceManager();

    private App() {}

    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();
        }
        return httpClient;
    }

    public static PreferenceManager getPreferenceManager() {
        return PREFS;
    }
}
