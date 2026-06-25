package com.example.zhinongbao.utils;

/* ============================================================
 * 【验证码 / Captcha】图形验证码工具（utils 工具类）
 * ============================================================
 * 这个文件是干什么的：
 *   生成「登录/注册时看到的那张扭曲字符图片验证码」，并记住正确答案，
 *   供页面校验用户输入是否正确。
 *
 * 核心技术：用代码「画」一张图片
 *   - Bitmap：一张空白图片（画布的底）。
 *   - Canvas：画笔操作台，往 Bitmap 上画线、画点、写字。
 *   - Paint：画笔，设置颜色、字号、粗细等。
 *   - 随机干扰线/点 + 字符随机旋转 = 让验证码不易被机器识别。
 *   - 单例 getInstance：全 App 共用一个，方便「生成时记答案、校验时取答案」。
 *
 * 提示：在 IDE 里搜索「验证码」可看相关用法（登录/注册/忘记密码页）。
 * ============================================================ */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.Random;

public class CaptchaUtils {

    // 验证码可能用到的字符（特意去掉了 O、o、l、1 等易混淆字符）
    private static final char[] CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };

    private static CaptchaUtils instance;   // 单例对象
    private String code;                    // 当前这张验证码的正确答案

    // 获取单例
    public static CaptchaUtils getInstance() {
        if (instance == null) {
            instance = new CaptchaUtils();
        }
        return instance;
    }

    // 取出当前验证码答案（页面拿它和用户输入比对）
    public String getCode() {
        return code;
    }

    // 生成一张验证码图片，并把答案记到 code 里。返回这张 Bitmap 显示到界面。
    public Bitmap createBitmap() {
        int width = 120;     // 图片宽
        int height = 50;     // 图片高
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#F0F0F0"));

        Paint paint = new Paint();
        paint.setTextSize(30);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);

        Random random = new Random();
        StringBuilder sb = new StringBuilder();   // 用来拼出 4 位答案

        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            paint.setColor(getRandomColor());
            paint.setStrokeWidth(1.5f);
            canvas.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height), paint);
        }

        // 绘制干扰点
        for (int i = 0; i < 50; i++) {
            paint.setColor(getRandomColor());
            canvas.drawPoint(random.nextInt(width), random.nextInt(height), paint);
        }

        // 生成验证码并绘制
        for (int i = 0; i < 4; i++) {
            char c = CHARS[random.nextInt(CHARS.length)];
            sb.append(c);
            paint.setColor(getRandomColor());
            // 随机旋转
            canvas.save();
            canvas.rotate(random.nextInt(20) - 10, 15 + i * 25, 35);
            canvas.drawText(String.valueOf(c), 15 + i * 25, 35, paint);
            canvas.restore();
        }

        code = sb.toString();    // 记住这 4 个字符就是正确答案
        return bitmap;
    }

    // 生成一个随机的深色（rgb 都<150，保证字在浅灰底上看得清）
    private int getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(150);
        int g = random.nextInt(150);
        int b = random.nextInt(150);
        return Color.rgb(r, g, b);
    }
}
