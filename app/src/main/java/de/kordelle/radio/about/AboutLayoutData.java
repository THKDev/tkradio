package de.kordelle.radio.about;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import de.kordelle.radio.BuildConfig;

/**
 * Created by thomask on 05.10.16.
 */
public class AboutLayoutData extends BaseObservable
{
    private static final String GITHUB_REPRO = "https://github.com/THKDev/tkradio";

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

    @Bindable
    public String getRepository()
    {
        return GITHUB_REPRO;
    }
}
