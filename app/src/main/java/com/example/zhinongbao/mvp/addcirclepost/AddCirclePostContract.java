package com.example.zhinongbao.mvp.addcirclepost;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface AddCirclePostContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        void submit(String content, String imageUris);
    }
}
