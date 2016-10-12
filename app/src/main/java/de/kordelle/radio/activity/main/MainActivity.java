package de.kordelle.radio.activity.main;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import de.kordelle.radio.R;
import de.kordelle.radio.about.AboutDialog;
import de.kordelle.radio.activity.settings.SettingsActivity;
import de.kordelle.radio.app.TkRadioApp;
import de.kordelle.radio.data.RadioStation;
import de.kordelle.radio.data.settings.SettingsHolder;
import de.kordelle.radio.http.DownLoadPlayList;
import de.kordelle.radio.logging.LogHelper;
import de.kordelle.radio.receiver.BootupReceiver;
import de.kordelle.radio.service.MediaPlayerService;

/**
 *
 */
public final class MainActivity extends AppCompatActivity
                                implements RadioStationListViewAdapter.OnListItemSelectionListener,
                                           DownLoadPlayList.DownLoadPlayListListener,
                                           ViewTreeObserver.OnGlobalLayoutListener
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static String STRING_MISSING = "Missing";

    private List<RadioStation>  stationList = new ArrayList<>();

    private Unbinder butterUnbinder;
    @BindView(R.id.radio_list)
    protected RecyclerView  radioListView;
    @BindView(R.id.toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.slider_volume)
    protected SeekBar volumeSlider;
    @BindView(R.id.fab)
    protected FloatingActionButton fab;
    @BindView(R.id.fabProgressBar)
    protected ProgressBar fabProgressBar;
    @BindView(R.id.channel_name)
    protected TextView channelName;
    @BindView(R.id.status_message)
    protected TextView statusMessage;
    @BindView(R.id.bufferstate)
    protected TextView bufferState;
    @BindView(R.id.loading_progress)
    protected View loadingProgress;

    // used to wait for network is settled
    private boolean startAfterBooting = false;
    private boolean changedSelectionProgrammaticly = false;
    private RadioStationListViewAdapter listAdapter;
    private AsyncTask<String, Void, String> downloadTask;
    private MediaPlayerService mediaPlayerController;
    private VolumeControl volControl;

    /**
     * Binder to the background service
     */
    private Intent mediaServiceIntent;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaPlayerController = ((MediaPlayerService.MediaPlayerBinder)service).getService();
            if (stationList.isEmpty())
                downloadPlayList(startAfterBooting);
            else
                playLastStation();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaPlayerController = null;
        }
    };

    /**
     * delay selection. {@link RecyclerView} must first scroll and update the UI before the selected
     * entry can be highlighted.
     */
    private class DelayedSelection implements Runnable {
        private final int pos;
        private final RecyclerView  recyclerView;
        /** */
        public DelayedSelection(final int pos, final RecyclerView recyclerView) {
            this.pos = pos;
            this.recyclerView = recyclerView;
        }
        @Override
        public void run() {
            RecyclerView.ViewHolder vHolder = recyclerView.findViewHolderForAdapterPosition(pos);
            if (vHolder != null) {
                vHolder.itemView.performClick();
            }
        }
    }

    /**
     * @see AppCompatActivity#onCreate(Bundle, PersistableBundle)
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        try
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            butterUnbinder = ButterKnife.bind(this);
            setSupportActionBar(toolbar);
            SettingsHolder.init(getBaseContext());

            startMediaService();

            if (listAdapter == null)
            {
                listAdapter = new RadioStationListViewAdapter(stationList, this);
                radioListView.setAdapter(listAdapter);

                volControl = new VolumeControl(volumeSlider);
                volControl.onCreate();
            }

            EventBus.getDefault().register(this);

            startAfterBooting = getIntent().getBooleanExtra(BootupReceiver.KEY_CALLED_DURING_BOOT, false);
        }
        catch (Exception ex)
        {
            LogHelper.e(TAG, "Unable to create main activity.", ex);
        }
    }

    private void startMediaService()
    {
        if (mediaServiceIntent == null)
            mediaServiceIntent = new Intent(this, MediaPlayerService.class);

        if (bindService(mediaServiceIntent, serviceConnection, BIND_AUTO_CREATE))
            LogHelper.i(TAG, "Media service startet. ");
        else
            LogHelper.e(TAG, "Failed to bind service.");

    }

    /**
     * @see AppCompatActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        volControl.onDestroy();
        unbindService(serviceConnection);
        butterUnbinder.unbind();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!stationList.isEmpty())
            playLastStation();
    }

    /**
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed()
    {
        moveTaskToBack(false);
    }

    /**
     *
     * @param duringBoot
     */
    private void downloadPlayList(final boolean duringBoot)
    {
        if (downloadTask != null)
            downloadTask.cancel(true);

        loadingProgress.setVisibility(View.VISIBLE);

        LogHelper.d(TAG, "During boot: " + duringBoot);
        downloadTask = new DownLoadPlayList(getBaseContext(), this, duringBoot).execute(SettingsHolder.playlist());
    }

    @OnClick(R.id.fab)
    protected void onFabClick()
    {
        onPlayButtonClick(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handle action bar item clicks here. The action bar will
     * automatically handle clicks on the Home/Up button, so long
     * as you specify a parent activity in AndroidManifest.xml.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_reload_playlist:
                downloadPlayList(false);
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_about:
                new AboutDialog(this).show();
                break;
            case R.id.action_exit:
                finishAffinity();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * {@link de.kordelle.radio.activity.main.RadioStationListViewAdapter.ViewHolder#ViewHolder(View, RadioStationListViewAdapter.OnListItemSelectionListener)}
     * @param item
     */
    @Override
    public void onSelection(final RadioStation item)
    {
        if (!changedSelectionProgrammaticly)
           onPlayButtonClick(true);
        else
            changedSelectionProgrammaticly = false;

        if (mediaPlayerController.currentStation() == null)
           mediaPlayerController.changeStation(item);
    }

    /**
     *
     * @param is
     * @throws IOException
     */
    private void readPlayList(final InputStream is)
    {
        try
        {
            stationList.clear();

            Ini iniFile = new Ini(is);
            Profile.Section sec = iniFile.get("playlist");
            Integer numberOf = Integer.valueOf(sec.get("NumberOfEntries", "0"));
            Boolean isLocal = Boolean.parseBoolean(sec.get("IsLocal", Boolean.FALSE.toString()));

            for (int i = 1; i <= numberOf; i++)
            {
                try
                {
                    Uri uri = Uri.parse(new URL(sec.get(String.format("File%d", i), StringUtils.EMPTY)).toURI().toString());
                    String title = sec.get(String.format("Title%d", i), STRING_MISSING);
                    Integer length = Integer.valueOf(sec.get(String.format("Length%d", i), "-1"));
                    stationList.add(new RadioStation(title, uri, length));
                }
                catch (MalformedURLException | URISyntaxException ex)
                {
                    LogHelper.e(TAG, String.format("Found invalid URL '%s'.", sec.get(String.format("File%d", i), StringUtils.EMPTY)), ex);
                }
            }

            Collections.sort(stationList, new Comparator<RadioStation>() {
                @Override
                public int compare(RadioStation lhs, RadioStation rhs) {
                    return lhs.getTitle().compareTo(rhs.getTitle());
                }
            });

            mediaPlayerController.stationList(stationList);
            listAdapter.notifyDataSetChanged();

            if (Boolean.FALSE.equals(isLocal))
                DownLoadPlayList.storePlayList(getBaseContext(), is);
        }
        catch (IOException ex)
        {
            LogHelper.e(TAG, "Error while parsing playlist file.", ex);
        }
        finally
        {
            IOUtils.closeQuietly(is);
            loadingProgress.setVisibility(View.GONE);
        }
    }

    /**
     *
     * @param forcePlaying
     */
    public void onPlayButtonClick(boolean forcePlaying)
    {
        final boolean startPlaying = forcePlaying ||
                                     ((mediaPlayerController.state() != MediaPlayerService.MediaPlayerState.PLAYING) &&
                                      (mediaPlayerController.state() != MediaPlayerService.MediaPlayerState.PREPARING));

        mediaPlayerController.stop();

        mediaPlayerController.changeStation(listAdapter.getSelectedItem());

        if (startPlaying)
            mediaPlayerController.start();
    }

    /**
     * @see de.kordelle.radio.http.DownLoadPlayList.DownLoadPlayListListener#onPlayListLoaded(InputStream)
     * @param content
     */
    @Override
    public void onPlayListLoaded(final InputStream content)
    {
        radioListView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        readPlayList(content);
        playLastStation();
    }

    /**
     *
     */
    private void playLastStation()
    {
        final TkRadioApp app = (TkRadioApp)getApplication();
        if (!mediaPlayerController.playing() && SettingsHolder.playLastStation() && app.inBackground())
        {
            mediaPlayerController.changeStation(SettingsHolder.lastStation());
            mediaPlayerController.start();
        }
        app.inBackground(false);
    }

    /**
     * select last played station in the recycle view
     * @see ViewTreeObserver.OnGlobalLayoutListener#onGlobalLayout()
     */
    @Override
    public void onGlobalLayout()
    {
        String lastStation = SettingsHolder.lastStation();
        if (StringUtils.isNotBlank(lastStation)) {
            int pos = 0;
            for (RadioStation item : stationList) {
                if (item.getTitle().equals(lastStation)) {
                    changedSelectionProgrammaticly = true;
                    radioListView.scrollToPosition(pos);
                    new Handler().postDelayed(new DelayedSelection(pos, radioListView), 200); // 200ms to refresh the UI after scrolling
                    break;
                }
                pos++;
            }
        }
        radioListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     *
     * @param data
     */
    private void handleMediaPlayerState(MediaPlayerService.ChangedData data)
    {
        switch (data.state())
        {
            case PLAYING:
                fabProgressBar.setVisibility(View.INVISIBLE);
                fab.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), android.R.drawable.ic_media_pause));
                statusMessage.setText(R.string.label_connected);
                break;
            case PREPARING:
                fabProgressBar.setVisibility(View.VISIBLE);
                fab.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), android.R.drawable.ic_media_pause));
                statusMessage.setText(R.string.label_connecting);
                break;
            case ERROR:
                if (data.data() instanceof Exception)
                    Snackbar.make(toolbar, ((Exception)data.data()).getMessage(), Snackbar.LENGTH_LONG).show();
                else
                    Snackbar.make(toolbar, R.string.error_unknow, Snackbar.LENGTH_LONG).show();
                // durchfallen ist erwünscht
            case STOPPED:
                fabProgressBar.setVisibility(View.INVISIBLE);
                fab.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), android.R.drawable.ic_media_play));
                statusMessage.setText(StringUtils.EMPTY);
                bufferState.setText(StringUtils.EMPTY);
                break;
            default:
                break;
        }
    }

    /**
     * called by EventBus at {@link MediaPlayerService#state(MediaPlayerService.MediaPlayerState, Object)}
     * @param cData
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaPlayerEvent(final MediaPlayerService.ChangedData<?> cData)
    {
        switch (cData.type()) {
            case STATE_CHANGED:
                handleMediaPlayerState(cData);
                break;
            case BUFFER_UPDATE:
                LogHelper.d(TAG, String.format("Buffering: %s", cData.data()));
                bufferState.setText((String)cData.data());
                break;
            case STATION_CHANGED:
                String channel = ((MediaPlayerService.ChangedData<RadioStation>)cData).data().getTitle();
                SettingsHolder.lastStation(channel);
                channelName.setText(channel);
                onGlobalLayout();
                break;
            default:
                break;
        }
    }

    /**
     * called by EventBus at {@link de.kordelle.radio.data.settings.SettingsHolder.SettingsAccessor#notify(String, Serializable)}
     * @param data
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSettingsChangeEvent(final SettingsHolder.SettingsChangeData data)
    {
        if (data.key().equals(SettingsHolder.KEY_PLAY_LAST_STATION))
        {
            if (Boolean.TRUE.equals(data.value()) && (mediaPlayerController.currentStation() != null))
                SettingsHolder.lastStation(mediaPlayerController.currentStation().getTitle());
        }
    }
}
