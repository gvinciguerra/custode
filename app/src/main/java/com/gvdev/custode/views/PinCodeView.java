package com.gvdev.custode.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.gvdev.custode.R;

/**
 * Una view che disegna n cerchi (setMaxPinLength(n)) e ne colora i primi k<=n (setPinLength(k)).
 */
public class PinCodeView extends View {

    private int pinLength = 0;
    private int maxPinLength = 4;
    private final int circleMargin = 20;
    private int radius;
    private Paint offStrokePaint;
    private Paint offFillPaint;
    private Paint onPaint;

    private void init() {
        offStrokePaint = new Paint();
        offStrokePaint.setStrokeWidth(10);
        offStrokePaint.setColor(ContextCompat.getColor(getContext(), R.color.md_grey_600));
        offStrokePaint.setAntiAlias(true);
        offFillPaint = new Paint();
        offFillPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_grey_500));
        onPaint = new Paint();
        onPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        onPaint.setAntiAlias(true);
    }

    public PinCodeView(Context context) {
        super(context);
        init();
    }

    public PinCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PinCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (width > height)
            radius = Math.min(height, (width - maxPinLength * circleMargin) / maxPinLength) / 2;
        else
            radius = Math.min(height, 150) / 2;

        setMeasuredDimension(maxPinLength * (radius * 2 + circleMargin), radius * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int offset = (getWidth() - maxPinLength * (circleMargin + radius * 2)) / 2;
        for (int i = 0; i < maxPinLength; i++) {
            float x = offset + (circleMargin + radius * 2) * i + radius;
            if (i < pinLength)
                canvas.drawCircle(x, radius, radius, onPaint);
            else {
                canvas.drawCircle(x, radius, radius, offFillPaint);
                canvas.drawCircle(x, radius - onPaint.getStrokeWidth() / 2, radius - offStrokePaint.getStrokeWidth() / 2, offStrokePaint);
            }
        }
    }

    public void setMaxPinLength(int maxPinLength) {
        this.maxPinLength = Math.max(maxPinLength, 0);
        this.pinLength = Math.min(maxPinLength, this.pinLength);
        requestLayout();
    }

    public void setPinLength(int pinLength) {
        this.pinLength = Math.min(maxPinLength, pinLength);
        invalidate();
    }

    public void startShakeAnimation() {
        Animation shakeAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        startAnimation(shakeAnimation);
    }

}
