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

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient());
        new AgriTechPresenter(this).start();
    }

    @Override
    public void loadAgriTechPage(String url) {
        webView.loadUrl(url);
    }
}
