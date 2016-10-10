package de.kordelle.radio.about;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import de.kordelle.radio.BuildConfig;

/**
 * Created by thomask on 05.10.16.
 */
public class AboutLayoutData extends BaseObservable
{
    @Bindable
    public String getVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Bindable
    public String getBuildType()
    {
        return BuildConfig.BUILD_TYPE;
    }
}
