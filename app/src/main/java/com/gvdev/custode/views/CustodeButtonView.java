package com.gvdev.custode.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.gvdev.custode.R;

/**
 * Una custom view che reagisce a tocchi e trascinamenti del dito con animazioni.
 */
public class CustodeButtonView extends View implements ValueAnimator.AnimatorUpdateListener {

    private float x;
    private float y;
    private float fingerRadius;
    private float animatedValue; // 0 ≤ x ≤ 1
    private Paint calmPaints[];
    private Paint warningPaints[];
    private ValueAnimator valueAnimator;
    private AnimationEffect animationEffect = AnimationEffect.CALM;

    public enum AnimationEffect {
        CALM,
        WARNING,
        RIPPLE
    }

    public CustodeButtonView(Context context) {
        super(context);
        init();
    }

    public CustodeButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustodeButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        calmPaints = new Paint[4];
        warningPaints = new Paint[4];

        (calmPaints[0] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.light_blue_A100));
        (calmPaints[1] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.light_blue_A200));
        (calmPaints[2] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.light_blue_A400));
        (calmPaints[3] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.light_blue_A700));
        (warningPaints[0] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.deep_orange_A100));
        (warningPaints[1] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.deep_orange_A200));
        (warningPaints[2] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.deep_orange_A400));
        (warningPaints[3] = new Paint()).setColor(ContextCompat.getColor(getContext(), R.color.deep_orange_A700));

        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.addUpdateListener(this);
        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
        setAnimationEffect(AnimationEffect.CALM);
        valueAnimator.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setBackground(ContextCompat.getDrawable(getContext(), R.drawable.ripple));
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        animatedValue = (float) valueAnimator.getAnimatedValue();
        CustodeButtonView.this.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (animationEffect == AnimationEffect.RIPPLE) {
            x = event.getX();
            y = event.getY();
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (animationEffect) {
            case RIPPLE:
                drawRippleAnimation(canvas);
                break;
            case WARNING:
                drawWarningAnimation(canvas);
                break;
            case CALM:
                drawCalmAnimation(canvas);
                break;
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE)
            valueAnimator.start();
        else
            valueAnimator.cancel();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        x = getWidth() / 2;
        y = getHeight() / 2;
        fingerRadius = getWidth() / 10;
    }

    public AnimationEffect getAnimationEffect() {
        return animationEffect;
    }

    public void setAnimationEffect(AnimationEffect effect) {
        for (Paint calmPaint : calmPaints)
            calmPaint.setAlpha(255);
        for (Paint warningPaint : warningPaints)
            warningPaint.setAlpha(255);

        x = getWidth() / 2;
        y = getHeight() / 2;
        animationEffect = effect;
        valueAnimator.setCurrentPlayTime(0);
        switch (effect) {
            case RIPPLE:
                valueAnimator.setFloatValues(0, 1);
                valueAnimator.setDuration(3500);
                valueAnimator.setRepeatMode(ValueAnimator.RESTART);
                break;
            case CALM:
                valueAnimator.setFloatValues(0, 1);
                valueAnimator.setDuration(1000);
                valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
                break;
            case WARNING:
                valueAnimator.setFloatValues(0, 1, 0.6f, 1, 0);
                valueAnimator.setDuration(1000);
                valueAnimator.setRepeatMode(ValueAnimator.REVERSE);
                break;
        }
    }

    private void drawRippleAnimation(Canvas canvas) {
        final int rippleCount = 5;
        final float maxRadiusIncrease = getWidth() / 2;
        final float alphaMaxThreshold = 150f;
        final Paint circlePaint = calmPaints[calmPaints.length - 1];
        final Paint ripplesPaint = calmPaints[calmPaints.length - 2];

        for (int i = 0; i < rippleCount - 1; i++) {
            float rippleAnimatedValue = (animatedValue + i / rippleCount) % 1.f;
            float radiusFactor = (calmPaints.length - i) / (float) rippleCount;
            float radius = fingerRadius + maxRadiusIncrease * rippleAnimatedValue * radiusFactor;
            int alpha = (int) (alphaMaxThreshold - alphaMaxThreshold * rippleAnimatedValue);
            ripplesPaint.setAlpha(alpha);
            canvas.drawCircle(x, y, radius, ripplesPaint);
        }

        canvas.drawCircle(x, y, fingerRadius, circlePaint);
    }

    private double step = 0;

    private void drawCalmAnimation(Canvas canvas) {
        final int alphaMinThreshold = 70;
        final float outerCircleMaxRadiusIncrease = getWidth() / 6;

        for (int i = 0; i < calmPaints.length; i++) {
            float animatedRadius = 50 * animatedValue;
            float factor = (calmPaints.length - i) / (float) calmPaints.length;
            float radius = fingerRadius * factor + animatedRadius + outerCircleMaxRadiusIncrease / (i + 1);

            if (i != calmPaints.length - 1)
                calmPaints[i].setAlpha(255 - (int) ((255f - alphaMinThreshold) * animatedValue));
            float offsetX = (radius / 3f) * (float) Math.cos(step);
            float offsetY = (radius / 3f) * (float) Math.sin(step);
            canvas.drawCircle(x + offsetX, y + offsetY, radius, calmPaints[i]);
        }
        step += 0.005f;
    }

    private void drawWarningAnimation(Canvas canvas) {
        final int alphaMinThreshold = 180;

        canvas.drawPaint(warningPaints[0]);
        for (int i = 0; i < warningPaints.length; i++) {
            float animatedRadius = 50 * animatedValue;
            float factor = (warningPaints.length - i) / (float) warningPaints.length;
            float radius = fingerRadius * factor + animatedRadius;
            if (i != warningPaints.length - 1)
                warningPaints[i].setAlpha(255 - (int) ((255.0 - alphaMinThreshold) * animatedValue));
            canvas.drawCircle(x, y, radius, warningPaints[i]);
        }
    }

}
