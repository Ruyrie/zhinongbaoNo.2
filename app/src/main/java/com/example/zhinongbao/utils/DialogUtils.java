package com.example.zhinongbao.utils;

/* ============================================================
 * 【弹窗 / 对话框 / DialogUtils】统一风格的弹窗工具（utils 工具类）
 * ============================================================
 * 这个文件是干什么的：
 *   全 App 的各种弹窗都从这里出，保证「圆角白卡片 + 绿色按钮」的统一风格。
 *   对外提供几种常用弹窗（都是 static 方法，直接 DialogUtils.xxx(...) 调用）：
 *     - showConfirm   ：确认框（标题+内容+取消/确认两个按钮），购物车清空就用它。
 *     - showTextInput ：带一个输入框的弹窗（让用户填一段文字）。
 *     - showRefundReason：退款原因选择弹窗（预置原因胶囊单选 + 选填说明）。
 *     - showRoleSelection：选择「买家/卖家」身份的弹窗。
 *
 * 关键点：本类不靠 XML 布局，而是「用 Java 代码动态创建控件」拼出弹窗。
 *   - createRoot/createTitle/createMessage/createButton：分别造出卡片、标题、正文、按钮。
 *   - rounded(...)：用 GradientDrawable 画「圆角背景」。
 *   - dp(...)：把「dp 单位」换算成实际像素（不同屏幕清晰度下保证大小一致）。
 *   - 回调接口 ConfirmAction 等：让调用方决定「点了确认之后做什么」。
 *
 * 提示：在 IDE 里搜索「弹窗」或「对话框」可看用到它的地方。
 * ============================================================ */

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
    // ↓ 统一配色常量（rgb 三个数=红绿蓝），保证全 App 弹窗风格一致
    private static final int GREEN = Color.rgb(76, 175, 80);        // 主色绿（确认按钮）
    private static final int GREEN_DARK = Color.rgb(46, 125, 50);   // 深绿（取消按钮文字）
    private static final int GREEN_LIGHT = Color.rgb(235, 248, 237);// 浅绿（取消按钮底色）
    private static final int RED = Color.rgb(229, 57, 53);          // 红（危险操作按钮）
    private static final int TEXT = Color.rgb(33, 37, 41);          // 主文字色
    private static final int MUTED = Color.rgb(109, 117, 125);      // 次要文字色（灰）
    private static final int BORDER = Color.rgb(229, 234, 229);     // 边框色

    // 确认回调：onConfirm 返回 true 表示「处理完了，关闭弹窗」
    public interface ConfirmAction {
        boolean onConfirm();
    }

    // 多选项回调：返回被点中的选项序号
    public interface OptionAction {
        void onSelect(int index);
    }

    // 私有构造方法：工具类不允许 new（只用静态方法）
    private DialogUtils() {
    }

    // 【确认框】最常用：标题 + 内容 + 「取消/确认」两个按钮。dangerous=true 时确认按钮变红。
    public static AlertDialog showConfirm(Context context, String title, String message,
            String cancelText, String confirmText, boolean dangerous, ConfirmAction action) {
        return showContent(context, title, message, null, cancelText, confirmText, dangerous, action);
    }

    // 【内容框】比确认框更灵活：可在标题/正文之间塞入任意自定义控件 content（如输入框）
    public static AlertDialog showContent(Context context, String title, @Nullable String message,
            @Nullable View content, String cancelText, String confirmText, boolean dangerous,
            ConfirmAction action) {
        LinearLayout root = createRoot(context);   // 造一张圆角白卡片当容器
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

    // 【输入框弹窗】在内容框里塞一个 EditText，确认时把用户输入的文字回调出去
    public static AlertDialog showTextInput(Context context, String title, String message, String hint,
            String cancelText, String confirmText, boolean dangerous, ConfirmActionWithText action) {
        EditText input = createInput(context, hint, false);
        AlertDialog dialog = showContent(context, title, message, input, cancelText, confirmText,
                dangerous, () -> action.onConfirm(input.getText().toString().trim()));
        input.requestFocus();
        return dialog;
    }

    // 带文字的确认回调：把用户输入的内容传回去
    public interface ConfirmActionWithText {
        boolean onConfirm(String text);
    }

    /** 买家退款原因选择回调，返回 true 时关闭弹窗 */
    public interface RefundReasonAction {
        boolean onSubmit(String reason);
    }

    /**
     * 买家申请退款弹窗：预置原因「胶囊」单选 + 选填补充说明。
     * 仅选择类别即可提交；补充说明可选。
     */
    // 【退款原因弹窗】6 个原因「胶囊」点一个高亮（单选）+ 可选补充说明；未选会提示
    public static AlertDialog showRefundReason(Context context, RefundReasonAction action) {
        android.view.View view = android.view.LayoutInflater.from(context)
                .inflate(com.example.zhinongbao.R.layout.dialog_refund_reason, null);
        final int[] chipIds = {
                com.example.zhinongbao.R.id.chipRefundReason1,
                com.example.zhinongbao.R.id.chipRefundReason2,
                com.example.zhinongbao.R.id.chipRefundReason3,
                com.example.zhinongbao.R.id.chipRefundReason4,
                com.example.zhinongbao.R.id.chipRefundReason5,
                com.example.zhinongbao.R.id.chipRefundReason6
        };
        final TextView[] chips = new TextView[chipIds.length];
        final int[] selected = { -1 };
        for (int i = 0; i < chipIds.length; i++) {
            chips[i] = view.findViewById(chipIds[i]);
            final int index = i;
            chips[i].setOnClickListener(v -> {
                selected[0] = index;
                for (int j = 0; j < chips.length; j++) {
                    chips[j].setSelected(j == index);
                }
            });
        }

        final EditText manual = view.findViewById(com.example.zhinongbao.R.id.etRefundManual);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();
        view.findViewById(com.example.zhinongbao.R.id.btnRefundCancel)
                .setOnClickListener(v -> dialog.dismiss());
        view.findViewById(com.example.zhinongbao.R.id.btnRefundSubmit).setOnClickListener(v -> {
            if (selected[0] < 0) {
                android.widget.Toast.makeText(context, "请选择退款原因", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            String category = chips[selected[0]].getText().toString();
            String extra = manual.getText().toString().trim();
            String reason = extra.isEmpty() ? category : category + "：" + extra;
            if (action == null || action.onSubmit(reason)) {
                dialog.dismiss();
            }
        });
        showRounded(dialog, context);
        return dialog;
    }

    // 【身份选择弹窗】用于「既是买家又是卖家」的账号登录后选择以什么身份进入
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

    // 造一个统一风格的输入框：hint=灰色提示文字，multiline=是否允许多行
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

    // 造弹窗的「圆角白卡片」根容器（竖向排列标题/正文/按钮）
    private static LinearLayout createRoot(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 20));
        root.setBackground(rounded(Color.WHITE, dp(context, 22), Color.TRANSPARENT, 0));
        return root;
    }

    // 造标题文字（加粗大字）
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

    // 造正文文字（灰色小字）
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

    // 造底部「取消 + 确认」一行两个按钮，并绑定点击：取消=关闭；确认=执行回调后关闭
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

    // 造单个按钮：primary=true 是主按钮(实心)，dangerous=true 主按钮变红色
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

    // 真正显示弹窗：去掉系统默认白底（换成透明，露出我们的圆角卡片）、限制最大宽度，再 show
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

    // 工具：生成一个「圆角 + 可选描边」的背景，color=填充色，radius=圆角，stroke=边框
    private static GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) {
            drawable.setStroke(strokeWidthDp, strokeColor);
        }
        return drawable;
    }

    // 工具：把 dp 换算成像素。density 是屏幕密度，+0.5f 是为了四舍五入。
    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
