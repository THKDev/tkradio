package de.kordelle.radio.service;

import android.app.Service;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.kordelle.radio.data.RadioStation;
import de.kordelle.radio.data.RadioStationComparator;
import de.kordelle.radio.data.settings.SettingsHolder;
import de.kordelle.radio.logging.LogHelper;
import de.kordelle.radio.receiver.MediaEventBroadcastReceiver;
import de.kordelle.radio.util.CircleList;

/**
 * Created by thomas.kordelle on 19.09.16.
 */
public class MediaPlayerService extends Service
                                implements MediaPlayer.OnBufferingUpdateListener,
                                           MediaPlayer.OnErrorListener,
                                           MediaPlayer.OnInfoListener,
                                           MediaPlayer.OnPreparedListener,
                                           MediaPlayer.OnCompletionListener,
                                           MediaPlayer.OnSeekCompleteListener
{
    private static final String TAG = MediaPlayerService.class.getSimpleName();

    public enum MediaPlayerState {
        UNKNOWN,
        STOPPED,
        PLAYING,
        PREPARING,
        BUFFERING,
        ERROR
    }

    public enum ChangeType {
        STATE_CHANGED,
        BUFFER_UPDATE,
        STATION_CHANGED
    }

    /**
     * value object to transfer by {@link EventBus}
     */
    public class ChangedData<T> {
        private ChangeType type;
        private MediaPlayerState state;
        private T data;

        public ChangedData(final ChangeType type) {
            this.type = type;
        }

        public ChangeType type() {
            return this.type;
        }
        public MediaPlayerState state() {
            return this.state;
        }
        public T data() {
            return this.data;
        }
    }

    /**
     * simple binder to return an instance of the service
     * @link https://developer.android.com/guide/components/bound-services.html
     */
    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
    private final IBinder mediaBinder = new MediaPlayerBinder();

    /**
     * if network is to slow or has misfires wait some time and then restart media play.
     * task will be canceled if media play can recover by self within the time frame.
     */
    private static final long BUFFER_RESTART_DELAY = 10 * 1000; // wait 10 seconds before restart mediaplayer
    private Runnable reanimateMediaPlay = new Runnable() {
        @Override
        public void run() {
            try {
                LogHelper.i(TAG, "Try to reanimate media player.");
                stop();
                Thread.sleep(500);
                start();
            }
            catch (Exception ex) {
                LogHelper.e(TAG, "Unable to reanimate media player.", ex);
                state(MediaPlayerState.ERROR, ex);
            }
        }
    };

    /////////////////////////////////////////////////////////////////
    private MediaPlayer  mediaPlayer = null;
    private WifiManager.WifiLock  wifiLock = null;
    private MediaPlayerState playerState = MediaPlayerState.UNKNOWN;
    private RadioStation currentStation;
    private Handler bufferingTimer;
    private ComponentName componentNameMediaButton;
    private CircleList<RadioStation> stationList = new CircleList<>();

    public MediaPlayerService() {
        super();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        bufferingTimer = new Handler();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        playerState = MediaPlayerState.STOPPED;

        registerForMediaButtons();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy()
    {
        stop();
        mediaPlayer.release();
        unregisterForMediaButtons();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void registerForMediaButtons()
    {
        componentNameMediaButton = new ComponentName(getPackageName(), MediaEventBroadcastReceiver.class.getName());
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        am.registerMediaButtonEventReceiver(componentNameMediaButton);
    }

    private void unregisterForMediaButtons()
    {
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        am.unregisterMediaButtonEventReceiver(componentNameMediaButton);
    }

    /**
     * @see Service#onBind(Intent)
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mediaBinder;
    }

    /**
     *
     * @return
     */
    public RadioStation currentStation()
    {
        return currentStation;
    }

    /**
     *
     * @return
     */
    public MediaPlayerState state()
    {
        return playerState;
    }

    /**
     * notify all listener by {@link EventBus}
     * @param type
     * @param value
     * @param <V>
     */
    private <V> void notify(final ChangeType type, final V value)
    {
        ChangedData data = new ChangedData<V>(type);
        data.state = state();
        data.data = value;

        EventBus.getDefault().post(data);
    }

    /**
     *
     * @param newState
     * @param value
     */
    private <V> void state(final MediaPlayerState newState, final V value)
    {
        this.playerState = newState;
        notify(ChangeType.STATE_CHANGED, value);
    }

    /**
     * has player a state of {@link MediaPlayerState#PLAYING} or {@link MediaPlayerState#PREPARING} or
     * {@link MediaPlayerState#BUFFERING}
     * @return
     */
    public boolean playing()
    {
        return (state() == MediaPlayerState.PLAYING ||
                state() == MediaPlayerState.PREPARING ||
                state() == MediaPlayerState.BUFFERING);
    }

    /**
     * set current list with all radio stations. so it is possible to change station in background by media buttons
     * @param stationList
     */
    public void stationList(final List<RadioStation> stationList)
    {
        this.stationList = new CircleList<>(stationList);
    }

    /**
     *
     * @param station
     * @return
     */
    public void changeStation(final RadioStation station)
    {
        LogHelper.d(TAG, "change station to " + station);
        currentStation = station;
        notify(ChangeType.STATION_CHANGED, currentStation);
    }

    /**
     *
     * @param title
     */
    public void changeStation(final String title)
    {
        for (final RadioStation item : stationList)
        {
            if (item.getTitle().equals(title))
            {
                changeStation(item);
                break;
            }
        }
    }

    /**
     *
     * @return
     */
    public boolean start()
    {
        try
        {
            if ((currentStation == null) && !stationList.isEmpty())
                changeStation(stationList.get(0));

            if (!playing() && currentStation != null)
            {
                aquireWifiLock();

                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(getBaseContext(), currentStation.getUri());
                mediaPlayer.setWakeMode(getBaseContext(), PowerManager.PARTIAL_WAKE_LOCK);

                mediaPlayer.prepareAsync();

                state(MediaPlayerState.PREPARING, null);

                return true;
            }
        }
        catch (Exception ex)
        {
            LogHelper.e(TAG, "Error while starting media player.", ex);
            state(MediaPlayerState.ERROR, ex);
        }
        return false;
    }

    /**
     *
     */
    public boolean stop()
    {
        try
        {
            if (state() == MediaPlayerState.PLAYING || state() == MediaPlayerState.PREPARING || state() == MediaPlayerState.BUFFERING) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                state(MediaPlayerState.STOPPED, null);
            }
            releaseWifiLock();
            return true;
        }
        catch (Exception ex)
        {
            LogHelper.e(TAG, "Error while stopping media player.", ex);
            state(MediaPlayerState.ERROR, ex);
        }
        return false;
    }

    public void next()
    {

    }

    /**
     *
     */
    private void aquireWifiLock()
    {
        if (wifiLock == null)
        {
            WifiManager wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null)
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, getBaseContext().getApplicationInfo().name);
        }
        if (!wifiLock.isHeld())
        {
            try
            {
                wifiLock.acquire();
            }
            catch (NullPointerException ex)
            {   // on NVidia Shield, first aquire will fail with a NullPointerException
                wifiLock.acquire();
            }
        }
    }

    /**
     *
     */
    private void releaseWifiLock()
    {
        if ((wifiLock != null) && wifiLock.isHeld())
        {
            wifiLock.release();
            wifiLock = null;
        }
    }


    /**
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(MediaPlayer)
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        LogHelper.d(TAG, "start playing of media.");
        try
        {
            mediaPlayer.start();
            state(MediaPlayerState.PLAYING, null);
        }
        catch (Exception ex)
        {
            LogHelper.e(TAG, "Error starting playback", ex);
            state(MediaPlayerState.ERROR, ex);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        LogHelper.d(TAG, "On completion");
    }

    @Override
    public void onSeekComplete(MediaPlayer mp)
    {
        LogHelper.d(TAG, "On seek complete");
    }

    /**
     * @see android.media.MediaPlayer.OnErrorListener#onError(MediaPlayer, int, int)
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        LogHelper.d(TAG, String.format("What: %d, Extra: %d", what, extra));

        try
        {
            releaseWifiLock();
            mp.stop();
            mp.reset();

            state(MediaPlayerState.STOPPED, String.format("What: %d, Extra: %d", what, extra));
        }
        catch (Exception ex)
        {
            LogHelper.e(TAG, "Error while handling media player error.", ex);
            state(MediaPlayerState.ERROR, ex);
        }

        return true;
    }

    /**
     * @see android.media.MediaPlayer.OnInfoListener#onInfo(MediaPlayer, int, int)
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra)
    {
        LogHelper.d(TAG, String.format("What: %d, Extra: %d", what, extra));

        switch (what)
        {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                LogHelper.i(TAG, "Starting buffer timer task.");
                state(MediaPlayerState.BUFFERING, null);
                bufferingTimer.postDelayed(reanimateMediaPlay, BUFFER_RESTART_DELAY);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                LogHelper.i(TAG, "Cancel buffer timer task.");
                bufferingTimer.removeCallbacks(reanimateMediaPlay);
                state(MediaPlayerState.PLAYING, null);
                break;
            default:
                MediaPlayer.TrackInfo[] trackInfos = mediaPlayer.getTrackInfo();
                for (MediaPlayer.TrackInfo ti : trackInfos) {
                    LogHelper.d(TAG, "TrackInfo: " + ti.toString());
                    MediaFormat fmt = ti.getFormat();
                    if (fmt != null) {
                        LogHelper.d(TAG, "Bitrate: " + fmt.getInteger(MediaFormat.KEY_BIT_RATE));
                        LogHelper.d(TAG, "Channel: " + fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                    }
                }
                break;
        }

        return true;
    }

    /* update buffer is called on some devices very often. Avoid notify if nothing has changed. */
    private int lastBufferState = -1;
    /**
     * @see android.media.MediaPlayer.OnBufferingUpdateListener#onBufferingUpdate(MediaPlayer, int)
     * @param mp
     * @param percent
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent)
    {
        if (percent < 0 || percent > 100) {
            percent = (int) Math.round((((Math.abs(percent)-1)*100.0/Integer.MAX_VALUE)));
        }
        if (lastBufferState != percent) {
            lastBufferState = percent;
            notify(ChangeType.BUFFER_UPDATE, String.format("Buffer: %d %%", percent));
        }
    }

    /**
     * called by {@link EventBus} at {@link MediaEventBroadcastReceiver#notify(Object)}
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onAudioBecomeNoisyEvent(final MediaEventBroadcastReceiver.AudioBecomeNoisyEvent event)
    {
        stop();
    }

    /**
     * called by {@link EventBus} at {@link MediaEventBroadcastReceiver#notify(Object)}
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onBluetoothConnectionChangedEvent(final MediaEventBroadcastReceiver.BluetoothConnectChangedEvent event)
    {
        switch (event.state())
        {
            case BluetoothProfile.STATE_DISCONNECTED:
                stop();
                break;
            case BluetoothProfile.STATE_CONNECTED:
                if (SettingsHolder.playOnBTConnected())
                    start();
                break;
            default:
                break;
        }
    }

    /**
     * called by {@link EventBus} at {@link MediaEventBroadcastReceiver#notify(Object)}
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMediaKeyEvent(final MediaEventBroadcastReceiver.MediaButtonEvent event)
    {
        if (event.keyEvent().getAction() == KeyEvent.ACTION_UP)
        {
            switch (event.keyEvent().getKeyCode())
            {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    start();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    stop();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    stop();
                    changeStation(stationList.nextOf(currentStation, new RadioStationComparator()));
                    start();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    stop();
                    changeStation(stationList.previousOf(currentStation, new RadioStationComparator()));
                    start();
                    break;
                default:
                    break;
            }
        }
    }
}
