package com.example.zhinongbao.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.zhinongbao.base.BaseMvpFragment;
import com.example.zhinongbao.R;
import com.example.zhinongbao.mvp.agritech.AgriTechContract;
import com.example.zhinongbao.mvp.agritech.AgriTechPresenter;

/**
 * ============================================================
 * 【农技学堂 / AgriTech】View（Fragment）
 * 整体逻辑：onViewCreated 里拿到 WebView 并开启 JavaScript、DOM 存储、
 *   自适应宽度，然后创建 Presenter 并调用 start()；当 Presenter 回调
 *   loadAgriTechPage(url) 时，用 webView.loadUrl(url) 把网页加载进来。
 * 数据来源：不经过数据库，网页内容来自本地资产文件
 *   assets/agritech.html（离线 HTML5，内置示例新闻数据）。
 * 配合的文件：AgriTechContract（接口约定）、AgriTechPresenter（决定加载哪个地址）、
 *   布局 fragment_agritech.xml、网页 assets/agritech.html。
 * 在 MVP 中的位置：View 层，只负责展示，不含业务逻辑。
 * 提示：在 IDE 里搜索「农技学堂」可看本组相关文件。
 * ============================================================
 */
public class AgriTechFragment extends BaseMvpFragment<AgriTechContract.Presenter> implements AgriTechContract.View {

    private WebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agritech, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = view.findViewById(R.id.webView);

        // WebView 配置：开启 JS、DOM 存储，并让网页自适应屏幕宽度
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);      // 允许网页执行 JavaScript（点击、切换详情等）
        webSettings.setDomStorageEnabled(true);      // 开启 DOM 本地存储（部分网页需要）
        webSettings.setUseWideViewPort(true);        // 使用网页自带的 viewport 宽度
        webSettings.setLoadWithOverviewMode(true);   // 缩放到屏幕宽度显示，避免横向滚动

        // 让链接在应用内的 WebView 打开，而不是跳到系统浏览器
        webView.setWebViewClient(new WebViewClient());
        // 创建 Presenter 并启动：由它决定加载哪个网页地址
        new AgriTechPresenter(this).start();
    }

    // Presenter 回调：收到网页地址后加载显示（这里传入的是本地 assets 网页）
    @Override
    public void loadAgriTechPage(String url) {
        webView.loadUrl(url);
    }
}
