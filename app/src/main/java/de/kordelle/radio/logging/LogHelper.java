package de.kordelle.radio.logging;

import android.util.Log;

import de.kordelle.radio.BuildConfig;

/**
 * Created by thomask on 20.09.16.
 */
public class LogHelper
{
    public static int v(String tag, String msg)
    {
        if (BuildConfig.DEBUG)
            return Log.v(tag, msg);

        return 0;
    }

    public static int v(String tag, String msg, Throwable tr)
    {
        if (BuildConfig.DEBUG)
            return Log.v(tag, msg, tr);

        return 0;
    }

    public static int d(String tag, String msg)
    {
        if (BuildConfig.DEBUG)
            return Log.d(tag, msg);

        return 0;
    }

    public static int d(String tag, String msg, Throwable tr)
    {
        if (BuildConfig.DEBUG)
            return Log.d(tag, msg, tr);

        return 0;
    }

    public static int i(String tag, String msg)
    {
        return Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr)
    {
        return Log.i(tag, msg, tr);
    }

    public static int w(String tag, String msg)
    {
        return Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr)
    {
        return Log.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr)
    {
        return Log.w(tag, tr);
    }

    public static int e(String tag, String msg)
    {
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr)
    {
        return Log.e(tag, msg, tr);
    }

    public static int wtf(String tag, String msg)
    {
        return Log.wtf(tag, msg);
    }

    public static int wtf(String tag, Throwable tr)
    {
        return Log.wtf(tag, tr);
    }

    public static int wtf(String tag, String msg, Throwable tr)
    {
        return Log.wtf(tag, msg, tr);
    }

    public static boolean isLoggable(String s, int i)
    {
        return Log.isLoggable(s, i);
    }
}
