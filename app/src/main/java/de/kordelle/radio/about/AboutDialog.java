package de.kordelle.radio.about;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;

import de.kordelle.radio.R;
import de.kordelle.radio.databinding.AboutLayoutBinding;

/**
 * Created by thomask on 08.10.16.
 */
public class AboutDialog extends Dialog
{
    /**
     *
     * @param context
     */
    public AboutDialog(Context context)
    {
        super(context);
    }

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AboutLayoutBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.about_layout, null, false);
        binding.setAboutData(new AboutLayoutData());
        setContentView(binding.getRoot());
    }
}
