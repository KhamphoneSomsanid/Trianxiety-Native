package com.trianxiety.myapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class DiamondView extends View {

    Path path;
    Paint paint;

    public DiamondView(Context context) {
        super(context);
        init(null);
    }

    public DiamondView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public DiamondView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attributeSet) {
        path = new Path();
        paint = new Paint();
        paint.setColor(Color.BLACK);
    }

    public void drawDiamond(Path path) {
        this.path = path;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.clipPath(path);
        canvas.drawPath(path, paint);

    }

    @Override
    public void draw(Canvas canvas) {
        if (path != null) {
            canvas.clipPath(path);
            Log.i("Test", "Alfa");
        } else Log.i("Test", "Beta");

        super.draw(canvas);
    }

}
