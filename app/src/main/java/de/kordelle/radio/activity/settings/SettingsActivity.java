package de.kordelle.radio.activity.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Switch;

import org.apache.commons.lang.StringUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import de.kordelle.radio.R;
import de.kordelle.radio.data.settings.SettingsHolder;

public final class SettingsActivity extends AppCompatActivity
{
    @BindView(R.id.switch_start_on_bt_connected)
    protected Switch btConnected;
    @BindView(R.id.switch_start_after_boot)
    protected Switch startAfterBoot;
    @BindView(R.id.switch_play_last_station)
    protected Switch playLastStation;
    @BindView(R.id.et_playlist_uri)
    protected EditText playlistUri;
    // prevent loop on dialog setup and switch changes
    private boolean isOnStart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);
    }

    @Override
    protected void onStart()
    {
        isOnStart = true;
        super.onStart();

        playlistUri.setText(StringUtils.EMPTY); // clear because of append. append places the cursor at the end

        btConnected.setChecked(SettingsHolder.playOnBTConnected());
        startAfterBoot.setChecked(SettingsHolder.startAfterBoot());
        playLastStation.setChecked(SettingsHolder.playLastStation());
        playlistUri.append(SettingsHolder.playlist());

        isOnStart = false;
    }

    @OnCheckedChanged(R.id.switch_start_on_bt_connected)
    protected void onBTConnectChecked(boolean checked)
    {
        if (!isOnStart)
            SettingsHolder.playOnBTConnected(checked);
    }

    @OnCheckedChanged(R.id.switch_start_after_boot)
    protected void onStartAfterBootChecked(boolean checked)
    {
        if (!isOnStart)
            SettingsHolder.startAfterBoot(checked);
    }

    @OnCheckedChanged(R.id.switch_play_last_station)
    protected void onPlayLastStationChecked(boolean checked)
    {
        if (!isOnStart)
            SettingsHolder.playLastStation(checked);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        SettingsHolder.playlist(playlistUri.getText().toString());
    }
}
