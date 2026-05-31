package com.diploma.communicator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Клас для спрощеного доступу до локального сховища SharedPreferences.
 * Зберігає конфігураційні налаштування та дані для авторизації.
 */
public class SharedPrefs {
    private static final String PREFS_NAME = "CommunicatorPrefs";
    private static final String DEF_BASE_URL = "http://192.168.1.102:8080/";
    private static final String DEF_SERVICE_UUID = "46a2774b-c1b9-41ad-a55f-9c21e91f4aeb";
    private static final String DEF_CHAR_UUID = "8ec5ad34-2ce2-4cdb-a700-4d1de01870af";
    private static final String DEF_USERNAME = "";
    private static final String DEF_PASSWORD = "";

    /**
     * Повертає об'єкт для доступу до локального сховища застосунку.
     *
     * @param context контекст застосунку
     * @return екземпляр SharedPreferences
     */
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context context) {
        return getPrefs(context).getString("BASE_URL", DEF_BASE_URL);
    }

    public static void setBaseUrl(Context context, String url) {
        getPrefs(context).edit().putString("BASE_URL", url).apply();
    }

    public static String getUsername(Context context) {
        return getPrefs(context).getString("USERNAME", DEF_USERNAME);
    }

    public static void setUsername(Context context, String user) {
        getPrefs(context).edit().putString("USERNAME", user).apply();
    }

    public static String getPassword(Context context) {
        return getPrefs(context).getString("PASSWORD", DEF_PASSWORD);
    }

    public static void setPassword(Context context, String pass) {
        getPrefs(context).edit().putString("PASSWORD", pass).apply();
    }

    public static String getServiceUuid(Context context) {
        return getPrefs(context).getString("SERVICE_UUID", DEF_SERVICE_UUID);
    }

    public static void setServiceUuid(Context context, String uuid) {
        getPrefs(context).edit().putString("SERVICE_UUID", uuid).apply();
    }

    public static String getCharUuid(Context context) {
        return getPrefs(context).getString("CHAR_UUID", DEF_CHAR_UUID);
    }

    public static void setCharUuid(Context context, String uuid) {
        getPrefs(context).edit().putString("CHAR_UUID", uuid).apply();
    }
}
