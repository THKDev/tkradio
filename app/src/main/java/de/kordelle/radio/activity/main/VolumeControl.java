package de.kordelle.radio.activity.main;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.widget.SeekBar;

import de.kordelle.radio.logging.LogHelper;

/**
 * Created by thomas.kordelle on 20.09.16.
 */
public class VolumeControl implements SeekBar.OnSeekBarChangeListener
{
    private static final transient String TAG = VolumeControl.class.getSimpleName();

    private Context context;
    private AudioManager audioManager;
    private int streamMaxVolume;
    private int lastVolume;
    private final SeekBar volumeSlider;

    private final ContentObserver volumeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            LogHelper.d(TAG, "SettingsContentObserver::onChange(" + selfChange + ")");
            super.onChange(selfChange);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (lastVolume != currentVolume) {
                volumeSlider.setProgress(volume());
            }
        }
    };

    public VolumeControl(final SeekBar volumeSlider)
    {
        this.context = volumeSlider.getContext();
        this.volumeSlider = volumeSlider;
        this.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        this.streamMaxVolume = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public void onCreate()
    {
        this.volumeSlider.setMax(maxVolume());
        this.volumeSlider.setProgress(volume());
        this.volumeSlider.setOnSeekBarChangeListener(this);
        this.context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeObserver);
    }

    public void onDestroy()
    {
        this.context.getContentResolver().unregisterContentObserver(volumeObserver);
    }

    /**
     *
     * @return
     */
    public int maxVolume()
    {
        return streamMaxVolume;
    }

    /**
     *
     * @return
     */
    public int volume()
    {
        lastVolume = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return lastVolume;
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser)
    {
        if (progress > streamMaxVolume)
            lastVolume = streamMaxVolume;
        else if (progress < 0)
            lastVolume = 0;
        else
            lastVolume = progress;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolume, AudioManager.FLAG_VIBRATE);
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar)
    {  }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar)
    {  }
}
