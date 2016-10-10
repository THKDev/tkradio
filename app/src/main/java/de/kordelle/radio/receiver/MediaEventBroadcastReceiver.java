package de.kordelle.radio.receiver;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;

import de.kordelle.radio.logging.LogHelper;

/**
 * @link https://developer.android.com/guide/topics/media/mediaplayer.html#noisyintent
 */
public class MediaEventBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG = MediaEventBroadcastReceiver.class.getSimpleName();

    /** event type for {@link AudioManager#ACTION_AUDIO_BECOMING_NOISY} */
    public class AudioBecomeNoisyEvent { }

    /** event type for {@link BluetoothA2dp#ACTION_CONNECTION_STATE_CHANGED} */
    public class BluetoothConnectChangedEvent {
        private final int state;
        private final int previousState;
        public BluetoothConnectChangedEvent(final Bundle extras) {
            this.state = extras.getInt(BluetoothProfile.EXTRA_STATE, -1);
            this.previousState = extras.getInt(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
        }
        public int state() {
            return state;
        }
        public int previousState() {
            return previousState;
        }
    }

    /** event type for {@link Intent#ACTION_MEDIA_BUTTON} */
    public class MediaButtonEvent {
        private final KeyEvent  keyEvent;
        public MediaButtonEvent(final Bundle extras)        {
            this.keyEvent = (KeyEvent)extras.get(Intent.EXTRA_KEY_EVENT);
        }
        public KeyEvent keyEvent() {
            return keyEvent;
        }
    }

    /**
     * defined in AndroidManifest.xml
     * {@link BroadcastReceiver#onReceive(Context, Intent)}
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        LogHelper.d(TAG, "Got intent with action: " + action);

        if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            notify(new AudioBecomeNoisyEvent());
        else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED))
            notify(new BluetoothConnectChangedEvent(intent.getExtras()));
        else if (action.equals(Intent.ACTION_MEDIA_BUTTON))
            notify(new MediaButtonEvent(intent.getExtras()));
    }

    /**
     * send event via {@link EventBus}
     * @param event
     * @param <T>
     */
    private <T> void notify(final T event)
    {
        EventBus.getDefault().post(event);
    }
}
