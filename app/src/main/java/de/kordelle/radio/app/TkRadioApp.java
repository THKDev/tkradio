package de.kordelle.radio.app;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import de.kordelle.radio.R;
import de.kordelle.radio.logging.LogHelper;

/**
 * Created by thomask on 25.09.16.
 */
@ReportsCrashes(mailTo = "", mode = ReportingInteractionMode.TOAST, resToastText = R.string.error_unknow)
public class TkRadioApp extends Application
{
    private static final String TAG = TkRadioApp.class.getCanonicalName();

    private boolean firstStart = true;
    private boolean isInBackground = true;

    @Override
    public void onCreate()
    {
        super.onCreate();
        // init Automated Android Crash Reports
        ACRA.init(this);
    }

    /**
     * detect is application is in background.
     *
     * @param level
     */
    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_UI_HIDDEN)
        {
            LogHelper.d(TAG, "Application is in background now.");
            isInBackground = true;
        }
    }

    public boolean firstStart()
    {
        return firstStart;
    }

    public void firstStart(boolean firstStart)
    {
        this.firstStart = firstStart;
    }

    public boolean inBackground()
    {
        return isInBackground;
    }

    public void inBackground(boolean inBackground)
    {
        isInBackground = inBackground;
    }
}
