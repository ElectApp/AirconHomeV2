package com.apyeng.airconhomev2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    //Thank: https://gist.github.com/lopspower/6e20680305ddfcb11e1e
    //Modified: Somsak Elect, Mar-2019

    public static void hideKeyboard(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm!=null){
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    public static void showKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager!=null){
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        //activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
    }

    public static void addKeyboardVisibilityListener(final View rootLayout, final OnKeyboardVisibiltyListener onKeyboardVisibiltyListener) {
       rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
           @Override
           public void onGlobalLayout() {
               Rect r = new Rect();
               rootLayout.getWindowVisibleDisplayFrame(r);
               int screenHeight = rootLayout.getRootView().getHeight();

               // r.bottom is the position above soft keypad or device button.
               // if keypad is shown, the r.bottom is smaller than that before.
               int keypadHeight = screenHeight - r.bottom;

               boolean isVisible = keypadHeight > screenHeight * 0.15; // 0.15 ratio is perhaps enough to determine keypad height.
               onKeyboardVisibiltyListener.onVisibilityChange(isVisible);
           }
       });

        /* rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(({
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootLayout.getRootView().getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            int keypadHeight = screenHeight - r.bottom;

            boolean isVisible = keypadHeight > screenHeight * 0.15; // 0.15 ratio is perhaps enough to determine keypad height.
            onKeyboardVisibiltyListener.onVisibilityChange(isVisible);
        }); */
    }

    public interface OnKeyboardVisibiltyListener {
        void onVisibilityChange(boolean isVisible);
    }
}
