package com.example.gestionresmas.ui;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

public final class UiUtils {

    private UiUtils() {
        // Clase utilitaria, no instanciar
    }

    public static void setupEdgeToEdge(AppCompatActivity activity, View rootView, View appBar, View scrollingContent, View fab) {
        EdgeToEdge.enable(activity);

        final int appBarInitialLeft = (appBar != null) ? appBar.getPaddingLeft() : 0;
        final int appBarInitialTop = (appBar != null) ? appBar.getPaddingTop() : 0;
        final int appBarInitialRight = (appBar != null) ? appBar.getPaddingRight() : 0;
        final int appBarInitialBottom = (appBar != null) ? appBar.getPaddingBottom() : 0;

        final int scrollInitialLeft = (scrollingContent != null) ? scrollingContent.getPaddingLeft() : 0;
        final int scrollInitialTop = (scrollingContent != null) ? scrollingContent.getPaddingTop() : 0;
        final int scrollInitialRight = (scrollingContent != null) ? scrollingContent.getPaddingRight() : 0;
        final int scrollInitialBottom = (scrollingContent != null) ? scrollingContent.getPaddingBottom() : 0;

        final int[] fabInitialMargins = new int[2]; // [0] = bottom, [1] = right
        final boolean[] fabMarginsCaptured = new boolean[1];
        if (fab != null && fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            fabInitialMargins[0] = lp.bottomMargin;
            fabInitialMargins[1] = lp.rightMargin;
            fabMarginsCaptured[0] = true;
        }

        if (scrollingContent instanceof ViewGroup) {
            ((ViewGroup) scrollingContent).setClipToPadding(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());

            int topInset = Math.max(systemBars.top, cutout.top);
            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            int leftInset = Math.max(systemBars.left, cutout.left);
            int rightInset = Math.max(systemBars.right, cutout.right);

            if (appBar != null) {
                appBar.setPadding(
                        appBarInitialLeft + leftInset,
                        appBarInitialTop + topInset,
                        appBarInitialRight + rightInset,
                        appBarInitialBottom
                );
            }

            if (scrollingContent != null) {
                if (scrollingContent instanceof ViewGroup) {
                    ((ViewGroup) scrollingContent).setClipToPadding(false);
                }
                int fabExtraBottom = (fab != null) ? dpToPx(activity, 80) : 0;
                scrollingContent.setPadding(
                        scrollInitialLeft + leftInset,
                        scrollInitialTop,
                        scrollInitialRight + rightInset,
                        scrollInitialBottom + bottomInset + fabExtraBottom
                );
            }

            if (fab != null && fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
                if (!fabMarginsCaptured[0]) {
                    fabInitialMargins[0] = lp.bottomMargin;
                    fabInitialMargins[1] = lp.rightMargin;
                    fabMarginsCaptured[0] = true;
                }
                lp.bottomMargin = fabInitialMargins[0] + bottomInset;
                lp.rightMargin = fabInitialMargins[1] + rightInset;
                fab.setLayoutParams(lp);
                fab.requestLayout();
            }

            return windowInsets;
        });
    }

    public static GridLayoutManager createAdaptiveLayoutManager(Context context, int minColumnWidthDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = metrics.widthPixels / metrics.density;
        int spanCount = Math.max(1, (int) (screenWidthDp / minColumnWidthDp));
        return new GridLayoutManager(context, spanCount);
    }

    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        ));
    }
}
