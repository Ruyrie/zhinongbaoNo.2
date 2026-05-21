package com.example.zhinongbao.mvp.agritech;

public class AgriTechPresenter implements AgriTechContract.Presenter {
    private final AgriTechContract.View view;

    public AgriTechPresenter(AgriTechContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        view.loadAgriTechPage("file:///android_asset/agritech.html");
    }
}
