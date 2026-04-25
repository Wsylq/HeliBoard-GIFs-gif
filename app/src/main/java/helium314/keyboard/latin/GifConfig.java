package helium314.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuration for Klipy GIF support.
 */
public class GifConfig {
    /**
     * Returns the Klipy API key to use, preferring the user-provided key if present.
     */
    public static String getTenorApiKey(Context ctx) {
        String userKey = GifPrefs.getStoredApiKey(ctx);
        if (userKey != null && !userKey.trim().isEmpty()) {
            return userKey.trim();
        }
        return BuildConfig.KLIPY_API_KEY;
    }
    /**
     * Deprecated: use getTenorApiKey(Context) instead.
     */
    @Deprecated
    public static String getTenorApiKey() {
        return BuildConfig.KLIPY_API_KEY;
    }
}