package de.kordelle.radio.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.commons.lang.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.Serializable;

import de.kordelle.radio.logging.LogHelper;

/**
 * Created by thomas.kordelle on 16.09.16.
 */
public final class SettingsHolder
{
    public static final String DEFAULT_PLAYLIST_URI = "http://droid.kordelle.de/tkradio/playlist.pls";
    public static final String KEY_SETTINGS = "Settings";
    public static final String KEY_START_PLAY_BLUETOOTH = "startPlayingOnBTConnected";
    public static final String KEY_START_AFTER_BOOT = "startAfterBoot";
    public static final String KEY_PLAYLIST = "playlistUri";
    public static final String KEY_PLAY_LAST_STATION = "playLastStation";
    public static final String KEY_LAST_STATION = "lastStation";

    /**
     *
     * @param <T>
     */
    public final class SettingsChangeData<T> {
        private final String key;
        private final T value;
        public SettingsChangeData(final String key, final T value) {
            this.key = key;
            this.value = value;
        }
        public String key() {
            return key;
        }
        public T value() {
            return value;
        }
    }

    /**
     *
     */
    private final class SettingsAccessor
    {
        private static final String TAG = "SettingsAccessor";

        private Context context;
        private SharedPreferences  prefs;

        private SettingsAccessor(Context context)
        {
            this.context = context;
            this.prefs = context.getSharedPreferences(KEY_SETTINGS, Context.MODE_PRIVATE);
        }

        public String playlist()
        {
            return prefs.getString(KEY_PLAYLIST, DEFAULT_PLAYLIST_URI);
        }

        public void playlist(final String url)
        {
            storeSetting(KEY_PLAYLIST, url);
        }

        public Boolean playOnBTConnected()
        {
            return prefs.getBoolean(KEY_START_PLAY_BLUETOOTH, Boolean.FALSE);
        }

        public void playOnBTConnected(final Boolean flag)
        {
            storeSetting(KEY_START_PLAY_BLUETOOTH, flag);
        }

        public  Boolean startAfterBoot()
        {
            return prefs.getBoolean(KEY_START_AFTER_BOOT, Boolean.FALSE);
        }

        public void startAfterBoot(final Boolean flag)
        {
            storeSetting(KEY_START_AFTER_BOOT, flag);
        }

        public  Boolean playLastStation()
        {
            return prefs.getBoolean(KEY_PLAY_LAST_STATION, Boolean.FALSE);
        }

        public void playLastStation(final Boolean flag)
        {
            storeSetting(KEY_PLAY_LAST_STATION, flag);
        }

        public String lastStation()
        {
            return prefs.getString(KEY_LAST_STATION, StringUtils.EMPTY);
        }

        public void lastStation(final String name)
        {
            storeSetting(KEY_LAST_STATION, name);
        }

        private <T extends Serializable> void storeSetting(final String key, T value)
        {
            SharedPreferences.Editor editor = prefs.edit();

            if (value instanceof String)
                editor.putString(key, (String)value);
            else if (value instanceof  Boolean)
                editor.putBoolean(key, (Boolean)value);
            else
                LogHelper.e(TAG, String.format("Cannot save settings for '%s' as class '%s'.", key, value.getClass()));

            editor.commit();

            notify(key, value);
        }

        /**
         * notify async all listener about settings change
         * @param key
         * @param value
         */
        private <T extends Serializable> void notify(final String key, final T value)
        {
            SettingsChangeData<T> data = new SettingsChangeData<T>(key, value);
            EventBus.getDefault().post(data);
        }
    }

    /**
     *
     */
    private static SettingsAccessor instance = null;

    public static void init(final Context context)
    {
        instance = new SettingsHolder().new SettingsAccessor(context);
    }

    public static String playlist()
    {
        return instance.playlist();
    }

    public static void playlist(final String url)
    {
        instance.playlist(url);
    }

    public static boolean playOnBTConnected()
    {
        return instance.playOnBTConnected();
    }

    public static void playOnBTConnected(final boolean flag)
    {
        instance.playOnBTConnected(flag);
    }

    public static boolean startAfterBoot()
    {
        return instance.startAfterBoot();
    }

    public static void startAfterBoot(final boolean flag)
    {
        instance.startAfterBoot(flag);
    }

    public static boolean playLastStation()
    {
        return instance.playLastStation();
    }

    public static void playLastStation(final boolean flag)
    {
        instance.playLastStation(flag);
    }

    public static String lastStation()
    {
        return instance.lastStation();
    }

    public static void lastStation(final String name)
    {
        instance.lastStation(name);
    }
}
