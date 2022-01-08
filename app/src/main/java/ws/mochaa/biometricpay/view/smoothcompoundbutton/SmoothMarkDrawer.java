package ws.mochaa.biometricpay.view.smoothcompoundbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public abstract class SmoothMarkDrawer {

    protected static final boolean REAL_RIPPLE = true;

    protected final Context mContext;
    protected final float mDensity;
    protected final RectF mBounds;
    protected final Drawable mCompatBackgroundDrawable;
    protected final Paint mPaint;
    protected int mColorOn, mColorOff;
    protected int mWidth, mHeight;
    protected ColorFilter mCheckDisableColorFilter;

    public SmoothMarkDrawer(Context context, int colorOn, int colorOff) {
        this.mContext = context;
        this.mDensity = context.getResources().getDisplayMetrics().density;
        //this.mSize = dp2px(32);
        this.mBounds = new RectF();
        this.mCompatBackgroundDrawable = makeCompatBackgroundDrawable();
        this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mColorOn = colorOn;
        this.mColorOff = colorOff;
    }


    public int getDefaultWidth() {
        return dp2px(32);
    }

    public int getDefaultHeight() {
        return dp2px(32);
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setBounds(RectF rectF) {
        mBounds.set(rectF);
        mCompatBackgroundDrawable.setBounds((int) (mBounds.left), (int) (mBounds.top), (int) (mBounds.right), (int) (mBounds.bottom));
    }

    public void draw(Canvas canvas, final float fraction, View view) {
        drawMark(canvas, fraction, mBounds.left, mBounds.top, mBounds.width(), view);
        mCompatBackgroundDrawable.draw(canvas);
    }


    protected abstract void drawMark(Canvas canvas, final float fraction, float left, float top, float size, View view);


    protected void updateCheckPaintColorFilter(View view) {
        if (view.isEnabled()) {
            mPaint.setColorFilter(null);
        } else {
            if (mCheckDisableColorFilter == null) {
                mCheckDisableColorFilter = makeDisableColorFilter();
            }
            mPaint.setColorFilter(mCheckDisableColorFilter);// eclipse预览无效
        }
    }

    private static ColorFilter makeDisableColorFilter() {
        // http://www.2cto.com/kf/201605/509332.html
        final float rgbValue = 1f;
        float alphaValue = 1f;
        float[] colorArray = new float[]{rgbValue, 0, 0, 0, 0, 0, rgbValue, 0, 0, 0, 0, 0, rgbValue, 0, 0, 0, 0,
                0, alphaValue, 0};
        ColorMatrix cm = new ColorMatrix(colorArray);
        return new ColorMatrixColorFilter(cm);
    }

    //处理drawable
    protected void drawableStateChanged(View view) {
        int[] myDrawableState = view.getDrawableState();
        // Set the state of the Drawable
        mCompatBackgroundDrawable.setState(myDrawableState);
        view.invalidate();
    }

    protected void drawableHotspotChanged(float x, float y) {
        // DrawableCompat.setHotspot(mThumbDrawable, x, y);
        mCompatBackgroundDrawable.setHotspot(x, y);

    }

    protected boolean verifyDrawable(Drawable who) {
        return who == mCompatBackgroundDrawable;
    }

    public void jumpDrawablesToCurrentState() {
        mCompatBackgroundDrawable.jumpToCurrentState();
    }

    protected void onAttachedToWindow(View view) {
        mCompatBackgroundDrawable.setCallback(view);
        // 设置这两个属性 使mCompatBackgroundDrawable可以超出View边界绘制
        // 不过为什么5.0默认的checkbox的ripple可以自动超过边界？
        ViewParent viewParent = view.getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) viewParent;
            viewGroup.setClipToPadding(false);
            viewGroup.setClipChildren(false);
        }

    }

    protected void onDetachedFromWindow(View view) {
        mCompatBackgroundDrawable.setCallback(null);
        view.unscheduleDrawable(mCompatBackgroundDrawable);
    }

    /**
     * 是否自身处理onTouchEvent，用于Switch滑动操作
     **/
    public boolean onTouchEvent(MotionEvent event, SmoothCompoundButton smoothCompoundButton) {
        return false;
    }

    /**
     * 是否自身正在处理Fraction，用于Switch滑动操作
     **/
    public boolean isUpdatingFractionBySelf() {
        return false;
    }

    protected final int dp2px(float dp) {
        return (int) (dp * mDensity + 0.5f);
    }

    private Drawable makeCompatBackgroundDrawable() {
        // item_background_borderless_material.xml
        // <ripple
        // xmlns:android="http://schemas.android.com/apk/res/android"
        // android:color="?attr/colorControlHighlight" />
        // RippleDrawable rippleDrawable = new RippleDrawable(color,
        // content, mask)
        // RippleDrawable rippleDrawable = new RippleDrawable(new
        // ColorStateList(new int[][]{{}},
        // new int[]{colorRipple}), backgroundDrawable, null);
        // 可以参考http://stackoverflow.com/questions/27787870/how-to-use-rippledrawable-programmatically-in-code-not-xml-with-android-5-0-lo
        return new RippleDrawable(ColorStateList.valueOf(MaterialColor.DefaultLight.colorControlHighlight), null,
                null);
        // 这么写出来和item_background_borderless_material.xml效果一致，
        // RippleDrawable貌似默认就是圆的，而且会自动扩散到View边界外面
        // return rippleDrawable;
    }

    protected boolean isMarkInRight() {
        return false;
    }

    protected int convertColorAlpha(float alpha, int color) {
        final int originalAlpha = Color.alpha(color);
        return (color & 0x00ffffff) | (Math.round(originalAlpha * alpha) << 24);
    }

    protected int convertColorFraction(float fraction, int color0, int color1) {
        int a0 = Color.alpha(color0);
        int r0 = Color.red(color0);
        int g0 = Color.green(color0);
        int b0 = Color.blue(color0);

        int a1 = Color.alpha(color1);
        int r1 = Color.red(color1);
        int g1 = Color.green(color1);
        int b1 = Color.blue(color1);
        int a = (int) (a0 + (a1 - a0) * fraction);
        int r = (int) (r0 + (r1 - r0) * fraction);
        int g = (int) (g0 + (g1 - g0) * fraction);
        int b = (int) (b0 + (b1 - b0) * fraction);
        return Color.argb(a, r, g, b);
    }

}
