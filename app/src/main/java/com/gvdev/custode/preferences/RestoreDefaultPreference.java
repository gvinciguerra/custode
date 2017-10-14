package com.gvdev.custode.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;

import com.gvdev.custode.R;

/**
 * Un EditTextPreference con un bottone per ripristinare il testo di default.
 */
public class RestoreDefaultPreference extends EditTextPreference implements View.OnClickListener {

    private String defaultValue;

    public RestoreDefaultPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RestoreDefaultPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RestoreDefaultPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setNeutralButton(R.string.restore_defaults, null);
        // Aggiungere il listener con setOnClickListener anziché setNeutralButton impedisce la
        // chiusura della finestra alla pressione del bottone.
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        View neutralButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (defaultValue != null)
            getEditText().setText(defaultValue);
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        super.setDefaultValue(defaultValue);
        this.defaultValue = (String) defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (this.defaultValue = (String) super.onGetDefaultValue(a, index));
    }

}