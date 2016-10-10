package de.kordelle.radio.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.kordelle.radio.activity.main.MainActivity;
import de.kordelle.radio.data.settings.SettingsHolder;
import de.kordelle.radio.logging.LogHelper;

/**
 * Created by thomas.kordelle on 15.09.16.
 */
public class BootupReceiver extends BroadcastReceiver
{
    private static final String TAG = BootupReceiver.class.getSimpleName();

    public static final String KEY_CALLED_DURING_BOOT = "calledDuringBoot";

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        try {
            final String action = intent.getAction();

            LogHelper.d(TAG, "receive action = " + action);

            SettingsHolder.init(context);
            if (SettingsHolder.startAfterBoot()) {
                Intent mainActivity = new Intent(context, MainActivity.class);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mainActivity.putExtra(KEY_CALLED_DURING_BOOT, Boolean.TRUE);
                context.startActivity(mainActivity);
            }
        }
        catch (Exception ex) {
            LogHelper.e(TAG, "Error while starting radio on startup.", ex);
        }
    }
}
