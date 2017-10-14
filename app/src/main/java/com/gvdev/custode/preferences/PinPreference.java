package com.gvdev.custode.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;

import com.gvdev.custode.CustodeUtils;
import com.gvdev.custode.R;

/**
 * Un EditTextPreference che accetta un valore numerico di 4 cifre e salva nelle preferenze una
 * stringa con l'hash crittografico del valore.
 */
public class PinPreference extends EditTextPreference {

    public PinPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PinPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PinPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        View positiveButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        getEditText().setError(null);
        getEditText().setText("");
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPin = getEditText().getText().toString();
                if (validatePin(newPin)) {
                    getEditText().setError(null);
                    getDialog().dismiss();
                    onDialogClosed(true);
                }
                else
                    getEditText().setError(getDialog().getContext().getString(R.string.pin_length_error));
            }
        });
    }

    private static boolean validatePin(String value) {
        return value.matches("[0-9]{4}");
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = getEditText().getText().toString();
            if (callChangeListener(value) && validatePin(value))
                setText(CustodeUtils.SHA1(value));
        }
    }

}