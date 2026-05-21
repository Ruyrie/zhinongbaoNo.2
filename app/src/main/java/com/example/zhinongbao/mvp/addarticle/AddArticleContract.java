package com.example.zhinongbao.mvp.addarticle;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface AddArticleContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void submit(String title, String content, String coverUri, String category);
    }
}
