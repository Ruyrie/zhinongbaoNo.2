package com.example.zhinongbao.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public final class DialogUtils {
    private static final int GREEN = Color.rgb(76, 175, 80);
    private static final int GREEN_DARK = Color.rgb(46, 125, 50);
    private static final int GREEN_LIGHT = Color.rgb(235, 248, 237);
    private static final int RED = Color.rgb(229, 57, 53);
    private static final int TEXT = Color.rgb(33, 37, 41);
    private static final int MUTED = Color.rgb(109, 117, 125);
    private static final int BORDER = Color.rgb(229, 234, 229);

    public interface ConfirmAction {
        boolean onConfirm();
    }

    public interface OptionAction {
        void onSelect(int index);
    }

    private DialogUtils() {
    }

    public static AlertDialog showConfirm(Context context, String title, String message,
            String cancelText, String confirmText, boolean dangerous, ConfirmAction action) {
        return showContent(context, title, message, null, cancelText, confirmText, dangerous, action);
    }

    public static AlertDialog showContent(Context context, String title, @Nullable String message,
            @Nullable View content, String cancelText, String confirmText, boolean dangerous,
            ConfirmAction action) {
        LinearLayout root = createRoot(context);
        root.addView(createTitle(context, title));
        if (message != null && !message.isEmpty()) {
            root.addView(createMessage(context, message));
        }
        if (content != null) {
            root.addView(content);
        }
        AlertDialog dialog = new AlertDialog.Builder(context).setView(root).create();
        root.addView(createButtonRow(context, dialog, cancelText, confirmText, dangerous, action));
        showRounded(dialog, context);
        return dialog;
    }

    public static AlertDialog showTextInput(Context context, String title, String message, String hint,
            String cancelText, String confirmText, boolean dangerous, ConfirmActionWithText action) {
        EditText input = createInput(context, hint, false);
        AlertDialog dialog = showContent(context, title, message, input, cancelText, confirmText,
                dangerous, () -> action.onConfirm(input.getText().toString().trim()));
        input.requestFocus();
        return dialog;
    }

    public interface ConfirmActionWithText {
        boolean onConfirm(String text);
    }

    public static AlertDialog showRoleSelection(Context context, String title, String[] options,
            OptionAction action) {
        LinearLayout root = createRoot(context);
        root.addView(createTitle(context, title));
        TextView hint = createMessage(context, "请选择进入后的功能身份");
        hint.setGravity(Gravity.START);
        root.addView(hint);
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            TextView row = new TextView(context);
            row.setText(options[i]);
            row.setTextColor(TEXT);
            row.setTextSize(16);
            row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(context, 16), 0, dp(context, 16), 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 58));
            lp.topMargin = dp(context, i == 0 ? 6 : 10);
            row.setLayoutParams(lp);
            row.setBackground(rounded(Color.rgb(247, 249, 247), dp(context, 16), BORDER, 1));
            row.setOnClickListener(v -> {
                AlertDialog dialog = (AlertDialog) root.getTag();
                dialog.dismiss();
                action.onSelect(index);
            });
            root.addView(row);
        }
        AlertDialog dialog = new AlertDialog.Builder(context).setView(root).create();
        dialog.setCancelable(false);
        root.setTag(dialog);
        showRounded(dialog, context);
        return dialog;
    }

    public static EditText createInput(Context context, String hint, boolean multiline) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setTextSize(15);
        input.setTextColor(TEXT);
        input.setHintTextColor(Color.rgb(170, 176, 180));
        input.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        input.setBackground(rounded(Color.rgb(248, 250, 248), dp(context, 18), BORDER, 1));
        input.setSingleLine(!multiline);
        input.setGravity(multiline ? Gravity.TOP | Gravity.START : Gravity.CENTER_VERTICAL);
        input.setInputType(multiline
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                : InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, multiline ? dp(context, 92) : dp(context, 52));
        lp.topMargin = dp(context, 10);
        input.setLayoutParams(lp);
        return input;
    }

    private static LinearLayout createRoot(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 20));
        root.setBackground(rounded(Color.WHITE, dp(context, 22), Color.TRANSPARENT, 0));
        return root;
    }

    private static TextView createTitle(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(TEXT);
        tv.setTextSize(21);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setGravity(Gravity.START);
        tv.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static TextView createMessage(Context context, String message) {
        TextView tv = new TextView(context);
        tv.setText(message);
        tv.setTextColor(MUTED);
        tv.setTextSize(15);
        tv.setLineSpacing(dp(context, 2), 1.0f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 14);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static LinearLayout createButtonRow(Context context, AlertDialog dialog, String cancelText,
            String confirmText, boolean dangerous, ConfirmAction action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(context, 22);
        row.setLayoutParams(rowLp);

        TextView cancel = createButton(context, cancelText, false, false);
        TextView confirm = createButton(context, confirmText, true, dangerous);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            if (action == null || action.onConfirm()) {
                dialog.dismiss();
            }
        });
        row.addView(cancel);
        row.addView(confirm);
        return row;
    }

    private static TextView createButton(Context context, String text, boolean primary, boolean dangerous) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(primary ? Color.WHITE : GREEN_DARK);
        int fill = primary ? (dangerous ? RED : GREEN) : GREEN_LIGHT;
        int stroke = primary ? fill : Color.rgb(198, 231, 202);
        button.setBackground(rounded(fill, dp(context, 14), stroke, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(context, 48), 1);
        lp.leftMargin = primary ? dp(context, 8) : 0;
        lp.rightMargin = primary ? 0 : dp(context, 8);
        button.setLayoutParams(lp);
        return button;
    }

    private static void showRounded(AlertDialog dialog, Context context) {
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = context.getResources().getDisplayMetrics().widthPixels - dp(context, 48);
                window.setLayout(Math.min(width, dp(context, 360)), LinearLayout.LayoutParams.WRAP_CONTENT);
            }
        });
        if (!(context instanceof Activity) || !((Activity) context).isFinishing()) {
            dialog.show();
        }
    }

    private static GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) {
            drawable.setStroke(strokeWidthDp, strokeColor);
        }
        return drawable;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
