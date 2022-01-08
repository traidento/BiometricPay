package ws.mochaa.biometricpay.util;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.QuickContactBadge;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.ButtonBarLayout;
import androidx.appcompat.widget.SwitchCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ws.mochaa.biometricpay.util.log.L;

/**
 * Created by Jason on 2017/9/9.
 */

public class ViewUtils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static void performActionClick(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width < 0) {
            width = 0;
        }
        if (height < 0) {
            height = 0;
        }
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 200;

        float x = width > 0 ? new Random(downTime).nextInt(width) : 0;
        float y = height > 0 ? new Random(eventTime).nextInt(height) : 0;

        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );
        view.dispatchTouchEvent(motionEvent);
        downTime = SystemClock.uptimeMillis() + 120;
        eventTime = SystemClock.uptimeMillis() + 200;
        motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
        view.dispatchTouchEvent(motionEvent);
    }

    @Nullable
    public static View findViewByName(AppCompatActivity activity, String packageName, String... names) {
        Resources resources = activity.getResources();
        for (String name : names) {
            int id = resources.getIdentifier(name, "id", packageName);
            if (id == 0) {
                continue;
            }
            View rootView = activity.getWindow().getDecorView();
            List<View> viewList = new ArrayList<>();
            getChildViews((ViewGroup) rootView, id, viewList);
            sortViewListByYPosition(viewList);
            int outViewListSize = viewList.size();
            if (outViewListSize == 1) {
                return viewList.get(0);
            } else if (outViewListSize > 1) {
                for (View view : viewList) {
                    if (view.isShown()) {
                        return view;
                    }
                }
                return viewList.get(0);
            }
        }
        return null;
    }

    @Nullable
    public static View findViewByText(View rootView, String... names) {
        for (String name : names) {
            List<View> viewList = new ArrayList<>();
            getChildViews((ViewGroup) rootView, name, viewList);
            int outViewListSize = viewList.size();
            if (outViewListSize == 1) {
                return viewList.get(0);
            } else if (outViewListSize > 1) {
                for (View view : viewList) {
                    if (view.isShown()) {
                        return view;
                    }
                }
                return viewList.get(0);
            }
        }
        return null;
    }

    private static Class sRecycleViewClz;

    private static String getViewBaseDesc(View view) {
        if (sRecycleViewClz == null) {
            try {
                sRecycleViewClz = Class.forName("android.support.v7.widget.RecyclerView");
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (view instanceof FrameLayout) {
            return FrameLayout.class.getName();
        } else if (view instanceof RatingBar) {
            return RatingBar.class.getName();
        } else if (view instanceof SeekBar) {
            return SeekBar.class.getName();
        } else if (view instanceof TableLayout) {
            return TableLayout.class.getName();
        } else if (view instanceof ButtonBarLayout) {
            return ButtonBarLayout.class.getName();
        } else if (view instanceof TableRow) {
            return TableRow.class.getName();
        } else if (view instanceof LinearLayout) {
            return LinearLayout.class.getName();
        } else if (view instanceof RelativeLayout) {
            return RelativeLayout.class.getName();
        } else if (view instanceof GridLayout) {
            return GridLayout.class.getName();
        } else if (view instanceof CheckBox) {
            return CheckBox.class.getName();
        } else if (view instanceof RadioButton) {
            return RadioButton.class.getName();
        } else if (view instanceof CheckedTextView) {
            return CheckedTextView.class.getName();
        } else if (view instanceof Spinner) {
            return Spinner.class.getName();
        } else if (view instanceof ProgressBar) {
            return ProgressBar.class.getName();
        } else if (view instanceof QuickContactBadge) {
            return QuickContactBadge.class.getName();
        } else if (view instanceof SwitchCompat) {
            return SwitchCompat.class.getName();
        } else if (view instanceof Switch) {
            return Switch.class.getName();
        } else if (view instanceof Space) {
            return Space.class.getName();
        } else if (view instanceof TextView) {
            return TextView.class.getName();
        } else if (view instanceof AppCompatImageView) {
            return AppCompatImageView.class.getName();
        } else if (view instanceof ImageView) {
            return ImageView.class.getName();
        } else if (view instanceof ListView) {
            return ListView.class.getName();
        } else if (view instanceof GridView) {
            return ListView.class.getName();
        } else if (sRecycleViewClz != null && view.getClass().isAssignableFrom(sRecycleViewClz)) {
            return sRecycleViewClz.getName();
        }
        return view.getClass().getName();
    }

    public static String getViewInfo(View view) {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(view);
        stringBuffer.append(" type:").append(getViewBaseDesc(view));
        stringBuffer.append(" clz:").append(view.getClass().getName());
        if (view instanceof EditText) {
            stringBuffer.append(" text:").append(((EditText) view).getText()).append(" hint:").append(((EditText) view).getHint());
        } else if (view instanceof TextView) {
            stringBuffer.append(" text:").append(((TextView) view).getText());
        }
        int[] location = new int[]{0, 0};
        view.getLocationOnScreen(location);
        stringBuffer.append(" cor x:").append(location[0]).append(" y:").append(location[1]);
        CharSequence desc = view.getContentDescription();
        if (!TextUtils.isEmpty(desc)) {
            stringBuffer.append(" desc:").append(desc);
        }
        stringBuffer.append(" tag:").append(view.getTag());
        return stringBuffer.toString();
    }

    public static void recursiveLoopChildren(View view) {
        if (view instanceof ViewGroup) {
            recursiveLoopChildren((ViewGroup) view);
        } else {
            L.d("Empty view");
        }
    }

    public static void recursiveLoopChildren(ViewGroup parent) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                recursiveLoopChildren((ViewGroup) child);
                // DO SOMETHING WITH VIEWGROUP, AFTER CHILDREN HAS BEEN LOOPED
                L.d("ViewGroup", getViewInfo(child));
            } else {
                if (child != null) {
                    try {
                        L.d("view", getViewInfo(child));
                    } catch (Exception e) {

                    }
                    // DO SOMETHING WITH VIEW
                }
            }
        }
    }

    public static void getChildViews(ViewGroup parent, String text, List<View> outList) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child == null) {
                continue;
            }
            if (child instanceof EditText) {
                if (text.equals(String.valueOf(((TextView) child).getText()))) {
                    outList.add(child);
                } else if (text.equals(String.valueOf(((EditText) child).getHint()))) {
                    outList.add(child);
                }
            } else if (child instanceof TextView) {
                if (text.equals(String.valueOf(((TextView) child).getText()))) {
                    outList.add(child);
                }
            }
            if (child instanceof ViewGroup) {
                getChildViews((ViewGroup) child, text, outList);
            }
        }
    }

    public static void getChildViews(ViewGroup parent, int id, List<View> outList) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child == null) {
                continue;
            }
            if (id == child.getId()) {
                outList.add(child);
            }
            if (child instanceof ViewGroup) {
                getChildViews((ViewGroup) child, id, outList);
            }
        }
    }

    public static void getChildViewsByType(ViewGroup parent, String type, List<View> outList) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child == null) {
                continue;
            }
            if (child.getClass().getName().contains(type)) {
                outList.add(child);
            }
            if (child instanceof ViewGroup) {
                getChildViewsByType((ViewGroup) child, type, outList);
            }
        }
    }

    public static void getChildViews(ViewGroup parent, List<View> outList) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child == null) {
                continue;
            }
            outList.add(child);
            if (child instanceof ViewGroup) {
                getChildViews((ViewGroup) child, outList);
            }
        }
    }

    private static final Comparator<View> sYLocationLHCompator = new Comparator<View>() {
        @Override
        public int compare(View o1, View o2) {
            int y1 = getViewYPosInScreen(o1);
            int y2 = getViewYPosInScreen(o2);
            return Integer.compare(y1, y2);
        }
    };

    public static void sortViewListByYPosition(List<View> viewList) {
        Collections.sort(viewList, sYLocationLHCompator);
    }

    public static int getViewYPosInScreen(View v) {
        int[] location = new int[]{0, 0};
        v.getLocationOnScreen(location);
        return location[1];
    }

    public static void removeFromSuperView(View v) {
        ViewParent parentView = v.getParent();
        if (parentView == null) {
            return;
        }
        if (parentView instanceof ViewGroup) {
            ViewGroup parentLayout = (ViewGroup) parentView;
            parentLayout.removeView(v);
        }
    }

    public static String viewsDesc(List<View> childView) {
        StringBuilder stringBuffer = new StringBuilder();
        for (View view : childView) {
            stringBuffer.append(ViewUtils.getViewInfo(view)).append("\n");
        }
        return stringBuffer.toString();
    }

    /**
     * @param viewGroup
     * @param childView
     * @return found >= 0, not found -1
     */
    public static int findChildViewPosition(ViewGroup viewGroup, View childView) {
        int childViewCount = viewGroup.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View view = viewGroup.getChildAt(i);
            if (view == childView) {
                return i;
            }
        }
        return -1;
    }

    public static List<View> getWindowManagerViews() {
        try {

            // get the list from WindowManagerGlobal.mViews
            Class wmgClass = Class.forName("android.view.WindowManagerGlobal");
            Object wmgInstance = wmgClass.getMethod("getInstance").invoke(null);

            return viewsFromWM(wmgClass, wmgInstance);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<View>();
    }

    private static List<View> viewsFromWM(Class wmClass, Object wmInstance) throws Exception {

        Field viewsField = wmClass.getDeclaredField("mViews");
        viewsField.setAccessible(true);
        Object views = viewsField.get(wmInstance);

        if (views instanceof List) {
            return new ArrayList<View>((List<View>) viewsField.get(wmInstance));
        } else if (views instanceof View[]) {
            return Arrays.asList((View[]) viewsField.get(wmInstance));
        }

        return new ArrayList<View>();
    }


    public static boolean isShown(View v) {
        Rect r = new Rect();
        v.getGlobalVisibleRect(r);
        return r.left != 0 || r.right != 0 || r.top != 0 || r.bottom != 0;
    }

    public static boolean isViewVisibleInScreen(View view) {
        if (!isShown(view)) {
            return false;
        }
        if (!view.isAttachedToWindow()) {
            return false;
        }
        if (view.getAlpha() == 0) {
            return false;
        }
        if (view.getWidth() <= 0 && view.getHeight() <= 0) {
            return false;
        }
        return view.getWindowVisibility() == View.VISIBLE;
    }

}
