package com.example.zhinongbao.mvp.addproductcomment;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface AddProductCommentContract {
    interface View extends BaseView<Presenter> {
        void showToast(String message);
        void closePage();
    }

    interface Presenter extends BasePresenter {
        boolean canComment();
        void submit(String content, String images);
    }
}
