package de.kordelle.radio.data;

import android.net.Uri;

/**
 *
 */
public class RadioStation
{
    private String title;
    private Uri uri;
    private Integer length;

    public RadioStation(final String title, final Uri uri, final Integer length)
    {
        this.title = title;
        this.uri = uri;
        this.length = length;
    }

    public String getTitle()
    {
        return title;
    }

    public Uri getUri()
    {
        return uri;
    }

    public Integer getLength()
    {
        return length;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getCanonicalName()).append("[");
        sb.append("Name:").append(getTitle()).append(",URI:").append(getUri()).append("]");
        return sb.toString();
    }
}
