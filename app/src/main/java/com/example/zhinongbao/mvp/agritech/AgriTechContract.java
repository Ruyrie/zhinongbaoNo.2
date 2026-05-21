package com.example.zhinongbao.mvp.agritech;

import com.example.zhinongbao.base.BasePresenter;
import com.example.zhinongbao.base.BaseView;

public interface AgriTechContract {
    interface View extends BaseView<Presenter> {
        void loadAgriTechPage(String url);
    }

    interface Presenter extends BasePresenter {
    }
}
