package com.gvdev.custode.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Un DialogPreference con un NumberPicker.
 */
public class NumberPickerPreference extends DialogPreference {

    private int value;
    private int minValue = 1;
    private int maxValue = 100;
    private NumberPicker numberPicker;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NumberPickerPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateDialogView() {
        numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(minValue);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setValue(value);
        return numberPicker;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 1);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(getDefaultValue()) : (Integer) defaultValue);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            int value = numberPicker.getValue();
            if (callChangeListener(value))
                setValue(value);
        }
    }

    public void setValue(int value) {
        if (value != this.value) {
            persistInt(value);
            this.value = value;
        }
    }

    public int getValue() {
        return value;
    }

    private int getDefaultValue() {
        return minValue;
    }

}
