package com.example.ereke2048;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontUtils {

    public static void applyCustomFont(View view, Typeface typeface) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyCustomFont(group.getChildAt(i), typeface);
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }
    }
}

