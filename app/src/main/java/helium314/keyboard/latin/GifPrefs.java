package helium314.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferences for Klipy GIF support.
 */
public final class GifPrefs {
    public static final String PREFS_NAME = "heli_keyboard_prefs";
    public static final String KEY_TENOR_ENABLED = "tenor_enabled";   // kept for backwards compat
    public static final String KEY_TENOR_API_KEY = "tenor_api_key";   // kept for backwards compat
    /**
     * Preference key for GIF share size.
     * Values: "tinygif", "mediumgif", "gif" (original).
     */
    public static final String KEY_GIF_SHARE_SIZE = "gif_share_size";
    /**
     * Default GIF share size: original GIF.
     */
    public static final String DEFAULT_GIF_SHARE_SIZE = "auto";

    private GifPrefs() { /* no instance */ }

    public static boolean isTenorEnabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_TENOR_ENABLED, false);
    }

    public static void setTenorEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TENOR_ENABLED, enabled).apply();
    }

    public static String getStoredApiKey(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TENOR_API_KEY, "");
    }

    public static void setApiKey(Context ctx, String key) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TENOR_API_KEY, key).apply();
    }
    
    /**
     * Get the preferred GIF share size.
     * @param ctx context
     * @return one of "tinygif", "mediumgif", or "gif"
     */
    public static String getShareSize(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_GIF_SHARE_SIZE, DEFAULT_GIF_SHARE_SIZE);
    }
    
    /**
     * Set the preferred GIF share size.
     * @param ctx context
     * @param size one of "tinygif", "mediumgif", or "gif"
     */
    public static void setShareSize(Context ctx, String size) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GIF_SHARE_SIZE, size).apply();
    }
}