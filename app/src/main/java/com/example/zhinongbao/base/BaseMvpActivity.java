package com.example.zhinongbao.base;

/* ============================================================
 * 【MVP 基础设施 / Base】所有「页面(Activity)」的爸爸类（基类）
 * ============================================================
 *
 * 说明：
 *   - abstract（抽象类）：自己不能直接用，专门给别人继承。
 *   - 泛型 <P extends BasePresenter>：P 是「这个页面对应的 Presenter 类型」。
 *   - AppCompatActivity：安卓官方的 Activity 基类，提供兼容性支持。
 *
 * 提示：在 IDE 里搜索「MVP 基础」可看本组基础类。
 * ============================================================ */

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// 所有 MVP 页面的基类：泛型 P 就是该页面的 Presenter 类型
public abstract class BaseMvpActivity<P extends BasePresenter> extends AppCompatActivity implements BaseView<P> {
    protected P presenter;   // 业务大脑：子类页面里随时可用 presenter.xxx() 调业务

    // 保存 Presenter（一般由 Presenter 的构造方法回调过来）
    @Override
    public void setPresenter(P presenter) {
        this.presenter = presenter;
    }

    // onCreate：页面创建入口。这里只调用父类，具体初始化由各子类页面自己写。
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
