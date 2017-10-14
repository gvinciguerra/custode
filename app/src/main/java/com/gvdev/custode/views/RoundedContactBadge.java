package com.gvdev.custode.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

import com.gvdev.custode.R;

/**
 * Una sottoclasse di QuickContactBadge che disegna l'immagine del contatto circolare e mostra in
 * basso a destra di essa un'icona.
 */
public class RoundedContactBadge extends QuickContactBadge {

    private Rect statusDrawableBounds;
    private Paint whitePaint;
    private ContactStatus contactStatus = ContactStatus.NONE;

    public RoundedContactBadge(Context context) {
        super(context);
        init();
    }

    public RoundedContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RoundedContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        whitePaint = new Paint();
        whitePaint.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        whitePaint.setAntiAlias(true);
    }

    public enum ContactStatus {
        NONE,
        SENT,
        DELIVERED,
        ERROR
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int radius = Math.min(w, h) / 2;
        int statusR = (int) (radius / 2.4);
        int statusX = w - statusR * 2;
        int statusY = h - statusR * 2;
        statusDrawableBounds = new Rect(statusX, statusY, statusX + statusR * 2, statusY + statusR * 2);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.concat(getImageMatrix());
        getDrawable().draw(canvas);
        canvas.restore();

        if (contactStatus == ContactStatus.NONE)
            return;

        int statusResource;
        switch (contactStatus) {
            case SENT:
                statusResource = R.drawable.ic_sent;
                break;
            case DELIVERED:
                statusResource = R.drawable.ic_delivered;
                break;
            default:
                statusResource = R.drawable.ic_error;
                break;
        }

        canvas.drawCircle(statusDrawableBounds.centerX(), statusDrawableBounds.centerY(), statusDrawableBounds.width() / 2, whitePaint);
        Drawable statusDrawable = ContextCompat.getDrawable(getContext(), statusResource);
        statusDrawable.setBounds(statusDrawableBounds);
        statusDrawable.draw(canvas);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        Bitmap bitmap = drawableToBitmap(drawable);
        RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        d.setCircular(true);
        super.setImageDrawable(d);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null)
                return bitmapDrawable.getBitmap();
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        else
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void setContactStatus(ContactStatus contactStatus) {
        this.contactStatus = contactStatus;
        invalidate();
    }
}