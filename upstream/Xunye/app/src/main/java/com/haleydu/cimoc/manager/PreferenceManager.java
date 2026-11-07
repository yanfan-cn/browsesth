package com.haleydu.cimoc.manager;

import java.util.HashMap;
import java.util.Map;

/** In-memory prefs shim (HHAAZZ/MH50 baseUrl keys). */
public class PreferenceManager {
    public static final String PREF_HHAAZZ_BASEURL = "PREF_HHAAZZ_BASEURL";
    public static final String PREF_HHAAZZ_SW = "PREF_HHAAZZ_SW";
    public static final String PREFERENCES_MH50_KEY_MSG = "PREFERENCES_MH50_KEY_MSG";
    public static final String PREFERENCES_MH50_IV_MSG = "PREFERENCES_MH50_IV_MSG";

    private final Map<String, String> map = new HashMap<>();

    public String getString(String key, String def) {
        String v = map.get(key);
        return v != null ? v : def;
    }

    public void putString(String key, String value) {
        map.put(key, value);
    }
}
